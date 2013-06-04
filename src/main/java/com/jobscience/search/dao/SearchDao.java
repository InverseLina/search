package com.jobscience.search.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.log4j.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.jobscience.search.db.DBHelper;

@Singleton
public class SearchDao {

    static private String QUERY_SELECT = "select distinct a.\"Name\", a.\"id\", a.\"Title\",to_char(a.\"CreatedDate\",'yyyy-mm-dd') as CreateDate" + " from contact a ";

    static private String QUERY_COUNT  = "select count (distinct a.id)" + " from contact a ";

    private Logger log = Logger.getLogger(SearchDao.class);
    @Inject
    private DBHelper      dbHelper;

    public SearchResult search(Map<String, String> searchValues, SearchMode searchMode, Integer pageIdx, Integer pageSize) {
        Connection con = dbHelper.getConnection();
        
        //builder statements
        SearchStatements statementAndValues = buildSearchStatements(con,searchValues,searchMode, pageIdx, pageSize);


        //excute query and caculate times
        long start = System.currentTimeMillis();
        List<Map> result = dbHelper.preparedStatementExecuteQuery(statementAndValues.queryStmt, statementAndValues.values);
        long mid = System.currentTimeMillis();
        int count = dbHelper.preparedStatementExecuteCount(statementAndValues.countStmt, statementAndValues.values);
        long end = System.currentTimeMillis();

        SearchResult searchResult = new SearchResult(result, count).setDuration(end - start).setCountDuration(mid - start).setSelectDuration(end - mid);
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
     * get education top of the most contacts by size
     * @param size
     */
    public List getTopMostEducation(Integer size) {
        if(size == null){
            size = 5;
        }
        Connection con = dbHelper.getConnection();
        String querySql = "select e.\"ts2__Name__c\" as name, count(distinct c.\"id\") as count from ts2__education_history__c e inner join  contact c on c.\"sfId\"=e.\"ts2__Contact__c\"  where e.\"ts2__Name__c\" !='' group by e.\"ts2__Name__c\"  order by count desc limit "+size;
        List<Map> result = dbHelper.preparedStatementExecuteQuery(dbHelper.prepareStatement(con,querySql.toString()), new Object[0]);
        return result;
    }
    
    /**
     * get company top of the most contacts by size
     * @param size
     */
    public List getTopMostCompanies(Integer size) {
        if(size == null){
            size = 5;
        }
        Connection con = dbHelper.getConnection();
        String querySql = "select \"ts2__Name__c\" as name, count(distinct c.\"id\") as count from ts2__employment_history__c e inner join  contact c on c.\"sfId\"=e.\"ts2__Contact__c\"  where e.\"ts2__Name__c\" !='' group by e.\"ts2__Name__c\"  order by count desc limit "+size;
        List<Map> result = dbHelper.preparedStatementExecuteQuery(dbHelper.prepareStatement(con,querySql.toString()), new Object[0]);
        return result;
    }

    /**
     * @param searchValues
     * @param searchMode
     * @return SearchStatements
     */
    private SearchStatements buildSearchStatements(Connection con, Map<String, String> searchValues,SearchMode searchMode,
                                                   Integer pageIdx, Integer pageSize) {
        SearchStatements ss = new SearchStatements();
        if(pageIdx < 1){
            pageIdx = 1;
        }
        int offset = (pageIdx -1) * pageSize;

        //the select query  that will query data
        StringBuilder querySql = new StringBuilder();
        //the count query sql that will query the count of data
        StringBuilder countSql = new StringBuilder();
        // the part of query that build join tables sql
        StringBuilder joinTables = new StringBuilder();
        // the part of query that build conditions sql
        StringBuilder conditions = new StringBuilder();
        // the params will be put in sql
        List values = new ArrayList();
        
        //to test if need to add "where", if true, add ' where 1=1 ', so that will join " and .." condition
        boolean hasCondition = false;
        
        querySql.append(QUERY_SELECT);
        countSql.append(QUERY_COUNT);
        
        if(searchValues!=null){
            //keyword mode, use ilike search
            if(searchMode == SearchMode.KEYWORD){
                
                //add the 'search' filter
                if (searchValues.get("search") != null && !"".equals(searchValues.get("search"))) {
                    hasCondition = true;
                    String value = searchValues.get("search");
                    conditions.append(" and ( a.\"Name\" ilike ? or a.\"Title\" ilike ?) ");
                    if(!value.contains("%")){
                        value = "%" + value + "%";
                    }
                    values.add(value);
                    values.add(value);
                }

                //add the 'FirstName' filter
                if (searchValues.get("FirstName") != null && !"".equals(searchValues.get("FirstName"))) {
                    hasCondition = true;
                    String value = searchValues.get("FirstName");
                    conditions.append(" and a.\"FirstName\" ilike ? ");
                    if(!value.contains("%")){
                        value = "%" + value + "%";
                    }
                    values.add(value);
                }

                //add the 'LastName' filter
                if (searchValues.get("LastName") != null && !"".equals(searchValues.get("LastName"))) {
                    hasCondition = true;
                    String value = searchValues.get("LastName");
                    conditions.append(" and a.\"LastName\" ilike ? ");
                    if(!value.contains("%")){
                        value = "%" + value + "%";
                    }
                    values.add(value);
                }
                boolean searchEmployment = false;
                //add the 'Title' filter
                if (searchValues.get("Title") != null && !"".equals(searchValues.get("Title"))) {
                    String value = searchValues.get("Title");
                    hasCondition = true;
                    if(searchValues.get("curTitle")!=null){

                        conditions.append(" and a.\"Title\" ilike ? ");
                        if(!value.contains("%")){
                            value = "%" + value + "%";
                        }
                        values.add(value);
                    }else{
                        searchEmployment = true;
                        joinTables.append(" left outer join ts2__employment_history__c c on a.\"sfId\" = c.\"ts2__Contact__c\" ");
                        conditions.append(" and ( c.\"ts2__Job_Title__c\" ilike ?  or a.\"Title\" ilike ? )");
                        if(!value.contains("%")){
                            value = "%" + value + "%";
                        }
                        values.add(value);
                        values.add(value);
                    }

                }
               
                //add the 'Skill' filter, and join Skill table
                if (searchValues.get("Skill") != null && !"".equals(searchValues.get("Skill"))) {
                    hasCondition = true;
                    String value = searchValues.get("Skill");
                    joinTables.append(" inner join ts2__skill__c b on a.\"sfId\" = b.\"ts2__Contact__c\" ");
                    conditions.append(" and b.\"ts2__Skill_Name__c\" ilike ? ");
                    if(!value.contains("%")){
                        value = "%" + value + "%";
                    }
                    values.add(value);
                }
                


                //add the 'Company' filter, and join Employment table
                if (searchValues.get("Company") != null && !"".equals(searchValues.get("Company"))) {
                	 hasCondition = true;
                     String value = searchValues.get("Company");
                     if(searchEmployment){
                    	 conditions.append(" and  c.\"ts2__Name__c\" ilike ? ");
                     }else{
	                     joinTables.append(" left outer join ts2__employment_history__c c on a.\"sfId\" = c.\"ts2__Contact__c\" ");
	                     conditions.append(" and  c.\"ts2__Name__c\" ilike ? ");
                     }

                    if(searchValues.get("curCompany") != null){
                        conditions.append(" and c.\"ts2__Employment_End_Date__c\" is  null ");
                    }

                     if(!value.contains("%")){
                         value = "%" + value + "%";
                     }
                     values.add(value);
                }
                
                //add the 'Education' filter, and join Education table
                if (searchValues.get("Education") != null && !"".equals(searchValues.get("Education"))) {
                    hasCondition = true;
                    String value = searchValues.get("Education");
                    joinTables.append(" inner join ts2__education_history__c d on a.\"sfId\" = d.\"ts2__Contact__c\" ");
                    conditions.append(" and d.\"ts2__Name__c\" ilike ? ");
                    if(!value.contains("%")){
                        value = "%" + value + "%";
                    }
                    values.add(value);
                }
                
                
            // the simple mode, use full text search
            }else if(searchMode == SearchMode.SIMPLE){
                hasCondition = true;
                String value = searchValues.get("search");
                String searchTsq = Joiner.on(" & ").join(Splitter.on(" ").omitEmptyStrings().split(value));
                String searchILike = value;
                if (!searchILike.contains("%")) {
                    searchILike = "%" + value + "%";
                }
                conditions.append(" and (a.resume_tsv @@ to_tsquery(?) or a.\"Title\" ilike ? or a.\"Name\" ilike ? )");
                values.add(searchTsq);
                values.add(searchILike);
                values.add(searchILike);
            }else if(searchMode == SearchMode.ADVANCED){
                
                if (searchValues.get("search") != null && !"".equals(searchValues.get("search"))) {
                    hasCondition = true;
                    String value = searchValues.get("search");
                    System.out.println("........"+value);
                    String searchTsq = Joiner.on(" & ").join(Splitter.on(" ").omitEmptyStrings().split(value));
                    System.out.println("========"+searchTsq);
                    String searchILike = value;
                    if (!searchILike.contains("%")) {
                        searchILike = "%" + value + "%";
                    }
                    conditions.append(" and (a.resume_tsv @@ to_tsquery(?) or a.\"Title\" ilike ? or a.\"Name\" ilike ? )");
                    values.add(searchTsq);
                    values.add(searchILike);
                    values.add(searchILike);
                }
                
                //add the 'educationNames' filter, and join Education table
                if (searchValues.get("educationNames") != null && !"".equals(searchValues.get("educationNames"))) {
                    hasCondition = true;
                    String value = searchValues.get("educationNames");
                    if(!"All Education".equals(value)){
	                    String[] educationNames = value.split(","); 
	                    joinTables.append(" inner join ts2__education_history__c d on a.\"sfId\" = d.\"ts2__Contact__c\" ");
	                    conditions.append(" and d.\"ts2__Name__c\" in ");
	                    for(int i = 0; i < educationNames.length; i++){
	                        if(i == 0){
	                            conditions.append("(");
	                        }else{
	                            conditions.append(",");
	                        }
	                        conditions.append("?");
	                        if(i == educationNames.length - 1){
	                            conditions.append(")");
	                        }
	                        values.add(educationNames[i]);
	                    }
                    }
                }
                
                //add the 'companyNames' filter, and join Education table
                if (searchValues.get("companyNames") != null && !"".equals(searchValues.get("companyNames"))) {
                    hasCondition = true;
                    String value = searchValues.get("companyNames");
                    if(!"All Company".equals(value)){
	                    String[] companyNames = value.split(","); 
	                    joinTables.append(" inner join ts2__employment_history__c c on a.\"sfId\" = c.\"ts2__Contact__c\" ");
	                    conditions.append(" and c.\"ts2__Name__c\" in ");
	                    for(int i = 0; i < companyNames.length; i++){
	                        if(i == 0){
	                            conditions.append("(");
	                        }else{
	                            conditions.append(",");
	                        }
	                        conditions.append("?");
	                        if(i == companyNames.length - 1){
	                            conditions.append(")");
	                        }
	                        values.add(companyNames[i]);
	                    }
                    }
                }
                
                
            }
        }
        
        querySql.append(joinTables);
        countSql.append(joinTables);
        
        if(hasCondition){
            String whereStr = " where 1=1 ";
            querySql.append(whereStr);
            countSql.append(whereStr);
        }
        
        querySql.append(conditions);
        countSql.append(conditions);
        
        querySql.append(" offset ").append(offset).append(" limit ").append(pageSize);
        
        log.debug(querySql);
        // build the statement
        ss.queryStmt = dbHelper.prepareStatement(con,querySql.toString());
        ss.countStmt = dbHelper.prepareStatement(con,countSql.toString());
        ss.values = values.toArray();

        return ss;
    }
    
}


class SearchStatements {

    PreparedStatement queryStmt;
    PreparedStatement countStmt;
    Object[]          values;

}
