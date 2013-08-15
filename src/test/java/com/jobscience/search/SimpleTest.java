package com.jobscience.search;

import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.britesnow.snow.testsupport.SnowTestSupport;
import com.britesnow.snow.util.MapUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.jobscience.search.dao.OldSearchDao;
import com.jobscience.search.dao.SearchResult;

public class SimpleTest extends SnowTestSupport {

    @BeforeClass
    public static void initTestClass() throws Exception {
        // Here we override one property to use the in memory DB
        SnowTestSupport.initWebApplication("src/main/webapp");
    }
    
    //@Test
    public void preparedTest(){
       OldSearchDao searchDao = appInjector.getInstance(OldSearchDao.class);
       Map values = MapUtil.mapIt("search","java");
       long start = System.currentTimeMillis();
       SearchResult result = searchDao.search("id,name,title",values, 1, 30,"");
       long end = System.currentTimeMillis();
       Assert.assertEquals(1011, result.getCount());
       System.out.println("search cost time: " + (end - start));
    }
    
    @Test
    public void stringTest(){
        String search = "Hello ";
        String searchTq = Joiner.on(" & ").join(Splitter.on(" ").omitEmptyStrings().split(search));
        System.out.println("-" + searchTq + "-");
    }
    
    
}
