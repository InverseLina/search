package com.jobscience.search.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.jobscience.search.searchconfig.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jasql.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.britesnow.snow.util.ObjectUtil;
import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.google.common.base.Strings;
import com.jobscience.search.log.LoggerType;
import com.jobscience.search.log.QueryLogger;
import com.jobscience.search.organization.OrgContext;

@Singleton
public class SearchDao {

    static private String QUERY_SELECT = "select distinct ";

    private Logger log = LoggerFactory.getLogger(SearchDao.class);
    
    @Inject
    private DatasourceManager datasourceManager;

    @Inject
    private UserDao userDao;
    
    @Inject
    private QueryLogger queryLogger;
    
    @Inject
    private SearchLogDao searchLogDao;
    
    @Inject
    private SearchConfigurationManager searchConfigurationManager;
    
    @Inject
    CurrentRequestContextHolder crch;  
   
    private static Pattern pattern = Pattern.compile("\\srows=(\\d*)\\s",Pattern.CASE_INSENSITIVE);
    private static int EXACT_COUNT_TIMEOUT = 2000;//ms
    private static int ESTIMATE_COUNT_TIMEOUT = 1000;//ms
    private static String separator = "&&&";
    
    /**
     * do the search
     * @param searchRequest
     * @param token
     * @param org
     * @return
     */
    public SearchResult search(SearchRequest searchRequest,String token,OrgContext org) {

		SearchResult searchResult = null;
		SearchStatements statementAndValues = buildSearchStatements(searchRequest,org);
		//excute query and caculate times
		searchResult = executeSearch(statementAndValues,searchRequest,org);

		queryLogger.debug(LoggerType.SEARCH_PERF,searchResult.getSelectDuration());
		queryLogger.debug(LoggerType.SEARCH_COUNT_PERF,searchResult.getCountDuration());

		Long userId = -1L;
		Map user =  userDao.getUserByTokenAndOrg(token, (String)org.getOrgMap().get("name"));
		if(user != null){
			userId=Long.parseLong(user.get("id").toString());
		}

		searchLogDao.addSearchLog(searchRequest.toString(), searchResult.getCountDuration(), searchResult.getSelectDuration(), userId,org);

		if (searchResult != null){
			searchResult.setPageIdx(searchRequest.getPageIndex());
			searchResult.setPageSize(searchRequest.getPageSize());
		}

        return searchResult;
    }

	/**
	 * Get the auto complete data
	 * @param searchValues
	 * @param type
	 * @param queryString
	 * @param orderByCount
	 * @param min
	 * @param pageSize
	 * @param pageNum
	 * @param org
	 * @return
	 * @throws SQLException
	 */
    public SearchResult getGroupValuesForAdvanced(Map<String, String> searchValues, String type,String queryString,Boolean orderByCount,String min,Integer pageSize,Integer pageNum,OrgContext org) throws SQLException {
        return simpleAutoComplete(searchValues, type, queryString, orderByCount, min, pageSize, pageNum,org);
    }

    /**
     * boolean search handler for search box
     * @param searchValue
     * @param type
     * @param org
     * @param exact
     * @return
     */
    public  String booleanSearchHandler(String searchValue,String type,OrgContext org,boolean exact){
        String schemaname = (String)org.getOrgMap().get("schemaname");
    	SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.getOrgMap().get("name"));
    	StringBuilder sb = new StringBuilder();
    	searchValue = searchValue.replaceAll("[\\(\\)%\\^\\@#~\\*]", "").trim();
    	if(!exact){
        	if(!searchValue.contains("NOT ") &&
        	   !searchValue.contains("AND ") &&
        	   !searchValue.contains("OR ")){
            	if(!searchValue.matches("^\\s*\"[^\"]+\"\\s*$")){//if there not in quotes,replace space to OR
            	    searchValue = searchValue.replaceAll("\\s+", " OR ");
            	    exact = false;
            	}else{
            	    exact = true;
                    searchValue = searchValue.replaceAll("\"", "");//.replaceAll("\\s+", " AND ");
            	}
        	}else{
        	    if(!searchValue.matches("^\\s*\"[^\"]+\"\\s*$")){//if there not in quotes,replace space to OR
                    searchValue = searchValue.replaceAll("\"", "");
        	    }
        	}
    	}
    	//if no search value,just return sql with 1!=1
    	if(searchValue.equals("")){
    		return  " select id,sfid from "+schemaname+"."+sc.getContact().getTable()+" where 1!=1 ";
    	}
    	String temp = "";
    	if(type == null || "OR".equals(type)){//if params split with space or "OR",we do in OR logic
        	String[] orConditions = searchValue.trim().split("\\s+OR\\s+");
        	boolean hasSearch = false;
        	for(int i=0;i<orConditions.length;i++){
        		String orCondition = orConditions[i];
        		sb.append("select a_extr"+i+".id,a_extr"+i+".sfid from (");
        		sb.append(booleanSearchHandler(orCondition, "AND",org,exact));
        		sb.append(" a_extr"+i+" union ");
        		hasSearch = true;
        	}
        	if(hasSearch){
        	    sb.delete(sb.length()-6, sb.length());
        	}
    	}else if("AND".equals(type)){//if params split with AND,we do in AND logic
    		String[] andConditions = searchValue.trim().split("\\s+AND\\s+");
    		boolean hasSearch = false;
        	for(int i = 0; i < andConditions.length; i++){
        	    hasSearch = true;
        		String andCondition = andConditions[i];
    			if(i == 0){
    				sb.append(" select n_ext0.id as id,n_ext0.sfid as sfid from ");
    			}
        		sb.append(booleanSearchHandler(andCondition, "NOT",org,exact)+(i));
        		if(i > 0){
        			sb.append(" on n_ext"+i+".id=n_ext"+(i-1)+".id");
        		}
        		sb.append(" join ");
        	}
        	if(hasSearch){
                sb.delete(sb.length()-5, sb.length()).append(" ) ");
            }
    	}else if("NOT".equals(type)){//if params split with NOT,we do in NOT logic
    		String[] notConditions = searchValue.trim().split("\\s+NOT\\s+");
    		if(notConditions.length == 1){
    			sb.append("(");
    		}else{
    			sb.append(" (select n_ext.id,n_ext.sfid from (");
    		}
    		
    		temp = notConditions[0].trim();
    		sb.append(" select ex.id,ex.sfid from   "+schemaname
    		    +".jss_contact ex where "+renderKeywordSearch(temp,org,exact,"ex")  );
    		
    		if(notConditions.length == 1){
    			sb.append(") n_ext");
    		}else{
    			sb.append(") n_ext");
    		}
    		
    		boolean hasNot = false;
        	for(int i = 1; i < notConditions.length; i++){
        		hasNot = true;
        		temp = notConditions[i].trim();
        		sb.append(" except ");
        		sb.append("  (select ex.id,ex.sfid from  "+schemaname+".jss_contact ex where "+renderKeywordSearch(temp,org,exact,"ex") + " ) ");
        	}
        	if(hasNot){
        		sb.append(")n_ext");
        	}
    	}
    	return sb.toString();
    }

    /**
     * build SearchStatements by searchRequest
     * @param searchRequest
     * @param org
     * @return
     */
    protected SearchStatements buildSearchStatements(SearchRequest searchRequest,OrgContext org) {
        SearchStatements ss = new SearchStatements();
        SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.getOrgMap().get("name"));
        int offset = searchRequest.getOffest();
        String schemaname = (String)org.getOrgMap().get("schemaname");
        
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
        String search = searchRequest.getKeyword();
        // the params will be put in sql
        List values = new ArrayList();
        String cteSql = "";
        
        
        querySql.append("select ");
        querySql.append(getSearchColumnsForOuter(searchRequest.getColumns(),org));
        querySql.append(" from ( ");
        querySql.append(QUERY_SELECT);
        querySql.append(getSearchColumns(searchRequest.getColumns(),columnJoinTables,groupBy,org));
        
        
        //---------------------- add select columns ----------------------//
        String contactString = sc.toContactFieldsString("contact");
        String contactTable = sc.getContact().getTable();
        querySql.append(" from ( select  ")
                .append(contactString);
        for(Filter f : sc.getFilters()){
            if(f.getFilterType() == null && contactString.indexOf(f.getFilterField().toString("contact")) == -1){
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
        //---------------------- /add select columns----------------------//
        
        if(searchRequest.getOrder().contains("title")){
        	querySql.append(",case   when ")
        	        .append(sc.getContactField(ContactFieldType.TITLE).toString("contact"))
        	        .append(" is null then ''  else lower(")
        	        .append(sc.getContactField(ContactFieldType.TITLE).toString("contact"))
        	        .append(") END \"ltitle\" ");
        }else if(searchRequest.getOrder().contains("name")){
        	querySql.append(",lower(")
        	        .append(sc.getContactField(ContactFieldType.NAME).toString("contact")) 
        	        .append(") as \"lname\" ");
        }else if(searchRequest.getOrder().contains("email")){
        	querySql.append(",lower(")
                    .append(sc.getContactField(ContactFieldType.EMAIL).toString("contact")) 
                    .append(") as \"lemail\" ");
        }
        
        querySql.append( " from  "+schemaname+".")
                .append(sc.getContact().getTable())
                .append(" contact  " );
        
        JSONArray locationValues = searchRequest.getLocations();
        boolean hasContactsCondition = false;
        if(searchRequest.getContacts() != null){
            hasContactsCondition = searchRequest.getContacts().size()>0;
        }
        if(!hasExtraSearchColumn(searchRequest) || hasContactsCondition || (Strings.isNullOrEmpty(search) || search.length()<3) || (locationValues != null)){
            countSql.append( " from ( select ")
                    .append(sc.toContactFieldsString("contact"))
                    .append(" from  "+schemaname+".")
                    .append(sc.getContact().getTable())
                    .append(" contact   " );
        }
           
        // for all search mode, we preform the same condition
        String[] sqls = getCondtion(searchRequest,values,org);
        querySql.append(sqls[0]);
        countSql.append(sqls[1]);
        cteSql=sqls[2];
        
        querySql.append(joinTables);
        countSql.append(joinTables);
        for(String join : columnJoinTables){
        	if(!join.equals("No Join")){
            	querySql.append(join);
        	}
        }
        
        querySql.append(conditions);
        countSql.append(conditions);
        if(!"".equals(groupBy.toString())){
            querySql.append(" group by "+groupBy);
        }
        
        querySql.append(") b ");
    	if(!Strings.isNullOrEmpty(searchRequest.getOrder())){
    		querySql.append(" order by "+searchRequest.getOrder());
    		if(!searchRequest.getOrder().trim().startsWith("\"id\"")){
    			querySql.append(" offset ").append(offset).append(" limit ").append(searchRequest.getPageSize() + 1);
    		}
    	}
        if(log.isDebugEnabled()){
            log.debug(querySql.toString());
            log.debug(countSql.toString());
        }
        
        queryLogger.debug(LoggerType.SEARCH_SQL, querySql);
        queryLogger.debug(LoggerType.SEARCH_COUNT_SQL, countSql);
        queryLogger.debug(LoggerType.PARAMS, searchRequest.getSearchMap());
        // build the statement
        ss.querySql =cteSql+" "+querySql.toString();
        ss.countSql =countSql.toString();
        ss.cteSql =   cteSql;
        ss.values = values.toArray();
        return ss;
    }

    /**
     * execute the search use SearchStatements and searchRequest
     * @param statementAndValues
     * @param searchRequest
     * @param org
     * @return
     */
    protected SearchResult executeSearch(SearchStatements statementAndValues,SearchRequest searchRequest,OrgContext org){
    	Runner runner = datasourceManager.newOrgRunner(org.getOrgMap().get("name").toString());
    	SearchResult searchResult = null;
    	try{
    		long start = System.currentTimeMillis();
    		List<Map> result = null;

            result = runner.executeQuery(statementAndValues.querySql, statementAndValues.values);
//    		if(result != null && !searchRequest.searchModeChange()){
//        		result = setLoctionName(searchRequest,result,org);
//    		}
    		long mid = System.currentTimeMillis();
    		int count = 0;
    		boolean exactCount = false;
    		boolean hasNextPage = false;
    		
    		if(result != null && result.size() == searchRequest.getPageSize() + 1){
    		    result = result.subList(0, searchRequest.getPageSize());
    		    hasNextPage = true;
    		}
    		
    		//when the search result less than page size,this would be the last page,we just calculate the count
    		if(result != null && result.size() < searchRequest.getPageSize()){
    			count = searchRequest.getOffest() + result.size();
    			exactCount = true;
    		}else{
    			if(!searchRequest.isEstimateSearch()){
    				/********** Get the exact count **********/
	    			try{
	    				runner.executeUpdate("SET statement_timeout TO "+EXACT_COUNT_TIMEOUT+";");
	    				int exact= runner.executeCount(statementAndValues.cteSql
								+" select  count(distinct a.id) as count  "
								+statementAndValues.countSql);
	    				count = exact;
	    				exactCount = true;
	    			}catch(Exception e){
	    				log.debug("The count search timeout,use the estimate count");
	    				exactCount = true;
	    				count = -1;
	    			}
    	    		/********** /Get the exact count **********/
    			}else{
	    			/********** Get the exact count **********/
	    			try{
	    				runner.executeUpdate("SET statement_timeout TO "+ESTIMATE_COUNT_TIMEOUT+";");
	    				int exact= runner.executeCount(statementAndValues.cteSql
								+" select  count(distinct a.id) as count  "
								+statementAndValues.countSql);
	    				count = exact;
	    			}catch(Exception e){
	    				try{
	    					log.debug("The count search timeout,use the estimate count");
		    				/********** Get the estimate count **********/
		    	    		List<Map> explainPlans = runner.executeQuery("explain "+statementAndValues.cteSql
		    						 									+" select  distinct(a.id)  "
		    						 									+statementAndValues.countSql,
		    						 									statementAndValues.values);
		    	    		if(explainPlans.size() > 0){
		    	    			count = getCountFromExplainPlan((String)explainPlans.get(0).get("QUERY PLAN"));
		    	    			exactCount = false;
		    	    		}else{
		    	    			count = 0;
		    	    		}
		    	    		/********** /Get the estimate count **********/
	    				}catch(Exception ex){
		    				log.debug("The estimate count search timeout");
	    				}
	    			}
		    		/********** /Get the exact count **********/
    				exactCount = false;
                    if (count < searchRequest.getPageIndex() * searchRequest.getPageSize()) {
                        count = searchRequest.getPageIndex() * searchRequest.getPageSize();
                    }
	    		}
    		}
    		long end = System.currentTimeMillis();
    		handleResult(result);
    		searchResult = new SearchResult(result, count)
    				.setDuration(end - start)
    				.setSelectDuration(mid - start)
    				.setCountDuration(end - mid)
    				.setExactCount(exactCount)
    		        .setHasNextPage(hasNextPage);
    	}finally{
    		runner.executeUpdate("RESET statement_timeout; ");
    		runner.close();
    	}
    
    	return searchResult;
    }

    /**
     * 
     * @param searchValues
     * @param type
     * @param queryString
     * @param orderByCount
     * @param min
     * @param pageSize
     * @param pageNum
     * @param org
     * @return
     * @throws SQLException
     */
    protected SearchResult simpleAutoComplete(Map<String, String> searchValues, String type,String queryString,Boolean orderByCount,String min,Integer pageSize,Integer pageNum,OrgContext org) throws SQLException {
        SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.getOrgMap().get("name"));
        Filter filter = sc.getFilterByName(type);
        if(filter==null){
             return null;
        }
        FilterType f = filter.getFilterType();
        String baseTable = "";
        if(f == null && !type.equals("location")){
            baseTable=filter.getFilterField().getTable();
        }else if(f.equals(FilterType.COMPANY)){
            baseTable =  "jss_grouped_employers ";
        }else if(f.equals(FilterType.EDUCATION)){
            baseTable =  "jss_grouped_educations ";
        }else if(f.equals(FilterType.SKILL)){
            baseTable =  "jss_grouped_skills ";
        }else if(f.equals(FilterType.LOCATION)){
            baseTable =  "city_score ";
        }
        
        StringBuilder querySql;
        if(f == null){
            FilterField ff = sc.getFilterByName(type).getFilterField();
            querySql = new StringBuilder(" select count(*) as \"count\","+ff.toString("")+" as name from \"" + baseTable + "\" where 1=1 ");
            if(queryString != null && queryString.trim().length() > 0){
                querySql.append(" AND  "+ff.getColumn()+" ilike '"+ queryString+"%'");
            }
            querySql.append(" AND ("+ff.getColumn()+"||'')!='' ")
                    .append(" AND ("+ff.getColumn()+"||'')!='null' ")
                    .append(" AND "+ff.getColumn()).append(" is not null   group by "+ff.getColumn());
            querySql.append(" order by count desc limit 7 ");
        }else{
            if (type.equals("location")) {
                querySql = new StringBuilder(" select score as count, city as name, case when country = 'US' or country = 'CA' then region else country end as suffix, city_world_id as locationid" + " from " + baseTable + "  where 1=1 ");
                if (queryString != null && queryString.trim().length() > 0) {
                    querySql.append(" AND score > 0 ");
                    String[] values = queryString.split(",");
                    String firstQuery = values[0].trim();
                    if (values.length > 1) {
                        querySql.append(" AND region ilike '" + values[1].trim() + "%'");
                    }
                    querySql.append(" AND (city ilike '" + firstQuery + "%' OR region ilike '" + firstQuery + "%' OR country ilike '" + firstQuery + "%' )");
                }
                querySql.append(" order by count desc limit 7 ");
                // the final sql
                querySql = new StringBuilder("select a.*, b.latitude as latitude, b.longitude as longitude from (" + querySql + ") a left join jss_sys.city_world b on a.locationid = b.id");
            } else {
                querySql = new StringBuilder(" select a.count as count, a.name as name, a.id as groupedid from " + baseTable + " a where 1=1 ");
                if (queryString != null && queryString.trim().length() > 0) {
                    querySql.append(" AND a.name ilike '" + queryString + "%'");
                }
                querySql.append(" order by count desc limit 7 ");
            }

        }
        
        Long start = System.currentTimeMillis();
        Runner runner = datasourceManager.newOrgRunner((String)org.getOrgMap().get("name"));
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
     * Get the query column and add group by or join table if needed
     * @param orginalName
     * @param columnJoinTables
     * @param groupBy
     * @param searchedColumns
     * @param org
     * @return
     */
    private String getQueryColumnName(String orginalName ,List<String> columnJoinTables,StringBuilder groupBy,StringBuffer searchedColumns,OrgContext org){
        String schemaname = (String)org.getOrgMap().get("schemaname");
        if(searchedColumns.toString().contains(orginalName.toLowerCase()+",")){
            return "";
        }
        searchedColumns.append(orginalName.toLowerCase()).append(",");
        if(orginalName.toLowerCase().equals("name")){
    		return "lower(a.\"name\") as \"lname\"";
    	}else if(orginalName.toLowerCase().equals("id")){
    		return "";
    	}else if(orginalName.toLowerCase().equals("resume")){
            if(groupBy.length() > 0){
                groupBy.append(",");
            }
            groupBy.append("a.resume");
            return " a.resume as resume";
    	}else if(orginalName.toLowerCase().equals("email")){
     		if(groupBy.length() > 0){
	     			groupBy.append(",");
     		}
     		groupBy.append("a.\"email\"");
    		return " a.\"email\" as email ";
    	}else if(orginalName.toLowerCase().equals("title")){
    		if(groupBy.length() > 0){
    			groupBy.append(",");
    		}
    		groupBy.append("a.\"title\"");
    		return "case   when a.\"title\" is null then '' " +
        			        " else a.\"title\" END title ";
    	}else if(orginalName.toLowerCase().equals("createddate")){
    		if(groupBy.length() > 0){
    			groupBy.append(",");
    		}
    		groupBy.append("a.\"createddate\"");
    		return "to_char(a.\"createddate\",'yyyy-mm-dd') as createddate";
    	}else if(orginalName.toLowerCase().equals("company")){
    	    return " (select  string_agg(replace(c.\"name\",',','"+separator+"'),',')||'##'||string_agg(c.\"id\"::varchar,',') "
    	                            + "from "+schemaname+".jss_grouped_employers c join "+schemaname+".jss_contact_jss_groupby_employers groupby_employers "
    	                            + "on groupby_employers.jss_groupby_employers_id = c.id  "
    	                            + "where a.\"id\" = groupby_employers.\"jss_contact_id\"  ) as company ";
    	}else if(orginalName.toLowerCase().equals("skill")){
    	    return " (select  string_agg(b.\"name\",',')||'##'||string_agg(b.\"id\"::varchar,',' ) "
    	                            + "from "+schemaname+".jss_grouped_skills b join "+schemaname+".jss_contact_jss_groupby_skills groupby_skills "
    	                            + "on groupby_skills.jss_groupby_skills_id = b.id  "
    	                            + "where a.\"id\" = groupby_skills.\"jss_contact_id\"  ) as skill ";
    	}else if(orginalName.toLowerCase().equals("education")){
    	    return " (select  string_agg(d.\"name\",',' order by d.id)||'##'|| string_agg(d.\"id\"::varchar,',')"
                                    + "from "+schemaname+".jss_grouped_educations d join "+schemaname+".jss_contact_jss_groupby_educations groupby_educations "
                                    + "on groupby_educations.jss_groupby_educations_id = d.id  "
                                    + "where a.\"id\" = groupby_educations.\"jss_contact_id\"  ) as education";
    	}else if(orginalName.toLowerCase().equals("location")){
    		return "a.mailingcity as location ";
    	}
        
        SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.getOrgMap().get("name"));
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
    }
    
    /**
     * get the search columns for outer sql block
     * @param searchColumns
     * @param org
     * @return
     */
    private String getSearchColumnsForOuter(String searchColumns,OrgContext org){
        SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.getOrgMap().get("name"));
        StringBuilder sb = new StringBuilder();
    	if(searchColumns == null){
    		sb.append("id,name,email,title ,createddate");
    	}else{
	    	for(String column : searchColumns.split(",")){
	    	    if(sb.indexOf("as \""+column.toLowerCase()+"\"") != -1 || sb.indexOf(column.toLowerCase()+",") != -1
	    	       || sb.indexOf("as \"l"+column.toLowerCase()+"\"") != -1){
	    	        continue;
	    	    }
		    	if(column.toLowerCase().equals("name")){
		    		sb.append("lower(name) as \"lname\",");
		    	}else if(column.toLowerCase().equals("title")){
		    		sb.append("title, ");
		    	}else if(column.toLowerCase().equals("email")){
		    		sb.append( "email,");
		    	}else if(column.toLowerCase().equals("createddate")){
		    		sb.append("createddate as \"createddate\",");
		    	}else if(column.toLowerCase().equals("company")){
		    		sb.append("company, ");
		    	}else if(column.toLowerCase().equals("skill")){
		    		sb.append("skill,");
		    	}else if(column.toLowerCase().equals("education")){
		    		sb.append("education, ");
		    	}else if(column.toLowerCase().equals("resume")){
		    		sb.append("resume,");
		    	}else if(column.toLowerCase().equals("location")){
		    		sb.append("location as \"location\",");
		    	}else if(column.toLowerCase().equals("contact")){
		    		sb.append("name,lower(name) as \"lname\",");
		    		sb.append("title, ");
		    		sb.append( "email, ");
		    	}else{
		    	    Filter filter = sc.getFilterByName(column);
		    	    if(filter != null){
    		    	    if(sb.indexOf("as "+column) == -1 && sb.indexOf(column+",") == -1){
    		    	        sb.append("\""+filter.getName()+"\"").append(" as \"").append(filter.getName()).append("\",");
    		    	    }
		    	    }
		    	}
	    	}
	    	sb.append("id,name");//make id and name always return
    	}
    	sb.append(",sfid");//,phone
    	sb.append(",ts2__latitude__c,ts2__longitude__c");//ts2__latitude__c and ts2__longitude__c
        return sb.toString();
    }
    
    /**
     * get search columns for inner sql block
     * @param searchColumns
     * @param columnJoinTables
     * @param groupBy
     * @return
     */
    private String getSearchColumns(String searchColumns,List columnJoinTables,StringBuilder groupBy,OrgContext org){
    	 StringBuilder columnsSql = new StringBuilder();
    	 SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.getOrgMap().get("name"));
    	 if(searchColumns == null){//a.phone,
             columnsSql.append(sc.toContactFieldsString("a"));
             //,a.phone
             groupBy.append(","+sc.toContactFieldsString("a"));
    	 }else{
    		 String temp = "";
    		 StringBuffer sb = new StringBuffer("id,name,sfid,");
 	         for(String column : searchColumns.split(",")){
 	        	temp = getQueryColumnName(column,columnJoinTables,groupBy,sb,org);
 	        	if(!temp.trim().equals("")){
	 	            columnsSql.append(temp);
	 	            columnsSql.append(",");
 	        	}
 	        }
 	        columnsSql.append("a.id,a.name,a.sfid,a.ts2__latitude__c,a.ts2__longitude__c ");//,a.phone,
 	        if(groupBy.length() > 0){
 	        	groupBy.append(",");
 	        }
 	        groupBy.append("a.name,a.sfid,a.\"ts2__latitude__c\", a.\"ts2__longitude__c\",a.mailingcity");//always return these columns ,
         }
    	 return columnsSql.toString();
    }
    
    /**
     * render the List<Map> results set contact LocationName (Now not use,359)
     * @param searchRequest
     * @param results
     * @param org
     * @return
     */
    protected List<Map> setLoctionName(SearchRequest searchRequest,List<Map> results,OrgContext org){
    	List<Map> result = results;
    	JSONArray locations = searchRequest.getLocations();
    	String cityName = "",suffix="";
    	double minradius = -1;
    	double latitude = -1;
    	double longitude = -1;
    	for(Map contact : results){
    		if(locations == null){
    			contact.put("location", "");
    			continue;
    		}
    		boolean hasCityName = false;
    		minradius = -1;
    		if(contact.get("ts2__latitude__c") == null || contact.get("ts2__longitude__c") == null){
    			continue;
    		}
    		latitude = Float.parseFloat(contact.get("ts2__latitude__c").toString());
    		longitude = Float.parseFloat(contact.get("ts2__longitude__c").toString());
    		JSONObject jo = null;
    		String name = null;
    		double latitudes = 0;
    		double longituds = 0;
    		double radius = 0;
    		double deviation = 0;
    		for(int x = 0,y = locations.size(); x < y; x++){
    			 jo = JSONObject.fromObject(locations.get(x));
    			 name = jo.get("name").toString();
    			 latitudes = Float.parseFloat(jo.get("latitude").toString());
    			 longituds = Float.parseFloat(jo.get("longitude").toString());
    			 int checkradius = Integer.parseInt(jo.get("minRadius").toString());
    			 radius = getDistance(latitude,longitude,latitudes,longituds);
    			 if(((int)radius <= checkradius && !hasCityName)||((int)radius <= checkradius && (int)radius < minradius)){
        				 hasCityName = true;
        				 cityName = name;
        				 suffix = jo.get("suffix").toString();
        				 minradius = radius;
    			 }else{
    				 if(deviation == 0.0d){
    					 deviation = radius-checkradius;
    					 cityName = name;
    					 suffix = jo.get("suffix").toString();
    					 minradius = checkradius;
    				 }
    				
    				 if(radius-checkradius<deviation){
    					 deviation = radius-checkradius;
    					 cityName = name;
    					 suffix = jo.get("suffix").toString();
    					 minradius = checkradius;
    				 }
    				 
    			 }
    			 
    		}
    		contact.put("location", cityName +"("+suffix+")"+(
					(minradius == 0.0d) ? "":  ("("+(int)(minradius)+")")
				     ));
    	}
    	return result;
    }

    /**
     * render the search Condition
     * @param searchValues
     * @param values
     * @param org
     * @return
     */
    protected String[] renderSearchCondition(SearchRequest searchRequest, List values, OrgContext org){
        SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.getOrgMap().get("name"));
        StringBuilder querySql = new StringBuilder();
        StringBuilder conditions = new StringBuilder();
        List subValues = new ArrayList();
        boolean hasCondition = false;
        String schemaname = (String)org.getOrgMap().get("schemaname");
        StringBuilder prefixSql = new StringBuilder("");
        boolean hasSearchValue = false;//to check if the search box has value or not
        StringBuilder locationSql = new StringBuilder();
        
        if (searchRequest != null) {
            
           // for all search mode, we preform the same condition
           String search = searchRequest.getKeyword();
           if (!Strings.isNullOrEmpty(search)) {
               if(search.length() >= 3){
                   querySql.append(getSearchValueJoinTable(search, values, "contact", org,searchRequest));
                   if(search.matches("^\\s*\"[^\"]+\"\\s*$")){//when exact search,add condition for resume
                       conditions.append(" AND contact.\"ts2__text_resume__c\" ilike '")
                                 .append(search.replaceAll("\\\"", "%"))
                                 .append("'");
                   }
                   hasSearchValue = true;
                   hasCondition = true;
               }
           }
           
            //Get the contacts parameters and render them
            hasCondition = renderContactConditions(conditions,subValues,sc,searchRequest.getContacts())||hasCondition;
            
            //handle the objectType
            if (!Strings.isNullOrEmpty(searchRequest.getObjectType())) {
                String value = searchRequest.getObjectType();
                if ("Contact".equals(value)) {
                    conditions.append("  and contact.\"recordtypeid\" != '").append(org.getSfid()).append("'");
                }else if("Candidate".equals(value)){
                    conditions.append("  and contact.\"recordtypeid\" = '").append(org.getSfid()).append("'");
                }
            }
    
            //handle the status
            if (!Strings.isNullOrEmpty(searchRequest.getStatus())) {
                String value = searchRequest.getStatus();
                if ("Active".equals(value)) {
                    conditions.append("  and contact.\"ts2__people_status__c\" in('',null, 'Active') ");
                } else if ("Inactive".equals(value)){
                    conditions.append("  and contact.\"ts2__people_status__c\" = 'Inactive' ");
                }
            }
            // add the 'educations' filter, and join ts2__education_history__c table
            hasCondition = renderEducationCondition( searchRequest.getEducations(),
                    prefixSql, querySql, schemaname, sc,FilterType.EDUCATION,org) || hasCondition;
           // add the 'companies' filter, and join ts2__employment_history__c table
           hasCondition = renderCompanyCondition(searchRequest.getCompanyOperator(), searchRequest.getCompanies(),
                   prefixSql, querySql, schemaname, sc,FilterType.COMPANY,org) || hasCondition;
           
           // add the 'skillNames' filter, and join ts2__skill__c table
           hasCondition = renderSkillCondition(searchRequest.getSkillOperator(),searchRequest.getSkills(),
                   prefixSql, querySql, schemaname, sc,FilterType.SKILL,org) || hasCondition;
           
           //add the 'radius' filter
           hasCondition = renderLocationCondition(searchRequest.getLocations(), locationSql,
                   conditions, schemaname,sc,org)||hasCondition;
       }
       //at last,combine all part to complete sql
        
       values.addAll(subValues);
    
       hasCondition = renderCustomFilters(searchRequest.getCustomFilters(), querySql, conditions,values,schemaname, sc)
                      || hasCondition;
    
       //if there has no condition,just append 1!=1
       if(!hasCondition){
           conditions.append(" and 1!=1 ");
       }
    
       if(hasSearchValue){
           querySql = new StringBuilder(" join (").append(querySql);
       }
       return new String[]{querySql.toString(),prefixSql.toString(),conditions.toString(),locationSql.toString()};
    }

    
    /**
     * Get condition for Search logic 
     * @param searchRequest
     * @param values
     * @param org
     * @return
     */
    private String[] getCondtion(SearchRequest searchRequest,List values,OrgContext org){
    	StringBuilder joinSql = new StringBuilder();
    	StringBuilder countSql = new StringBuilder();
    	String prefixSql = "";
        SearchBuilder sb = new SearchBuilder(org);
        sb.addKeyWord(searchRequest.getKeyword(),searchRequest).addContactFilter(searchRequest.getContacts())
          .addCompany(searchRequest.getCompanies()).addEducation(searchRequest.getEducations())
          .addSkill(searchRequest.getSkills()).addLocation(searchRequest.getLocations())
          .addCustomFilter(searchRequest.getCustomFilters()).addObjectType(searchRequest.getObjectType())
          .addStatus(searchRequest.getStatus()).addCustomFields(searchRequest.getCustomFields());
        values.addAll(sb.getValues());
            
        String condition = sb.getConditions();
        boolean hasContactsCondition = false;
        String locationSql = sb.getLocationSql();
        joinSql = new StringBuilder(sb.getSearchSql());
        
        prefixSql = sb.getPrefixSql();
        countSql = new StringBuilder(sb.getCountSql());
        if(searchRequest.getContacts() != null){
            hasContactsCondition = searchRequest.getContacts().size()>0;
        }
        
        if(searchRequest.getLocations() != null && (Strings.isNullOrEmpty(searchRequest.getKeyword()) || searchRequest.getKeyword().length() < 3) && !searchRequest.getOrder().contains("location")){

        	joinSql.append(" where ").append(condition.subSequence(4, condition.length()))
        	       .append(locationSql)
        	       .append(sb.getExactSearchOrderSql());
        	countSql.append(" where ").append(condition.subSequence(3, condition.length()))
 	                .append(locationSql)
 	                .append(sb.getExactSearchOrderSql());
        	
        	joinSql.append(" ) subcontact on contact.id=subcontact.id ");
            countSql.append(" ) subcontact on contact.id=subcontact.id ");
        	
        	joinSql.append(" where 1=1 ");
            countSql.append(" where 1=1 ");
        }else{        
            joinSql.append(" ) subcontact on contact.id=subcontact.id ");
            countSql.append(" ) subcontact on contact.id=subcontact.id ");
            
            joinSql.append(locationSql);
            countSql.append(locationSql);
            
            joinSql.append(" where 1=1 ").append(condition).append(sb.getExactSearchOrderSql());
            countSql.append(" where 1=1 ").append(condition);
        }
       
        joinSql.append(") a ");
        if(!hasExtraSearchColumn(searchRequest) || hasContactsCondition || locationSql.length() > 0 ||
                Strings.isNullOrEmpty(searchRequest.getKeyword()) || searchRequest.getKeyword().length() < 3){
            countSql.append(") a ");
        }
       
        return new String[]{joinSql.toString(),countSql.toString(),prefixSql};
    }
    
    /**
     * handle the table joined for boolean search,mainly for contact table
     * @param keyword
     * @param values
     * @param alias
     * @param org
     * @param searchRequest
     * @return
     */
	private String getSearchValueJoinTable(String keyword, List values,
			String alias, OrgContext org, SearchRequest searchRequest) {
		StringBuilder joinSql = new StringBuilder();
		if ("a".equals(alias)) {
			joinSql.append(" right join (");
			joinSql.append(booleanSearchHandler(keyword, null, org, false));
			joinSql.append(")  a_ext on a_ext.id = " + alias + ".id ");
			return joinSql.toString();
		} else {
			boolean exact = keyword.matches("^\\s*\".+\"\\s*$");
			joinSql.append(" select  distinct contact.id,contact.sfid  from (");
			if (exact) {
				if (searchRequest.isOnlyKeyWord()) {
					joinSql = new StringBuilder();
				}
				ArrayList<String> keys = new ArrayList<String>();
				Map<Integer, String> operators = new HashMap<Integer, String>();
				//spilt keywords
				spiltKeywords(keyword,keys,operators);
				//exactkeywordSql
				joinSql.append(exactkeywordSql(org ,keys , operators , searchRequest));
				return joinSql.toString();
			} else {
				if (searchRequest.isOnlyKeyWord()) {
					if (keyword.contains("NOT ") || keyword.contains("AND ")
							|| keyword.contains("OR ")) {
						joinSql.append(booleanSearchHandler(keyword, null, org,
								false));
					} else {
						return "  select contact.id,contact.sfid from  "
								+ org.getOrgMap().get("schemaname")
								+ ".jss_contact contact where "
								+ renderKeywordSearch(keyword.trim()
										.replaceAll("\\s+", "|"), org, exact,
										"contact") + "  ";
					}
				} else {
					joinSql.append(booleanSearchHandler(keyword, null, org,
							false));
				}
			}
			joinSql.append(")  contact ");
			return joinSql.toString();
		}
	}
	
	/**
	 * 
	 * @param param
	 * @param org
	 * @param exact
	 * @param alias
	 * @return
	 */
    private String renderKeywordSearch(String param,OrgContext org,boolean exact,String alias){
        SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.getOrgMap().get("name"));
        StringBuilder sb = new StringBuilder();
        String exactFilter = "";
        exact = false;
        if(exact){
            exactFilter = " ts_rank(resume_tsv,'"+param.replaceAll("\\s+", "&")+"')>0 AND (";
        }
        for(Field f : sc.getKeyword().getFields()){
            sb.append("OR ").append(f.toString(alias)).append("@@ to_tsquery(")
              .append("'")
              .append(param)
              .append("')");
        }
        return exactFilter+sb.delete(0, 2).toString()+(exact?")":"");
    }

	private void spiltKeywords(String keyword,ArrayList<String> keys,Map<Integer, String> operators){
		int flag = 0;
		while (keyword.length() > 0) {
			if (keyword.startsWith("\"")) {
				flag++;
				int next = keyword.indexOf("\"", 1);
				keys.add(keyword.substring(1, next).trim());
				keyword = keyword.substring(next + 1);
			} else {
				int start = keyword.indexOf("\"", 1);
				operators.put(flag, keyword.substring(0, start).trim());
				keyword = keyword.substring(start);
			}
		}
	}
	
	private String exactkeywordSql(OrgContext org , ArrayList<String> keys , Map<Integer, String> operators , SearchRequest searchRequest) {
		StringBuilder joinSql = new StringBuilder();
		joinSql.append("  select contact.id,contact.sfid from  ")
				.append(org.getOrgMap().get("schemaname"))
				.append(".contact  where (");
		boolean like = true;
		boolean lastOne = false;
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			if (like) {
				if (lastOne) {
					joinSql.append(" and ");
					lastOne = false;
				}
				joinSql.append(" contact.\"ts2__text_resume__c\" like '")
						.append("%" + key.trim() + "%").append("' ");
			} else {
				joinSql.append(" contact.\"ts2__text_resume__c\" not like '")
						.append("%" + key.trim() + "%").append("' ");
			}
			String operator = operators.get(i + 1);
			if (operator != null) {
				if (operator.trim().equals("NOT")) {
					like = false;
					joinSql.append(" and ");
				} else if (operator.trim().equals("AND")
						|| operator.trim().equals("OR")) {
					joinSql.append(" " + operator.toLowerCase().trim() + " ");
				} else {
					joinSql.append(" or ");
				}
			} else {
				lastOne = true;
			}
		}
		joinSql.append(")");
		if (!searchRequest.isOnlyKeyWord()) {
			joinSql.append(")  contact ");
		}
		return joinSql.toString();
	}

    /**
     *
     * @param conditionSql
     * @param values
     * @param sc
     * @param contactParamsString
     * @return
     */
    private boolean renderContactConditions(StringBuilder conditionSql,List values,
            SearchConfiguration sc,JSONArray contacts){
        boolean hasContactCondition = false;
        if (contacts != null) {
            if (contacts.size() > 0) {//First add 1!=1,cause for all contacts,would do with "OR"
                conditionSql.append(" AND (1!=1 ");
            }

            for (Object contactString : contacts) {
                JSONObject contact = JSONObject.fromObject(contactString);
                conditionSql.append(" OR (1=1 ");//for single contact,would do with "AND"
                //handle for first name
                hasContactCondition=
                        renderContactPropertyCondition(conditionSql,values,sc,contact,"firstName")
                        ||hasContactCondition;
                //handle for last name
                hasContactCondition= 
                        renderContactPropertyCondition(conditionSql,values,sc,contact,"lastName")
                        ||hasContactCondition;
                //handle for email
                hasContactCondition= 
                        renderContactPropertyCondition(conditionSql,values,sc,contact,"email")
                        ||hasContactCondition;
                //handle for title
                hasContactCondition=
                        renderContactPropertyCondition(conditionSql,values,sc,contact,"title")
                        ||hasContactCondition;

                conditionSql.append(" ) ");
            }

            if (contacts.size() > 0) {
                conditionSql.append(" ) ");
            }
        }
        return hasContactCondition;
    }

    private boolean renderContactPropertyCondition(StringBuilder conditionSql,List values,
            SearchConfiguration sc,JSONObject contact,String propertyName){
        if (contact.containsKey(propertyName) && !"".equals(contact.getString(propertyName))) {
            conditionSql.append("  and ")
                      .append(sc.getContactField(propertyName.toLowerCase()).toString("contact"))
                      .append(" ilike '").append(addPercentageIfNecessary(contact.getString(propertyName)))
                      .append("' ");
            return true;
        }
        return false;
    }

    @SuppressWarnings("serial")
	private Map<FilterType,String> manyToManyTables = new HashMap<FilterType, String>(){{
    	put(FilterType.COMPANY, "employers");
    	put(FilterType.EDUCATION, "educations");
    	put(FilterType.SKILL, "skills");
    }};
    
    private boolean renderEducationCondition(JSONArray values,StringBuilder prefixSql,
                                       StringBuilder filterSql,String schemaname,
                                       SearchConfiguration sc,FilterType filterType,OrgContext org){
            boolean hasCondition = false;
            if(values != null){
                StringBuilder condition = new StringBuilder();
                for(int i = 0, j = values.size(); i < j; i++){
                    JSONObject value = JSONObject.fromObject(values.get(i));
                    Object groupedId = value.get("groupedid");
                    if(groupedId != null){
                    	if(condition.length() == 0){
                    		condition.append(" AND (1!=1 ");
                    	}
					condition.append(" OR ").append(filterType)
							 .append(".jss_groupby_")
							 .append(manyToManyTables.get(filterType))
							 .append("_id=")
                    		 .append(groupedId);
                    	if(value.containsKey("minYears")){
                    		if(FilterType.COMPANY.equals(filterType)){
                    			condition.append(" AND ").append(filterType).append(".year>=").append(value.getInt("minYears"));
                    		}else if(FilterType.SKILL.equals(filterType)){
                    			condition.append(" AND ").append(filterType).append(".rating>=").append(value.getInt("minYears"));
                    		}
                    		
                    	}
                    }
                }
                if(condition.length() > 0) condition.append(")");
                filterSql.append(" inner join  jss_contact_jss_groupby_") 
                	     .append(manyToManyTables.get(filterType)).append(" ").append(filterType)
                	     .append(" ON contact.id=").append(filterType).append(".jss_contact_id ")
                	     .append(condition);
                hasCondition = true;
            }
            return hasCondition;
    }
    
    private boolean renderSkillCondition(String operator, JSONArray values,StringBuilder prefixSql,
                            StringBuilder filterSql,String schemaname, 
                            SearchConfiguration sc, FilterType filterType,OrgContext org) {
        boolean hasCondition = false;
        if (values != null) {
            StringBuilder condition = new StringBuilder();
            if(operator.equals("R")){
                condition.append(setSkillCondition("and", filterType, values));
            }else{
                condition.append(setSkillCondition("or", filterType, values));                
            }
            filterSql.append(condition);
            hasCondition = true;
        }
        return hasCondition;
    }

	private String setSkillCondition(String logic, FilterType filterType,
			List<JSONObject> list) {
		StringBuilder condition = new StringBuilder();
		if (logic.equals("or")) {
			condition.append(" inner join  jss_contact_jss_groupby_")
					.append(manyToManyTables.get(filterType)).append(" ")
					.append(filterType).append(" ON contact.id=")
					.append(filterType).append(".jss_contact_id ")
					.append(" AND (1!=1 ").append("or ").append("( ");
			for (int i = 0, j = list.size(); i < j; i++) {
				JSONObject value = list.get(i);
				Object groupedId = value.get("groupedid");
				if (i != 0) {
					condition.append(" OR ");
				}
				condition.append("( ").append(filterType)
						.append(".jss_groupby_")
						.append(manyToManyTables.get(filterType))
						.append("_id=").append(groupedId);
				if (value.containsKey("minYears")) {
					condition.append(" AND ").append(filterType)
							.append(".rating>=")
							.append(value.getInt("minYears"));
				}
				condition.append(" )");
			}
			condition.append(" ) )");
		} else if (logic.equals("and") && list.size() == 1) {
			return setSkillCondition("or", filterType, list);
		} else if (logic.equals("and") && list.size() != 1) {
			for (int i = 0, j = list.size(); i < j; i++) {
				JSONObject value = list.get(i);
				Object groupedId = value.get("groupedid");
				condition.append(" inner join  jss_contact_jss_groupby_")
						.append(manyToManyTables.get(filterType)).append(" ")
						.append(filterType).append(i).append(" ON contact.id=")
						.append(filterType).append(i)
						.append(".jss_contact_id ");
				condition.append("AND ( ").append(filterType).append(i)
						.append(".jss_groupby_")
						.append(manyToManyTables.get(filterType))
						.append("_id=").append(groupedId);
				if (value.containsKey("minYears")) {
					condition.append(" AND ").append(filterType).append(i)
							.append(".rating>=")
							.append(value.getInt("minYears"));
				}
				condition.append(" )");
			}
		}
		return condition.toString();
	}
    
	private boolean renderCompanyCondition(String operator, JSONArray values,StringBuilder prefixSql,
			                    StringBuilder filterSql,String schemaname, 
			                    SearchConfiguration sc, FilterType filterType,OrgContext org) {
		boolean hasCondition = false;
		if (values != null) {
            StringBuilder condition = new StringBuilder();
            if(operator.equals("R")){
                condition.append(setCompanyCondition("and", filterType, values));
            }else{
                condition.append(setCompanyCondition("or", filterType, values));                
            }
            filterSql.append(condition);
            hasCondition = true;
        }
        return hasCondition;
	}

	private String setCompanyCondition(String logic, FilterType filterType, List<JSONObject> list) {
		StringBuilder condition = new StringBuilder();
		if (logic.equals("or")) {
			condition.append(" inner join  jss_contact_jss_groupby_")
	         .append(manyToManyTables.get(filterType)).append(" ")
	         .append(filterType).append(" ON contact.id=")
             .append(filterType).append(".jss_contact_id ")
	         .append(" AND (1!=1 ").append("or ").append("( ");
			for (int i = 0, j = list.size(); i < j; i++) {
				JSONObject value = list.get(i);
				Object groupedId = value.get("groupedid");
				if (i != 0) {
					condition.append(" OR ");
				}
				condition.append("( ").append(filterType)
						.append(".jss_groupby_")
						.append(manyToManyTables.get(filterType))
						.append("_id=").append(groupedId);
				if (value.containsKey("minYears")) {
					condition.append(" AND ").append(filterType)
							.append(".year>=")
							.append(value.getInt("minYears"));
				}
				condition.append(" )");
			}
			condition.append(" ) )");
		} else if (logic.equals("and") && list.size() == 1) {
        	return setCompanyCondition("or", filterType, list);
        } else if (logic.equals("and") && list.size() != 1)  {
			for (int i = 0, j = list.size(); i < j; i++) {
				JSONObject value = list.get(i);
				Object groupedId = value.get("groupedid");
				condition.append(" inner join  jss_contact_jss_groupby_")
                   .append(manyToManyTables.get(filterType)).append(" ")
                   .append(filterType).append(i).append(" ON contact.id=")
                   .append(filterType).append(i).append(".jss_contact_id ");
		        condition.append("AND ( ").append(filterType).append(i)
		           .append(".jss_groupby_")
		           .append(manyToManyTables.get(filterType))
		           .append("_id=").append(groupedId);
				if (value.containsKey("minYears")) {
					condition.append(" AND ").append(filterType).append(i)
							.append(".year>=")
							.append(value.getInt("minYears"));
				}
				condition.append(") ");
			}
		}
		return condition.toString();
	}

    private boolean renderLocationCondition(JSONArray locationValues,StringBuilder locationSql,
            StringBuilder conditions,String schemaname,
            SearchConfiguration sc,OrgContext org){
        boolean hasCondition = false;
        if(locationValues != null){
            StringBuilder joinCity = new StringBuilder();
            JSONObject ol;
            List<Map> cities = new ArrayList<Map>();
            for (Object location : locationValues) {
                Map city = new HashMap();
                ol = (JSONObject) location;
                double minRadius = 10;
                if(ol.containsKey("minRadius")){
                    minRadius = ol.getDouble("minRadius");
                }
                city.put("radius", minRadius);
                city.put("locationid", ol.getInt("locationid"));
                city.put("latitude", ol.getDouble("latitude"));
                city.put("longitude", ol.getDouble("longitude"));
                cities.add(city);
            }
            
            if(cities.size() != 0){
                Runner runner = datasourceManager.newSysRunner();
                for(Map city : cities){//minLat,minLng,maxLat,maxLng
                    double[] range = getAround(Double.valueOf(city.get("latitude").toString()),
                                               Double.valueOf(city.get("longitude").toString()),
                                               Double.valueOf(city.get("radius").toString()) * 1609);
                    joinCity.append(" OR (").append("contact.ts2__latitude__c>="+range[0])
                              .append(" AND contact.ts2__latitude__c<="+range[2])
                              .append(" AND contact.ts2__longitude__c>="+range[1])
                              .append(" AND contact.ts2__longitude__c<="+range[3])
                              .append(") ");
                }
                runner.close();
            }
            
            if (joinCity.length() > 0) {
                conditions.append(" AND (1!=1 ").append(joinCity).append(") ");
                locationSql.append(" ");
            }
            hasCondition = true;
       }
        return hasCondition;
    }
    
    private boolean renderCustomFilters(Map<String, JSONArray> searchValues,StringBuilder filterSql,
            StringBuilder conditions,List values,String schemaname,SearchConfiguration sc){
        boolean hasCondition = false;
        String contactTable = sc.getContact().getTable();
        for(String name : searchValues.keySet()){
            String filterName = name.substring(0, name.length()-1);//remove the s at last, example filters-->filter
            Filter filter = sc.getFilterByName(filterName);
            if (filter == null) {
                continue;
            }
            FilterField ff = filter.getFilterField();
            if(!contactTable.equals(ff.getTable())){
                JSONArray extraValues = searchValues.get(name);
                filterSql.append(" join ")
                         .append( schemaname)
                         .append( ".\"")
                         .append( ff.getTable())
                         .append( "\" on ")
                         .append(ff.toJoinToString("contact"))
                         .append(" = ")
                         .append( ff.toJoinFromString("\"" + ff.getTable() + "\""));
                if(extraValues.size()>0){
                    filterSql.append(" AND (1!=1 ");
                }
                for(int i = 0, j = extraValues.size(); i < j; i++){
                    JSONObject v = JSONObject.fromObject(extraValues.get(i));
                    filterSql.append(" OR \"").append(ff.getTable()).append("\".")
                                .append(ff.getColumn()).append("= '").append(v.get("name")).append("' ");
                }
                if(extraValues.size() > 0){
                    filterSql.append(" ) ");
                }
                hasCondition = true;
            }else{//for the contact table filter
                JSONArray extraValues = searchValues.get(name);
                if(extraValues.size() > 0){
                    conditions.append(" AND (1!=1 ");
                    for(int i = 0, j = extraValues.size(); i < j; i++){
                        JSONObject value = JSONObject.fromObject(extraValues.get(i));
                        conditions.append(" OR ").append(filterName).append("= '").append(value.get("name")).append("'");
                    }
                    conditions.append(" ) ");
                    hasCondition = true;
                }
            
            }
        }
        return hasCondition;
    }
    
    private boolean renderCustomFields(JSONArray searchValues,StringBuilder conditions,SearchConfiguration sc){
    	if(searchValues == null){
            return false;
        }
        boolean hasCondition = false;
        JSONObject jo;
        CustomField customField;
        String temp;
        JSONObject conditionsParam;
        for(int position = 0, length=searchValues.size(); position < length; position++){
            jo = searchValues.getJSONObject(position);
            customField = sc.getCustomFieldByName(jo.getString("field"));
            if(customField == null){
                continue;
            }
            temp = customField.toString("contact");
            conditionsParam = jo.getJSONObject("conditions");
            for(Object op : conditionsParam.keySet()){
                conditions.append(" AND ( 1=1 ").append(wrap(temp,customField,conditionsParam.get(op),op));
            }
            hasCondition = true;
        }
        return hasCondition;
    }
    
    private String wrap(String temp, CustomField customField, Object conditionParam, Object op){
    	StringBuilder wrapStr = new StringBuilder();
        if("String".equalsIgnoreCase( customField.getType())){
        	JSONArray terms = JSONArray.fromObject(conditionParam);
        	if(terms.size() > 0 &&("==".equals(op) || "!=".equals(op))){
        		wrapStr.append("and (");
        		for(int i = 0,j = terms.size(); i < j; i++){
        			if(i > 0){
        				wrapStr.append(" or ");
        			}
        			wrapStr.append(temp);
        			if("==".equals(op)){
        				wrapStr.append("=");
        			}else{
        				wrapStr.append("!=");
        			}
        			wrapStr.append(" '").append(terms.get(i)).append("'");
            	}
        		wrapStr.append(")");
        	}
        }else if("Date".equalsIgnoreCase( customField.getType())){
        	wrapStr.append("and").append(temp).append(op).append(" to_date('").append(conditionParam).append("','MM/DD/YYYY')");
        }else if("Boolean".equalsIgnoreCase( customField.getType())){
            if("=".equals(op) || "==".equals(op)){
            	wrapStr.append("and").append(temp).append("= ").append(conditionParam);
            }else if("!=".equals(op)){
            	wrapStr.append("and").append(temp).append("!= ").append(conditionParam);
            }
        }else if("Number".equalsIgnoreCase( customField.getType())){
        	wrapStr.append("and").append(temp).append(op).append(" ").append(conditionParam);
        }
        return wrapStr.append(" )").toString();
    }
    
    private double getDistance(double lat1, double lng1, double lat2, double lng2){
    	double EARTH_RADIUS = 6378.137;
        double radLat1 = rad(lat1);
	    double radLat2 = rad(lat2);
	    double a = radLat1 - radLat2;
	    double b = rad(lng1) - rad(lng2);
	    double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a/2),2) + 
	    Math.cos(radLat1)*Math.cos(radLat2)*Math.pow(Math.sin(b/2),2)));
	    s = s * EARTH_RADIUS;
	    s = Math.round(s * 10000) / 10000;
	    return s*0.62137;
    }
	
    private boolean hasExtraSearchColumn(SearchRequest searchRequest){
        return (searchRequest.getCustomFilters().keySet().size()>0);
    }
    
    private String addPercentageIfNecessary(String src){
        if (!src.contains("%")) {
            return src + "%";
        }
        return src;
    }
   
    private void handleResult(List<Map> results){
    	String[] temp = new String[2];
    	if(results == null || results.size() == 0){
    		return;
    	}
    	for(Map contact : results){
    		if(contact.containsKey("skill")){
    			temp = contact.get("skill").toString().split("##");
    			contact.put("skill", temp[0]);
    			contact.put("skillgroupedids", temp[1]);
    		}
    		if(contact.containsKey("education")){
    			temp = contact.get("education").toString().split("##");
    			contact.put("education", temp[0]);
    			contact.put("educationgroupedids", temp[1]);
    		}
    		if(contact.containsKey("company")){
    			temp = contact.get("company").toString().split("##");
    			contact.put("company", temp[0]);
    			contact.put("companygroupedids", temp[1]);
    		}
    		
    	}
    }

    private static Integer getCountFromExplainPlan(String explainPlan){
        String value = null;
        if(explainPlan == null){
            return null;
        }
        Matcher matcher = pattern.matcher(explainPlan);
        if(matcher.find()){
            String rowsResult = matcher.group(1).trim();
            value = rowsResult.substring(rowsResult.indexOf("=") + 1, rowsResult.length());
        }
        return ObjectUtil.getValue(value, Integer.class, 0);
    }    
 
    private static double rad(double d){
       return d * Math.PI / 180.0;
    }
    
    private  static double[] getAround(double lat,double lon,double raidus){  
        
        double latitude = lat;  
        double longitude = lon;  
          
        double degree = (24901*1609)/360.0;  
        double raidusMile = raidus;  
          
        double dpmLat = 1/degree;  
        double radiusLat = dpmLat*raidusMile;  
        double minLat = latitude - radiusLat;  
        double maxLat = latitude + radiusLat;  
          
        double mpdLng = degree*Math.cos(latitude * (Math.PI/180));  
        double dpmLng = 1 / mpdLng;  
        double radiusLng = dpmLng*raidusMile;  
        double minLng = longitude - radiusLng;  
        double maxLng = longitude + radiusLng;  
        return new double[]{minLat,minLng,maxLat,maxLng};  
    }  

    class SearchBuilder {
 
    	private StringBuilder orderSql = new StringBuilder();
    	private StringBuilder exactSearchOrderSql = new StringBuilder();
        
        StringBuilder keyWordSql = new StringBuilder();
        StringBuilder keyWordCountSql = new StringBuilder();
        List values = new ArrayList();
        private StringBuilder conditions = new StringBuilder();
        private OrgContext org;
        private SearchConfiguration sc;
        
        private StringBuilder prefixSql = new StringBuilder("");
        private StringBuilder filterSql = new StringBuilder("");
        private StringBuilder locationSql = new StringBuilder("");
        private String schemaname;
        private SearchRequest searchRequest;
        
        private boolean hasCondition = false;
        private boolean hasContactCondition = false;
        public SearchBuilder(OrgContext org){
            this.org = org;
            this.sc = searchConfigurationManager.getSearchConfiguration((String)org.getOrgMap().get("name"));
            this.schemaname =(String)org.getOrgMap().get("schemaname");
        }
        
        public SearchBuilder addKeyWord(String keyword,SearchRequest searchRequest){
        	this.searchRequest =searchRequest;
            if(!Strings.isNullOrEmpty(keyword) && keyword.length() >= 3){
                hasCondition = true;
                keyWordSql.append(getSearchValueJoinTable(keyword, values, "contact", org,searchRequest));
                keyWordCountSql.append(keyWordSql.toString());
                if(keyword.matches("^\\s*\"[^\"]+\"\\s*$")){//when exact search,add condition for resume
                	if(searchRequest.getOrder().trim().startsWith("\"id\"")&&
                           	searchRequest.isOnlyKeyWord()){
                		keyWordSql.append(" order by ").append(searchRequest.getOrder())
				  				  .append(" offset ")
				  				  .append((searchRequest.getPageIndex() - 1)* searchRequest.getPageSize())
				  				  .append(" limit ").append(searchRequest.getPageSize() + 1);
                	}
                	if(!searchRequest.isOnlyKeyWord()){
                		 exactSearchOrderSql.append(" order by ").append(searchRequest.getOrder())
				  				   .append(" offset ")
				  				   .append((searchRequest.getPageIndex() - 1)* searchRequest.getPageSize())
				  				   .append(" limit ").append(searchRequest.getPageSize() + 1);
                	}
                }else{
                	   if (searchRequest.getOrder().trim().startsWith("\"id\"")&&
                           	searchRequest.isOnlyKeyWord()) {
                           	keyWordSql.append(" order by ").append(searchRequest.getOrder())
               						  .append(" offset ")
               						  .append((searchRequest.getPageIndex() - 1)* searchRequest.getPageSize())
               						  .append(" limit ").append(searchRequest.getPageSize() + 1);
               			}else if(searchRequest.getCustomFields() != null && searchRequest.getCustomFields().size()>0){
                            exactSearchOrderSql.append(" order by ").append(searchRequest.getOrder())
                                               .append(" offset ")
                                               .append((searchRequest.getPageIndex() - 1)* searchRequest.getPageSize())
                                               .append(" limit ").append(searchRequest.getPageSize() + 1);
               			}else{
                            orderSql.append(" order by ").append(searchRequest.getOrder())
                                    .append(" offset ")
                                    .append((searchRequest.getPageIndex() - 1)* searchRequest.getPageSize())
                                    .append(" limit ").append(searchRequest.getPageSize() + 1);
                       }
                }
            }else{
            	keyWordSql.append(" select distinct contact.id from contact ");
            	keyWordCountSql.append(" select  distinct contact.id from contact ");
            	if (searchRequest.getOrder().trim().startsWith("\"id\"")) {
            		orderSql.append(" order by ").append(searchRequest.getOrder())
     						  .append(" offset ")
     						  .append((searchRequest.getPageIndex() - 1)* searchRequest.getPageSize())
     						  .append(" limit ").append(searchRequest.getPageSize() + 1);
            	}
            }
            return this;
        }
        
        public SearchBuilder addEducation(JSONArray educations){
            hasCondition = renderEducationCondition( educations,
                    prefixSql, filterSql, schemaname, sc,FilterType.EDUCATION,org)||hasCondition;
            return this;
        }
        
        
        public SearchBuilder addCompany(JSONArray companies){
            hasCondition = renderCompanyCondition(searchRequest.getCompanyOperator(), companies,
                    prefixSql, filterSql, schemaname, sc,FilterType.COMPANY,org)||hasCondition;
            return this;
        }
      
       
        public SearchBuilder addSkill(JSONArray skills){
            hasCondition = renderSkillCondition(searchRequest.getSkillOperator(), skills,
                    prefixSql, filterSql, schemaname, sc,FilterType.SKILL,org)||hasCondition;
            return this;
        }
        
        public SearchBuilder addLocation(JSONArray locations){
            hasCondition = renderLocationCondition( locations,
                    locationSql, conditions, schemaname, sc,org)||hasCondition;
            return this;
        }
              
        public SearchBuilder addObjectType(String objectType){
            if(Strings.isNullOrEmpty(objectType)){
                return this;
            }
            if ("Contact".equals(objectType)) {
                conditions.append("  and contact.\"recordtypeid\" != '").append(org.getSfid()).append("'");
                hasContactCondition = true;
            }else if("Candidate".equals(objectType)){
                conditions.append("  and contact.\"recordtypeid\" = '").append(org.getSfid()).append("'");
                hasContactCondition = true;
            }
            hasCondition = true;
            return this;
        }
        
        public SearchBuilder addStatus(String status){
            if(Strings.isNullOrEmpty(status)){
                return this;
            }
            if ("Active".equals(status)) {
                conditions.append("  and (contact.\"ts2__people_status__c\" in('', 'Active') ")
                		  .append(" OR contact.\"ts2__people_status__c\" is null )");
                hasContactCondition = true;
            } else if ("Inactive".equals(status)){
                conditions.append("  and contact.\"ts2__people_status__c\" = 'Inactive' ");
                hasContactCondition = true;
            }
            hasCondition = true;
            return this;
        }
        
        public SearchBuilder addContactFilter(JSONArray contacts){
        	hasContactCondition = renderContactConditions(conditions,values,sc,contacts)||hasContactCondition;
            return this;
        }
       
        public SearchBuilder addCustomFilter(Map searchValues){
            renderCustomFilters(searchValues, filterSql, conditions,values,schemaname, sc);
            return this;
        }
        public SearchBuilder addCustomFields(JSONArray searchValues){
            hasContactCondition = renderCustomFields(searchValues,  conditions, sc)||hasContactCondition;
            hasCondition = hasContactCondition|| hasCondition;
            return this;
        }
        public String getSearchSql(){
        	if(locationSql.length() > 0 ||
        			(searchRequest.getOrder().trim().startsWith("\"id\"")&&searchRequest.isOnlyKeyWord())||
        			hasContactCondition||!searchRequest.getOrder().trim().startsWith("\"id\"")){
        		  return " join ( "+keyWordSql+filterSql;
        	}else{
        		  return " join ( "+keyWordSql+filterSql+orderSql;
        	}
          
        }
        
        public String getCountSql(){
              return " join ( "+keyWordCountSql+filterSql;
        }
        
        public String getPrefixSql(){
            return prefixSql.toString();
        }
        
        public String getLocationSql(){
            return locationSql.toString();
        }
        
        public String getConditions(){
            if(!hasCondition){
                return conditions+" and 1!=1 ";
            }else{
                return conditions.toString();
            }
        }
        
        public String getExactSearchOrderSql(){
        	if((locationSql.length() > 0 || hasContactCondition) && searchRequest.getOrder().trim().startsWith("\"id\"")){
        		if(exactSearchOrderSql.length() == 0)
            		exactSearchOrderSql.append(" order by ").append(searchRequest.getOrder())
     						  .append(" offset ")
     						  .append((searchRequest.getPageIndex() - 1)* searchRequest.getPageSize())
     						  .append(" limit ").append(searchRequest.getPageSize() + 1);
        	}
        	return exactSearchOrderSql.toString();
        }
        
        public List getValues(){
            return values;
        }
    }
    
}

class SearchStatements {
    
    String cteSql;
    String querySql;
    String countSql;
    Object[]  values;

}