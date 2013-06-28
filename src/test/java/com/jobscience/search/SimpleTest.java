package com.jobscience.search;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.britesnow.snow.testsupport.SnowTestSupport;
import com.britesnow.snow.util.MapUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.jobscience.search.dao.SearchDao;
import com.jobscience.search.service.LocationService;

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
       searchDao.search(null,values, 1, 30,"");
       long end = System.currentTimeMillis();
       System.out.println("search result: " + (end - start));
    }
    
    @Test
    public void searchDistanceTest() throws SQLException{
       LocationService locationService = appInjector.getInstance(LocationService.class);
       long start = System.currentTimeMillis();
       List<Map> contacts = locationService.findContactsNear(30.0, 30.0, 621.0);
       long end = System.currentTimeMillis();
       System.out.println("search result size(by lat/long): " + contacts.size());
       System.out.println("cost time(by lat/long): " + (end - start));
       
       start = System.currentTimeMillis();
       contacts = locationService.findContactsNear("210",6.21);
       end = System.currentTimeMillis();
       System.out.println("search result size(by zip): " + contacts.size());
       System.out.println("cost time(by zip): " + (end - start));
    }
    
    //@Test
    public void stringTest(){
        String search = "Hello ";
        String searchTq = Joiner.on(" & ").join(Splitter.on(" ").omitEmptyStrings().split(search));
        System.out.println("-" + searchTq + "-");
    }
    
    
}
