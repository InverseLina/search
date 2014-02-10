package com.jobscience.search.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jasql.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.jobscience.search.log.LoggerType;
import com.jobscience.search.log.QueryLogger;
import com.jobscience.search.searchconfig.ContactFieldType;
import com.jobscience.search.searchconfig.Field;
import com.jobscience.search.searchconfig.Filter;
import com.jobscience.search.searchconfig.FilterField;
import com.jobscience.search.searchconfig.FilterType;
import com.jobscience.search.searchconfig.SearchConfiguration;
import com.jobscience.search.searchconfig.SearchConfigurationManager;

@Singleton
public class SearchDao {

    static private String QUERY_SELECT = "select distinct ";

    static private String QUERY_COUNT  = "select count (distinct a.id) ";

    private Logger log = LoggerFactory.getLogger(SearchDao.class);
    
    @Inject
    private DaoHelper      daoHelper;

    @Inject
    private ConfigManager configManager;
    
    @Inject
    private UserDao userDao;
    
    @Inject
    private QueryLogger queryLogger;
    
    @Inject
    private SearchLogDao searchLogDao;
    
    @Inject
    private SearchConfigurationManager searchConfigurationManager;
    /**
     * @param searchColumns
     * @param searchValues
     * @param pageIdx
     * @param pageSize
     * @param orderCon
     * @return
     */
    public SearchResult search(String searchColumns,Map<String, String> searchValues,
    		Integer pageIdx, Integer pageSize,String orderCon,String searchValuesString,String token,Map org) {
        Runner runner = daoHelper.openDefaultRunner();
        
        //builder statements
        SearchStatements statementAndValues = 
        		buildSearchStatements(searchColumns,searchValues, pageIdx, pageSize,orderCon,org);
        //excute query and caculate times
        long start = System.currentTimeMillis();
        List<Map> result = runner.executeQuery(statementAndValues.querySql, statementAndValues.values);
        long mid = System.currentTimeMillis();
        int count =  runner.executeCount(statementAndValues.countSql, statementAndValues.values);
        long end = System.currentTimeMillis();

        queryLogger.debug(LoggerType.SEARCH_PERF,mid - start);
        queryLogger.debug(LoggerType.SEARCH_COUNT_PERF,end - mid);
        
        Long userId = -1L;
        Map user =  userDao.getUserByToken(token);
        if(user!=null){
            userId=Long.parseLong(user.get("id").toString());
        }
       
        searchLogDao.addSearchLog(searchValuesString, end - mid, mid - start, userId);
        
        SearchResult searchResult = new SearchResult(result, count)
        							.setDuration(end - start)
        							.setSelectDuration(mid - start)
        							.setCountDuration(end - mid);
        runner.close();
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
    public SearchResult getGroupValuesForAdvanced(Map<String, String> searchValues, String type,String queryString,Boolean orderByCount,String min,Integer pageSize,Integer pageNum,Map org) throws SQLException {
        String advancedAutoCompleteStr = configManager.getConfig("advanced_auto_complete", (Integer)org.get("id"));
        Boolean advancedAutoComplete  = false;
        if(advancedAutoCompleteStr!=null){
            advancedAutoComplete= "true".equals(advancedAutoCompleteStr);
        }
        if(!advancedAutoComplete){
            return simpleAutoComplete(searchValues, type, queryString, orderByCount, min, pageSize, pageNum,org);
        }
        return advancedAutoComplete(searchValues, type, queryString, orderByCount, min, pageSize, pageNum,org);
    }
    
    public SearchResult advancedAutoComplete(Map<String, String> searchValues, String type,String queryString,Boolean orderByCount,String min,Integer pageSize,Integer pageNum,Map org) throws SQLException {
        StringBuilder querySql = new StringBuilder();
        StringBuilder groupBy = new StringBuilder();
        String column = null;
        String baseTableIns = null;
        querySql.append("select result.name, count(*) as count from ( select ");
        String baseTable = new String();
        List values = new ArrayList();
        String schemaname = (String)org.get("schemaname");
        
        if(type.equals("company")){
            baseTable = schemaname+".ts2__employment_history__c ";
            baseTableIns = "c";
            column = " c.\"ts2__contact__c\" as id, c.\"ts2__name__c\" as name";
            groupBy.append(" group by c.\"ts2__contact__c\", c.\"ts2__name__c\" ");
        }else if(type.equals("education")){
            baseTableIns = "d";
            baseTable = schemaname+".ts2__education_history__c ";
            groupBy.append(" group by d.\"ts2__contact__c\", d.\"ts2__name__c\" ");
            column = " d.\"ts2__contact__c\" as id, d.\"ts2__name__c\" as name";
        }else if(type.equals("skill")){
            baseTableIns = "b";
            baseTable = schemaname+".ts2__skill__c ";
            groupBy.append(" group by b.\"ts2__contact__c\", b.\"ts2__skill_name__c\" ");
            column = " b.\"ts2__contact__c\" as id, b.\"ts2__skill_name__c\" as name";
        }else if(type.equals("location")){
            baseTableIns = "z";
            baseTable = " jss_sys.zipcode_us ";
            column = " z.\"city\" as name ";
        }
        
        querySql.append(column);
        querySql.append(" from ");
            
        String appendJoinTable = "";
        
        //this flag is used to check if need join with ts2__assessment__c
        boolean skill_assessment_rating = false;
        
        //------- get the skill_assessment_rating config for current org ----------//
        if(min!=null&&!"0".equals(min)&&type.equals("skill")){
           String skillAssessmentRatingStr = configManager.getConfig("skill_assessment_rating", (Integer)org.get("id"));
            if (!"true".equals(skillAssessmentRatingStr)) {
                skill_assessment_rating = false;
            } else {
                skill_assessment_rating = true;
            }
            appendJoinTable=(" inner join "+schemaname+".ts2__assessment__c ass on ass.\"ts2__skill__c\"=b.\"sfid\" ");
        }
        //-------- /get the skill_assessment_rating config for current org ---------//
        
        querySql.append(renderSearchCondition(searchValues,"advanced",baseTable,baseTableIns,values,appendJoinTable,org)[0]);
        
        //if has min year or min raidus or min rating,need do filter for this
        if(min!=null&&!"0".equals(min)){
                if(type.equals("company")){
                         querySql.append("  AND EXTRACT(year from age(c.\"ts2__employment_end_date__c\",c.\"ts2__employment_start_date__c\"))>="+min);
                }else if(type.equals("education")){
                         querySql.append("  AND EXTRACT(year from age(now(),d.\"ts2__graduationdate__c\"))>="+min);
                }else if(type.equals("skill")){
                           if(skill_assessment_rating){
                                   querySql.append("  AND ass.\"ts2__rating__c\" >="+min);
                           }else{
                                   querySql.append("  AND b.\"ts2__rating__c\" >="+min);
                           }
                }else if(type.equals("location")){
                         querySql.append("   AND  public.earth_distance(public.ll_to_earth(z.\"latitude\",z.\"longitude\"),public.ll_to_earth(a.\"ts2__latitude__c\",a.\"ts2__longitude__c\"))/1000<="+min);
                }
        }
        //add group by statement
        if(!"".equals(groupBy.toString())){
            querySql.append(groupBy);
        }

        if(orderByCount){//order by count
            querySql.append(") result where result.name != '' and result.name ilike '"+(queryString.length()>2?"%":"")+queryString.replaceAll("\'", "\'\'")+"%' group by result.name order by result.count desc offset "+(pageNum-1)*pageSize+" limit "+pageSize);
        }else{//order by name
            querySql.append(") result where result.name != '' and result.name ilike '"+(queryString.length()>2?"%":"")+queryString.replaceAll("\'", "\'\'")+"%' group by result.name order by result.name offset "+(pageNum-1)*pageSize+" limit "+pageSize);
        }
        if(log.isDebugEnabled()){
            log.debug(querySql.toString());
        }
        Long start = System.currentTimeMillis();
        Runner runner = daoHelper.openNewOrgRunner((String)org.get("name"));
        List<Map> result =runner.executeQuery(querySql.toString(),values.toArray());
        runner.close();
        Long end = System.currentTimeMillis();

        queryLogger.debug(LoggerType.SEARCH_SQL, querySql);
        queryLogger.debug(LoggerType.AUTO_PERF, end-start);
        SearchResult searchResult = new SearchResult(result, result.size());
        searchResult.setDuration(end - start);
        searchResult.setSelectDuration(searchResult.getDuration());
        return searchResult;
    }
    
    public SearchResult simpleAutoComplete(Map<String, String> searchValues, String type,String queryString,Boolean orderByCount,String min,Integer pageSize,Integer pageNum,Map org) throws SQLException {
        SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.get("name"));
        Filter filter = sc.getFilterByName(type);
        if(filter==null){
             return null;
        }
        FilterType f = filter.getFilterType();
        String baseTable = "";
        if(f==null&&!type.equals("location")){
            baseTable=filter.getFilterField().getTable();
        }else if(f.equals(FilterType.COMPANY)){
            baseTable =  "ex_grouped_employers ";
        }else if(f.equals(FilterType.EDUCATION)){
            baseTable =  "ex_grouped_educations ";
        }else if(f.equals(FilterType.SKILL)){
            baseTable =  "ex_grouped_skills ";
        }else if(f.equals(FilterType.LOCATION)){
            baseTable =  "ex_grouped_locations ";
        }
        
        StringBuilder querySql;
        if(f==null){
            FilterField ff = sc.getFilterByName(type).getFilterField();
            querySql = new StringBuilder(" select count(*) as \"count\","+ff.toString("")+" as name from \"" + baseTable + "\" where 1=1 ");
            if(queryString!=null&&queryString.trim().length()>0){
                querySql.append(" AND  "+ff.getColumn()+" ilike '"+ queryString+"%'");
            }
            querySql.append(" AND ("+ff.getColumn()+"||'')!='' ")
                    .append(" AND ("+ff.getColumn()+"||'')!='null' ")
                    .append(" AND "+ff.getColumn()).append(" is not null   group by "+ff.getColumn());
            
        }else{
             querySql = new StringBuilder(" select count,name from " + baseTable + " where 1=1 ");
            if(queryString!=null&&queryString.trim().length()>0){
                querySql.append(" AND name ilike '"+ queryString+"%'");
            }
        }
        querySql.append(" order by count desc limit 7 ");
        
        Long start = System.currentTimeMillis();
        Runner runner = daoHelper.openNewOrgRunner((String)org.get("name"));
        queryLogger.debug(LoggerType.SEARCH_SQL, querySql);
        List<Map> result =runner.executeQuery(querySql.toString());
        runner.close();
        Long end = System.currentTimeMillis();
        //log for performance

        
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
    private String[] renderSearchCondition(Map<String, String> searchValues,String type,String baseTable,String baseTableIns,List values,  String appendJoinTable ,Map org){
        SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.get("name"));
        StringBuilder joinTables = new StringBuilder();
        StringBuilder searchConditions = new StringBuilder();
        StringBuilder querySql = new StringBuilder();
    	StringBuilder conditions = new StringBuilder();
    	List subValues = new ArrayList();
    	boolean advanced = "advanced".equals(type);
    	boolean hasCondition = false;
    	String schemaname = (String)org.get("schemaname");
    	StringBuilder contactQuery = new StringBuilder();
    	StringBuilder contactExQuery = new StringBuilder();
    	StringBuilder prefixSql = new StringBuilder("");
    	String contactQueryCondition="",contactExQueryCondition="";
    	boolean hasSearchValue = false;//to check if the search box has value or not
    	StringBuilder labelSql = new StringBuilder();
    	StringBuilder locationSql = new StringBuilder();
    	String userlistFeatureStr = configManager.getConfig("jss.feature.userlist",(Integer)org.get("id"));
    	boolean userlistFeature = Boolean.valueOf(userlistFeatureStr);
    	
    	boolean needJoinRecordtype = false;
    	 
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
                   searchConditions.append(getSearchValueJoinTable(search, values,"a",org));
                   hasCondition = true;
        	   }else{
        	       if(search.length()>=3){
        	           contactQuery.append(getSearchValueJoinTable(search, values,"contact",org));
        	           hasSearchValue = true;
            	       hasCondition = true;
        	       }
        	   }
        	 
           }
           
           //Get the label parameters and render them
           String label = searchValues.get("label");
           String labelAssigned = searchValues.get("labelAssigned");
           String sfid = (String) userDao.getCurrentUser().get("sfid");
           if(sfid==null){
               sfid = "1";
           }
           if(label==null){
               label="Favorites";
           }
           if(userlistFeature){
	           if(label!=null){
	               if(advanced){
	                   if(!"true".equals(labelAssigned)){
	                       joinTables.append(" left ");
	                   }
	                   joinTables.append(" join (select label.\"id\",label_contact.\"ts2__r_contact__c\" from "+schemaname+".ts2__s_userlistlink__c label_contact ")
	                   		     .append(" join "+schemaname+".\"ts2__s_userlist__c\" label on label.\"sfid\"=label_contact.\"ts2__r_user_list__c\"")
	                   		     .append(" and \"ownerid\"='")
	                   		     .append(sfid)
	               		         .append("' and label.\"name\" ='")
	                             .append(label)
	                             .append("' ) labelcontact on labelcontact.\"contact_id\" = a.\"sfid\" ");
	                           
	               }else{
	                   if(!"true".equals(labelAssigned)){
	                       labelSql.append(" left ");
	                   }else{
	                       hasCondition = true;
	                   }
	                   labelSql.append(" join (select label.\"id\" as \"id\",label_contact.\"ts2__r_contact__c\" as \"contact_id\" from "+schemaname+".ts2__s_userlistlink__c label_contact ")
	                             .append(" join "+schemaname+".\"ts2__s_userlist__c\" label on label.\"sfid\"=label_contact.\"ts2__r_user_list__c\"")
	                             .append(" and \"ownerid\"='")
	                             .append(sfid)
	                             .append("' and label.\"name\" ='")
	                             .append(label)
	                             .append("' ) labelcontact on labelcontact.\"contact_id\" = %s.\"sfid\" ");
	                   
	               }
	             
	           }
           }
       	   //Get the contacts parameters and render them
            String contactVal = searchValues.get("contacts");
            if (contactVal != null) {
                JSONArray contacts = JSONArray.fromObject(contactVal);
                if (contacts.size() > 0) {//First add 1!=1,cause for all contacts,would do with "OR"
                    conditions.append(" AND (1!=1 ");
                }

                for (int i = 0, j = contacts.size(); i < j; i++) {
                    JSONObject contact = JSONObject.fromObject(contacts.get(i));
                    conditions.append(" OR (1=1 ");//for single contact,would do with "AND"
                    String value;
                    //handle for first name
                    if (contact.containsKey("firstName") && !"".equals(contact.getString("firstName"))) {
                        value = contact.getString("firstName");
                        if (advanced) {
                            if (baseTable.indexOf("contact") == -1) {
                                if (baseTableIns.indexOf("z") > -1) {
                                    joinTables.append(" inner join " + baseTable + " " + baseTableIns);
                                    joinTables.append(" on " + baseTableIns + ".\"zip\" = a.\"mailingpostalcode\" ");
                                } else {
                                    joinTables.append(" inner join " + baseTable + " " + baseTableIns);
                                    joinTables.append(" on " + baseTableIns + ".\"ts2__contact__c\" = a.\"sfid\" ");
                                }
                                baseTable = schemaname + ".contact ";
                                baseTableIns = "a";
                            }
                            conditions.append(" and a.\"firstname\" ilike ? ");
                            if (!value.contains("%")) {
                                value = value + "%";
                            }
                            values.add(value);
                        } else {
                            conditions.append("  and " + sc.getContactField("firstname").toString("contact") + " ilike ? ");
                            if (!value.contains("%")) {
                                value = value + "%";
                            }
                            subValues.add(value);
                        }
                        hasCondition = true;
                    }

                    //handle for last name
                    if (contact.containsKey("lastName") && !"".equals(contact.getString("lastName"))) {
                        value = contact.getString("lastName");
                        if (advanced) {
                            if (baseTable.indexOf("contact") == -1) {
                                if (baseTableIns.indexOf("z") > -1) {
                                    joinTables.append(" inner join " + baseTable + " " + baseTableIns);
                                    joinTables.append(" on " + baseTableIns + ".\"zip\" = a.\"mailingpostalcode\" ");
                                } else {
                                    joinTables.append(" inner join " + baseTable + " " + baseTableIns);
                                    joinTables.append(" on " + baseTableIns + ".\"ts2__contact__c\" = a.\"sfid\" ");
                                }
                                baseTable = schemaname + ".contact ";
                                baseTableIns = "a";
                            }
                            conditions.append(" and a.\"lastname\" ilike ? ");
                            if (!value.contains("%")) {
                                value = value + "%";
                            }
                            values.add(value);
                        } else {
                            conditions.append("  and " + sc.getContactField("lastname").toString("contact") + " ilike ? ");
                            if (!value.contains("%")) {
                                value = value + "%";
                            }
                            subValues.add(value);
                        }
                        hasCondition = true;
                    }

                    //handle for email
                    if (contact.containsKey("email") && !"".equals(contact.getString("email"))) {
                        value = contact.getString("email");
                        if (advanced) {
                            if (baseTable.indexOf("contact") == -1) {
                                if (baseTableIns.indexOf("z") > -1) {
                                    joinTables.append(" inner join " + baseTable + " " + baseTableIns);
                                    joinTables.append(" on " + baseTableIns + ".\"zip\" = a.\"mailingpostalcode\" ");
                                } else {
                                    joinTables.append(" inner join " + baseTable + " " + baseTableIns);
                                    joinTables.append(" on " + baseTableIns + ".\"ts2__contact__c\" = a.\"sfid\" ");
                                }
                                baseTable = schemaname + ".contact ";
                                baseTableIns = "a";
                            }
                            conditions.append(" and a.\"email\" ilike ? ");
                            if (!value.contains("%")) {
                                value = value + "%";
                            }
                            values.add(value);
                        } else {
                            conditions.append("  and " + sc.getContactField("email").toString("contact") + " ilike ? ");
                            if (!value.contains("%")) {
                                value = value + "%";
                            }
                            subValues.add(value);
                        }
                        hasCondition = true;
                    }

                    //handle the title
                    if (contact.containsKey("title") && !"".equals(contact.getString("title"))) {
                        value = contact.getString("title");
                        if (advanced) {
                            if (baseTable.indexOf("contact") == -1) {
                                if (baseTableIns.indexOf("z") > -1) {
                                    joinTables.append(" inner join " + baseTable + " " + baseTableIns);
                                    joinTables.append(" on " + baseTableIns + ".\"zip\" = a.\"mailingpostalcode\" ");
                                } else {
                                    joinTables.append(" inner join " + baseTable + " " + baseTableIns);
                                    joinTables.append(" on " + baseTableIns + ".\"ts2__contact__c\" = a.\"sfid\" ");
                                }
                                baseTable = schemaname + ".contact ";
                                baseTableIns = "a";
                            }
                            conditions.append(" and a.\"title\" ilike ? ");
                            if (!value.contains("%")) {
                                value = value + "%";
                            }
                            values.add(value);
                        } else {
                            conditions.append("  and " + sc.getContactField("title").toString("contact") + " ilike ? ");
                            if (!value.contains("%")) {
                                value = value + "%";
                            }
                            subValues.add(value);
                        }
                        hasCondition = true;
                    }


                    conditions.append(" ) ");
                }

                if (contacts.size() > 0) {
                    conditions.append(" ) ");

                    //handle the ojectType
                    if (searchValues.get("objectType") != null && !"".equals(searchValues.get("objectType"))) {
                        String value = searchValues.get("objectType");
                        if (!advanced) {
                            if ("Both".equals(value)) {
                                conditions.append("  and (rt.\"sobjecttype\" = 'Contact' or rt.\"sobjecttype\" = 'Candidate') ");
                                needJoinRecordtype = true;
                            } else {
                                conditions.append("  and rt.\"sobjecttype\" = ? ");
                                subValues.add(value);
                                needJoinRecordtype = true;
                            }

                        }
                        hasCondition = true;
                    }

                    //handle the status
                    if (searchValues.get("status") != null && !"".equals(searchValues.get("status"))) {
                        String value = searchValues.get("status");
                        if (!advanced) {
                            if ("Active".equals(value)) {
                                conditions.append("  and contact.\"ts2__people_status__c\" = 'Active' ");
                            } else if ("Inactive".equals(value)) {
                                conditions.append("  and contact.\"ts2__people_status__c\" = 'Inactive' ");
                            }
                        }
                        hasCondition = true;
                    }


                }
            }

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
            	       FilterField f = sc.getFilter(FilterType.EDUCATION).getFilterField();
            	       prefixSql.append( " ed1 as (select "+f.toJoinToString("ed")+" as \"ts2__contact__c\" ")
            	                .append(" from  "+schemaname+"."+f.getTable()+" ed where (1!=1 " );
            		   for(int i=0,j=educationValues.size();i<j;i++){
            			   JSONObject educationValue = JSONObject.fromObject(educationValues.get(i));
            			   prefixSql.append(" OR ( "+f.toString("ed")+" = ")
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
            		       contactQuery.append(" inner join  ed1 on "+f.toJoinFromString("con")+" = ed1.\"ts2__contact__c\" ");
            		   }else{
            		       contactQuery.append(" inner join  ed1 on "+f.toJoinFromString("contact")+" = ed1.\"ts2__contact__c\" ");
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
	            	       FilterField f = sc.getFilter(FilterType.COMPANY).getFilterField();
	            	       prefixSql.append( " em1 as (select "+f.toJoinToString("em")+" as ts2__contact__c "
	            	                               + " from   "+schemaname+"."+f.getTable()+" em where (1!=1  " );
	            		   for(int i=0,j=companyValues.size();i<j;i++){
	            			   JSONObject educationValue = JSONObject.fromObject(companyValues.get(i));
	            			   prefixSql.append(" OR ( "+f.toString("em")+" = ")
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
            		           contactQuery.append(" join em1 on "+f.toJoinFromString("con")+" = em1.\"ts2__contact__c\"");
                           }else{
                               contactQuery.append(" join em1 on "+f.toJoinFromString("contact")+" = em1.\"ts2__contact__c\"");
                           }
	            	   }
            		   hasCondition = true;
        	   }
           }
           
           // add the 'skillNames' filter, and join ts2__skill__c table
           if (searchValues.get("skills") != null && !"".equals(searchValues.get("skills"))) {
        	   
        	   //Get the skill_assessment_rating for current org,if true,will join with ts2__assessment__c
        	   String skillAssessmentRatingStr = configManager.getConfig("skill_assessment_rating", (Integer)org.get("id"));
         	   boolean skill_assessment_rating = false;
         	   if(!"true".equals(skillAssessmentRatingStr)){
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
            	       FilterField f = sc.getFilter(FilterType.SKILL).getFilterField();
            		   if(skill_assessment_rating){//join with the ts2__assessment__c
            		       if(prefixSql.length()==0){
            		           prefixSql.append(" with ");
            		       }else{
            		           prefixSql.append(",");
            		       }
            		       prefixSql.append( "  sk1 as (select "+f.toJoinToString("sk")+" as ts2__contact__c from   "+schemaname+"."+f.getTable()+" sk " );
            			   		   
	            		   if(value.contains("minYears")){
	            		       prefixSql.append(" inner join "+schemaname+".ts2__assessment__c ass on ass.\"ts2__skill__c\"=sk.\"sfid\" ");
	            		   }
	            		   
	            		   prefixSql.append("  where (1!=1  ");
            			   for(int i=0,j=skillValues.size();i<j;i++){
	            			   JSONObject skillValue = JSONObject.fromObject(skillValues.get(i));
	            			   prefixSql.append(" OR ( "+f.toString("sk")+" = ")
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
        			           contactQuery.append(" join sk1 on "+f.toJoinFromString("con")+" = sk1.\"ts2__contact__c\"");
                           }else{
                               contactQuery.append(" join sk1 on "+f.toJoinFromString("contact")+" = sk1.\"ts2__contact__c\"");
                           }
            		   }else{// just join with the ts2__skill__c
            		       if(prefixSql.length()==0){
                               prefixSql.append(" with ");
                           }else{
                               prefixSql.append(",");
                           }
            		       prefixSql.append( " sk1 as (select "+ f.toJoinToString("sk") +" as ts2__contact__c  from   "+schemaname+"."+f.getTable()+" sk where (1!=1  " );
	            		   for(int i=0,j=skillValues.size();i<j;i++){
	            			   JSONObject skillValue = JSONObject.fromObject(skillValues.get(i));
	            			   prefixSql.append(" OR ( "+f.toString("sk")+" = ")
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
                               contactQuery.append(" join sk1 on "+f.toJoinFromString("con")+" = sk1.\"ts2__contact__c\"");
                           }else{
                               contactQuery.append(" join sk1 on "+f.toJoinFromString("contact")+" = sk1.\"ts2__contact__c\"");
                           }
            		   }
            	   }
            	   hasCondition = true;
        	   }
           }
           
           //add the 'radius' filter
           if (searchValues.get("locations") != null && !"".equals(searchValues.get("locations"))) {
               Field f = sc.getContactField(ContactFieldType.MAILINGPOSTALCODE);
        	   String value = searchValues.get("locations");
        	   JSONArray locationValues = JSONArray.fromObject(value);
         	   if(locationValues!=null){
	         	    if(advanced){
                        if(baseTable.indexOf("zipcode_us") == -1 && joinTables.indexOf("zipcode_us") == -1){
                    	    joinTables.append("  join jss_sys.zipcode_us z on ");
                            joinTables.append(f.toString("a")+" =z.\"zip\"");
                        }
                        JSONObject ol;
                        conditions.append(" AND (1!=1  ");
                        for (Object location : locationValues) {
                            ol = (JSONObject) location;
                            String name = (String) ol.get("name");
                            
                            conditions.append(" OR ( z.\"city\"='").append(name).append("'");
                             if(ol.containsKey("minRadius")){
            				   double minRadius = ol.getDouble("minRadius");
                               if(minRadius >0) {
            				   conditions.append(" AND  earth_distance(ll_to_earth(z.\"latitude\",z.\"longitude\"),ll_to_earth("+sc.getContactField("ts2__latitude__c").toString("a")
            				       +","+sc.getContactField("ts2__longitude__c").toString("a")+"))<=").append(minRadius*1000);
                               }
                             }
                             conditions.append(")");
                        }
                        conditions.append(" ) ");
            	   }else{
            	       locationSql.append(" join jss_sys.zipcode_us z on ");
            		   JSONObject ol;
            		   locationSql.append("  (1!=1  ");
                       for (Object location : locationValues) {
                           ol = (JSONObject) location;
                           String name = (String) ol.get("name");
                           locationSql.append(" OR ( z.\"city\"='").append(name).append("'");
                           if(ol.containsKey("minRadius")){
            				   double minRadius = ol.getDouble("minRadius");
                               if(minRadius > 0){
            				   locationSql.append(" AND  earth_distance(ll_to_earth(z.\"latitude\",z.\"longitude\")," )
            				   			 .append(" ll_to_earth("+sc.getContactField("ts2__latitude__c").toString("contact")
            				   			     +","+sc.getContactField("ts2__longitude__c").toString("contact")+"))<=")
            				   			 .append(minRadius*1000);
                               }
                            }else{
                                locationSql.append(" AND ").append(f.toString("contact")+" =z.\"zip\"");
                            }
                           locationSql.append(")");
                       }
                       locationSql.append(" ) ");
                       hasCondition = true;
            	   }
    	      }
           } 
       }
	   String contactTable = sc.getContact().getTable();
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
	       
	       for(String name:searchValues.keySet()){
	           if(isNativeSearchParam(name)){
	               continue;
	           }
	           String filterName = name.substring(0, name.length()-1);//remove the s at last, example filters-->filter
	           Filter filter = sc.getFilterByName(filterName);
               if (filter == null) {
                   continue;
               }
               FilterField ff = filter.getFilterField();
	           if(!contactTable.equals(ff.getTable())){
	               JSONArray extraValues = JSONArray.fromObject(searchValues.get(name));
	               if(hasSearchValue){
                       contactQuery.append(" join "+schemaname+".\""+ff.getTable()+"\" on "+ff.toJoinToString("con")+" = "+ff.toJoinFromString("\""+ff.getTable()+"\""));
                   }else{
                       contactQuery.append(" join "+schemaname+".\""+ff.getTable()+"\" on "+ff.toJoinToString("contact")+" = "+ff.toJoinFromString("\""+ff.getTable()+"\""));
                   }
	               if(extraValues.size()>0){
	                   contactQuery.append(" AND (1!=1 ");
	               }
                   for(int i=0,j=extraValues.size();i<j;i++){
                       JSONObject v = JSONObject.fromObject(extraValues.get(i));
                       contactQuery.append(" OR \"").append(ff.getTable()).append("\".")
                                   .append(ff.getColumn()).append("= '").append(v.get("name")).append("' ");
                      // values.add(v.get("name"));
                   }
                   if(extraValues.size()>0){
                       contactQuery.append(" ) ");
                   }
                   hasCondition = true;
	           }
	          
	       }
	       
	       if(contactExQuery.length()>0){
	           querySql.append(contactExQuery)
               .append(contactExQueryCondition)
               .append(" UNION ");
	       }
	       querySql.append(contactQuery)
	               .append(contactQueryCondition);
		   values.addAll(subValues);
	   }
	 
	 
	   

	   for(String name:searchValues.keySet()){
	       if(isNativeSearchParam(name)){
	           continue;
	       }
	       String filterName = name.substring(0, name.length()-1);
	       Filter filter = sc.getFilterByName(filterName);
           if (filter == null) {
               continue;
           }
           FilterField ff = filter.getFilterField();
	       if(contactTable.equals(ff.getTable())){
    	       JSONArray extraValues = JSONArray.fromObject(searchValues.get(name));
    	       if(extraValues.size()>0){
    	           conditions.append(" AND (1!=1 ");
    	           for(int i=0,j=extraValues.size();i<j;i++){
    	               JSONObject value = JSONObject.fromObject(extraValues.get(i));
    	               conditions.append(" OR ").append(filterName).append("= ? ");
    	               values.add(value.get("name"));
    	           }
    	           conditions.append(" ) ");
    	           hasCondition = true;
    	       }
	       }
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
       String recordType = "";
       if(needJoinRecordtype){
           recordType = (" inner join  "+schemaname+".recordtype rt on contact.\"recordtypeid\" = rt.\"sfid\" ");
       }
	   return new String[]{querySql.toString(),prefixSql.toString(),conditions.toString(),labelSql.toString(),locationSql.toString(),recordType};
    }
    
    /**
     * Get the query column and add group by or join table if needed
     * @param orginalName
     * @param columnJoinTables
     * @param groupBy
     * @return
     */
    private String getQueryColumnName(String orginalName ,List<String> columnJoinTables,StringBuilder groupBy,StringBuffer searchedColumns,Map org){
        String schemaname = (String)org.get("schemaname");
        if(searchedColumns.toString().contains(orginalName.toLowerCase()+",")){
            return "";
        }
        searchedColumns.append(orginalName.toLowerCase()).append(",");
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
    		return "(select  string_agg(distinct c.\"ts2__name__c\",',') " +
    				"from "+schemaname+".ts2__employment_history__c c where  a.\"sfid\" = c.\"ts2__contact__c\" ) as company ";
    	}else if(orginalName.toLowerCase().equals("skill")){
    	    return " (select  string_agg(distinct b.\"ts2__skill_name__c\",',') " +
    	    		"from "+schemaname+".ts2__skill__c b where a.\"sfid\" = b.\"ts2__contact__c\"  ) as skill";
    	}else if(orginalName.toLowerCase().equals("education")){
    	    return " (select  string_agg(distinct d.\"ts2__name__c\",',') " +
    	    		"from "+schemaname+".ts2__education_history__c d  where a.\"sfid\" = d.\"ts2__contact__c\"   ) as education ";
    	}else if(orginalName.toLowerCase().equals("location")){
    		columnJoinTables.add(getAdvancedJoinTable("location",org));
    		 if(groupBy.length()>0){
                 groupBy.append(",");
             }
             groupBy.append("z.\"city\"");
    		return "  z.\"city\" as location ";
    	}
        
        SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.get("name"));
        Filter f = sc.getFilterByName(orginalName);
        String tableName = f.getFilterField().getTable();
        String contactTableName = sc.getContact().getTable();
        if(tableName.equals(contactTableName)){
            return f.getFilterField().toString("a")+" as \""+f.getName()+"\"";
        }else{
            FilterField ff = f.getFilterField();
            return " (select  string_agg(distinct d.\""+ff.getColumn()+"\",',') " +
                                    "from "+schemaname+"."+ff.getTable()+" d  where a.\""+ff.getJoinTo()+"\" = d.\""+ff.getJoinFrom()+"\"   ) as \""+f.getName()+"\"";
        }
    	//return orginalName;
    }
    
    /**
     * get the search columns for outer sql block
     * @param searchColumns
     * @return
     */
    private String getSearchColumnsForOuter(String searchColumns,boolean userlistFeature,Map org){
        SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.get("name"));
        StringBuilder sb = new StringBuilder();
    	if(searchColumns==null){
    		sb.append("id,name,lower(name) as \"lname\",email,lower(\"email\") as \"lemail\"lower(title) as \"ltitle\",title ,createddate");
    	}else{
	    	for(String column:searchColumns.split(",")){
	    	    if(sb.indexOf("as \""+column.toLowerCase()+"\"")!=-1||sb.indexOf(column.toLowerCase()+",")!=-1
	    	       ||sb.indexOf("as \"l"+column.toLowerCase()+"\"")!=-1){
	    	        continue;
	    	    }
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
		    	}else{
		    	    Filter filter = sc.getFilterByName(column);
		    	    if(filter!=null){
    		    	    if(sb.indexOf("as "+column)==-1&&sb.indexOf(column+",")==-1){
    		    	        sb.append("\""+filter.getName()+"\"").append(" as \"").append(filter.getName()).append("\",");
    		    	    }
		    	    }
		    	}
	    	}
	    	sb.append("id,name");//make id and name always return
    	}
    	sb.append(",sfid");//,phone
    	if(userlistFeature){
    		sb.append(",haslabel");
    	}
        return sb.toString();
    }
    
    /**
     * get search columns for inner sql block
     * @param searchColumns
     * @param columnJoinTables
     * @param groupBy
     * @return
     */
    private String getSearchColumns(String searchColumns,List columnJoinTables,StringBuilder groupBy,boolean userlistFeature,Map org){
    	 StringBuilder columnsSql = new StringBuilder();
    	 SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.get("name"));
    	 if(searchColumns==null){//a.phone,
             columnsSql.append(sc.toContactFieldsString("a"));
             //,a.phone
             groupBy.append(","+sc.toContactFieldsString("a")+",a.\"haslabel\" ");//
    	 }else{
    		 String temp = "";
    		 StringBuffer sb = new StringBuffer("id,name,sfid,");
    		 if(userlistFeature){
    			 sb.append("haslabel,");
    		 }
 	        for(String column:searchColumns.split(",")){
 	        	temp = getQueryColumnName(column,columnJoinTables,groupBy,sb,org);
 	        	if(!temp.trim().equals("")){
	 	            columnsSql.append(temp);
	 	            columnsSql.append(",");
 	        	}
 	        }
 	        columnsSql.append("a.id,a.name,a.sfid");//,a.phone,
 	        if(userlistFeature){
 	        	columnsSql.append(",a.haslabel");
 	        }
 	        if(groupBy.length()>0){
 	        	groupBy.append(",");
 	        }
 	        //a.phone,
 	        groupBy.append("a.name,a.sfid");//always return these columns ,
 	        if(userlistFeature){
 	    	  groupBy.append(",a.haslabel");
  	        }
         }
    	 return columnsSql.toString();
    }
    
    /**
     * @param searchValues
     * @return SearchStatements
     */
    private SearchStatements buildSearchStatements(String searchColumns, Map<String, String> searchValues,
                                                   Integer pageIdx, Integer pageSize,String orderCon,Map org) {
        SearchStatements ss = new SearchStatements();
        SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.get("name"));
        if(pageIdx < 1){
            pageIdx = 1;
        }
        int offset = (pageIdx -1) * pageSize;
        String schemaname = (String)org.get("schemaname");
        
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
        String search = searchValues.get("search");
        // the params will be put in sql
        List values = new ArrayList();
        String cteSql = "";
        
        //get the userlist feature
        String userlistFeatureStr = configManager.getConfig("jss.feature.userlist",(Integer)org.get("id"));
        boolean userlistFeature = Boolean.valueOf(userlistFeatureStr);
        
        querySql.append("select ");
        querySql.append(getSearchColumnsForOuter(searchColumns,userlistFeature,org));
        querySql.append(" from ( ");
        querySql.append(QUERY_SELECT);
        countSql.append(QUERY_COUNT);
        querySql.append(getSearchColumns(searchColumns,columnJoinTables,groupBy,userlistFeature,org));
        
        
        //---------------------- add select columns ----------------------//
        String contactString = sc.toContactFieldsString("contact");
        String contactTable = sc.getContact().getTable();
        querySql.append(" from ( select  distinct ")
	            .append(contactString);
        for(Filter f:sc.getFilters()){
            if(f.getFilterType()==null&&contactString.indexOf(f.getFilterField().toString("contact"))==-1){
                if(contactTable.equals(f.getFilterField().getTable())){
                    querySql.append(",").append(f.getFilterField().toString("contact"));
                    groupBy.append(",").append(f.getFilterField().toString("a"));
                }
            }
        }
        querySql.append(",case  when ")
                .append(sc.getContactField(ContactFieldType.RESUME).toString("contact"))
                .append(" is null  or " )
                .append("char_length(")
                .append(sc.getContactField(ContactFieldType.RESUME).toString("contact"))
                .append(") = 0 then -1  else contact.id end as resume ");
                if(userlistFeature){
                	querySql.append(",case when labelcontact.contact_id is null then false else true end haslabel ");
                }
        //---------------------- /add select columns----------------------//
        
        
        if(orderCon.contains("title")){
        	querySql.append(",case   when ")
        	        .append(sc.getContactField(ContactFieldType.TITLE).toString("contact"))
        	        .append(" is null then ''  else lower(")
        	        .append(sc.getContactField(ContactFieldType.TITLE).toString("contact"))
        	        .append(") END \"ltitle\" ");
        }else if(orderCon.contains("name")){
        	querySql.append(",lower(")
        	        .append(sc.getContactField(ContactFieldType.NAME).toString("contact")) 
        	        .append(") as \"lname\" ");
        }else if(orderCon.contains("email")){
        	querySql.append(",lower(")
                    .append(sc.getContactField(ContactFieldType.EMAIL).toString("contact")) 
                    .append(") as \"lemail\" ");
        }
        
        querySql.append( " from  "+schemaname+".")
                .append(sc.getContact().getTable())
                .append(" contact  " );
        
        String value = searchValues.get("locations");
        JSONArray locationValues = JSONArray.fromObject(value);
        boolean hasContactsCondition = false;
        if(searchValues.get("contacts")!=null){
            hasContactsCondition = JSONArray.fromObject(searchValues.get("contacts")).size()>0;
        }
        if(hasExtraSearchColumn(searchValues)||hasContactsCondition||(Strings.isNullOrEmpty(search)||search.length()<3)||(value!=null&&locationValues!=null)){
            countSql.append( " from ( select ")
                    .append(sc.toContactFieldsString("contact"))
                    .append(" from  "+schemaname+".")
                    .append(sc.getContact().getTable())
                    .append(" contact   " );
        }
       
        
        if(searchValues!=null){
           
            // for all search mode, we preform the same condition
            String[] sqls = getCondtion(search, searchValues,values,orderCon,offset,pageSize,org);
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
        ss.querySql = cteSql+" "+querySql.toString();
        ss.countSql =cteSql+" "+countSql.toString();
        ss.cteSql =   cteSql;
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
    		Integer offset,Integer pageSize,Map org){
    	StringBuilder joinSql = new StringBuilder();
    	StringBuilder countSql = new StringBuilder();
    	String prefixSql = "";
        if(searchValues!=null){
            String[] sqls = renderSearchCondition(searchValues,"search",null,null,values,null,org);
            String condition = sqls[2];
	        boolean hasContactsCondition = false;
	        String locationSql = sqls[4];
	        joinSql = new StringBuilder(sqls[0]);
	        prefixSql = sqls[1];
	        String labelSql = sqls[3];
	        boolean userlistFeature = (labelSql.length()>0);
	        countSql = new StringBuilder(joinSql.toString());
	        String recordTypeSql = sqls[5];
	        String labelAssigned = searchValues.get("labelAssigned");
	        if(searchValues.get("contacts")!=null){
	            hasContactsCondition = JSONArray.fromObject(searchValues.get("contacts")).size()>0;
	        }
	        if(!Strings.isNullOrEmpty(searchValue)&&searchValue.length()>=3){
	            if(!hasContactsCondition&&userlistFeature&&!"true".equals(labelAssigned)&&locationSql.length()==0&&!hasExtraSearchColumn(searchValues)){
	                joinSql.append(" offset "+offset+" limit "+pageSize);
	            }
    	        joinSql.append(") subcontact on contact.id=subcontact.id ").append(String.format(labelSql,"subcontact"));
    	       if(locationSql.length()==0&&!hasContactsCondition&&!hasExtraSearchColumn(searchValues)){
    	           countSql.replace(0, 6," from ");
    	           countSql.append(" ) a  ");
    	       }else{
    	           countSql.append(" ) subcontact on contact.id=subcontact.id ");
    	           if(userlistFeature){
    	        	   countSql.append(String.format(labelSql,"subcontact"));
    	           }
    	       }
	        }else{
	        	if(userlistFeature){
		            joinSql.append(String.format(labelSql,"contact"));
	                countSql.append(String.format(labelSql,"contact"));
	        	}
	        }
	        joinSql.append(locationSql);
	        countSql.append(locationSql);
	        joinSql.append(recordTypeSql);
	        countSql.append(recordTypeSql);
	        
	        joinSql.append(" where 1=1 ").append(condition);
	        countSql.append(" where 1=1 ").append(condition);
	        if(!Pattern.matches("^.*(Company|Skill|Education|location)+.*$", orderCon)){
	        	if(orderCon.contains("resume")){
	    			orderCon = orderCon.replace("resume", "id");
	    		}
	        	if(orderCon!=null&&!"".equals(orderCon)){
	        		joinSql.append(" order by "+orderCon);
	        	}
	        	if(hasContactsCondition||"true".equals(labelAssigned)||locationSql.length()>0){
                    joinSql.append(" offset "+offset);
                }else{
                    joinSql.append(" offset 0 ");
                }
	        	joinSql.append(" limit ").append(pageSize);
	        }
	       
	        joinSql.append(") a ");
	        if(hasExtraSearchColumn(searchValues)||hasContactsCondition||locationSql.length()>0||Strings.isNullOrEmpty(searchValue)||searchValue.length()<3){
	            countSql.append(") a ");
	        }
        }
        return new String[]{joinSql.toString(),countSql.toString(),prefixSql};
    }
    
    /**
     * get the table joined for auto complete by type
     * @param type available value : company,education,skill and location
     * @return
     */
    private String getAdvancedJoinTable(String type,Map org){
        StringBuilder joinSql = new StringBuilder();
        String schemaname = (String)org.get("schemaname");
        
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
    private String getSearchValueJoinTable(String searchValue, List values,String alias,Map org){
        StringBuilder joinSql = new StringBuilder();
        if("a".equals(alias)){
            joinSql.append(" right join (");
            joinSql.append(booleanSearchHandler(searchValue, null, values,org));
            joinSql.append(")  a_ext on a_ext.id = "+alias+".id ");
            return joinSql.toString();
        }else{
            joinSql.append(" select  distinct con.id,con.sfid  from (");
    	    joinSql.append(booleanSearchHandler(searchValue, null, values,org));
    	    joinSql.append(")  con ");
    	    return joinSql.toString();
	    }
    }
    
    /**
     * boolean search handler for big search box
     * @param searchValue
     * @param type
     * @param values
     * @return
     */
    public  String booleanSearchHandler(String searchValue,String type, List values,Map org){
    	String schemaname = (String)org.get("schemaname");
    	SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.get("name"));
    	StringBuilder sb = new StringBuilder();
    	searchValue = searchValue.replaceAll("[\\(\\)%\\^\\@#~\\*]", "").trim();
    	if(!searchValue.contains("NOT ")&&
    	   !searchValue.contains("AND ")&&
    	   !searchValue.contains("NOT ")){
        	if(!searchValue.matches("^\\s*\"[^\"]+\"\\s*$")){//if there not in quotes,replace space to OR
        	    searchValue = searchValue.replaceAll("\\s+", " OR ");
        	}else{
                searchValue = searchValue.replaceAll("\\\"", "").replaceAll("\\s+", " AND ");
        	}
    	}else{
    	    if(!searchValue.matches("^\\s*\"[^\"]+\"\\s*$")){//if there not in quotes,replace space to OR
                searchValue = searchValue.replaceAll("\\\"", "");
            }
    	}
    	//if no search value,just return sql with 1!=1
    	if(searchValue.equals("")){
    		return  " select id,sfid from "+schemaname+"."+sc.getContact().getTable()+" where 1!=1 ";
    	}
    	String temp = "";
    	if(type==null||"OR".equals(type)){//if params split with space or "OR",we do in OR logic
	    	String[] orConditions = searchValue.trim().split("\\s+OR\\s+");
	    	boolean hasSearch = false;
	    	for(int i=0;i<orConditions.length;i++){
	    		String orCondition = orConditions[i];
	    		sb.append("select a_extr"+i+".id,a_extr"+i+".sfid from (");
	    		sb.append(booleanSearchHandler(orCondition, "AND",values,org));
	    		sb.append(" a_extr"+i+" union ");
	    		hasSearch = true;
	    	}
	    	if(hasSearch){
	    	    sb.delete(sb.length()-6, sb.length());
	    	}
    	}else if("AND".equals(type)){//if params split with AND,we do in AND logic
    		String[] andConditions = searchValue.trim().split("\\s+AND\\s+");
    		boolean hasSearch = false;
	    	for(int i=0;i<andConditions.length;i++){
	    	    hasSearch = true;
	    		String andCondition = andConditions[i];
    			if(i==0){
    				sb.append(" select n_ext0.id as id,n_ext0.sfid as sfid from ");
    			}
	    		sb.append(booleanSearchHandler(andCondition, "NOT",values,org)+(i));
	    		if(i>0){
	    			sb.append(" on n_ext"+i+".id=n_ext"+(i-1)+".id");
	    		}
	    		sb.append(" join ");
	    	}
	    	if(hasSearch){
                sb.delete(sb.length()-5, sb.length()).append(" ) ");
            }
    	}else if("NOT".equals(type)){//if params split with NOT,we do in NOT logic
    		String[] notConditions = searchValue.trim().split("\\s+NOT\\s+");
    		if(notConditions.length==1){
    			sb.append("(");
    		}else{
    			sb.append(" (select n_ext.id,n_ext.sfid from (");
    		}
    		
    		temp = notConditions[0].trim();

			sb.append(" select ex.id,ex.sfid from   "+schemaname
			    +".contact_ex ex where "+renderKeywordSearch(values,temp,org)  );
    		
			if(notConditions.length==1){
    			sb.append(") n_ext");
    		}else{
    			sb.append(") n_ext");
    		}
    		
    		//values.add(temp);
    		//values.add(temp);
    		boolean hasNot = false;
	    	for(int i=1;i<notConditions.length;i++){
	    		hasNot = true;
	    		temp = notConditions[i].trim();
	    		sb.append(" except ");
                        
	    		sb.append("  (select ex.id,ex.sfid from  "+schemaname+".contact_ex ex where "+renderKeywordSearch(values,temp,org) + " ) ");
	    		//values.add(temp);
	    		//values.add(temp);
	    	}
	    	if(hasNot){
	    		sb.append(")n_ext");
	    	}
    	}
    	
    	return sb.toString();
    }
    
    private String renderKeywordSearch(List values,String param,Map org){
        SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.get("name"));
        StringBuilder sb = new StringBuilder();
        for(Field f:sc.getKeyword().getFields()){
            sb.append("OR ").append(f.toString("ex")).append("@@ plainto_tsquery(?)");
            values.add(param);
        }
        
        return sb.delete(0, 2).toString();
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
    public List getTopAdvancedType(Integer offset,Integer size,String type,String keyword,String min,Map org) throws SQLException {
        if(size == null||size<8){
            size = 7;
        }
        offset = offset < 0 ? 0 : offset;
        Runner runner = daoHelper.openNewOrgRunner((String)org.get("name"));
        String name = getNameExpr(type);
        String table = getTable(type,org);
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
        List<Map> result =runner.executeQuery(querySql.toString());
        runner.close();
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
    private String getTable(String type,Map org){
        String table = null;
        String schemaname = (String)org.get("schemaname");
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
    
    private boolean hasExtraSearchColumn( Map<String, String> searchValues){
        for(String key:searchValues.keySet()){
            if(!isNativeSearchParam(key)){
                return true;
            }
        }
        return false;
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
        
        for(String s:searchParams){
            if(s.equals(name)){
                return true;
            }
        }
        return false;
    }
    
}

class SearchStatements {
    
    String cteSql;
    String querySql;
    String countSql;
    Object[]          values;

}
