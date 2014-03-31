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
    private Map<String,JSONArray> customFilters = new HashMap<String, JSONArray>();
    private String searchValues;
    
    private int hashCode = 0;
    public SearchRequest(Map searchParams){
        
        searchValues = (String)searchParams.get("searchValues");
        if(!Strings.isNullOrEmpty(searchValues)){
            searchValues = searchValues.replaceFirst("#", "").replaceAll("\\\\\"", "#");
            JSONObject jo = JSONObject.fromObject(searchValues);
            // resolve the search parameters,cause all parameters begin with "q_"
            for(Object key:jo.keySet()){
                searchMap.put(key.toString().substring(2),jo.get(key).toString().replaceAll("#", "\\\""));
            }
        }
        
        columns = (String)searchParams.get("columns");
        if(Strings.isNullOrEmpty(columns)){
            columns = "contact";
        }
        columns = columns.replaceAll("contact", "id,name,title,email,CreatedDate,resume");
        
        String orderBy = (String)searchParams.get("orderBy");
        boolean asc = searchParams.get("orderType") instanceof Boolean?
                (Boolean)searchParams.get("orderType"):
                Boolean.valueOf((String)searchParams.get("orderType"));
        if(orderBy!=null){
            if(orderBy.equals("contact")){
                orderBy = "name";
                order = " \""+getOrderColumn(orderBy)+ "\" " +(asc?"asc":"desc");
            }else if(columns.contains(orderBy)){
                order = " \""+getOrderColumn(orderBy)+ "\" " +(asc?"asc":"desc");
            }else{
                order = " \"id\" asc";
            }
            
        }else{
            order = " \"id\" asc";
        }
        
        
        pageIndex = (Integer)searchParams.get("pageIndex");
        pageSize = (Integer)(searchParams.get("pageSize"));
        
        setContacts();
        setSkills();
        setEducations();
        setCompanies();
        setLocations();
        setCustomFilters();
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
    private void setCustomFilters(){
        for(String name:searchMap.keySet()){
            if(isNativeSearchParam(name)){
                continue;
            }
            customFilters.put(name, JSONArray.fromObject(searchMap.get(name)));
        }
    }
    
    public Map<String,JSONArray> getCustomFilters(){
        return customFilters;
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
    
    public String toString(){
        return searchValues;
    }
    
    @Override
    public int hashCode(){
        if(hashCode==0){
            int result = 17;
            result=(result<<5)-result+columns.hashCode();
            result=(result<<5)-result+pageIndex;
            result=(result<<5)-result+order.hashCode();
            result=(result<<5)-result+pageSize;
            result=(result<<5)-result+searchValues.hashCode();
            hashCode = result;
        }
        return hashCode;
    }
    
    
    @Override
    public boolean equals(Object o){
        return hashCode()==o.hashCode();
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
}