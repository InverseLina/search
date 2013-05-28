package com.jobscience.search;

import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;


import com.britesnow.snow.testsupport.SnowTestSupport;
import com.britesnow.snow.util.MapUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.jobscience.search.dao.SearchDao;
import com.jobscience.search.dao.SearchResult;

public class SimpleTest extends SnowTestSupport {

    @BeforeClass
    public static void initTestClass() throws Exception {
        // Here we override one property to use the in memory DB
        SnowTestSupport.initWebApplication("src/main/webapp");
    }
    
    @Test
    public void preparedTest(){
       SearchDao searchDao = appInjector.getInstance(SearchDao.class);
       Map values = MapUtil.mapIt("search","java");
       long start = System.currentTimeMillis();
       SearchResult searchResult = searchDao.search(values);
       long end = System.currentTimeMillis();
       System.out.println("search result: " + (end - start));
    }
    
    //@Test
    public void stringTest(){
        String search = "Hello ";
        String searchTq = Joiner.on(" & ").join(Splitter.on(" ").omitEmptyStrings().split(search));
        System.out.println("-" + searchTq + "-");
    }
    
    
}
