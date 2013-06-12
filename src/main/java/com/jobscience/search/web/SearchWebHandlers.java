package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.jobscience.search.dao.SearchDao;
import com.jobscience.search.dao.SearchMode;
import com.jobscience.search.dao.SearchResult;

@Singleton
public class SearchWebHandlers {


    @Inject
    private SearchDao searchDao;
    
    @WebGet("/search")
    public WebResponse search(@WebParam("q_") Map searchValues,@WebParam("searchMode") String searchModeStr,
                              @WebParam("pageIdx") Integer pageIdx, @WebParam("pageSize") Integer pageSize,
                              @WebParam("column")String column,@WebParam("order")String order,
                              @WebParam("searchColumns")String searchColumns ){
        
        SearchMode searchMode = SearchMode.SIMPLE;
        if (searchModeStr != null) {
            searchMode = SearchMode.valueOf(searchModeStr.toUpperCase());
        }
        
        if(pageIdx == null ){
            pageIdx = 0;
        }
        if(pageSize == null ){
            pageSize = 30;
        }
        String orderCon = "";
        if(column!=null){
        	orderCon = " order by  " +getOrderColumn(column)+ " " +(("desc".equals(order))?"desc":"asc");
        }
        // FIXME: needs to get the search map from request.
       // Map searchValues = MapUtil.mapIt("search",search);
        SearchResult searchResult = searchDao.search(searchColumns,searchValues,searchMode, pageIdx, pageSize,orderCon);
        WebResponse wr = WebResponse.success(searchResult);
        return wr;
    }
    
    @WebGet("/getTopCompaniesAndEducations")
    public WebResponse getTopCompanies(@WebParam("type") String type, @WebParam("companyOffset") Integer companyOffset,
                            @WebParam("companyLimit") Integer companyLimit, @WebParam("educationOffset") Integer educationOffset,
                            @WebParam("educationLimit") Integer educationLimit, @WebParam("skillOffset") Integer skillOffset,
                            @WebParam("skillLimit") Integer skillLimit, @WebParam("offset") Integer offset,
                            @WebParam("limit") Integer limit) throws SQLException {
    	Map result = new HashMap();
        if(offset==null){
        	offset = 0;
        }
        if(limit==null){
        	limit=5;
        }
        
        long start = System.currentTimeMillis();
        if(type==null || "".equals(type) || "company".equals(type)){
            if(companyOffset == null){
                companyOffset = offset;
            }
            if(companyLimit == null){
                companyLimit = limit;
            }
	        List companies = searchDao.getTopMostCompanies(companyOffset,companyLimit);
	        result.put("companies", companies);
	        long tempEnd = System.currentTimeMillis();
	        result.put("companyDuration", tempEnd - start);
        }
        long mid = System.currentTimeMillis();
        if(type==null || "".equals(type) || "education".equals(type)){
            if(educationOffset == null){
                educationOffset = offset;
            }
            if(educationLimit == null){
                educationLimit = limit;
            }
	        List educations = searchDao.getTopMostEducation(educationOffset,educationLimit);
	        result.put("educations", educations);
	        long tempEnd = System.currentTimeMillis();
	        result.put("educationDuration", tempEnd - mid);
        }
        long mid1 = System.currentTimeMillis();
        if(type==null || "".equals(type) || "skill".equals(type)){
            if(skillOffset == null){
                skillOffset = offset;
            }
            if(skillLimit == null){
                skillLimit = limit;
            }
	        List skills = searchDao.getTopMostSkills(skillOffset,skillLimit);
	        result.put("skills", skills);
	        long tempEnd = System.currentTimeMillis();
            result.put("skillDuration", tempEnd - mid1);
        }
        long end = System.currentTimeMillis();
        result.put("duration", end - start);
        WebResponse wr = WebResponse.success(result);
        return wr;
    }
    
    @WebGet("/getGroupValuesForAdvanced")
    public WebResponse getGroupValuesForAdvanced(@WebParam("q_") Map searchValues) throws SQLException{
         // FIXME: needs to get the search map from request.
        // Map searchValues = MapUtil.mapIt("search",search);
        Map result = new HashMap();
        List<Map> companies = searchDao.getGroupValuesForAdvanced(searchValues,"company");
        List<Map> educations = searchDao.getGroupValuesForAdvanced(searchValues,"education");
        List<Map> skills = searchDao.getGroupValuesForAdvanced(searchValues,"skill");
        
        result.put("companies", companies);
        result.put("educations", educations);
        result.put("skills", skills);
        WebResponse wr = WebResponse.success(result);
        return wr;
    }

    
    private String getOrderColumn(String originalName){
    	if("Name".equalsIgnoreCase(originalName)||
    	   "Title".equalsIgnoreCase(originalName)||
    	   "Company".equalsIgnoreCase(originalName)||
    	   "Skill".equalsIgnoreCase(originalName)||
    	   "Education".equalsIgnoreCase(originalName)){
    		return "l"+originalName;
    	}
    	
    	return originalName;
    }
}
