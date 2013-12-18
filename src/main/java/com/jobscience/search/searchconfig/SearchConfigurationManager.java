package com.jobscience.search.searchconfig;

import static com.britesnow.snow.util.MapUtil.mapIt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SearchConfigurationManager {

    @Inject
    private CurrentRequestContextHolder currentRequestContextHolder;
    
   // @Inject
    //private CurrentOrgHolder orgHolder;
    
    private volatile SearchConfiguration searchConfiguration;
    
    private void load() throws Exception{
        JAXBContext jc = JAXBContext.newInstance(SearchConfiguration.class);
        Unmarshaller ums =  jc.createUnmarshaller();
        searchConfiguration = (SearchConfiguration) ums.unmarshal(getMergedNode());
    }
    
    public SearchConfiguration getSearchConfiguration(){
        if(searchConfiguration==null){
            try {
                load();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return searchConfiguration;
    }
    
    public List<Map> getFilters(String orgName){
        List<Map> filters = new ArrayList<Map>();
        SearchConfiguration sc = getSearchConfiguration();
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
    
    private  Node getMergedNode() throws Exception {
        StringBuilder path = new StringBuilder(currentRequestContextHolder.getCurrentRequestContext().getServletContext().getRealPath("/"));
        DocumentBuilder db  = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document sys = db.parse(path+"/WEB-INF/config/sys/searchconfig.val");
        Document org ;
        try{
           org = db.parse(path+"/WEB-INF/config/org/searchconfig.val");
        }catch(Exception e){
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
    
}
