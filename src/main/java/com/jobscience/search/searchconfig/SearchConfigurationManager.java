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
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

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
    
    private  Node getMergedNode() throws SAXException, IOException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException, JAXBException {
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
        org.w3c.dom.Element e =  result.createElement("searchconfig");
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
                boolean delete = false;
                if(nameMap.getNamedItem("delete")!=null&&"true".equals(nameMap.getNamedItem("delete").getNodeValue())){
                    delete = true;
                }
                sysNodesList.remove(sysNodeMap.get(name));
                sysNodeMap.remove(name);
                if(!delete){
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
