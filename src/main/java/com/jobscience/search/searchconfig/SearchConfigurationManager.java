package com.jobscience.search.searchconfig;

import static com.britesnow.snow.util.MapUtil.mapIt;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.DaoHelper;
import com.jobscience.search.dao.OrgConfigDao;

@Singleton
public class SearchConfigurationManager {

    @Inject
    private CurrentRequestContextHolder currentRequestContextHolder;
    @Inject
    private DaoHelper daoHelper;
    @Inject
    private OrgConfigDao orgConfigDao;
   // @Inject
    //private CurrentOrgHolder orgHolder;
    
    private volatile SearchConfiguration searchConfiguration;
    
    private void load(String orgName) throws Exception{
        JAXBContext jc = JAXBContext.newInstance(SearchConfiguration.class);
        Unmarshaller ums =  jc.createUnmarshaller();
        searchConfiguration = (SearchConfiguration) ums.unmarshal(getMergedNode(orgName));
    }
    
    public SearchConfiguration getSearchConfiguration(String orgName){
            try {
                load(orgName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        return searchConfiguration;
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
        
         filters.add(mapIt(          "name",   "resume",
                                    "title",   "Resume",
                                   "native",   true,
                                     "show",   false,
                                     "type",   "resume"));
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
        List<Map> orgConfig = daoHelper.executeQuery(daoHelper.openNewSysRunner(),
            "select val_text from config where name = ? and org_id =?", "searchconfig",orgId);
        if(orgConfig.size()>0){
           return orgConfig.get(0).get("val_text").toString();
        }else{
            return "<searchconfig></searchconfig>";
        }
    }
    
    private  Node getMergedNode(String orgName) throws Exception {
        StringBuilder path = new StringBuilder(currentRequestContextHolder.getCurrentRequestContext().getServletContext().getRealPath("/"));
        DocumentBuilder db  = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        //get the sys config
        List<Map> sysConfig = daoHelper.executeQuery(daoHelper.openNewSysRunner(),
            "select val_text from config where name = ? and org_id is null", "searchconfig");
        Document sys = null;
        if (sysConfig.size() == 0) {
            sys = db.parse(path+"/WEB-INF/config/sys/searchconfig.val");
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
        List<Map> orgConfig = daoHelper.executeQuery(daoHelper.openNewSysRunner(),
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
        
        return e;
    }
    
    public boolean isValid(String content){
        boolean valid = false;
        if(content!=null){
            try{
            DocumentBuilder db  = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            db.parse(new ByteArrayInputStream(content.getBytes()));
            valid = true;
            }catch(Exception e){
                valid = false;
            }
        }
        return valid;
    }
}
