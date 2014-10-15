package com.jobscience.search.searchconfig;

import static com.britesnow.snow.util.MapUtil.mapIt;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.DaoRwHelper;
import com.jobscience.search.dao.OrgConfigDao;

@Singleton
public class SearchConfigurationManager {

    @Inject
    private CurrentRequestContextHolder currentRequestContextHolder;
    @Inject
    private DaoRwHelper daoRwHelper;
    @Inject
    private OrgConfigDao orgConfigDao;
    
    private volatile Document sysDocument;
    
    private volatile LoadingCache<String, SearchConfiguration> searchuiconfigCache;
    private static String[] systemIncludeColumn = {"skill", "education", "company", "location"};
    private volatile ConcurrentMap<String,Integer> customFieldsSize = new ConcurrentHashMap<String, Integer>();
    private String[] notAllowCustomIndexColumnName = {"ts2__text_resume__c"};
    
    private static String[] supportCustomFieldsTypes = {"Date", "String", "Number", "Boolean"};
    
    public SearchConfigurationManager() {
        searchuiconfigCache = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build(new CacheLoader<String, SearchConfiguration>() {
            @Override
            public SearchConfiguration load(String orgName){
                return loadSearchConfiguration(orgName);
            }
        });
    }
    
    public SearchConfiguration getSearchConfiguration(String orgName){
        try {
            return searchuiconfigCache.get(orgName);
        } catch (ExecutionException e) {
        }
        return null;
    }
    
    public void updateCache(String orgName){
        if(orgName == null){
            searchuiconfigCache.invalidateAll();
        }else{
            searchuiconfigCache.invalidate(orgName);
        }
    }
    
    public Integer getcustomFieldsSize(String orgName){
    	if(Strings.isNullOrEmpty(orgName)){
    		return customFieldsSize.get("sys");
    	}else{
    		return customFieldsSize.get(orgName);
    	}
    }
    
    public void resetcustomFieldsSize(String orgName){
    	if(Strings.isNullOrEmpty(orgName)){
        	customFieldsSize.put("sys", 0);
        }else{
        	customFieldsSize.put(orgName, 0);
        }
    }
    
    public List<Map> getFilters(String orgName){
        List<Map> filters = new ArrayList<Map>();
        SearchConfiguration sc = getSearchConfiguration(orgName);
         filters.add(mapIt(          "name",   "contact",
                                    "title",   sc.getContact().getTitle(),
                                   "native",   true,
                                     "show",   true,
                                     "type",   "contact"));


        for(Filter f : sc.getSysColumnFilters()){
                if(!f.isDelete()){
                    Map m = mapIt(      "name",   f.getName(),
                                       "title",   f.getTitle(),
                                      "native",   (f.getType()!=null),
                                        "show",   f.isNeedShow(),
                                        "all-any",   f.isAll_any(),
                                        "orderable",   f.isOrderable(),
                    					"bg_color",   f.getBg_color());
                    if(f.getType() == null){
                        m.put("paramName",   f.getFilterField().getColumn());
                        m.put("type", "custom");
                    }else{
                        m.put("type",   f.getFilterType().value());
                    }
                    filters.add(m);
                }
        }
        return filters;
    }
    
    public String getMergedNodeContent(String orgName) throws Exception {
        Node node = getMergedNode(orgName);
        StringWriter writer = new StringWriter();
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(node), new StreamResult(writer));
        return writer.toString();
    }
    
    public String getOrgConfig(String orgName){
        int orgId = -1;
        List<Map> orgs = orgConfigDao.getOrgByName(orgName);
        if(orgs.size() > 0){
            orgId = Integer.parseInt( orgs.get(0).get("id").toString());
        }
        List<Map> orgConfig = daoRwHelper.executeQuery(daoRwHelper.newSysRunner(),
            "select val_text from config where name = ? and org_id =?", "searchconfig",orgId);
        if(orgConfig.size() > 0){
           return orgConfig.get(0).get("val_text").toString();
        }else{
            return "<searchconfig></searchconfig>";
        }
    }

    protected SearchConfiguration loadSearchConfiguration(String orgName){
        try{
            JAXBContext jc = JAXBContext.newInstance(SearchConfiguration.class);
            Unmarshaller ums =  jc.createUnmarshaller();
            return (SearchConfiguration) ums.unmarshal(getMergedNode(orgName));
        }catch(Exception e){
            e.printStackTrace();
        }
        return new SearchConfiguration();
    }
    
    private Document getSysDocument() throws Exception{
        if(sysDocument == null){
            DocumentBuilder db  = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            StringBuilder path = new StringBuilder(currentRequestContextHolder.getCurrentRequestContext().getServletContext().getRealPath("/"));
            sysDocument =  db.parse(path+"/WEB-INF/config/sys/searchconfig.val");
        }
        return sysDocument;
    }
    
    private  Node getMergedNode(String orgName) throws Exception {
        DocumentBuilder db  = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        //get the sys config
        List<Map> sysConfig = daoRwHelper.executeQuery(daoRwHelper.newSysRunner(),
            "select val_text from config where name = ? and org_id is null", "searchconfig");
        Document sys = null;
        if (sysConfig.size() == 0) {
            sys = getSysDocument();
        }else{
            ByteArrayInputStream in = new ByteArrayInputStream(sysConfig.get(0).get("val_text").toString().getBytes());
            sys = db.parse(in);
        }
        
        //get the org config
        int orgId = -1;
        List<Map> orgs = orgConfigDao.getOrgByName(orgName);
        if(orgs.size() > 0){
            orgId = Integer.parseInt( orgs.get(0).get("id").toString());
        }
        Document org ;
        List<Map> orgConfig = daoRwHelper.executeQuery(daoRwHelper.newSysRunner(),
            "select val_text from config where name = ? and org_id =?", "searchconfig",orgId);
        if(orgConfig.size() > 0){
            ByteArrayInputStream in = new ByteArrayInputStream(orgConfig.get(0).get("val_text").toString().getBytes());
            org = db.parse(in);
        }else{
            org = db.newDocument();
        }
        
        Document result = db.newDocument();
        Element e =  result.createElement("searchconfig");

        //handle filter
        NodeList sysNodes = sys.getElementsByTagName("filter");
        NodeList orgNodes = org.getElementsByTagName("filter");
        Map<String,Node> sysNodeMap = new HashMap<String, Node>();
        List<Node> sysNodesList = new ArrayList<Node>();
        List<Node> nodesList = new ArrayList<Node>();
        for(int current = 0, length=sysNodes.getLength(); current < length; current++){
            Node n = sysNodes.item(current);
            sysNodeMap.put(n.getAttributes().getNamedItem("name").getNodeValue(), n);
            sysNodesList.add(n);
        }
        for(int current = 0, length=orgNodes.getLength(); current < length; current++){
            Node n = orgNodes.item(current);
            NamedNodeMap nameMap = n.getAttributes();
            String name = n.getAttributes().getNamedItem("name").getNodeValue();
            if(sysNodeMap.containsKey(name) && checkAttribute(n,"display") && "column".equals(n.getAttributes().getNamedItem("display").getNodeValue())){
                boolean delete = false;
                if(nameMap.getNamedItem("delete") != null && "true".equals(nameMap.getNamedItem("delete").getNodeValue())){
                	delete = true;
                }
                Node sysNode = sysNodeMap.get(name);
                NamedNodeMap sysNodeNameMap =  sysNode.getAttributes();
                sysNodesList.remove(sysNode);
                sysNodeMap.remove(name);
                
                if(!delete){
                    if(nameMap.getNamedItem("title") == null && sysNodeNameMap.getNamedItem("title") != null){
                        ((Element)n).setAttribute("title", sysNodeNameMap.getNamedItem("title").getNodeValue());
                    }
                    if(nameMap.getNamedItem("type") == null && sysNodeNameMap.getNamedItem("type") != null){
                        ((Element)n).setAttribute("type", sysNodeNameMap.getNamedItem("type").getNodeValue());
                    }
                    if(nameMap.getNamedItem("show") == null && sysNodeNameMap.getNamedItem("show") != null){
                        ((Element)n).setAttribute("show", sysNodeNameMap.getNamedItem("show").getNodeValue());
                    }
                    if(nameMap.getNamedItem("display") == null && sysNodeNameMap.getNamedItem("display") != null){
                        ((Element)n).setAttribute("display", sysNodeNameMap.getNamedItem("display").getNodeValue());
                    }
                    if(nameMap.getNamedItem("bg-color") == null && sysNodeNameMap.getNamedItem("bg-color") != null){
                        ((Element)n).setAttribute("bg-color", sysNodeNameMap.getNamedItem("bg-color").getNodeValue());
                    }
                    NodeList filterNodes = n.getChildNodes();
                    boolean hasField = false; 
                    for(int c = 0, l = filterNodes.getLength(); c < l; c++){
                        if(filterNodes.item(c).getNodeType() == 1){
                            hasField = true;
                        }
                    }
                    
                    if(!hasField){
                        NodeList sysFilterNodes = sysNode.getChildNodes();
                        for(int c = 0, l = sysFilterNodes.getLength(); c < l; c++){
                            if(sysFilterNodes.item(c).getNodeType()==1){
                                n.appendChild(org.importNode(sysFilterNodes.item(c),true));
                            }
                        }
                    }
                    nodesList.add(n);
                }
            }else{
                nodesList.add(n);
            }
        }
        
        nodesList.addAll(sysNodesList);
        for(Node n : nodesList){
            e.appendChild(result.importNode(n, true));
        }
        
        //handle keyword
        NodeList sysKeywords = sys.getElementsByTagName("keyword");
        NodeList orgKeywords  = org.getElementsByTagName("keyword");
        Map<String,Node> keywordMap = new HashMap<String,Node>();
        
        for(int current = 0, length = sysKeywords.getLength(); current < length; current++){
            Node n = sysKeywords.item(current);
            NodeList keywordFields = n.getChildNodes();
            for(int c = 0, l = keywordFields.getLength(); c < l; c++){
                Node keywordField = keywordFields.item(c);
                if(keywordField.getNodeType()==1){
                    NamedNodeMap field =  keywordField.getAttributes();
                    keywordMap.put(field.getNamedItem("name").getNodeValue(), keywordField);
                }
            }
        }
        
        for(int current = 0, length = orgKeywords.getLength(); current < length; current++){
            Node n = orgKeywords.item(current);
            NodeList keywordFields = n.getChildNodes();
            for(int c = 0,l=keywordFields.getLength();c<l;c++){
                Node keywordField = keywordFields.item(c);
                if(keywordField.getNodeType()==1){
                    NamedNodeMap nameMap =  keywordField.getAttributes();
                    String name = nameMap.getNamedItem("name").getNodeValue();
                    boolean delete = false;
                    if(nameMap.getNamedItem("delete") != null && "true".equals(nameMap.getNamedItem("delete").getNodeValue())){
                    	delete = true;
                    }
                    if(delete){
                        keywordMap.remove(name);
                        continue;
                    }
                    if(keywordMap.containsKey(name)){
                        NamedNodeMap sysNodeNameMap = keywordMap.get(name).getAttributes();
                        if(nameMap.getNamedItem("column") == null && sysNodeNameMap.getNamedItem("column") != null){
                            ((Element)keywordField).setAttribute("column", sysNodeNameMap.getNamedItem("column").getNodeValue());
                        }
                        if(nameMap.getNamedItem("table") == null && sysNodeNameMap.getNamedItem("table") != null){
                            ((Element)keywordField).setAttribute("table", sysNodeNameMap.getNamedItem("table").getNodeValue());
                        }
                    }
                    keywordMap.put(name, keywordField);
                }
            }
        }
        
        Element keyword =  result.createElement("keyword");
        for(Node n:keywordMap.values()){
            keyword.appendChild(result.importNode(n, true));
        }
        e.appendChild(keyword);
        
        //handle contact
        NodeList sysContacts = sys.getElementsByTagName("contact");
        NodeList orgContacts = org.getElementsByTagName("contact");
        Map<String,Node> contactMap = new HashMap<String,Node>();
        String contactTable="",contactTitle = "";
        for(int current = 0, length=sysContacts.getLength(); current < length; current++){
            Node n = sysContacts.item(current);
            contactTable = n.getAttributes().getNamedItem("table").getNodeValue();
            contactTitle = n.getAttributes().getNamedItem("title").getNodeValue();
            NodeList contactFields = n.getChildNodes();
            for(int c = 0, l = contactFields.getLength(); c < l; c++){
                Node contactField = contactFields.item(c);
                if(contactField.getNodeType()==1){
                    NamedNodeMap field =  contactField.getAttributes();
                    contactMap.put(field.getNamedItem("name").getNodeValue(), contactField);
                }
            }
        }
        for(int current = 0, length = orgContacts.getLength(); current < length; current++){
            Node n = orgContacts.item(current);
            contactTable = n.getAttributes().getNamedItem("table").getNodeValue();
            contactTitle = n.getAttributes().getNamedItem("title").getNodeValue();
            NodeList contactFields = n.getChildNodes();
            for(int c = 0, l = contactFields.getLength(); c < l; c++){
                Node contactField = contactFields.item(c);
                if(contactField.getNodeType()==1){
                    NamedNodeMap field =  contactField.getAttributes();
                    contactMap.put(field.getNamedItem("name").getNodeValue(), contactField);
                }
            }
        }
        
        Element contact = result.createElement("contact");
        contact.setAttribute("table", contactTable);
        contact.setAttribute("title", contactTitle);
        for(Node n:contactMap.values()){
            contact.appendChild(result.importNode(n, true));
        }
        e.appendChild(contact);

        StringWriter sw = new StringWriter();
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.transform(new DOMSource(e), new StreamResult(sw));

        return e;
    }
    
    public String getErrorMsg(String content,boolean isOrg,String orgName){
        String errorMsg = "";
        boolean hasError = false;
        if(content!=null){
            try{
                DocumentBuilder db  = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = db.parse(new ByteArrayInputStream(content.getBytes()));
                if(!isOrg){
                    NodeList sysKeywords = document.getElementsByTagName("keyword");
                    //handle for keywords
                    if(sysKeywords.getLength()==0){
                        errorMsg = "Missing keyword element.";
                        hasError = true;
                    }else{
                        NodeList keywordFields = sysKeywords.item(0).getChildNodes();
                        boolean contactTsv = false,resumeTsv = false;
                        for(int i = 0, j = keywordFields.getLength(); i < j; i++){
                            Node keywordField = keywordFields.item(i);
                            if(keywordField.getNodeType() == 1 && "field".equals(keywordField.getNodeName())){
                               String val = keywordField.getAttributes().getNamedItem("name").getNodeValue();
                               if(!checkAttribute(keywordField, "column")){
                                   errorMsg = "Missing column attribute for keyword field";
                               }
                               if(!checkAttribute(keywordField, "table")){
                                   errorMsg = "Missing table attribute for keyword field";
                               }
                               if("contactResumeTsv".equals(val)){
                                   resumeTsv = true;
                               }
                            }
                        }
                        if(!contactTsv && !resumeTsv){
                            errorMsg="Missing contactInfoTsv and contactResumeTsv keyword field.";
                        }else if(!resumeTsv){
                            errorMsg="Missing contactResumeTsv keyword field.";
                        }
                    }
                    //handle for Systemfilter
                    if(!hasError){
                        String checkSystemFielterInfo = checkSearchConfigSystemFilter(document,orgName);
                        if(checkSystemFielterInfo != null){
                        	errorMsg = checkSystemFielterInfo;
                        	hasError = true;
                        }
                    }
                    
                    //handle for customFielter
                    if(!hasError){
                        String checkCustomFielterInfo = checkSearchConfigCustomFilter(document,orgName);
                        if(checkCustomFielterInfo != null){
                        	errorMsg = checkCustomFielterInfo;
                        	hasError = true;
                        }
                    }
                     
                    //handle for customFields
                    if(!hasError){
                        String checkCustomFieldsInfo = checkSearchConfigCustomFields(document,orgName);
                        if(checkCustomFieldsInfo != null){
                        	errorMsg = checkCustomFieldsInfo;
                        	hasError = true;
                        }
                    }
                     
                    //handle for contact 
                    if(!hasError){
                        String checkContactInfo = checkSearchConfigContact(document,orgName);
                        if(checkContactInfo != null){
                        	errorMsg = checkContactInfo;
                        	hasError = true;
                        }
                    }
                    
              }else{
                  //handle for customFielter
                  if(!hasError){
                      String checkCustomFielterInfo = checkSearchConfigCustomFilter(document,orgName);
                      if(checkCustomFielterInfo != null){
                      	errorMsg = checkCustomFielterInfo;
                      	hasError = true;
                      }
                  }
                   
                  //handle for customFields
                  if(!hasError){
                      String checkCustomFieldsInfo = checkSearchConfigCustomFields(document,orgName);
                      if(checkCustomFieldsInfo != null){
                      	errorMsg = checkCustomFieldsInfo;
                      	hasError = true;
                      }
                  }
              }
            }catch(Exception e){
                errorMsg = "The search config xml has grammer issues.";
            }
        }else{
            errorMsg = "The search config xml has grammer issues.";
        }
        return errorMsg;
    }
    
    private String checkSearchConfigSystemFilter(Document document, String orgName) {
    	String errorMsg = null;
        boolean hasError = false;
    	NodeList filterNodes = document.getElementsByTagName("filter");
        boolean skillFilter = false,educationFilter = false,locationFilter = false,companyFilter = false;
        for(int i = 0, j = filterNodes.getLength(); i < j; i++){
            Node filterField = filterNodes.item(i);
            if(filterField.getNodeType() == 1 && checkAttribute(filterField, "name") && checkAttribute(filterField, "display")){
            	String name = filterField.getAttributes().getNamedItem("name").getNodeValue();
            	String display = filterField.getAttributes().getNamedItem("display").getNodeValue();
            	if("side".equalsIgnoreCase(display) || ("column".equalsIgnoreCase(display) && !Arrays.asList(systemIncludeColumn).contains(name))){
            		continue;
            	}
            	if(checkAttribute(filterField, "type")){
            		 String val = filterField.getAttributes().getNamedItem("type").getNodeValue();
                     if("skill".equals(val)){
                         skillFilter = true;
                     }
                     if("education".equals(val)){
                         educationFilter = true;
                     }
                     if("location".equals(val)){
                         locationFilter = true;
                     }
                     if("company".equals(val)){
                         companyFilter = true;
                     }
                     for(int m = 0, n = filterField.getChildNodes().getLength(); m < n; m++){
                         Node fieldNode = filterField.getChildNodes().item(m);
                         if(fieldNode.getNodeType() == 1){
                             if(!checkAttribute(fieldNode,"table")){
                                 errorMsg = "Missing table attribute for "+val+" filter field";
                                 hasError = true;
                             }
                             if(!checkAttribute(fieldNode,"column")){
                                 errorMsg = "Missing column attribute for "+val+" filter field";
                                 hasError = true;
                             }
                             if(!"location".equals(val)){
                                 if(!checkAttribute(fieldNode,"joinfrom")){
                                     errorMsg = "Missing joinfrom attribute for "+val+" filter field";
                                     hasError = true;
                                 }
                                 if(!checkAttribute(fieldNode,"jointo")){
                                     errorMsg = "Missing jointo attribute for "+val+" filter field";
                                     hasError = true;
                                 }
                             }
                             if("skill".equals(val)){
                                 if(!checkAttribute(fieldNode,"slider")){
                                     errorMsg = "Missing slider attribute for "+val+" filter field";
                                     hasError = true;
                                 }
                             }
                         }
                     }
            	}
            } else {
            	errorMsg="Missing display attribute filter field";
            }
            if(hasError){
            	break;
            }
        }

        if(!hasError){
        	StringBuilder sb = new StringBuilder();
            if(!skillFilter){
                sb.append("skill,");
            }
            if(!companyFilter){
                sb.append("company,");
            }
            if(!locationFilter){
                sb.append("location,");
            }
            if(!educationFilter){
                sb.append("education,");
            }
            if(sb.length()>0){
                errorMsg = "Missing filters : "+sb.deleteCharAt(sb.length()-1).toString();
            }
        }
        return errorMsg;
    }
    
    private String checkSearchConfigContact(Document document, String orgName) {
    	String errorMsg = null;
        boolean hasError = false;
    	NodeList contactFiler = document.getElementsByTagName("contact");
    	if(checkAttribute(contactFiler.item(0), "table")){
    		if(!"contact".equals(getVal(contactFiler.item(0), "table"))){
    			return "Mistake table value for contact";
    		}
    	}else{
    		return "Missing table attribute for contact";
    	}
        if(contactFiler.getLength()==0){
            errorMsg = "Missing contact element.";
            hasError = true;
        }else{
            NodeList contactFields = contactFiler.item(0).getChildNodes();
            Map columns = mapIt("id",1,"email",1,"name",1,"sfid",1,
                    "title",1,"createddate",1,"resume",1,"mailingpostalcode",1);
            for(int i = 0, j = contactFields.getLength(); i < j; i++){
                Node contactField = contactFields.item(i);
                if(contactField.getNodeType() == 1){
                    if(!checkAttribute(contactField, "column")){
                        errorMsg = "Missing column attribute for contact field";
                        hasError = true;
                    }
                    if("id".equals(getVal(contactField, "name"))){
                        columns.remove("id");
                    }
                    if("email".equals(getVal(contactField, "name"))){
                        columns.remove("email");
                    }
                    if("name".equals(getVal(contactField, "name"))){
                        columns.remove("name");
                    }
                    if("sfid".equals(getVal(contactField, "name"))){
                        columns.remove("sfid");
                    }
                    if("title".equals(getVal(contactField, "name"))){
                        columns.remove("title");
                    }
                    if("createddate".equals(getVal(contactField, "name"))){
                        columns.remove("createddate");
                    }
                    if("resume".equals(getVal(contactField, "name"))){
                        columns.remove("resume");
                    }
                    if("mailingpostalcode".equals(getVal(contactField, "name"))){
                        columns.remove("mailingpostalcode");
                    }
                }
                if(hasError){
                	break;
                }
            }
            if(!hasError){
            	if(columns.keySet().size() > 0){
                    errorMsg="Missing contact fields: ";
                    for(Object s:columns.keySet()){
                        errorMsg+=s+",";
                    }
                    errorMsg = errorMsg.substring(0,errorMsg.length()-1);
                }
            }
        }
        return errorMsg;
    }
    
    private String checkSearchConfigCustomFilter(Document document, String orgName) {
    	String errorMsg = null;
        boolean hasError = false;
    	NodeList filterNodes = document.getElementsByTagName("filter");
    	for(int i = 0, j = filterNodes.getLength(); i < j; i++){
            Node filterNode = filterNodes.item(i);
            String filterName = "";
            if(filterNode.getNodeType() == 1 && checkAttribute(filterNode, "name") && checkAttribute(filterNode, "display")){
            	String name = filterNode.getAttributes().getNamedItem("name").getNodeValue();
            	String display = filterNode.getAttributes().getNamedItem("display").getNodeValue();
            	if("side".equalsIgnoreCase(display) || ("column".equals(display) && Arrays.asList(systemIncludeColumn).contains(name))){
            		continue;
            	}	
            	if("column".equals(display)){
            		 if(!checkAttribute(filterNode,"name")){
                         errorMsg="Missing name attribute for filter field";
                         hasError = true;
                         break;
                     }else {
                    	 filterName = filterNode.getAttributes().getNamedItem("name").getNodeValue();
                     }
                     if(!hasError && !checkAttribute(filterNode,"title")){
                         errorMsg="Missing title attribute for " + filterName + " filter field";
                         hasError = true;
                     }
                     if(!hasError && !checkAttribute(filterNode,"type")){
                         errorMsg="Missing type attribute for " + filterName + " filter field";
                         hasError = true;
                     }
                     if(!hasError && !"string".equalsIgnoreCase(getVal(filterNode, "type"))){
                         errorMsg = "Not support type \"" + getVal(filterNode, "type") + "\"";
                         hasError = true;
                     }
                     if(!hasError){
                		 for(int m = 0, n = filterNode.getChildNodes().getLength(); m < n; m++){
                             Node fieldNode = filterNode.getChildNodes().item(m);
                             if(fieldNode.getNodeType() == 1){
                                 if(!hasError && !checkAttribute(fieldNode,"table")){
                                     errorMsg="Missing table attribute for "+filterName+" filter field";
                                     hasError = true;
                                 }else if(!hasError && !checkTableExist(orgName, getVal(fieldNode, "table"))){
                                	 errorMsg="Table not exist for "+filterName+" filter field";
                                     hasError = true;
                                 }
                                 if(!hasError && !checkAttribute(fieldNode,"column")){
                                     errorMsg="Missing column attribute for "+filterName+" filter field";
                                     hasError = true;
                                 }else if(!hasError && !checkTableColumn(orgName, getVal(fieldNode, "table"), getVal(fieldNode, "column"))){
                                	 errorMsg="Column not exist for "+filterName+" filter field";
                                     hasError = true;
                                 }
                                 if(!hasError && !"contact".equals(getVal(fieldNode, "table"))){
                                 	if(!hasError && !checkAttribute(fieldNode,"joinfrom")){
                                         errorMsg="Missing joinfrom attribute for "+filterName+" filter field";
                                         hasError = true;
                                     }else if(!hasError && !checkTableColumn(orgName, getVal(fieldNode, "table"), getVal(fieldNode, "joinfrom"))){
                                    	 errorMsg="Joinfrom column not exist for "+filterName+" filter field";
                                         hasError = true;
                                     }
                                 	if(!hasError && !checkAttribute(fieldNode,"jointo")){
                                         errorMsg="Missing jointo attribute for "+filterName+" filter field";
                                         hasError = true;
                                     }else if(!hasError && !checkTableColumn(orgName, "contact", getVal(fieldNode, "jointo"))){
                                    	 errorMsg="Jointo column not exist fo "+filterName+" filter field";
                                         hasError = true;
                                     }
                                 }
                             }
                             if(hasError) {
                             	break;
                             }
                         }
                     }
            	}
            }else {
            	errorMsg="Missing display attribute for filter field";
                hasError = true;
            }
            if(hasError) {
            	break;
            }
    	}
    	return errorMsg;
    }
  
    private String checkSearchConfigCustomFields(Document document, String orgName) {
    	String errorMsg = null;
        boolean hasError = false;
    	NodeList filterNodes = document.getElementsByTagName("filter");
    	int customFieldSize = 0;
    	for(int i = 0, j = filterNodes.getLength(); i < j; i++){
            Node filterNode = filterNodes.item(i);
            String filterName = "";
            if(filterNode.getNodeType() == 1 && checkAttribute(filterNode, "display")){
            	if("side".equals(filterNode.getAttributes().getNamedItem("display").getNodeValue())){
            		 if(!hasError && !checkAttribute(filterNode,"name")){
                         errorMsg="Missing name attribute for filter field";
                         hasError = true;
                         break;
                     }else {
                    	 filterName = filterNode.getAttributes().getNamedItem("name").getNodeValue();
                     }
                     if(!hasError && !checkAttribute(filterNode,"title")){
                         errorMsg="Missing title attribute for " + filterName + " filter field";
                         hasError = true;
                     }
                     if(!hasError && !checkAttribute(filterNode,"type")){
                         errorMsg="Missing type attribute for " + filterName + " filter field";
                         hasError = true;
                     }
                     if(!hasError && !isSupportType(getVal(filterNode, "type"))){
                         errorMsg = "Not support type \"" + getVal(filterNode, "type") + "\"";
                         hasError = true;
                     }
                     if(!hasError){
                    	 for(int m = 0, n = filterNode.getChildNodes().getLength(); m < n; m++){
                             Node fieldNode = filterNode.getChildNodes().item(m);
                             if(fieldNode.getNodeType() == 1){
                                 if(!hasError && !checkAttribute(fieldNode,"table")){
                                     errorMsg="Missing table attribute for "+filterName+" filter field";
                                     hasError = true;
                                 }else if(!hasError && !checkTableExist(orgName, getVal(fieldNode, "table"))){
                                	 errorMsg="Table not exist for "+filterName+" filter field";
                                     hasError = true;
                                 }
                                 if(!hasError && !checkAttribute(fieldNode,"column")){
                                     errorMsg="Missing column attribute for "+filterName+" filter field";
                                     hasError = true;
                                 }else if(!hasError && !checkTableColumn(orgName, getVal(fieldNode, "table"), getVal(fieldNode, "column"))){
                                	 errorMsg="Column not exist for "+filterName+" filter field";
                                     hasError = true;
                                 }
                                 if(!hasError && "contact".equals(getVal(fieldNode, "table"))){
                                     if(!checkIfAllowCustomFieldsColumn(getVal(fieldNode, "column"))){
                                         errorMsg = "Not allow config for the column of " + getVal(fieldNode, "columnName");
                                         hasError = true;
                                     }else if(!checkTableColumn(orgName, "contact", getVal(fieldNode, "column"))){
                                      	 errorMsg="Joinfrom column not exist for "+filterName+" filter field";
                                         hasError = true;
                                     }
                                  }
                                 if(!hasError && !"contact".equals(getVal(fieldNode, "table"))){
                                   	if(!checkAttribute(fieldNode, "joinfrom")){
                                           errorMsg="Missing joinfrom attribute for "+filterName+" filter field";
                                           hasError = true;
                                       }else if(!checkTableColumn(orgName, getVal(fieldNode, "table"), getVal(fieldNode, "joinfrom"))){
                                      	 errorMsg="Joinfrom column not exist for "+filterName+" filter field";
                                           hasError = true;
                                       }
                                   	if(!checkAttribute(fieldNode, "jointo")){
                                           errorMsg="Missing jointo attribute for "+filterName+" filter field";
                                           hasError = true;
                                       }else if(!checkTableColumn(orgName, "contact", getVal(fieldNode, "jointo"))){
                                      	 errorMsg="Jointo column not exist fo "+filterName+" filter field";
                                           hasError = true;
                                       }
                                   }
                             }
                         }
                     }
            		if(!hasError){
            			customFieldSize++;
            		}
            	}
            }else {
            	errorMsg="Missing display attribute for filter field";
                hasError = true;
            }
            if(hasError) {
            	break;
            }
    	}
    	if(!hasError){
            if(Strings.isNullOrEmpty(orgName)){
            	customFieldsSize.put("sys", customFieldSize);
            }else{
            	customFieldsSize.put(orgName, customFieldSize);
            }
        }
    	return errorMsg;
    }
    
    private boolean checkIfAllowCustomFieldsColumn(String columnName){
    	if(Arrays.asList(notAllowCustomIndexColumnName).contains(columnName)){
			return false;
		}
    	return true;
    }
    
    private boolean checkAttribute(Node n, String name){
        if(n.getAttributes().getNamedItem(name) != null){
            return true;
        }
        return false;
    }
    
    private String getVal(Node n, String attributeName){
        if(!checkAttribute(n,attributeName)){
            return null;
        }
        return n.getAttributes().getNamedItem(attributeName).getNodeValue();
    }
    
    private boolean isSupportType(String type){
        boolean isSupport = false;
        for(String s : supportCustomFieldsTypes){
            if(s.equalsIgnoreCase(type.toLowerCase())){
                isSupport = true;
                break;
            }
        }
        return isSupport;
    }
    
    /**
     * check a table if exist
     * 
     * @param columnName
     * @param table
     * @param schemaName
     * @return boolean
     */
    private boolean checkTableExist(String orgName,String table){
    	String sql = "select distinct table_name from information_schema.columns where table_schema=current_schema and"
    	             +" table_name = '"
    	             +table+"';";
    	List<Map> results = daoRwHelper.executeQuery(orgName, sql);
    	if(results.size() == 1){
    		return true;
    	}else{
    		return false;
    	}
    }
    
    /**
     * check a table has a column or not
     * 
     * @param columnName
     * @param table
     * @param schemaName
     * @return boolean
     */
    private boolean checkTableColumn(String orgName, String table, String columnName){
    	String sql = "select distinct column_name from information_schema.columns where table_schema=current_schema and"
    				+ " table_name = '" + table + "'and column_name='" + columnName + "'";
		List<Map> results = daoRwHelper.executeQuery(orgName, sql);
		if(results.size() == 1){
			return true;
		}else{
			return false;
		}
    }
}
