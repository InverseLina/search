package com.jobscience.search.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.google.common.base.Strings;

public class SearchRequest {

    private Map<String,String> searchMap = new HashMap();
    private String order ,columns;
    private int pageIndex = 1,pageSize = 15;
    private JSONArray contacts;
    private JSONArray skills;
    private JSONArray educations;
    private JSONArray locations;
    private JSONArray companies;
    private Map<String,JSONArray> customFilters = new HashMap<String, JSONArray>(); //contains filter and column display
    private JSONArray customFields ; // only contains filter ,no column display
    private String searchValues;
    private String skillOperator;
    private String companyOperator;
    private boolean isOnlyKeyWord = true;
    private boolean estimateSearch = true;
    private boolean searchModeChange = false;
    private boolean hasContactTitle = false;

    public SearchRequest(Map searchParams){
        
        searchValues = (String)searchParams.get("searchValues");
        if(!Strings.isNullOrEmpty(searchValues)){
            JSONObject jo = JSONObject.fromObject(searchValues);
            // resolve the search parameters,cause all parameters begin with "q_"
            String temp ;
            for(Object key : jo.keySet()){
            	temp = jo.get(key).toString()/*.replaceAll("#", "\\\"")*/;
            	if("q_search".equals(key)){
            		temp = temp.replaceAll("\'", "");
            		temp = temp.replaceAll("[\\,\\:\\(\\)\\[\\]\\{\\}/\\*\\^!&]","");
            	}
            	if("q_search".equals(key) && !temp.matches("^\\s*\".+\"\\s*$")){
            		temp = ruleInexactKeyWord(temp).trim();
            	}
                searchMap.put(key.toString().substring(2),temp);
                if("q_search".equals(key)||
                   ("q_status".equals(key)&&"All".equals(temp))||
                   ("q_objectType".equals(key)&&"All".equals(temp))){
            		continue;
            	}else{
            		isOnlyKeyWord = false;
            	}
            }
        }
        
        columns = (String)searchParams.get("columns");
        columns =  columns.replaceAll("\\s+", "");
        if(Strings.isNullOrEmpty(columns)){
            columns = "contact";
        }
        columns = "," + columns +",";
        columns = columns.replaceAll(",contact,", ",id,name,title,email,CreatedDate,resume,");
        if(columns.startsWith(",")){
        	columns = columns.substring(1);
        }
        if(columns.endsWith(",")){
        	columns = columns.substring(0,columns.length()-1);
        }
        
        String orderBy = (String)searchParams.get("orderBy");
        boolean asc = searchParams.get("orderType") instanceof Boolean?
                (Boolean)searchParams.get("orderType"):
                Boolean.valueOf((String)searchParams.get("orderType"));
        if(orderBy != null){
            if(orderBy.equals("contact")){
                orderBy = "name";
                order = " \""+getOrderColumn(orderBy)+ "\" " +(asc?"asc":"desc");
            }/*else if(columns.contains(orderBy)){
                order = " \""+getOrderColumn(orderBy)+ "\" " +(asc?"asc":"desc");
            }*/else{
                order = " \"id\" desc";
            }
            
        }else{
            order = " \"id\" desc";
        }
        
        
        pageIndex = (Integer)searchParams.get("pageIndex");
        pageSize = (Integer)searchParams.get("pageSize");
        if(searchParams.get("searchMode")  != null){
        	if(searchParams.get("searchMode").toString().toLowerCase().equals("verify")){
        		estimateSearch = false;
        	}else{
        		estimateSearch = true;
        	}
        }
        
        if(searchParams.get("searchModeChange")  != null){
        	if(searchParams.get("searchModeChange").toString().toLowerCase().equals("true")){
        		searchModeChange = true;
        	}else{
        		searchModeChange = false;
        	}
        }
        
        if(searchParams.get("skillOperator")  != null){
            skillOperator = searchParams.get("skillOperator").toString();
        }else{
            skillOperator = "O";
        }
        
        if(searchParams.get("companyOperator")  != null){
            companyOperator = searchParams.get("companyOperator").toString();
        }else{
            companyOperator = "O";
        }
        
        setContacts();
        setSkills();
        setEducations();
        setCompanies();
        setLocations();
        setCustomFilters();
        setCustomFields();
        setHasContactTitle();
    }
    
    private void setContacts(){
        String paramsString = searchMap.get("contacts");
        if(!Strings.isNullOrEmpty(paramsString)){
            contacts = JSONArray.fromObject(paramsString);
        }
        
    }
    
    public JSONArray getContacts(){
        return contacts;
    }
    
    private void setSkills(){
        String paramsString = searchMap.get("skills");
        if(!Strings.isNullOrEmpty(paramsString)){
            skills = JSONArray.fromObject(paramsString);
        }
        
    }
    
    public JSONArray getSkills(){
        return skills;
    }
    
    private void setEducations(){
        String paramsString = searchMap.get("educations");
        if(!Strings.isNullOrEmpty(paramsString)){
            educations = JSONArray.fromObject(paramsString);
        }
        
    }
    
    public JSONArray getEducations(){
        return educations;
    }
    
    
    private void setCompanies(){
        String paramsString = searchMap.get("companies");
        if(!Strings.isNullOrEmpty(paramsString)){
            companies = JSONArray.fromObject(paramsString);
        }
        
    }
    
    public JSONArray getCompanies(){
        return companies;
    }
    
    
    private void setLocations(){
        String paramsString = searchMap.get("locations");
        if(!Strings.isNullOrEmpty(paramsString)){
            locations = JSONArray.fromObject(paramsString);
        }
        
    }
    
    public JSONArray getLocations(){
        return locations;
    }
    
    public Map getSearchMap(){
        return searchMap;
    }

    public void setHasContactTitle () {
    	if (contacts!=null) {
        	for (Object contactString : contacts) {
        		JSONObject contact = JSONObject.fromObject(contactString);
        		if (contact.containsKey("title")) {
        			hasContactTitle = true;
        		}
        	}
    	}
    }

    private void setCustomFilters () {
        for(String name : searchMap.keySet()){
            if(isNativeSearchParam(name) || isCustomFields(name)){ //when customFields should not contains in CustomFilter
                continue;
            }
            customFilters.put(name, JSONArray.fromObject(searchMap.get(name)));
        }
    }
    
    public Map<String,JSONArray> getCustomFilters(){
        return customFilters;
    }

    private void setCustomFields(){
        for(String name : searchMap.keySet()){
            if(isCustomFields(name)){
                customFields = JSONArray.fromObject(searchMap.get(name));
                break;
            }
        }
    }

    public JSONArray getCustomFields(){
        return customFields;
    }
    
    public String getOrder() {
        return order;
    }

    public String getColumns() {
        return columns;
    }

    public int getPageIndex() {
        if(pageIndex < 1){
            pageIndex = 1;
        }
        return pageIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getOffest() {
        return pageSize*(getPageIndex()-1);
    }
    
    public String getStatus(){
        return searchMap.get("status");
    }
    
    public String getObjectType(){
        return searchMap.get("objectType");
    }
    
    public String getKeyword(){
        return searchMap.get("search");
    }
    
    public boolean isOnlyKeyWord() {
		return isOnlyKeyWord;
	}
    
    public String getSkillOperator() {
        return skillOperator;
    }

    public String getCompanyOperator() {
        return companyOperator;
    }

    public boolean isEstimateSearch() {
		return estimateSearch;
	}
    
    public boolean searchModeChange() {
		return searchModeChange;
	}

    public boolean hasContactTitle() {
		return hasContactTitle;
	}
    
	public String toString(){
        return searchValues;
    }
    
    /**
     * get the order column name by original column
     * @param originalName
     * @return
     */
    private String getOrderColumn(String originalName){
        if("name".equalsIgnoreCase(originalName)||
           "title".equalsIgnoreCase(originalName)||
           "company".equalsIgnoreCase(originalName)||
           "skill".equalsIgnoreCase(originalName)||
           "education".equalsIgnoreCase(originalName)||
           "email".equalsIgnoreCase(originalName)){
            return "l"+originalName;
        }else if("createddate".equalsIgnoreCase(originalName)){
            return "createddate";
        }else if( "location".equalsIgnoreCase(originalName)){
            return "location";
        }
        return originalName;
    }
    
    private boolean isNativeSearchParam(String name){
        List<String> searchParams = new ArrayList<String>();
        searchParams.add("search");
        searchParams.add("label");
        searchParams.add("labelAssigned");
        searchParams.add("contacts");
        searchParams.add("skills");
        searchParams.add("companies");
        searchParams.add("educations");
        searchParams.add("locations");
        searchParams.add("status");
        searchParams.add("objectType");
        
        for(String s:searchParams){
            if(s.equals(name)){
                return true;
            }
        }
        return false;
    }

    private boolean isCustomFields(String name){
        return !Strings.isNullOrEmpty(name)&&name.equals("customFields");//means customFields
    }
    private String ruleInexactKeyWord(String keyWord){
    	StringBuffer ruleKey = new StringBuffer();
    	String keyword = keyWord.trim().replaceAll("\"", "");
    	String[] keywords = keyword.split("[ ]+");
    	boolean needOperator = false;
    	boolean hasOperator = false;
    	String operator = "";
    	for(String key : keywords){
    		if(key.trim().equals("AND")){
    			if(needOperator){
					operator = "AND ";
					hasOperator = true;
    			}
    			continue;
    		}else if(key.trim().equals("OR")){
    			if(needOperator){
					operator = "OR ";
					hasOperator = true;
    			}
    			continue;
    		}else if(key.trim().equals("NOT")){
    			if(needOperator){
					operator = "NOT ";
					hasOperator = true;
    			}
    			continue;
    		}
    		if(needOperator){
    			if(hasOperator){
    				ruleKey.append(operator);
    				hasOperator = false;
    			}else{
        			ruleKey.append("OR ");
    			}
    		}
			
    		ruleKey.append(key).append(" ");
    		needOperator = true;
    	}
		return ruleKey.toString();
    }
    
}
