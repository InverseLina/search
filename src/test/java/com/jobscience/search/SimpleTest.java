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
import com.jobscience.search.dao.SearchResult;

public class SimpleTest extends SnowTestSupport {

    @BeforeClass
    public static void initTestClass() throws Exception {
        // Here we override one property to use the in memory DB
        SnowTestSupport.initWebApplication("src/main/webapp");
    }
    
    
    @Test
    public void getTopAdvancedType() throws SQLException{
       SearchDao searchDao = appInjector.getInstance(SearchDao.class);
       List<Map> result = searchDao.getTopAdvancedType(0, 10, "company", "IBM","1");
       searchDao.getTopAdvancedType(0, 10, "skill", "Excel","1");
       searchDao.getTopAdvancedType(0, 10, "location", "new york","1");
       searchDao.getTopAdvancedType(0, 10, "education", "uni","1");
       for(Map m:result){
    	   System.out.println(m.get("name"));
       }
    }
    @Test
    public void newSearchTest(){
       SearchDao searchDao = appInjector.getInstance(SearchDao.class);
       Map values = MapUtil.mapIt("search","java");
       values.put("contacts", "[{firstName:'kevin'},{lastName:'cook'}]");
       values.put("educations", "{values:['education1','education2'],minYears:2}");
       values.put("skills", "{values:['skill1','skill2'],minYears:2}");
       values.put("companies", "{values:['employer1','employer2'],minYears:2}");
       values.put("locations", "{values:['New York','Amherst'],minRadius:30}");
       SearchResult result = searchDao.search("id,name,title,location",values, 1, 30,"");
       System.out.println("new search result : "+result.getCount());
    }
    
    @Test
    public void stringTest(){
        String search = "Hello ";
        String searchTq = Joiner.on(" & ").join(Splitter.on(" ").omitEmptyStrings().split(search));
        System.out.println("-" + searchTq + "-");
    }
    
    
}
