package com.jobscience.search;

import org.junit.BeforeClass;
import org.junit.Test;

import com.britesnow.snow.testsupport.SnowTestSupport;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.jobscience.search.dao.DBSetupManager;

public class SimpleTest extends SnowTestSupport {

    @BeforeClass
    public static void initTestClass() throws Exception {
        // Here we override one property to use the in memory DB
        SnowTestSupport.initWebApplication("src/main/webapp");
    }
    
    //@Test
    public void updateZipcode() throws Exception{
    	 DBSetupManager setupManager = appInjector.getInstance(DBSetupManager.class);
    	 setupManager.updateZipCode();
    }
    @Test
    public void stringTest(){
        String search = "Hello ";
        String searchTq = Joiner.on(" & ").join(Splitter.on(" ").omitEmptyStrings().split(search));
        System.out.println("-" + searchTq + "-");
    }
}
