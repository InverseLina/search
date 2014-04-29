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

    private Logger log = LoggerFactory.getLogger(SearchDao.class);
    
    @Inject
    private DatasourceManager datasourceManager;

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
    
    @Inject
    CurrentRequestContextHolder crch;  
   
    private static Pattern pattern = Pattern.compile("\\srows=(\\d*)\\s",Pattern.CASE_INSENSITIVE);

    private static int FAST_COUNT_TRESHOLD = 2000;
    private static int EXACT_COUNT_TIMEOUT = 1000;//ms
    /**
     * @param searchColumns
     * @param searchValues
     * @param pageIdx
     * @param pageSize
     * @param orderCon
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
		if(user!=null){
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
    public SearchResult getGroupValuesForAdvanced(Map<String, String> searchValues, String type,String queryString,Boolean orderByCount,String min,Integer pageSize,Integer pageNum,OrgContext org) throws SQLException {
        String advancedAutoCompleteStr = configManager.getConfig("advanced_auto_complete", (Integer) org.getOrgMap().get("id"));
        Boolean advancedAutoComplete  = false;
        if(advancedAutoCompleteStr!=null){
            advancedAutoComplete= "true".equals(advancedAutoCompleteStr);
        }
        if(!advancedAutoComplete){
            return simpleAutoComplete(searchValues, type, queryString, orderByCount, min, pageSize, pageNum,org);
        }
        return advancedAutoComplete(searchValues, type, queryString, orderByCount, min, pageSize, pageNum,org);
    }
    
    @SuppressWarnings("unused")
    public SearchResult advancedAutoComplete(Map<String, String> searchValues, String type,String queryString,Boolean orderByCount,String min,Integer pageSize,Integer pageNum,OrgContext org) throws SQLException {
        StringBuilder querySql = new StringBuilder();
        StringBuilder groupBy = new StringBuilder();
        String column = null;
        String baseTableIns = null;
        querySql.append("select result.name, count(*) as count from ( select ");
        String baseTable = new String();
        List values = new ArrayList();
        String schemaname = (String)org.getOrgMap().get("schemaname");
        
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
           String skillAssessmentRatingStr = configManager.getConfig("skill_assessment_rating", (Integer)org.getOrgMap().get("id"));
            if (!"true".equals(skillAssessmentRatingStr)) {
                skill_assessment_rating = false;
            } else {
                skill_assessment_rating = true;
            }
            appendJoinTable=(" inner join "+schemaname+".ts2__assessment__c ass on ass.\"ts2__skill__c\"=b.\"sfid\" ");
        }
        //-------- /get the skill_assessment_rating config for current org ---------//
        
        //Need to DO refactor here
        //querySql.append(renderSearchCondition(searchValues,"advanced",baseTable,baseTableIns,values,appendJoinTable,org)[0]);
        
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
        Runner runner = datasourceManager.newOrgRunner((String)org.getOrgMap().get("name"));
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
    
    /**
     * boolean search handler for search box
     * @param searchValue
     * @param type
     * @param values
     * @return
     */
    public  String booleanSearchHandler(String searchValue,String type,OrgContext org,boolean exact){
        String schemaname = (String)org.getOrgMap().get("schemaname");
    	SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.getOrgMap().get("name"));
    	StringBuilder sb = new StringBuilder();
    	searchValue = searchValue.replaceAll("[\\(\\)%\\^\\@#~\\*]", "").trim();
    	if(!exact){
        	if(!searchValue.contains("NOT ")&&
        	   !searchValue.contains("AND ")&&
        	   !searchValue.contains("NOT ")){
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
    	if(type==null||"OR".equals(type)){//if params split with space or "OR",we do in OR logic
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
        	for(int i=0;i<andConditions.length;i++){
        	    hasSearch = true;
        		String andCondition = andConditions[i];
    			if(i==0){
    				sb.append(" select n_ext0.id as id,n_ext0.sfid as sfid from ");
    			}
        		sb.append(booleanSearchHandler(andCondition, "NOT",org,exact)+(i));
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
    		    +".jss_contact ex where "+renderKeywordSearch(temp,org,exact,"ex")  );
    		
    		if(notConditions.length==1){
    			sb.append(") n_ext");
    		}else{
    			sb.append(") n_ext");
    		}
    		
    		boolean hasNot = false;
        	for(int i=1;i<notConditions.length;i++){
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
        Runner runner = datasourceManager.newOrgRunner((String)org.get("name"));
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

    protected SearchResult executeSearch(SearchStatements statementAndValues,SearchRequest searchRequest,OrgContext org){
    	Runner runner = datasourceManager.newOrgRunner(org.getOrgMap().get("name").toString());
    	SearchResult searchResult = null;
    	try{
    		long start = System.currentTimeMillis();
    		List<Map> result = runner.executeQuery(statementAndValues.querySql, statementAndValues.values);
    		long mid = System.currentTimeMillis();
    		int count = 0;
    		boolean exactCount = false;
    		
    		//when the search result less than page size,this would be the last page,we just calculate the count
    		if(result.size()<searchRequest.getPageSize()){
    			count = searchRequest.getOffest()+result.size();
    			exactCount = true;
    		}
    		
    		//only this page is not the last page,we do calculating count
    		if(count==0){
	    		/********** Get the estimate count **********/
	    		List<Map> explainPlans= runner.executeQuery("explain "+statementAndValues.cteSql
						 									+" select  distinct(a.id)  "
						 									+statementAndValues.countSql,
						 									statementAndValues.values);
	    		if(explainPlans.size()>0){
	    			count = getCountFromExplainPlan((String)explainPlans.get(0).get("QUERY PLAN"));
	    		}
	    		/********** /Get the estimate count **********/
	    		
	    		/********** Get the exact count **********/
	    		if(count<=FAST_COUNT_TRESHOLD){
	    			try{
	    				runner.executeUpdate("SET statement_timeout TO "+EXACT_COUNT_TIMEOUT+";");
	    				int exact= runner.executeCount(statementAndValues.cteSql
								+" select  count(distinct a.id) as count  "
								+statementAndValues.countSql);
	    				count = exact;
	    				exactCount = true;
	    			}catch(Exception e){
	    				log.debug("The count search timeout,use the estimate count");
	    			}
	    		}
	    		
	    		/********** /Get the exact count **********/
	    		
    		}
    		long end = System.currentTimeMillis();
    
    		searchResult = new SearchResult(result, count)
    				.setDuration(end - start)
    				.setSelectDuration(mid - start)
    				.setCountDuration(end - mid)
    				.setExactCount(exactCount);
    	}finally{
    		runner.executeUpdate("RESET statement_timeout; ");
    		runner.close();
    	}
    
    	return searchResult;
    }

    protected SearchResult simpleAutoComplete(Map<String, String> searchValues, String type,String queryString,Boolean orderByCount,String min,Integer pageSize,Integer pageNum,OrgContext org) throws SQLException {
        SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.getOrgMap().get("name"));
        Filter filter = sc.getFilterByName(type);
        if(filter==null){
             return null;
        }
        FilterType f = filter.getFilterType();
        String baseTable = "";
        if(f==null&&!type.equals("location")){
            baseTable=filter.getFilterField().getTable();
        }else if(f.equals(FilterType.COMPANY)){
            baseTable =  "jss_grouped_employers ";
        }else if(f.equals(FilterType.EDUCATION)){
            baseTable =  "jss_grouped_educations ";
        }else if(f.equals(FilterType.SKILL)){
            baseTable =  "jss_grouped_skills ";
        }else if(f.equals(FilterType.LOCATION)){
            baseTable =  "jss_grouped_locations ";
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
     * @param searchValues
     * @return SearchStatements
     */
    protected SearchStatements buildSearchStatements(SearchRequest searchRequest,OrgContext org) {
        SearchStatements ss = new SearchStatements();
        SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.getOrgMap().get("name"));
        int offset = (searchRequest.getPageIndex() -1) * searchRequest.getPageSize();
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
        if(searchRequest.getContacts()!=null){
            hasContactsCondition = searchRequest.getContacts().size()>0;
        }
        if(!hasExtraSearchColumn(searchRequest)||hasContactsCondition||(Strings.isNullOrEmpty(search)||search.length()<3)||(locationValues!=null)){
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
    	if(!Strings.isNullOrEmpty(searchRequest.getOrder())){
    		querySql.append(" order by "+searchRequest.getOrder());
    		if(!searchRequest.getOrder().trim().startsWith("\"id\"")){
    			querySql.append(" offset ").append(offset).append(" limit ").append(searchRequest.getPageSize());
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
               if(search.length()>=3){
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
            hasCondition = renderFilterCondition( searchRequest.getEducations(),
                    prefixSql, querySql, schemaname, sc,FilterType.EDUCATION,org)||hasCondition;
           // add the 'companies' filter, and join ts2__employment_history__c table
           hasCondition = renderFilterCondition( searchRequest.getCompanies(),
                   prefixSql, querySql, schemaname, sc,FilterType.COMPANY,org)||hasCondition;
           
           // add the 'skillNames' filter, and join ts2__skill__c table
           hasCondition = renderFilterCondition(searchRequest.getSkills(),
                   prefixSql, querySql, schemaname, sc,FilterType.SKILL,org)||hasCondition;
           
           //add the 'radius' filter
           hasCondition = renderLocationCondition(searchRequest.getLocations(), locationSql,
                   conditions, schemaname,sc)||hasCondition;
       }
       //at last,combine all part to complete sql
        
       values.addAll(subValues);
    
       hasCondition = renderCustomFilters(searchRequest.getCustomFilters(), querySql, conditions,values,schemaname, sc)
                      ||hasCondition;
    
       //if there has no condition,just append 1!=1
       if(!hasCondition){
           conditions.append(" and 1!=1 ");
       }
    
       if(hasSearchValue){
           querySql=new StringBuilder(" join (").append(querySql);
       }
       return new String[]{querySql.toString(),prefixSql.toString(),conditions.toString(),locationSql.toString()};
    }

    /**
     * Get the query column and add group by or join table if needed
     * @param orginalName
     * @param columnJoinTables
     * @param groupBy
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
    	//return orginalName;
    }
    
    /**
     * get the search columns for outer sql block
     * @param searchColumns
     * @return
     */
    private String getSearchColumnsForOuter(String searchColumns,OrgContext org){
        SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.getOrgMap().get("name"));
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
    	 if(searchColumns==null){//a.phone,
             columnsSql.append(sc.toContactFieldsString("a"));
             //,a.phone
             groupBy.append(","+sc.toContactFieldsString("a"));
    	 }else{
    		 String temp = "";
    		 StringBuffer sb = new StringBuffer("id,name,sfid,");
 	         for(String column:searchColumns.split(",")){
 	        	temp = getQueryColumnName(column,columnJoinTables,groupBy,sb,org);
 	        	if(!temp.trim().equals("")){
	 	            columnsSql.append(temp);
	 	            columnsSql.append(",");
 	        	}
 	        }
 	        columnsSql.append("a.id,a.name,a.sfid");//,a.phone,
 	        if(groupBy.length()>0){
 	        	groupBy.append(",");
 	        }
 	        groupBy.append("a.name,a.sfid");//always return these columns ,
         }
    	 return columnsSql.toString();
    }
    
    /**
     * Get condition for Search logic 
     * @param searchValue the value typed in search box 
     * @param searchValues all other search parameters
     * @param values 
     * @param orderCon
     * @param offset
     * @param pageSize
     * @return first for query sql,second for query sql
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
          .addStatus(searchRequest.getStatus());
        values.addAll(sb.getValues());
       
        
        String condition = sb.getConditions();
        boolean hasContactsCondition = false;
        String locationSql = sb.getLocationSql();
        joinSql = new StringBuilder(sb.getSearchSql());
        
        
        
        prefixSql = sb.getPrefixSql();
        countSql = new StringBuilder(sb.getCountSql());
        if(searchRequest.getContacts()!=null){
            hasContactsCondition = searchRequest.getContacts().size()>0;
        }
        
        joinSql.append(" ) subcontact on contact.id=subcontact.id ");
        countSql.append(" ) subcontact on contact.id=subcontact.id ");
        
        joinSql.append(locationSql);
        countSql.append(locationSql);
        
        joinSql.append(" where 1=1 ").append(condition).append(sb.getExactSearchOrderSql());
        countSql.append(" where 1=1 ").append(condition);
       
       
        joinSql.append(") a ");
        if(!hasExtraSearchColumn(searchRequest)||hasContactsCondition||locationSql.length()>0||
                Strings.isNullOrEmpty(searchRequest.getKeyword())||searchRequest.getKeyword().length()<3){
            countSql.append(") a ");
        }
        return new String[]{joinSql.toString(),countSql.toString(),prefixSql};
    }
    
    /**
     * get the table joined for auto complete by type
     * @param type available value : company,education,skill and location
     * @return
     */
    private String getAdvancedJoinTable(String type,OrgContext org){
        StringBuilder joinSql = new StringBuilder();
        String schemaname = (String)org.getOrgMap().get("schemaname");
        
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
    private String getSearchValueJoinTable(String keyword, List values,String alias,OrgContext org,SearchRequest searchRequest){
        StringBuilder joinSql = new StringBuilder();
        if("a".equals(alias)){
            joinSql.append(" right join (");
            joinSql.append(booleanSearchHandler(keyword, null, org,false));
            joinSql.append(")  a_ext on a_ext.id = "+alias+".id ");
            return joinSql.toString();
        }else{
        	boolean exact = keyword.matches("^\\s*\"[^\"]+\"\\s*$");
            joinSql.append(" select  distinct contact.id,contact.sfid  from (");
            if((!keyword.contains("NOT ")&&
         	   !keyword.contains("AND ")&&
        	   !keyword.contains("NOT ")&&
        	   searchRequest.isOnlyKeyWord())||exact
        	   ){
            	if(exact){
            		if(searchRequest.isOnlyKeyWord()){
            			joinSql = new StringBuilder();
            		}
            		
            		joinSql.append("  select contact.id,contact.sfid from  ")
                 		   .append(org.getOrgMap().get("schemaname"))
                	       .append(".contact  where ")
                	       .append(" contact.\"ts2__text_resume__c\" like '")
                           .append(keyword.replaceAll("\\\"", "%"))
                           .append("' ");
            		if(!searchRequest.isOnlyKeyWord()){
            			 joinSql.append(")  contact ");
            		}
            		return joinSql.toString();
            	}else{
            	return "  select contact.id,contact.sfid from  "+
            		   org.getOrgMap().get("schemaname")+
            	       ".jss_contact contact where "+
            			renderKeywordSearch(keyword.trim().replaceAll("\\s+", "|"),org,exact,"contact") + "  ";
            	}
   
            }else{
            	joinSql.append(booleanSearchHandler(keyword, null, org,false));
            }
    	    joinSql.append(")  contact ");
    	    return joinSql.toString();
	    }
    }
    private String renderKeywordSearch(String param,OrgContext org,boolean exact,String alias){
        SearchConfiguration sc = searchConfigurationManager.getSearchConfiguration((String)org.getOrgMap().get("name"));
        StringBuilder sb = new StringBuilder();
        String exactFilter = "";
        exact = false;
        if(exact){
            exactFilter=" ts_rank(resume_tsv,'"+param.replaceAll("\\s+", "&")+"')>0 AND (";
        }
        for(Field f:sc.getKeyword().getFields()){
            sb.append("OR ").append(f.toString(alias)).append("@@ to_tsquery(")
              .append("'")
              .append(param)
              .append("')");
        }
        return exactFilter+sb.delete(0, 2).toString()+(exact?")":"");
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
    
    private boolean hasExtraSearchColumn(SearchRequest searchRequest){
        return (searchRequest.getCustomFilters().keySet().size()>0);
    }
    
    private String addPercentageIfNecessary(String src){
        if (!src.contains("%")) {
            return src + "%";
        }
        return src;
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
        if (contacts!=null) {
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


    private boolean renderFilterCondition(JSONArray educationValues,StringBuilder prefixSql,
                                       StringBuilder filterSql,String schemaname,
                                       SearchConfiguration sc,FilterType filterType,OrgContext org){
        {
            boolean hasCondition = false;
            boolean temp = false;
            if(educationValues!=null){
                if(prefixSql.length()==0){
                    prefixSql.append(" with ");
                }else{
                    prefixSql.append(",");
                }
                FilterField f = sc.getFilter(filterType).getFilterField();
                
                prefixSql.append(filterType)
                         .append( "_ect  as (select "+f.toJoinToString(filterType+"_ect_inner")+" as \"ts2__contact__c\" ")
                         .append(" from  "+schemaname+"."+f.getTable()+" "+filterType+"_ect_inner" );
                
                //when for skill,need join the ts2__assessment__c
                if(filterType==FilterType.SKILL&&educationValues.contains("minYears")){
                    //Get the skill_assessment_rating for current org,if true,will join with ts2__assessment__c
                    String skillAssessmentRatingStr = configManager.getConfig("skill_assessment_rating", (Integer)org.getOrgMap().get("id"));
                    if(!"true".equals(skillAssessmentRatingStr)){
                        temp = false;
                    }else{
                        prefixSql.append(" inner join "+schemaname+".ts2__assessment__c ass on ass.\"ts2__skill__c\"="+filterType+"_ect_inner"+".\"sfid\" ");
                        temp = true;
                    }
                }
                
                prefixSql.append("  where (1!=1 ");
               
                for(int i=0,j=educationValues.size();i<j;i++){
                    JSONObject educationValue = JSONObject.fromObject(educationValues.get(i));
                    prefixSql.append(" OR ( "+f.toString(filterType+"_ect_inner")+" = ")
                             .append("'"+educationValue.get("name").toString().replaceAll("\'", "\'\'")+"'");
                    if(educationValue.containsKey("minYears")){
                        Integer minYears = educationValue.getInt("minYears");
                        if(!minYears.equals(0)){
                            if(FilterType.COMPANY==filterType){
                                prefixSql.append(" AND EXTRACT(year from age("+filterType+"_ect_inner.\"ts2__employment_end_date__c\","+filterType+"_ect_inner.\"ts2__employment_start_date__c\"))>="+minYears);
                            }else if(FilterType.EDUCATION==filterType){
                                prefixSql.append(" AND EXTRACT(year from age(now(),"+filterType+"_ect_inner.\"ts2__graduationdate__c\"))>="+minYears);
                            }else if(FilterType.SKILL==filterType){
                                if(temp){
                                    prefixSql.append(" AND ass.\"ts2__rating__c\" >="+minYears);
                                }else{
                                    prefixSql.append(" AND "+filterType+"_ect_inner.\"ts2__rating__c\" >="+minYears);
                                }
                            }
                           
                        }
                    }
                    prefixSql.append(" ) ");
                }
                prefixSql.append(") ) ");
                filterSql.append(" inner join  "+filterType+"_ect on " + f.toJoinFromString("contact") + " =  "+filterType+"_ect.\"ts2__contact__c\" ");
                hasCondition = true;
            }
            return hasCondition;
        }
    }
    
    private boolean renderLocationCondition(JSONArray locationValues,StringBuilder locationSql,
            StringBuilder conditions,String schemaname,
            SearchConfiguration sc){
        boolean hasCondition = false;
        Field f = sc.getContactField(ContactFieldType.MAILINGPOSTALCODE);
        if(locationValues!=null){
            StringBuilder joinZip = new StringBuilder();
            StringBuilder joinCity = new StringBuilder();
            StringBuilder cities = new StringBuilder();
            JSONObject ol;
            Map<String,Double> citiesWithRadius = new HashMap<String,Double>();
            for (Object location : locationValues) {
                ol = (JSONObject) location;
                String name = (String) ol.get("name");
                if(ol.containsKey("minRadius")){
                    double minRadius = ol.getDouble("minRadius");
                    citiesWithRadius.put(name, minRadius);
                    cities.append("'"+name+"',");
                    joinZip.append(" OR  z.\"city\"='").append(name).append("' ");
                }else{
                    joinZip.append(" OR  z.\"city\"='").append(name).append("' ");
                }
            }
            
            if(cities.length()!=0){
                Runner runner = datasourceManager.newSysRunner();
                List<Map> c = runner.executeQuery("select * from city where name in("+cities+"'#')");
                for(Map city:c){//minLat,minLng,maxLat,maxLng
                    double[] range = getAround(Double.valueOf(city.get("latitude").toString()),
                                               Double.valueOf(city.get("longitude").toString()),
                                               citiesWithRadius.get(city.get("name"))*1000);
                    joinCity.append(" OR (").append("contact.ts2__latitude__c>="+range[0])
                              .append(" AND contact.ts2__latitude__c<="+range[2])
                              .append(" AND contact.ts2__longitude__c>="+range[1])
                              .append(" AND contact.ts2__longitude__c<="+range[3])
                              .append(") ");
                }
                runner.close();
            }
            if(joinZip.length()>0){
                if(joinCity.length()>0){
                    locationSql.append(" left ");
                }
                locationSql.append(" join  jss_sys.zipcode_us z on (")
                           .append(f.toString("contact")+" =z.\"zip\" AND (1!=1 ")
                           .append(joinZip)
                           .append(")) ");
                conditions.append(" AND (1!=1 ").append(joinCity).append(" OR z.city is not null ").append(") ");
            }else{
                if(joinCity.length()>0){
                    conditions.append(" AND (1!=1 ").append(joinCity).append(") ");
                    locationSql.append(" ");
                }
            }
            hasCondition = true;
       }
        return hasCondition;
    }
    
    
    private boolean renderCustomFilters(Map<String, JSONArray> searchValues,StringBuilder filterSql,
            StringBuilder conditions,List values,String schemaname,SearchConfiguration sc){
        boolean hasCondition = false;
        String contactTable = sc.getContact().getTable();
        for(String name:searchValues.keySet()){
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
                for(int i=0,j=extraValues.size();i<j;i++){
                    JSONObject v = JSONObject.fromObject(extraValues.get(i));
                    filterSql.append(" OR \"").append(ff.getTable()).append("\".")
                                .append(ff.getColumn()).append("= '").append(v.get("name")).append("' ");
                }
                if(extraValues.size()>0){
                    filterSql.append(" ) ");
                }
                hasCondition = true;
            }else{//for the contact table filter
                JSONArray extraValues =searchValues.get(name);;
                if(extraValues.size()>0){
                    conditions.append(" AND (1!=1 ");
                    for(int i=0,j=extraValues.size();i<j;i++){
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
    
    
    private  double[] getAround(double lat,double lon,double raidus){  
        
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
            if(!Strings.isNullOrEmpty(keyword)&&keyword.length()>=3){
                hasCondition = true;
                keyWordSql.append(getSearchValueJoinTable(keyword, values, "contact", org,searchRequest));
                keyWordCountSql.append(keyWordSql.toString());
                if(keyword.matches("^\\s*\"[^\"]+\"\\s*$")){//when exact search,add condition for resume
                	if(searchRequest.getOrder().trim().startsWith("\"id\"")&&
                           	searchRequest.isOnlyKeyWord()){
                		keyWordSql.append(" order by ").append(searchRequest.getOrder())
				  				  .append(" offset ")
				  				  .append((searchRequest.getPageIndex() - 1)* searchRequest.getPageSize())
				  				  .append(" limit ").append(searchRequest.getPageSize());
                	}
                	if(!searchRequest.isOnlyKeyWord()){
//                		 conditions.append(" AND lower(contact.\"ts2__text_resume__c\") like '")
//			                       .append(keyword.replaceAll("\\\"", "%").toLowerCase())
//			                       .append("'");
                		 exactSearchOrderSql.append(" order by ").append(searchRequest.getOrder())
				  				   .append(" offset ")
				  				   .append((searchRequest.getPageIndex() - 1)* searchRequest.getPageSize())
				  				   .append(" limit ").append(searchRequest.getPageSize());
                	}
                }else{
                	   if (searchRequest.getOrder().trim().startsWith("\"id\"")&&
                           	searchRequest.isOnlyKeyWord()) {
                           	keyWordSql.append(" order by ").append(searchRequest.getOrder())
               						  .append(" offset ")
               						  .append((searchRequest.getPageIndex() - 1)* searchRequest.getPageSize())
               						  .append(" limit ").append(searchRequest.getPageSize());
               			}else{
               				orderSql.append(" order by ").append(searchRequest.getOrder())
			      					.append(" offset ")
			      					.append((searchRequest.getPageIndex() - 1)* searchRequest.getPageSize())
			      					.append(" limit ").append(searchRequest.getPageSize());
               			}
                }
            }else{
            	keyWordSql.append(" select distinct contact.id from contact ");
            	keyWordCountSql.append(" select distinct contact.id from contact ");
            	if (searchRequest.getOrder().trim().startsWith("\"id\"")) {
            		orderSql.append(" order by ").append(searchRequest.getOrder())
     						  .append(" offset ")
     						  .append((searchRequest.getPageIndex() - 1)* searchRequest.getPageSize())
     						  .append(" limit ").append(searchRequest.getPageSize());
            	}
            }
            return this;
        }
        
        public SearchBuilder addEducation(JSONArray educations){
            hasCondition = renderFilterCondition( educations,
                    prefixSql, filterSql, schemaname, sc,FilterType.EDUCATION,org)||hasCondition;
            return this;
        }
        
        
        public SearchBuilder addCompany(JSONArray companies){
            hasCondition = renderFilterCondition( companies,
                    prefixSql, filterSql, schemaname, sc,FilterType.COMPANY,org)||hasCondition;
            return this;
        }
      
       
        public SearchBuilder addSkill(JSONArray skills){
            hasCondition = renderFilterCondition( skills,
                    prefixSql, filterSql, schemaname, sc,FilterType.SKILL,org)||hasCondition;
            return this;
        }
        
        public SearchBuilder addLocation(JSONArray locations){
            hasCondition = renderLocationCondition( locations,
                    locationSql, conditions, schemaname, sc)||hasCondition;
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
        
        public String getSearchSql(){
        	if(locationSql.length()>0||
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
        	if(hasContactCondition&&searchRequest.getOrder().trim().startsWith("\"id\"")){
        		if(exactSearchOrderSql.length()==0)
            		exactSearchOrderSql.append(" order by ").append(searchRequest.getOrder())
     						  .append(" offset ")
     						  .append((searchRequest.getPageIndex() - 1)* searchRequest.getPageSize())
     						  .append(" limit ").append(searchRequest.getPageSize());
        	}
        			
        	return exactSearchOrderSql.toString();
        }
        
        public List getValues(){
            return values;
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
}

class SearchStatements {
    
    String cteSql;
    String querySql;
    String countSql;
    Object[]          values;

}


