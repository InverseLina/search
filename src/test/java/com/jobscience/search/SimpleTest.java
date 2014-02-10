package com.jobscience.search;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.britesnow.snow.testsupport.SnowTestSupport;
import com.britesnow.snow.util.MapUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.dao.SearchDao;
import com.jobscience.search.dao.SearchResult;

public class SimpleTest extends SnowTestSupport {

    @BeforeClass
    public static void initTestClass() throws Exception {
        // Here we override one property to use the in memory DB
        SnowTestSupport.initWebApplication("src/main/webapp");
    }
    //@Test
    public void newSearchTest(){
       SearchDao searchDao = appInjector.getInstance(SearchDao.class);
       CurrentOrgHolder orgHolder = appInjector.getInstance(CurrentOrgHolder.class);
       Map values = MapUtil.mapIt("search","java");
       values.put("contacts", "[{firstname:'kevin'},{lastname:'cook'}]");
       values.put("educations", "[{name:'education1',minYears:2},{name:'education2'}]");
       values.put("skills", "[{name:'skill1',minYears:2},{name:'skill2',minYears:0}]");
       values.put("companies", "[{name:'employer1',minYears:2},{name:'employer2',minYears:0}]");
       values.put("locations", "[{name:'New York',minRadius:3000000}]");
       SearchResult result = searchDao.search("id,name,title,location",values, 1, 30,"","","",orgHolder.getCurrentOrg());
       System.out.println("new search result : "+result.getCount());
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
