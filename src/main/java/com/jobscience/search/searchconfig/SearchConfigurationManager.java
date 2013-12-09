package com.jobscience.search.searchconfig;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.britesnow.snow.web.hook.AppPhase;
import com.britesnow.snow.web.hook.annotation.WebApplicationHook;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SearchConfigurationManager {

    @Inject
    private CurrentRequestContextHolder currentRequestContextHolder;
    
    private SearchConfiguration searchConfiguration;
    
    @WebApplicationHook(phase=AppPhase.INIT)
    public void init() throws JAXBException{
        StringBuilder path = new StringBuilder(currentRequestContextHolder.getCurrentRequestContext().getServletContext().getRealPath("/"));
        path.append("/WEB-INF/config/sys/searchconfig.val");

        JAXBContext jc = JAXBContext.newInstance(SearchConfiguration.class);
        Unmarshaller ums =  jc.createUnmarshaller();
        searchConfiguration = (SearchConfiguration) ums.unmarshal(new File(path.toString()));
    }
    
    public SearchConfiguration getSearchConfiguration(){
        return searchConfiguration;
    }
}
