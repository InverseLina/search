package com.jobscience.search.searchconfig;

import static com.britesnow.snow.util.MapUtil.mapIt;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

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
    
    private void load() throws JAXBException{
        StringBuilder path = new StringBuilder(currentRequestContextHolder.getCurrentRequestContext().getServletContext().getRealPath("/"));
        path.append("/WEB-INF/config/sys/searchconfig.val");

        JAXBContext jc = JAXBContext.newInstance(SearchConfiguration.class);
        Unmarshaller ums =  jc.createUnmarshaller();
        searchConfiguration = (SearchConfiguration) ums.unmarshal(new File(path.toString()));
    }
    
    public SearchConfiguration getSearchConfiguration(){
        if(searchConfiguration==null){
            try {
                load();
            } catch (JAXBException e) {
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
                Map m = mapIt(      "name",   f.getName(),
                                   "title",   f.getTitle(),
                                  "native",   (f.getFilterType()!=null),
                                    "show",   (f.getFilterType()!=null));
                if(f.getFilterType()==null){
                    m.put("paramName",   f.getFilterField().getColumn());
                    m.put("type", "custom");
                }else{
                    m.put("type",   f.getFilterType().value());
                }
                filters.add(m);
        }
        
        filters.add(mapIt(           "name",   "location",
                                    "title",   "Location",
                                   "native",   true,
                                     "show",   false,
                                     "type",   "location"));
        
         filters.add(mapIt(          "name",   "resume",
                                    "title",   "Resume",
                                   "native",   true,
                                     "show",   false,
                                     "type",   "resume"));
        return filters;
    }
}
