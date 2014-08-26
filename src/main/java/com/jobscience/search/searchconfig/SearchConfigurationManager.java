package com.jobscience.search.searchconfig;

import static com.britesnow.snow.util.MapUtil.mapIt;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
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
import com.jobscience.search.dao.DaoHelper;
import com.jobscience.search.dao.DatasourceManager;
import com.jobscience.search.dao.OrgConfigDao;

@Singleton
public class SearchConfigurationManager {

    @Inject
    private CurrentRequestContextHolder currentRequestContextHolder;
    @Inject
    private DaoHelper daoHelper;
    @Inject
    private OrgConfigDao orgConfigDao;
    @Inject
    private DatasourceManager datasourceManager;
    private volatile Document sysDocument;
    
    private volatile LoadingCache<String, SearchConfiguration> searchuiconfigCache;
    
    private volatile ConcurrentMap<String,Integer> customFieldsSize = new ConcurrentHashMap<String, Integer>();
    
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


        for(Filter f:sc.getFilters()){
                if(!f.isDelete()){
                    Map m = mapIt(      "name",   f.getName(),
                                       "title",   f.getTitle(),
                                      "native",   (f.getFilterType()!=null),
                                        "show",   f.isNeedShow());
                    if(f.getFilterType()==null){
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
        Node node= getMergedNode(orgName);
        StringWriter writer = new StringWriter();
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(node), new StreamResult(writer));
        return writer.toString();
    }
    
    public String getOrgConfig(String orgName){
        int orgId = -1;
        List<Map> orgs = orgConfigDao.getOrgByName(orgName);
        if(orgs.size()>0){
            orgId = Integer.parseInt( orgs.get(0).get("id").toString());
        }
        List<Map> orgConfig = daoHelper.executeQuery(datasourceManager.newSysRunner(),
            "select val_text from config where name = ? and org_id =?", "searchconfig",orgId);
        if(orgConfig.size()>0){
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
        if(sysDocument==null){
            DocumentBuilder db  = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            StringBuilder path = new StringBuilder(currentRequestContextHolder.getCurrentRequestContext().getServletContext().getRealPath("/"));
            sysDocument =  db.parse(path+"/WEB-INF/config/sys/searchconfig.val");
        }
        return sysDocument;
    }
    private  Node getMergedNode(String orgName) throws Exception {
        DocumentBuilder db  = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        //get the sys config
        List<Map> sysConfig = daoHelper.executeQuery(datasourceManager.newSysRunner(),
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
        if(orgs.size()>0){
            orgId = Integer.parseInt( orgs.get(0).get("id").toString());
        }
        Document org ;
        List<Map> orgConfig = daoHelper.executeQuery(datasourceManager.newSysRunner(),
            "select val_text from config where name = ? and org_id =?", "searchconfig",orgId);
        if(orgConfig.size()>0){
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
        for(int current = 0,length=sysNodes.getLength();current<length;current++){
            Node n = sysNodes.item(current);
            sysNodeMap.put(n.getAttributes().getNamedItem("name").getNodeValue(), n);
            sysNodesList.add(n);
        }
        for(int current = 0,length=orgNodes.getLength();current<length;current++){
            Node n = orgNodes.item(current);
            NamedNodeMap nameMap = n.getAttributes();
            String name = n.getAttributes().getNamedItem("name").getNodeValue();
            if(sysNodeMap.containsKey(name)){
                boolean remove = false;
                if(nameMap.getNamedItem("remove")!=null&&"true".equals(nameMap.getNamedItem("remove").getNodeValue())){
                    remove = true;
                }
                Node sysNode = sysNodeMap.get(name);
                NamedNodeMap sysNodeNameMap =  sysNode.getAttributes();
                sysNodesList.remove(sysNode);
                sysNodeMap.remove(name);
                
                if(!remove){
                    if(nameMap.getNamedItem("title")==null&&sysNodeNameMap.getNamedItem("title")!=null){
                        ((Element)n).setAttribute("title", sysNodeNameMap.getNamedItem("title").getNodeValue());
                    }
                    if(nameMap.getNamedItem("filtertype")==null&&sysNodeNameMap.getNamedItem("filtertype")!=null){
                        ((Element)n).setAttribute("filtertype", sysNodeNameMap.getNamedItem("filtertype").getNodeValue());
                    }
                    if(nameMap.getNamedItem("show")==null&&sysNodeNameMap.getNamedItem("show")!=null){
                        ((Element)n).setAttribute("show", sysNodeNameMap.getNamedItem("show").getNodeValue());
                    }

                    NodeList filterNodes = n.getChildNodes();
                    boolean hasField = false; 
                    for(int c = 0,l=filterNodes.getLength();c<l;c++){
                        if(filterNodes.item(c).getNodeType()==1){
                            hasField = true;
                        }
                    }
                    
                    if(!hasField){
                        NodeList sysFilterNodes = sysNode.getChildNodes();
                        for(int c = 0,l=sysFilterNodes.getLength();c<l;c++){
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
        for(Node n:nodesList){
            e.appendChild(result.importNode(n, true));
        }
        
        //handle keyword
        NodeList sysKeywords = sys.getElementsByTagName("keyword");
        NodeList orgKeywords  = org.getElementsByTagName("keyword");
        Map<String,Node> keywordMap = new HashMap<String,Node>();
        
        for(int current = 0,length=sysKeywords.getLength();current<length;current++){
            Node n = sysKeywords.item(current);
            NodeList keywordFields = n.getChildNodes();
            for(int c = 0,l=keywordFields.getLength();c<l;c++){
                Node keywordField = keywordFields.item(c);
                if(keywordField.getNodeType()==1){
                    NamedNodeMap field =  keywordField.getAttributes();
                    keywordMap.put(field.getNamedItem("name").getNodeValue(), keywordField);
                }
            }
        }
        
        for(int current = 0,length=orgKeywords.getLength();current<length;current++){
            Node n = orgKeywords.item(current);
            NodeList keywordFields = n.getChildNodes();
            for(int c = 0,l=keywordFields.getLength();c<l;c++){
                Node keywordField = keywordFields.item(c);
                if(keywordField.getNodeType()==1){
                    NamedNodeMap nameMap =  keywordField.getAttributes();
                    String name = nameMap.getNamedItem("name").getNodeValue();
                    boolean remove = false;
                    if(nameMap.getNamedItem("remove")!=null&&"true".equals(nameMap.getNamedItem("remove").getNodeValue())){
                        remove = true;
                    }
                    if(remove){
                        keywordMap.remove(name);
                        continue;
                    }
                    if(keywordMap.containsKey(name)){
                        NamedNodeMap sysNodeNameMap = keywordMap.get(name).getAttributes();
                        if(nameMap.getNamedItem("column")==null&&sysNodeNameMap.getNamedItem("column")!=null){
                            ((Element)keywordField).setAttribute("column", sysNodeNameMap.getNamedItem("column").getNodeValue());
                        }
                        if(nameMap.getNamedItem("table")==null&&sysNodeNameMap.getNamedItem("table")!=null){
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
        for(int current = 0,length=sysContacts.getLength();current<length;current++){
            Node n = sysContacts.item(current);
            contactTable = n.getAttributes().getNamedItem("table").getNodeValue();
            contactTitle = n.getAttributes().getNamedItem("title").getNodeValue();
            NodeList contactFields = n.getChildNodes();
            for(int c = 0,l=contactFields.getLength();c<l;c++){
                Node contactField = contactFields.item(c);
                if(contactField.getNodeType()==1){
                    NamedNodeMap field =  contactField.getAttributes();
                    contactMap.put(field.getNamedItem("name").getNodeValue(), contactField);
                }
            }
        }
        for(int current = 0,length=orgContacts.getLength();current<length;current++){
            Node n = orgContacts.item(current);
            contactTable = n.getAttributes().getNamedItem("table").getNodeValue();
            contactTitle = n.getAttributes().getNamedItem("title").getNodeValue();
            NodeList contactFields = n.getChildNodes();
            for(int c = 0,l=contactFields.getLength();c<l;c++){
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

        //handle customFields
        NodeList sysCustomFields = sys.getElementsByTagName("customFields");
        NodeList orgCustomFields = org.getElementsByTagName("customFields");
        Map<String,Node> customFieldsMap = new HashMap<String,Node>();
        List<String>  customFieldsName= new ArrayList<String>();

        for(int current = 0,length=sysCustomFields.getLength();current<length;current++){
            Node n = sysCustomFields.item(current);
            NodeList sysCustomFieldsChildren = n.getChildNodes();
            for(int c = 0,l=sysCustomFieldsChildren.getLength();c<l;c++){
                Node customField = sysCustomFieldsChildren.item(c);
                if(customField.getNodeType()==1){
                	String fieldName = customField.getAttributes().getNamedItem("name").getNodeValue();
                	if(!customFieldsName.contains(fieldName)){
                		customFieldsName.add(fieldName);
                	}
                	customFieldsMap.put(fieldName, customField);
                }
            }
        }

        for(int current = 0,length=orgCustomFields.getLength();current<length;current++){
            Node n = orgCustomFields.item(current);
            NodeList customFields = n.getChildNodes();
            for(int c = 0,l=customFields.getLength();c<l;c++){
                Node customField = customFields.item(c);
                if(customField.getNodeType()==1){
                    NamedNodeMap nameMap =  customField.getAttributes();
                    String fieldName = nameMap.getNamedItem("name").getNodeValue();
                    boolean remove = false;
                    if(nameMap.getNamedItem("remove")!=null&&"true".equals(nameMap.getNamedItem("remove").getNodeValue())){
                        remove = true;
                    }
                    if(remove){
                        customFieldsMap.remove(fieldName);
                        continue;
                    }
                    if(!customFieldsName.contains(fieldName)){
                		customFieldsName.add(fieldName);
                	}
                    customFieldsMap.put(fieldName, customField);
                }
            }
        }

        Element customFields =  result.createElement("customFields");
        for(String fieldName : customFieldsName){
        	Node n = customFieldsMap.get(fieldName);
        	if(n != null){
            	customFields.appendChild(result.importNode(n, true));
        	}
        }
        e.appendChild(customFields);

        StringWriter sw = new StringWriter();
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.transform(new DOMSource(e), new StreamResult(sw));

        return e;
    }
    
    public String getErrorMsg(String content,boolean isOrg,String orgName){
        String errorMsg = "";
        if(content!=null){
            try{
                DocumentBuilder db  = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = db.parse(new ByteArrayInputStream(content.getBytes()));
                if(!isOrg){
                    NodeList sysKeywords = document.getElementsByTagName("keyword");
                    //handle for keywords
                    if(sysKeywords.getLength()==0){
                        errorMsg = "Missing keyword element.";
                    }else{
                        NodeList keywordFields = sysKeywords.item(0).getChildNodes();
                        boolean contactTsv = false,resumeTsv = false;
                        for(int i=0,j=keywordFields.getLength();i<j;i++){
                            Node keywordField = keywordFields.item(i);
                            if(keywordField.getNodeType()==1&&"field".equals(keywordField.getNodeName())){
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
                        if(!contactTsv&&!resumeTsv){
                            errorMsg="Missing contactInfoTsv and contactResumeTsv keyword field.";
                        }else if(!resumeTsv){
                            errorMsg="Missing contactResumeTsv keyword field.";
                        }
                    }
                   //handle for customFields
                    String checkInfo = checkSearchConfigCustomfields(document,orgName);
                    if(checkInfo != null){
                    	errorMsg = checkInfo;
                    }
                    //handle filter
                    NodeList filterNodes = document.getElementsByTagName("filter");
                    boolean skillFilter = false,educationFilter = false,locationFilter = false,companyFilter = false;
                    for(int i=0,j=filterNodes.getLength();i<j;i++){
                        Node filterField = filterNodes.item(i);
                        if(filterField.getNodeType()==1&&checkAttribute(filterField, "filtertype")){
                           String val = filterField.getAttributes().getNamedItem("filtertype").getNodeValue();
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
                           for(int m=0,n=filterField.getChildNodes().getLength();m<n;m++){
                               Node fieldNode = filterField.getChildNodes().item(m);
                               if(fieldNode.getNodeType()==1){
                                   if(!checkAttribute(fieldNode,"table")){
                                       errorMsg="Missing table attribute for "+val+" filter field";
                                   }
                                   if(!checkAttribute(fieldNode,"column")){
                                       errorMsg="Missing column attribute for "+val+" filter field";
                                   }
                                   if(!"location".equals(val)){
                                       if(!checkAttribute(fieldNode,"joinfrom")){
                                           errorMsg="Missing joinfrom attribute for "+val+" filter field";
                                       }
                                       if(!checkAttribute(fieldNode,"table")){
                                           errorMsg="Missing jointo attribute for "+val+" filter field";
                                       }
                                   }
                                   if("skill".equals(val)){
                                       if(!checkAttribute(fieldNode,"slider")){
                                           errorMsg="Missing slider attribute for "+val+" filter field";
                                       }
                                   }
                               }
                           }
                        }
                    }
                        
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
                    
                    //handle contact
                    NodeList contactFiler = document.getElementsByTagName("contact");
                    if(contactFiler.getLength()==0){
                        errorMsg = "Missing contact element.";
                    }else{
                        NodeList contactFields = contactFiler.item(0).getChildNodes();
                        Map columns = mapIt("id",1,"email",1,"name",1,"sfid",1,
                                "title",1,"createddate",1,"resume",1,"mailingpostalcode",1);
                        for(int i=0,j=contactFields.getLength();i<j;i++){
                            Node contactField = contactFields.item(i);
                            if(contactField.getNodeType()==1){
                                if(!checkAttribute(contactField, "column")){
                                    errorMsg = "Missing column attribute for contact field";
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
                        }
                        if(columns.keySet().size()>0){
                            errorMsg="Missing contact fields: ";
                            for(Object s:columns.keySet()){
                                errorMsg+=s+",";
                            }
                            errorMsg = errorMsg.substring(0,errorMsg.length()-1);
                        }
                    }
              }else{
            	  String checkInfo = checkSearchConfigCustomfields(document,orgName);
                  if(checkInfo != null){
                  	errorMsg = checkInfo;
                  }
              }
            }catch(Exception e){
                errorMsg="The search config xml has grammer issues.";
            }
        }else{
            errorMsg="The search config xml has grammer issues.";
        }
        return errorMsg;
    }
    
    private String checkSearchConfigCustomfields(Document document, String orgName){
    	String errorMsg = null;
    	NodeList customFieldsContent = document.getElementsByTagName("customFields");
        if(customFieldsContent.getLength()!=0){
            NodeList customFields = customFieldsContent.item(0).getChildNodes();
            boolean hasError = false;
            int customFieldSize = 0;
            for(int i=0,j=customFields.getLength();i<j;i++){
                Node customField = customFields.item(i);
                if(customField.getNodeType()==1){
                	if("field".equals(customField.getNodeName())){
                	   boolean lackProperty = false;
                	   if(!checkAttribute(customField, "tableName")){
                            errorMsg = "Missing tableName attribute for customFields field";
                            lackProperty = true;
                        }
                        if(!lackProperty && !checkAttribute(customField, "name")){
                            errorMsg = "Missing name attribute for customFields field";
                            lackProperty = true;
                        }
                       if(!lackProperty && !checkAttribute(customField, "columnName")){
                           errorMsg = "Missing columnName attribute for customFields field";
                           lackProperty = true;
                       }
                       if(!lackProperty && !checkAttribute(customField, "label")){
                           errorMsg = "Missing label attribute for customFields field";
                           lackProperty = true;
                       }
                       if(!lackProperty && !checkAttribute(customField, "type")){
                           errorMsg = "Missing type attribute for customFields field";
                           lackProperty = true;
                       }
                       if(lackProperty){
                    	   hasError = true;
                       }else{
                    	   customFieldSize++;
                       }
                	}else{
                    	errorMsg="The search config xml has grammer issues.";
                    	hasError = true;
                    }
                }
                if(hasError){
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
        }else{
        	if(Strings.isNullOrEmpty(orgName)){
            	customFieldsSize.put("sys", 0);
            }else{
            	customFieldsSize.put(orgName, 0);
            }
        }
        return errorMsg;
    }
    
    private boolean checkAttribute(Node n,String name){
        if(n.getAttributes().getNamedItem(name)!=null){
            return true;
        }
        return false;
    }
    
    private String getVal(Node n,String attributeName){
        if(!checkAttribute(n,attributeName)){
            return null;
        }
        return n.getAttributes().getNamedItem(attributeName).getNodeValue();
    }
}
