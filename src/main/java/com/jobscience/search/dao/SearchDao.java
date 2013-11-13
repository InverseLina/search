package com.jobscience.search.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.jobscience.search.CurrentOrgHolder;
import com.jobscience.search.db.DBHelper;
import com.jobscience.search.log.LoggerType;
import com.jobscience.search.log.QueryLogger;

@Singleton
public class SearchDao {

    static private String QUERY_SELECT = "select distinct ";

    static private String QUERY_COUNT  = "select count (distinct a.id) ";

    private Logger log = LoggerFactory.getLogger(SearchDao.class);
    
    @Inject
    private DBHelper      dbHelper;

    @Inject
    private CurrentOrgHolder orgHolder;
    
    @Inject
    private ConfigManager configManager;
    
    @Inject
    private UserDao userDao;
    
    @Inject
    private QueryLogger queryLogger;
    /**
     * @param searchColumns
     * @param searchValues
     * @param pageIdx
     * @param pageSize
     * @param orderCon
     * @return
     */
    public SearchResult search(String searchColumns,Map<String, String> searchValues,
    		Integer pageIdx, Integer pageSize,String orderCon) {
        Connection con = dbHelper.openPublicConnection();
        //builder statements
        SearchStatements statementAndValues = 
        		buildSearchStatements(con,searchColumns,searchValues, pageIdx, pageSize,orderCon);
        //excute query and caculate times
        long start = System.currentTimeMillis();
        List<Map> result = dbHelper.preparedStatementExecuteQuery(statementAndValues.queryStmt, statementAndValues.values);
        long mid = System.currentTimeMillis();
        int count = dbHelper.preparedStatementExecuteCount(statementAndValues.countStmt, statementAndValues.values);
        long end = System.currentTimeMillis();

        queryLogger.debug(LoggerType.SEARCH_PERF,mid - start);
        queryLogger.debug(LoggerType.SEARCH_COUNT_PERF,end - mid);
        
        SearchResult searchResult = new SearchResult(result, count)
        							.setDuration(end - start)
        							.setSelectDuration(mid - start)
        							.setCountDuration(end - mid);
        try {
            statementAndValues.countStmt.close();
            statementAndValues.queryStmt.close();
            con.close();
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
        searchResult.setPageIdx(pageIdx);
        searchResult.setPageSize(pageSize);
        return searchResult;
    }
    
    /**
     * Get the auto complete data
     * @param searchValues the search parameters
     * @param type  the available value is  company,education,skill and location
     * @param queryString the value which user typed in auto complete box
     * @param orderByCount true or false
     * @param min  the min year or min rating or min radius
     * @param pageSize 
     * @param pageNum
     * @return
     * @throws SQLException
     */
    public SearchResult getGroupValuesForAdvanced(Map<String, String> searchValues, String type,String queryString,Boolean orderByCount,String min,Integer pageSize,Integer pageNum) throws SQLException {
        return simpleAutoComplete(searchValues, type, queryString, orderByCount, min, pageSize, pageNum);
    }
    public SearchResult simpleAutoComplete(Map<String, String> searchValues, String type,String queryString,Boolean orderByCount,String min,Integer pageSize,Integer pageNum) throws SQLException {

        String baseTable = "";
        if(type.equals("company")){
            baseTable =  "ex_grouped_employers ";
        }else if(type.equals("education")){
            baseTable =  "ex_grouped_educations ";
        }else if(type.equals("skill")){
            baseTable =  "ex_grouped_skills ";
        }else if(type.equals("location")){
            baseTable =  "ex_grouped_locations ";
        }

        StringBuilder querySql = new StringBuilder(" select count,name from " + baseTable + " ");
        if(queryString!=null&&queryString.trim().length()>0){
            querySql.append(" where name ilike '"+ queryString+"%'");
        }
        querySql.append(" order by count desc limit 7 ");
        Long start = System.currentTimeMillis();
        Connection con = dbHelper.openPublicConnection();
        PreparedStatement prepareStatement =   dbHelper.prepareStatement(con,querySql.toString());
        List<Map> result = dbHelper.preparedStatementExecuteQuery(prepareStatement);
        prepareStatement.close();
        con.close();
        Long end = System.currentTimeMillis();
        //log for performance


        queryLogger.debug(LoggerType.SEARCH_SQL, querySql);
        queryLogger.debug(LoggerType.AUTO_PERF, end-start);
        SearchResult searchResult = new SearchResult(result, result.size());
        searchResult.setDuration(end - start);
        searchResult.setSelectDuration(searchResult.getDuration());
        return searchResult;
    }
    
    /**
     * Render condition for auto complete and search
     * @param searchValues
     * @param type available value is advanced or search
     * @param baseTable
     * @param baseTableIns
     * @param values
     * @param appendJoinTable
     * @return
     */
    private String[] renderSearchCondition(Map<String, String> searchValues,String type,String baseTable,String baseTableIns,List values,  String appendJoinTable ){
    	StringBuilder joinTables = new StringBuilder();
        StringBuilder searchConditions = new StringBuilder();
        StringBuilder querySql = new StringBuilder();
    	StringBuilder conditions = new StringBuilder();
    	List subValues = new ArrayList();
    	boolean advanced = "advanced".equals(type);
    	boolean hasCondition = false;
    	String schemaname = orgHolder.getSchemaName();
    	StringBuilder contactQuery = new StringBuilder();
    	StringBuilder contactExQuery = new StringBuilder();
    	StringBuilder prefixSql = new StringBuilder("");
    	String contactQueryCondition="",contactExQueryCondition="";
    	boolean hasSearchValue = false;//to check if the search box has value or not
    	 StringBuilder labelSql = new StringBuilder();
    	if(baseTable==null){
    		baseTable = "";
    	}
    	if(baseTableIns==null){
    		baseTableIns = "";
    	}
	    if (searchValues != null) {
           // for all search mode, we preform the same condition
           String search = searchValues.get("search");
           if (!Strings.isNullOrEmpty(search)) {
        	   if(advanced){
        		   if(baseTableIns.indexOf("z")>-1){//if joined zipcode_us table
       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns );
       					joinTables.append(" on "+ baseTableIns+".\"zip\" = a.\"mailingpostalcode\" ");
       				}else{
       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns);
       					joinTables.append(" on "+ baseTableIns+".\"ts2__contact__c\" = a.\"sfid\" ");
       				}
        		   baseTable =  schemaname+".contact " ;
                   baseTableIns = "a";
                   searchConditions.append(getSearchValueJoinTable(search, values,"a"));
        	   }else{
        	      /* String[] sqls = getSearchValueJoinTable(search, values);
        	       contactExQuery.append(sqls[0]);
        	       contactQuery.append(sqls[1]);
        	       contactExQueryCondition = sqls[2];
        		   contactQueryCondition = sqls[3];
        		  */
        	       hasSearchValue = true;
        	       contactQuery.append(getSearchValueJoinTable(search, values,"contact"));
        	   }
        	   hasCondition = true;
           }
           
           //Get the label parameters and render them
           String label = searchValues.get("label");
           String labelAssigned = searchValues.get("labelAssigned");
           String userId = userDao.getCurrentUser().get("id").toString();
           if(userId==null){
               userId = "1";
           }
           if(label==null){
               label="Favorites";
           }
           if(label!=null){
               if(advanced){
                   if(!"true".equals(labelAssigned)){
                       joinTables.append(" left ");
                   }
                   joinTables.append(" join (select \"label_id\",\"contact_id\" from "+schemaname+".label_contact ")
                   		     .append(" join "+schemaname+".\"label\" on label.\"id\"=label_contact.\"label_id\"")
                   		     .append(" and \"user_id\"=")
                   		     .append(userId)
               		         .append(" and label.\"name\" ='")
                             .append(label)
                             .append("' ) labelcontact on labelcontact.\"contact_id\" = a.\"id\" ");
                           
               }else{
                   if(!"true".equals(labelAssigned)){
                       labelSql.append(" left ");
                   }else{
                       hasCondition = true;
                   }
                   labelSql.append(" join (select \"label_id\",\"contact_id\" from "+schemaname+".label_contact ")
                           .append(" join "+schemaname+".\"label\" on label.\"id\"=label_contact.\"label_id\"")
                           .append(" and \"user_id\"=")
                           .append(userId)
                           .append(" and label.\"name\" ='")
                           .append(label)
                           .append("' ) labelcontact on labelcontact.\"contact_id\" = %s.\"id\" ");
               }
             
           }
           
       	   //Get the contacts parameters and render them
           JSONArray contacts = JSONArray.fromObject(searchValues.get("contacts"));
           if(contacts.size()>0){//First add 1!=1,cause for all contacts,would do with "OR"
        	   conditions.append(" AND (1!=1 ");
           }
           for(int i=0,j=contacts.size();i<j;i++){
        	   JSONObject contact = JSONObject.fromObject(contacts.get(i));
        	   conditions.append(" OR (1=1 ");//for single contact,would do with "AND"
        	   String value ;
        	   //handle for first name
        	   if(contact.containsKey("firstName")&&!"".equals(contact.getString("firstName"))){
        		  value = contact.getString("firstName");
        		  if(advanced){
	                   if(baseTable.indexOf("contact") ==-1){
	   	                	 if(baseTableIns.indexOf("z")>-1){
	 	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns  );
	 	       					joinTables.append(" on "+ baseTableIns+".\"zip\" = a.\"mailingpostalcode\" ");
	 	       				}else{
	 	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns);
	 	       					joinTables.append(" on "+ baseTableIns+".\"ts2__contact__c\" = a.\"sfid\" ");
	 	       				}
	   	                	baseTable =  schemaname+".contact " ;
	        	            baseTableIns = "a";
	                    }
	                   conditions.append(" and a.\"firstname\" ilike ? ");
	                   if(!value.contains("%")){
	                       value =  value + "%";
	                   }
	                   values.add(value);
                  }else{
            		  conditions.append("  and contact.\"firstname\" ilike ? ");
            		  if(!value.contains("%")){
            			  value = value+"%";	
              		  }
                      subValues.add(value);
                  }
                   hasCondition = true;
        	  }
        	   
        	  //handle for last name 
        	  if(contact.containsKey("lastName")&&!"".equals(contact.getString("lastName"))){
        		  value = contact.getString("lastName");
        		  if(advanced){
	                   if(baseTable.indexOf("contact") ==-1){
		                	 if(baseTableIns.indexOf("z")>-1){
		 	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns );
		       						joinTables.append(" on "+ baseTableIns+".\"zip\" = a.\"mailingpostalcode\" ");
		                	 }else{
		 	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns );
		 	       					joinTables.append(" on "+ baseTableIns+".\"ts2__contact__c\" = a.\"sfid\" ");
		 	       			 }
		    	            baseTable =  schemaname+".contact " ;
		    	            baseTableIns = "a";
	                    }
	                   conditions.append(" and a.\"lastname\" ilike ? ");
	                   if(!value.contains("%")){
	                       value =  value + "%";
	                   }
	                   values.add(value);
                  }else{
            		  conditions.append("  and contact.\"lastname\" ilike ? ");
            		  if(!value.contains("%")){
            			  value = value+"%";	
              		  }
                      subValues.add(value);
                  }
                  hasCondition = true;
        	  }
        	  
        	  //handle for email
        	  if(contact.containsKey("email")&&!"".equals(contact.getString("email"))){
        		  value = contact.getString("email");
        		  if(advanced){
                     if(baseTable.indexOf("contact") ==-1){
                    	 if(baseTableIns.indexOf("z")>-1){
	 	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns );
	       						joinTables.append(" on "+ baseTableIns+".\"zip\" = a.\"mailingpostalcode\" ");
	                	 }else{
	 	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns );
	 	       					joinTables.append(" on "+ baseTableIns+".\"ts2__contact__c\" = a.\"sfid\" ");
	 	       			 }
                	 	 baseTable =  schemaname+".contact " ;
                         baseTableIns = "a";
                     }
                     conditions.append(" and a.\"email\" ilike ? ");
                     if(!value.contains("%")){
                       value = value + "%";
                     }
                     values.add(value);
                  }else{
            		  conditions.append("  and contact.\"email\" ilike ? ");
            		  if(!value.contains("%")){
            			  value = value+"%";	
              		  }
                      subValues.add(value);
                  }
        		  hasCondition = true;
        	  }
        	  
        	  //handle the title
        	  if(contact.containsKey("title")&&!"".equals(contact.getString("title"))){
        		  value = contact.getString("title");
        		  if(advanced){
  	                   if(baseTable.indexOf("contact") ==-1){
	  	                	 if(baseTableIns.indexOf("z")>-1){
		 	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns );
		       						joinTables.append(" on "+ baseTableIns+".\"zip\" = a.\"mailingpostalcode\" ");
		                	 }else{
		 	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns );
		 	       					joinTables.append(" on "+ baseTableIns+".\"ts2__contact__c\" = a.\"sfid\" ");
		 	       			 }
      	                     baseTable =  schemaname+".contact " ;
                             baseTableIns = "a";
  	                   }
  	                   conditions.append(" and a.\"title\" ilike ? ");
  	                   if(!value.contains("%")){
  	                       value =  value + "%";
  	                   }
  	                   values.add(value);
                 }else{
            		  conditions.append("  and contact.\"title\" ilike ? ");
            		  if(!value.contains("%")){
            			  value = value+"%";	
              		  }
                      subValues.add(value);
                 }
                  hasCondition = true;
        	  }
        	  conditions.append(" ) ");
           }
           
           if(contacts.size()>0)
           conditions.append(" ) ");
     
           // add the 'educations' filter, and join ts2__education_history__c table
           if (searchValues.get("educations") != null && !"".equals(searchValues.get("educations"))) {
        	   String value = searchValues.get("educations");
        	   JSONArray educationValues = JSONArray.fromObject(value);
        	   if(educationValues!=null){
        		   if(advanced){
                       if(baseTable.indexOf("ts2__education_history__c") == -1 && joinTables.indexOf("ts2__education_history__c") == -1){
                    	   joinTables.append( " inner join    "+schemaname+".ts2__education_history__c d on " );
                           joinTables.append("a.\"sfid\" = d.\"ts2__contact__c\" ");
                       }
                       conditions.append(getConditionForThirdNames(educationValues, "education",false));
            	   }else{
            	       if(prefixSql.length()==0){
                           prefixSql.append(" with ");
                       }else{
                           prefixSql.append(",");
                       }
            	       prefixSql.append( " ed1 as (select ed.\"ts2__contact__c\" from  "+schemaname+".ts2__education_history__c ed where (1!=1 " );
            		   for(int i=0,j=educationValues.size();i<j;i++){
            			   JSONObject educationValue = JSONObject.fromObject(educationValues.get(i));
            			   prefixSql.append(" OR ( ed.\"ts2__name__c\" = ")
            			   		   .append("'"+educationValue.get("name").toString().replaceAll("\'", "\'\'")+"'");
            			   if(educationValue.containsKey("minYears")){
            				   Integer minYears = educationValue.getInt("minYears");
            				   if(!minYears.equals(0)){
            				       prefixSql.append(" AND EXTRACT(year from age(now(),ed.\"ts2__graduationdate__c\"))>="+minYears);
            				   }
            			   }
            			   prefixSql.append(" ) ");		   
            		   }
            		   prefixSql.append(") ) ");
            		   if(hasSearchValue){
            		       contactQuery.append(" inner join  ed1 on con.\"sfid\" = ed1.\"ts2__contact__c\" ");
            		   }else{
            		       contactQuery.append(" inner join  ed1 on contact.\"sfid\" = ed1.\"ts2__contact__c\" ");
            		   }
            	   }
                   hasCondition = true;
        	   }
           }
           // add the 'companies' filter, and join ts2__employment_history__c table
           if (searchValues.get("companies") != null && !"".equals(searchValues.get("companies"))) {
               String value = searchValues.get("companies");
        	   JSONArray companyValues = JSONArray.fromObject(value);
        	   if (companyValues!=null){
            		   if(advanced){
	            		   if(baseTable.indexOf("ts2__employment_history__c") == -1 && joinTables.indexOf("ts2__employment_history__c") == -1){
	                           joinTables.append( " inner join  "+schemaname+".ts2__employment_history__c c on a.\"sfid\" =c.\"ts2__contact__c\" " );
	                       }
	            		  conditions.append(getConditionForThirdNames(companyValues, "company",false));
	            	   }else{
	            	       if(prefixSql.length()==0){
                               prefixSql.append(" with ");
                           }else{
                               prefixSql.append(",");
                           }
	            	       prefixSql.append( " em1 as (select em.\"ts2__contact__c\" as ts2__contact__c,em.\"ts2__job_title__c\" as ts2__job_title__c from   "+schemaname+".ts2__employment_history__c em where (1!=1  " );
	            		   for(int i=0,j=companyValues.size();i<j;i++){
	            			   JSONObject educationValue = JSONObject.fromObject(companyValues.get(i));
	            			   prefixSql.append(" OR ( em.\"ts2__name__c\" = ")
	            			   		   .append("'"+educationValue.get("name").toString().replaceAll("\'", "\'\'")+"'");
	            			   if(educationValue.containsKey("minYears")){
	            				   Integer minYears = educationValue.getInt("minYears");
	            				   if(!minYears.equals(0)){
	            				       prefixSql.append(" AND EXTRACT(year from age(em.\"ts2__employment_end_date__c\",em.\"ts2__employment_start_date__c\"))>="+minYears);
	            				   }
	            			   }
	            			   prefixSql.append(" ) ");		   
	            		   }
	            		   prefixSql.append(" ) ) ");
            		       if(hasSearchValue){
            		           contactQuery.append(" join em1 on con.\"sfid\" = em1.\"ts2__contact__c\"");
                           }else{
                               contactQuery.append(" join em1 on contact.\"sfid\" = em1.\"ts2__contact__c\"");
                           }
	            	   }
            		   hasCondition = true;
        	   }
           }
           
           // add the 'skillNames' filter, and join ts2__skill__c table
           if (searchValues.get("skills") != null && !"".equals(searchValues.get("skills"))) {
        	   
        	   //Get the skill_assessment_rating for current org,if true,will join with ts2__assessment__c
        	   Map m = configManager.getConfig("skill_assessment_rating");
         	   boolean skill_assessment_rating = false;
         	   if(m==null||!("true".equals((String)m.get("value")))){
         		   skill_assessment_rating = false;
         	   }else{
         		   skill_assessment_rating = true;
         	   }
         	   
        	   String value = searchValues.get("skills");
        	   JSONArray skillValues = JSONArray.fromObject(value);
        	   if(skillValues!=null){
            	   if(advanced){
            		   if(baseTable.indexOf("ts2__skill__c") == -1 && joinTables.indexOf("ts2__skill__c") == -1){
            			   joinTables.append( " inner join  "+schemaname+".ts2__skill__c b on " );
            			   joinTables.append("a.\"sfid\" = b.\"ts2__contact__c\" ");
                       }
            		   if(skill_assessment_rating){
            			   joinTables.append(" inner join "+schemaname+".ts2__assessment__c ass on ass.\"ts2__skill__c\"=b.\"sfid\" ");
            		   }
            		   conditions.append(getConditionForThirdNames(skillValues, "skill",skill_assessment_rating));
            	   }else{
            		   if(skill_assessment_rating){//join with the ts2__assessment__c
            		       if(prefixSql.length()==0){
            		           prefixSql.append(" with ");
            		       }else{
            		           prefixSql.append(",");
            		       }
            		       prefixSql.append( "  sk1 as (select sk.\"ts2__contact__c\" as ts2__contact__c from   "+schemaname+".ts2__skill__c sk " );
            			   		   
	            		   if(value.contains("minYears")){
	            		       prefixSql.append(" inner join "+schemaname+".ts2__assessment__c ass on ass.\"ts2__skill__c\"=sk.\"sfid\" ");
	            		   }
	            		   
	            		   prefixSql.append("  where (1!=1  ");
            			   for(int i=0,j=skillValues.size();i<j;i++){
	            			   JSONObject skillValue = JSONObject.fromObject(skillValues.get(i));
	            			   prefixSql.append(" OR ( sk.\"ts2__skill_name__c\" = ")
	            			   		   .append("'"+skillValue.get("name").toString().replaceAll("\'", "\'\'")+"'");
	            			   if(skillValue.containsKey("minYears")){
	            				   Integer minYears = skillValue.getInt("minYears");
	            				   if(!minYears.equals(0)){
	            				       prefixSql.append(" AND ass.\"ts2__rating__c\" >="+minYears);
	            				   }
	            			   }
	            			   prefixSql.append(" ) ");		   
	            		   }
            			   prefixSql.append(" ))  ");
        			       if(hasSearchValue){
        			           contactQuery.append(" join sk1 on con.\"sfid\" = sk1.\"ts2__contact__c\"");
                           }else{
                               contactQuery.append(" join sk1 on contact.\"sfid\" = sk1.\"ts2__contact__c\"");
                           }
            		   }else{// just join with the ts2__skill__c
            		       if(prefixSql.length()==0){
                               prefixSql.append(" with ");
                           }else{
                               prefixSql.append(",");
                           }
            		       prefixSql.append( " sk1 as (select sk.\"ts2__contact__c\" as ts2__contact__c  from   "+schemaname+".ts2__skill__c sk where (1!=1  " );
	            		   for(int i=0,j=skillValues.size();i<j;i++){
	            			   JSONObject skillValue = JSONObject.fromObject(skillValues.get(i));
	            			   prefixSql.append(" OR ( sk.\"ts2__skill_name__c\" = ")
	            			   		   .append("'"+skillValue.get("name").toString().replaceAll("\'", "\'\'")+"'");
	            			   if(skillValue.containsKey("minYears")){
	            				   Integer minYears = skillValue.getInt("minYears");
	            				   if(!minYears.equals(0)){
	            				       prefixSql.append(" AND sk.\"ts2__rating__c\" >="+minYears);
	            				   }
	            			   }
	            			   prefixSql.append(" ) ");		   
	            		   }
	            		   prefixSql.append(" )) ");
	            		   if(hasSearchValue){
                               contactQuery.append(" join sk1 on con.\"sfid\" = sk1.\"ts2__contact__c\"");
                           }else{
                               contactQuery.append(" join sk1 on contact.\"sfid\" = sk1.\"ts2__contact__c\"");
                           }
            		   }
            	   }
            	   hasCondition = true;
        	   }
           }
           
           //add the 'radius' filter
           if (searchValues.get("locations") != null && !"".equals(searchValues.get("locations"))) {
        	   String value = searchValues.get("locations");
        	   JSONArray locationValues = JSONArray.fromObject(value);
         	   if(locationValues!=null){
	         	    if(advanced){
                        if(baseTable.indexOf("zipcode_us") == -1 && joinTables.indexOf("zipcode_us") == -1){
                    	    joinTables.append("  join jss_sys.zipcode_us z on ");
                            joinTables.append("a.\"mailingpostalcode\" =z.\"zip\"");
                        }
                        JSONObject ol;
                        conditions.append(" AND (1!=1  ");
                        for (Object location : locationValues) {
                            ol = (JSONObject) location;
                            String name = (String) ol.get("name");
                            
                            conditions.append(" OR ( z.\"city\"='").append(name).append("'");
                             if(ol.containsKey("minRadius")){
            				   double minRadius = ol.getDouble("minRadius");
            				   conditions.append(" AND  earth_distance(ll_to_earth(z.\"latitude\",z.\"longitude\"),ll_to_earth(a.\"ts2__latitude__c\",a.\"ts2__longitude__c\"))<=").append(minRadius*1000);
                             }
                             conditions.append(")");
                        }
                        conditions.append(" ) ");
            	   }else{
            		   querySql.append(" join jss_sys.zipcode_us z on ");
            		   querySql.append(" contact.\"mailingpostalcode\" =z.\"zip\"");
            		   JSONObject ol;
                       conditions.append(" AND (1!=1  ");
                       for (Object location : locationValues) {
                           ol = (JSONObject) location;
                           String name = (String) ol.get("name");
                           conditions.append(" OR ( z.\"city\"='").append(name).append("'");
                            if(ol.containsKey("minRadius")){
            				   double minRadius = ol.getDouble("minRadius");
            				   conditions.append(" AND  earth_distance(ll_to_earth(z.\"latitude\",z.\"longitude\")," )
            				   			 .append(" ll_to_earth(contact.\"ts2__latitude__c\",contact.\"ts2__longitude__c\"))<=")
            				   			 .append(minRadius*1000);
                            }
                            conditions.append(")");
                       }
                       conditions.append(" ) ");
                       hasCondition = true;
            	   }
    	      }
           } 
       }
	    
	   //at last,combine all part to complete sql
	   if(advanced){
		   if(joinTables.indexOf(" contact ")==-1&&!baseTable.contains("contact")){
			   if(joinTables.indexOf(baseTable)==-1){
					if(baseTableIns.indexOf("z")>-1){
						joinTables.append(" inner join "+baseTable+ " "+ baseTableIns );
						joinTables.append(" on "+ baseTableIns+".\"zip\" = a.\"mailingpostalcode\" ");
					}else{
						joinTables.append(" inner join "+baseTable+ " "+ baseTableIns);
						joinTables.append(" on "+ baseTableIns+".\"ts2__contact__c\" = a.\"sfid\" ");
					}
			   }
	           baseTable =   schemaname+".contact " ;
	           baseTableIns = "a";
           }
		   
    	   querySql.append(baseTable);
           querySql.append(baseTableIns);
           querySql.append(searchConditions);
           querySql.append(joinTables);
           if(appendJoinTable!=null){
        	   querySql.append(appendJoinTable);
           }
           querySql.append("  where 1=1 ");
           querySql.append(conditions);
	   }else{
	       if(contactExQuery.length()>0){
	           querySql.append(contactExQuery)
               .append(contactExQueryCondition)
               .append(" UNION ");
	       }
	       querySql.append(contactQuery)
	               .append(contactQueryCondition);
		   values.addAll(subValues);
	   }
	   //if there has no condition,just append 1!=1
	   if(!hasCondition&&!advanced){
	       conditions.append(" and 1!=1 ");
	   }
	   
	   if(!advanced){
	       if(hasSearchValue){
	           querySql=new StringBuilder(" join (").append(querySql);
	       }
	   }
	   
	   return new String[]{querySql.toString(),prefixSql.toString(),conditions.toString(),labelSql.toString()};
    }
    
    /**
     * Get the query column and add group by or join table if needed
     * @param orginalName
     * @param columnJoinTables
     * @param groupBy
     * @return
     */
    private String getQueryColumnName(String orginalName ,List<String> columnJoinTables,StringBuilder groupBy){
        String schemaname = orgHolder.getSchemaName();
        if(orginalName.toLowerCase().equals("name")){
    		return "lower(a.\"name\") as \"lname\"";
    	}else if(orginalName.toLowerCase().equals("id")){
    		return "";
    	}else if(orginalName.toLowerCase().equals("resume")){
            if(groupBy.length()>0){
                groupBy.append(",");
            }
            groupBy.append("a.resume");
            return " a.resume as resume";
    	}else if(orginalName.toLowerCase().equals("email")){
     		if(groupBy.length()>0){
	     			groupBy.append(",");
     		}
     		groupBy.append("a.\"email\"");
    		return " a.\"email\" as email,lower(a.\"email\") as \"lemail\" ";
    	}else if(orginalName.toLowerCase().equals("title")){
    		if(groupBy.length()>0){
    			groupBy.append(",");
    		}
    		groupBy.append("a.\"title\"");
    		return "case   when a.\"title\" is null then '' " +
        			        " else a.\"title\" END title ";
    	}else if(orginalName.toLowerCase().equals("createddate")){
    		if(groupBy.length()>0){
    			groupBy.append(",");
    		}
    		groupBy.append("a.\"createddate\"");
    		return "to_char(a.\"createddate\",'yyyy-mm-dd') as createddate";
    	}else if(orginalName.toLowerCase().equals("company")){
    		//columnJoinTables.add(getAdvancedJoinTable("company"));
    		return "(select  string_agg(distinct c.\"ts2__name__c\",',') " +
    				"from "+schemaname+".ts2__employment_history__c c where  a.\"sfid\" = c.\"ts2__contact__c\" ) as company ";
    	   // return " case when string_agg(distinct c.\"ts2__name__c\",',') is null " +
    		//	   " then '' else string_agg(distinct c.\"ts2__name__c\",',') end  company";
    	}else if(orginalName.toLowerCase().equals("skill")){
    		//columnJoinTables.add(getAdvancedJoinTable("skill"));
    		//return "case when string_agg(distinct b.\"ts2__skill_name__c\",',') is null " +
    		//	   " then '' else string_agg(distinct b.\"ts2__skill_name__c\",',') end  skill";
    	    return " (select  string_agg(b.\"ts2__skill_name__c\",',') " +
    	    		"from "+schemaname+".ts2__skill__c b where a.\"sfid\" = b.\"ts2__contact__c\"  ) as skill";
    	}else if(orginalName.toLowerCase().equals("education")){
    		//columnJoinTables.add(getAdvancedJoinTable("education"));
    		//return " case when string_agg(distinct d.\"ts2__name__c\",',') is null " +
    		//	   " then '' else string_agg(distinct d.\"ts2__name__c\",',') end  education";
    	    return " (select  string_agg(d.\"ts2__name__c\",',') " +
    	    		"from "+schemaname+".ts2__education_history__c d  where a.\"sfid\" = d.\"ts2__contact__c\"   ) as education ";
    	}else if(orginalName.toLowerCase().equals("location")){
    		columnJoinTables.add(getAdvancedJoinTable("location"));
    		 if(groupBy.length()>0){
                 groupBy.append(",");
             }
             groupBy.append("z.\"city\"");
    		return "  z.\"city\" as location ";
    	}
    	return orginalName;
    }
    
    /**
     * get the search columns for outer sql block
     * @param searchColumns
     * @return
     */
    private String getSearchColumnsForOuter(String searchColumns){
    	StringBuilder sb = new StringBuilder();
    	if(searchColumns==null){
    		sb.append("id,name,lower(name) as \"lname\",email,lower(\"email\") as \"lemail\"lower(title) as \"ltitle\",title ,createddate");
    	}else{
	    	for(String column:searchColumns.split(",")){
		    	if(column.toLowerCase().equals("name")){
		    		sb.append("lower(name) as \"lname\",");
		    	}else if(column.toLowerCase().equals("title")){
		    		sb.append("title,lower(title) as \"ltitle\",");
		    	}else if(column.toLowerCase().equals("email")){
		    		sb.append( " email ,lower(email) as \"lemail\",");
		    	}else if(column.toLowerCase().equals("createddate")){
		    		sb.append("createddate as \"createddate\",");
		    	}else if(column.toLowerCase().equals("company")){
		    		sb.append("company,lower(company) as \"lcompany\",");
		    	}else if(column.toLowerCase().equals("skill")){
		    		sb.append("skill,lower(skill) as \"lskill\",");
		    	}else if(column.toLowerCase().equals("education")){
		    		sb.append("education,lower(education) as \"leducation\",");
		    	}else if(column.toLowerCase().equals("resume")){
		    		sb.append("resume,");
		    	}else if(column.toLowerCase().equals("location")){
		    		sb.append("location as \"location\",");
		    	}else if(column.toLowerCase().equals("contact")){
		    		sb.append("name,lower(name) as \"lname\",");
		    		sb.append("title,lower(title) as \"ltitle\",");
		    		sb.append( " email ,lower(email) as \"lemail\",");
		    	}
	    	}
	    	sb.append("id,name");//make id and name always return
    	}
    	sb.append(",sfid,haslabel");//,phone
        return sb.toString();
    }
    
    /**
     * get search columns for inner sql block
     * @param searchColumns
     * @param columnJoinTables
     * @param groupBy
     * @return
     */
    private String getSearchColumns(String searchColumns,List columnJoinTables,StringBuilder groupBy){
    	 StringBuilder columnsSql = new StringBuilder();
    	 if(searchColumns==null){//a.phone,
             columnsSql.append("a.sfid,  a.\"id\" as id,a.\"name\" as name,lower(a.\"name\") as lname,case   when a.\"title\" is null then ''  else a.\"title\" end title ,to_char(a.\"createddate\",'yyyy-mm-dd') as createddate");
             //,a.phone
             groupBy.append(",a.sfid, a.\"name\",a.\"title\",a.\"createddate\",a.\"haslabel\" ");//
    	 }else{
    		 String temp = "";
 	        for(String column:searchColumns.split(",")){
 	        	temp = getQueryColumnName(column,columnJoinTables,groupBy);
 	        	if(!temp.trim().equals("")){
	 	            columnsSql.append(temp);
	 	            columnsSql.append(",");
 	        	}
 	        }
 	        columnsSql.append("a.id,a.name,a.sfid,a.haslabel");//,a.phone,
 	        if(groupBy.length()>0){
 	        	groupBy.append(",");
 	        }
 	        //a.phone,
 	        groupBy.append("a.name,a.sfid,a.haslabel");//always return these columns ,
         }
    	 return columnsSql.toString();
    }
    
    /**
     * @param searchValues
     * @return SearchStatements
     */
    private SearchStatements buildSearchStatements(Connection con,String searchColumns, Map<String, String> searchValues,
                                                   Integer pageIdx, Integer pageSize,String orderCon) {
        SearchStatements ss = new SearchStatements();

        if(pageIdx < 1){
            pageIdx = 1;
        }
        int offset = (pageIdx -1) * pageSize;
        String schemaname = orgHolder.getSchemaName();
        
        //the select query  that will query data
        StringBuilder querySql = new StringBuilder();
        //the count query sql that will query the count of data
        StringBuilder countSql = new StringBuilder();
        // the part of query that build join tables sql
        StringBuilder joinTables = new StringBuilder();
        // the part of query that build join tables sql
        List<String> columnJoinTables = new ArrayList<String>();
        // the part of query that build conditions sql
        StringBuilder conditions = new StringBuilder();
        // the part of query that build group by sql
        StringBuilder groupBy= new StringBuilder("a.\"id\"");
        // the params will be put in sql
        List values = new ArrayList();
        String cteSql = "";
        
        querySql.append("select ");
        querySql.append(getSearchColumnsForOuter(searchColumns));
        querySql.append(" from ( ");
        querySql.append(QUERY_SELECT);
        countSql.append(QUERY_COUNT);
        querySql.append(getSearchColumns(searchColumns,columnJoinTables,groupBy));
        
        
        //---------------------- add select columns ----------------------//
        querySql.append(" from ( select  distinct contact.\"mailingpostalcode\",contact.id,contact.\"email\",");
	    //contact.\"phone\",
        querySql.append("contact.\"sfid\",contact.\"name\",contact.\"lastname\"," );
        querySql.append("contact.\"firstname\",contact.\"title\",contact.\"createddate\", " );
        querySql.append("case  when contact.\"ts2__text_resume__c\" is null  or " );
        querySql.append("char_length(contact.\"ts2__text_resume__c\") = 0 then -1  else contact.id end as resume ");
        querySql.append(",case when labelcontact.contact_id is null then false else true end haslabel ");
        //---------------------- /add select columns----------------------//
        
        
        if(orderCon.contains("title")){
        	querySql.append(",case   when contact.\"title\" is null then '' " +
        			        " else lower(contact.\"title\") END \"ltitle\" ");
        }else if(orderCon.contains("name")){
        	querySql.append(",lower(contact.\"name\") as \"lname\" ");
        }else if(orderCon.contains("email")){
        	querySql.append(",lower(contact.\"email\") as \"lemail\" ");
        }
        
        querySql.append( " from  "+schemaname+".contact contact  " );
        //contact.\"phone\",
        countSql.append( " from ( select  contact.\"mailingpostalcode\",contact.id,contact.\"email\",contact.\"sfid\",contact.\"name\",contact.\"lastname\"," +
        		"contact.\"firstname\",contact.\"title\",contact.\"createddate\" from  "+schemaname+".contact contact   " );
       
        
        if(searchValues!=null){
           
            // for all search mode, we preform the same condition
            String search = searchValues.get("search");
            String[] sqls = getCondtion(search, searchValues,values,orderCon,offset,pageSize);
            querySql.append(sqls[0]);
            countSql.append(sqls[1]);
            cteSql=sqls[2];
        }
        
        querySql.append(joinTables);
        countSql.append(joinTables);
        for(String join:columnJoinTables){
        	if(!join.equals("No Join")){
	        	querySql.append(join);
	        	//countSql.append(join);
        	}
        }
        
        querySql.append(conditions);
        countSql.append(conditions);
        if(!"".equals(groupBy.toString())){
	        querySql.append(" group by "+groupBy);
        }
        
        querySql.append(") b ");
    	if(orderCon!=null&&!"".equals(orderCon)){
    		querySql.append(" order by "+orderCon);
    	}
    	if(Pattern.matches("^.*(Company|Skill|Education|location)+.*$", orderCon)){
        	querySql.append(" offset ").append(offset).append(" limit ").append(pageSize);
        }
        if(log.isDebugEnabled()){
            log.debug(querySql.toString());
            log.debug(countSql.toString());
        }
        
        queryLogger.debug(LoggerType.SEARCH_SQL, querySql);
        queryLogger.debug(LoggerType.SEARCH_COUNT_SQL, countSql);
        queryLogger.debug(LoggerType.PARAMS, searchValues);
        // build the statement
        ss.queryStmt = dbHelper.prepareStatement(con,cteSql+" "+querySql.toString());
        ss.countStmt = dbHelper.prepareStatement(con,cteSql+" "+countSql.toString());
        ss.cteStmt =   dbHelper.prepareStatement(con,cteSql);
        ss.values = values.toArray();
        return ss;
    }
    
    /**
     * Get condition for Search logic 
     * @param searchValue the value typed in big search box 
     * @param searchValues all other search parameters
     * @param values 
     * @param orderCon
     * @param offset
     * @param pageSize
     * @return first for query sql,second for query sql
     */
    private String[] getCondtion(String searchValue, Map<String, String> searchValues, List values,String orderCon,
    		Integer offset,Integer pageSize){
    	StringBuilder joinSql = new StringBuilder();
    	StringBuilder countSql = new StringBuilder();
    	String prefixSql = "";
        if(searchValues!=null){
            String[] sqls = renderSearchCondition(searchValues,"search",null,null,values,null);
            String condition = sqls[2];
	        joinSql = new StringBuilder(sqls[0]);
	        prefixSql = sqls[1];
	        countSql = new StringBuilder(joinSql.toString());
	        boolean hasContactsCondition = false;
	        String labelAssigned = searchValues.get("labelAssigned");
	        if(searchValues.get("contacts")!=null){
	            hasContactsCondition = JSONArray.fromObject(searchValues.get("contacts")).size()>0;
	        }
	        if(!Strings.isNullOrEmpty(searchValues.get("search"))){
	            if(!hasContactsCondition&&!"true".equals(labelAssigned)){
	                joinSql.append(" offset "+offset+" limit "+pageSize);
	            }
    	        joinSql.append(") subcontact on contact.id=subcontact.id ").append(String.format(sqls[3],"subcontact"));
    	        countSql.append(" ) subcontact on contact.id=subcontact.id ").append(String.format(sqls[3],"subcontact"));
	        }else{
	            joinSql.append(String.format(sqls[3],"contact"));
                countSql.append(String.format(sqls[3],"contact"));
	        }
	        joinSql.append(" where 1=1 ").append(condition);
	        countSql.append(" where 1=1 ").append(condition);
	        if(!Pattern.matches("^.*(Company|Skill|Education|location)+.*$", orderCon)){
	        	if(orderCon.contains("resume")){
	    			orderCon = orderCon.replace("resume", "id");
	    		}
	        	if(orderCon!=null&&!"".equals(orderCon)){
	        		joinSql.append(" order by "+orderCon);
	        	}
	        	if(hasContactsCondition||"true".equals(labelAssigned)){
                    joinSql.append(" offset "+offset);
                }else{
                    joinSql.append(" offset 0 ");
                }
	        	joinSql.append(" limit ").append(pageSize);
	        }
	       
	        joinSql.append(") a ");
	        countSql.append(") a ");
        }
        return new String[]{joinSql.toString(),countSql.toString(),prefixSql};
    }
    
    /**
     * get the table joined for auto complete by type
     * @param type available value : company,education,skill and location
     * @return
     */
    private String getAdvancedJoinTable(String type){
        StringBuilder joinSql = new StringBuilder();
        String schemaname = orgHolder.getSchemaName();
        
        if(type.equals("company")){
            joinSql.append( " left join  "+schemaname+".ts2__employment_history__c c ");
            joinSql.append(" on a.\"sfid\" = c.\"ts2__contact__c\" and c.\"ts2__name__c\"!='' ");
        }else if(type.equals("education")){
            joinSql.append( " left join  "+schemaname+".ts2__education_history__c d " );
            joinSql.append(" on a.\"sfid\" = d.\"ts2__contact__c\" and d.\"ts2__name__c\"!='' ");
        }else if(type.equals("skill")){
            joinSql.append( " left join  "+schemaname+".ts2__skill__c b " );
            joinSql.append(" on a.\"sfid\" = b.\"ts2__contact__c\" and b.\"ts2__skill_name__c\"!='' ");
        }else if(type.equals("location")){
            joinSql.append(" left join jss_sys.zipcode_us z ");
            joinSql.append(" on a.\"mailingpostalcode\" = z.\"zip\" ");
        }
        return joinSql.toString();
    }
    
    /**
     * handle the table joined for boolean search,mainly for contact table
     * @param searchValue
     * @param values
     * @param alias
     * @return
     */
    private String getSearchValueJoinTable(String searchValue, List values,String alias){
	    StringBuilder joinSql = new StringBuilder();
	    joinSql.append(" select * from (");
	    joinSql.append(booleanSearchHandler(searchValue, null, values));
	    //joinSql.append(")  a_ext on a_ext.id = "+alias+".id ");
	    joinSql.append(")  con ");
	    return joinSql.toString();
    }
    
    
  /*  private String[] getSearchValueJoinTable(String searchValue, List values){
        searchValue = searchValue.replaceAll("[\\(\\)%\\^\\@#~\\*]", "").trim();
        values.add(searchValue);
        values.add(searchValue+"%");
        values.add(searchValue+"%");
        // joinSql.append(" right join (");
        //joinSql.append(booleanSearchHandler(searchValue, null, values));
        //joinSql.append(")  a_ext on a_ext.id = "+alias+".id ");
        return new String[]{"select con.sfid,con.id from   public.contact_ex con  ",
                "select con.sfid,con.id  from contact con ",
                " where con.resume_tsv @@ plainto_tsquery(?) ",
                " where  con.\"title\" ilike ? or  con.\"name\" ilike ?  "};
    }*/
    
    /**
     * boolean search handler for big search box
     * @param searchValue
     * @param type
     * @param values
     * @return
     */
    public  String booleanSearchHandler(String searchValue,String type, List values){
    	String schemaname = orgHolder.getSchemaName();
    	StringBuilder sb = new StringBuilder();
    	searchValue = searchValue.replaceAll("[\\(\\)%\\^\\@#~\\*]", "").trim();

    	//if no search value,just return sql with 1!=1
    	if(searchValue.equals("")){
    		return  " select id,sfid from contact where 1!=1 ";
    	}
    	String temp = "";
    	if(type==null||"OR".equals(type)){//if params split with space or "OR",we do in OR logic
	    	String[] orConditions = searchValue.trim().split("\\s+OR\\s+");
	    	for(int i=0;i<orConditions.length;i++){
	    		String orCondition = orConditions[i];
	    		sb.append("select a_extr"+i+".id,a_extr"+i+".sfid from (");
	    		sb.append(booleanSearchHandler(orCondition, "AND",values));
	    		sb.append(" a_extr"+i+" union ");
	    	}
	    	sb.append( "(select 1 as id,1::character as sfid  from  "+schemaname+".contact where 1!=1)" );
    	}else if("AND".equals(type)){//if params split with AND,we do in AND logic
    		String[] andConditions = searchValue.trim().split("\\s+AND\\s+");
	    	for(int i=0;i<andConditions.length;i++){
	    		String andCondition = andConditions[i];
    			if(i==0){
    				sb.append(" select n_ext0.id as id,n_ext0.sfid as sfid from ");
    			}
	    		sb.append(booleanSearchHandler(andCondition, "NOT",values)+(i));
	    		if(i>0){
	    			sb.append(" on n_ext"+i+".id=n_ext"+(i-1)+".id");
	    		}
	    		sb.append(" join ");
	    	}
	    	sb.append( " (select 1 as id,1::character as sfid  from   "+schemaname+".contact limit 1) last on 1=1) " );
    	}else if("NOT".equals(type)){//if params split with NOT,we do in NOT logic
    		String[] notConditions = searchValue.trim().split("\\s+NOT\\s+");
    		if(notConditions.length==1){
    			sb.append("(");
    		}else{
    			sb.append(" (select n_ext.id,n_ext.sfid from (");
    		}
    		
    		temp = notConditions[0].trim();

			sb.append(" select ex.id,ex.sfid from   "+schemaname+".contact_ex ex where ex.resume_tsv @@ plainto_tsquery(?)  OR ex.contact_tsv @@ plainto_tsquery(?) " 
			   /* + " union "
			        //" select a_copy.id as id from   "+schemaname+".contact a_copy right join (select ex.id from   "+schemaname+".contact_ex ex where ex.resume_tsv @@ plainto_tsquery(?)) b on a_copy.id = b.id " + " union "
                + " select a_copy1.id as id,a_copy1.sfid as sfid from   "+schemaname+".contact a_copy1 "
                + " where a_copy1.\"title\" ilike ? or  a_copy1.\"name\" ilike ? " */ );
    		
			if(notConditions.length==1){
    			sb.append(") n_ext");
    		}else{
    			sb.append(") n_ext");
    		}
    		
    		values.add(temp);
    		values.add(temp);
    		/*if(!temp.contains("%")){
    			temp = temp+"%";	
    		}
    		values.add(temp);
    		values.add(temp);*/
    		boolean hasNot = false;
	    	for(int i=1;i<notConditions.length;i++){
	    		hasNot = true;
	    		temp = notConditions[i].trim();
	    		sb.append(" except ");
                        
	    		sb.append("  (select ex.id,ex.sfid from  "+schemaname+".contact_ex ex where ex.resume_tsv @@ plainto_tsquery(?) OR ex.contact_tsv @@ plainto_tsquery(?) " + " ) "
                     /*   + " (select a_copy1.id as id,a_copy1.sfid as sfid from  "+schemaname+".contact a_copy1 "
                        + " where a_copy1.\"title\"  ilike ? or  a_copy1.\"name\"  ilike ? ) ) "*/);
	    		values.add(temp);
	    		values.add(temp);
	    		/*if(!temp.contains("%")){
	    			values.add(temp+"%");
		    		values.add(temp+"%");
	    		}else{
	    			values.add(temp);
		    		values.add(temp);
	    		}*/
	    	}
	    	if(hasNot){
	    		sb.append(")n_ext");
	    	}
    	}
    	
    	return sb.toString();
    }
    
    /**
     * Get auto complete data just for name not for count
     * Now use {@link #getGroupValuesForAdvanced(Map, String, String, Boolean, String, Integer, Integer)}
     * instead
     * @param offset
     * @param size
     * @param type
     * @param keyword
     * @param min
     * @return
     * @throws SQLException
     */
    @Deprecated
    public List getTopAdvancedType(Integer offset,Integer size,String type,String keyword,String min) throws SQLException {
        if(size == null||size<8){
            size = 7;
        }
        offset = offset < 0 ? 0 : offset;
        Connection con = dbHelper.openConnection(orgHolder.getOrgName());
        String name = getNameExpr(type);
        String table = getTable(type);
        StringBuilder querySql =new StringBuilder();
        if("location".equals(type)){
        	querySql.append("select city as name from jss_sys.zipcode_us  ");
        	if(keyword!=null&&!"".equals(keyword)){
	        	querySql.append(" where city ilike '"+keyword+ (keyword.length()>2?"%":"")+"' ");
	        }
		    querySql.append(" group by city order by city offset ").append( offset)
		            .append( " limit ") 
		            .append( size); 
        }else{
	        querySql.append(" select a.name, count(a.contact) from ( ")
                    .append( " select e."+name+" as name, e.\"ts2__contact__c\" as contact ")
                    .append( " from "+table+" e  ")
                    .append( " where e."+name+" !='' ");
	        if(min!=null&&!"".equals(min)){
	        	if("company".equals(type)){
	        	    querySql.append(" AND EXTRACT(year from age(e.\"ts2__employment_end_date__c\",e.\"ts2__employment_start_date__c\"))>="+min);
	        	}else if("education".equals("type")){
	        		querySql.append(" AND EXTRACT(year from age(now(),e.\"ts2__graduationdate__c\"))>="+min);
	        	}else if("skill".equals("type")){
	        		querySql.append(" AND e.\"ts2__rating__c\" >=  "+min);
	        	}
	        }
	        if(keyword!=null&&!"".equals(keyword)){
	        	querySql.append(" AND e."+name+" ilike '"+keyword+(keyword.length()>2?"%":"")+ "' ");
	        }
	        querySql.append(" group by e.\"ts2__contact__c\", e."+name+") a  ").
					 append(" group by a.name order by a.name offset " ).
	                 append(offset).
	                 append(" limit ").
	                 append(size);
        }
        PreparedStatement prepareStatement =   dbHelper.prepareStatement(con,querySql.toString());
        List<Map> result = dbHelper.preparedStatementExecuteQuery(prepareStatement);
        prepareStatement.close();
        con.close();
        return result;
    }
    
    /**
     * Get the column name for type 
     * @param type
     * @return
     */
    private String getNameExpr(String type){
        StringBuilder sql = new StringBuilder();
        if(type.equals("company")){
            sql.append("\"ts2__name__c\"");
        }else if(type.equals("education")){
            sql.append("\"ts2__name__c\"");
        }else if(type.equals("skill")){
            sql.append("\"ts2__skill_name__c\"");
        }else if(type.equals("location")){
            sql.append("\"zip\"");
        }
        return sql.toString();
    }
    
    /**
     * Get the table name for type
     * @param type
     * @return
     */
    private String getTable(String type){
        String table = null;
        String schemaname = orgHolder.getSchemaName();
        if(type.equals("company")){
            table =  schemaname+".ts2__employment_history__c";
        }else if(type.equals("education")){
            table = schemaname+".ts2__education_history__c";
        }else if(type.equals("skill")){
            table = "ts2__skill__c";
        }else if(type.equals("location")){
        	table = "jss.zipcode_us";
        }
        if (table.equals("zipcode_us")) {
            table = "jss_sys." + table;
        }
        return table;
    }
    /**
     * used for generate the condition for type of getting auto complete data
     * @param values
     * @param type
     * @param skill_assessment_rating
     * @return
     */
    private String getConditionForThirdNames(JSONArray values, String type,boolean skill_assessment_rating){
        StringBuilder conditions = new StringBuilder();
        String instance = getTableInstance(type);
        String nameExpr = getNameExpr(type);
        conditions.append(" AND (1!=1 ");
        for(int i=0,j=values.size();i<j;i++){
			   JSONObject educationValue = JSONObject.fromObject(values.get(i));
			   conditions.append(" OR ( "+instance+"."+nameExpr+" = ")
			   		     .append("'"+educationValue.get("name").toString().replaceAll("\'", "\'\'")+"' ");
			   if(educationValue.containsKey("minYears")){
				   Integer minYears = educationValue.getInt("minYears");
				   if(!minYears.equals(0)){
					   if(type.equals("education")){
						   conditions.append(" AND EXTRACT(year from age(now(),"+instance+".\"ts2__graduationdate__c\"))>="+minYears);
					   }else if(type.equals("company")){
						   conditions.append(" AND EXTRACT(year from age("+instance+".\"ts2__employment_end_date__c\","+instance+".\"ts2__employment_start_date__c\"))>="+minYears);
					   }else if(type.equals("skill")){
						   if(skill_assessment_rating){
							   conditions.append(" AND ass.\"ts2__rating__c\">="+minYears);
						   }else{
							   conditions.append(" AND "+instance+".\"ts2__rating__c\">="+minYears);
						   }
					   }
				   }
			   }
			   conditions.append(" ) ");		   
		   }
        conditions.append(" ) "); 
        return conditions.toString();
    }
    /**
     * get the table alias name by type
     * @param type
     * @return
     */
    private String getTableInstance(String type){
        String instance = null;
        if(type.equals("company")){
            instance = "c";
        }else if(type.equals("education")){
            instance = "d";
        }else if(type.equals("skill")){
            instance = "b";
        }else if(type.equals("location")){
            instance = "z";
        }
        return instance;
    }
    
}

class SearchStatements {

    PreparedStatement cteStmt;
    PreparedStatement queryStmt;
    PreparedStatement countStmt;
    Object[]          values;

}
