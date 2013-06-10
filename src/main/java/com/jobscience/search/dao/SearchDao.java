package com.jobscience.search.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.log4j.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.jobscience.search.db.DBHelper;

@Singleton
public class SearchDao {

    static private String QUERY_SELECT = "select distinct ";

    static private String QUERY_COUNT  = "select count (distinct a.id)" + " from contact a ";

    private Logger log = Logger.getLogger(SearchDao.class);
    @Inject
    private DBHelper      dbHelper;

    public SearchResult search(String searchColumns,Map<String, String> searchValues, SearchMode searchMode, Integer pageIdx, Integer pageSize,String orderCon) {
        Connection con = dbHelper.getConnection();
        
        //builder statements
        SearchStatements statementAndValues = buildSearchStatements(con,searchColumns,searchValues,searchMode, pageIdx, pageSize,orderCon);


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
     * @throws SQLException 
     */
    public List getTopMostEducation(Integer offset,Integer size) throws SQLException {
    	 if(size == null||size<6){
             size = 6;
         }
         size = size-1;
         offset = offset<0?0:offset;
        Connection con = dbHelper.getConnection();
        String querySql = "select e.\"ts2__Name__c\" as name, count(distinct c.\"id\") as count from ts2__education_history__c e inner join  contact c on c.\"sfId\"=e.\"ts2__Contact__c\"  where e.\"ts2__Name__c\" !='' group by e.\"ts2__Name__c\"  order by count desc offset "+offset+" limit "+size;
        String countSqlForNoCompany = "select count(distinct c.\"id\") as count from ts2__education_history__c e inner join  contact c on c.\"sfId\"=e.\"ts2__Contact__c\"  where e.\"ts2__Name__c\" ='' or  e.\"ts2__Name__c\" is null ";
        PreparedStatement prepareStatement =  dbHelper.prepareStatement(con,countSqlForNoCompany);
        List<Map> result = new ArrayList<Map>();
        final int noCompanyCount =  dbHelper.preparedStatementExecuteCount(prepareStatement,  new Object[0]);
        result.add(new HashMap(){{put("name", "No Education");put("count", noCompanyCount);}});
       
        prepareStatement =   dbHelper.prepareStatement(con,querySql.toString());
        result.addAll(dbHelper.preparedStatementExecuteQuery(prepareStatement, new Object[0]));
        prepareStatement.close();
        con.close();
        return result;
    }
    
    /**
     * get company top of the most contacts by size
     * @param size
     * @throws SQLException 
     */
    public List getTopMostCompanies(Integer offset,Integer size) throws SQLException {
        if(size == null||size<6){
            size = 6;
        }
        size = size-1;
        offset = offset<0?0:offset;
        Connection con = dbHelper.getConnection();
        String querySql = "select \"ts2__Name__c\" as name, count(distinct c.\"id\") as count from ts2__employment_history__c e inner join  contact c on c.\"sfId\"=e.\"ts2__Contact__c\"  where e.\"ts2__Name__c\" !='' group by e.\"ts2__Name__c\"  order by count desc offset "+offset+" limit "+size;
        String countSqlForNoCompany = "select count(distinct c.\"id\") as count from ts2__employment_history__c e inner join  contact c on c.\"sfId\"=e.\"ts2__Contact__c\"  where e.\"ts2__Name__c\" ='' or e.\"ts2__Name__c\" is null ";
        PreparedStatement prepareStatement =dbHelper.prepareStatement(con, countSqlForNoCompany);
        List<Map> result = new ArrayList<Map>();
        final int noCompanyCount =  dbHelper.preparedStatementExecuteCount(prepareStatement,  new Object[0]);
        result.add(new HashMap(){{put("name", "No Company");put("count", noCompanyCount);}});
       
        prepareStatement =   dbHelper.prepareStatement(con,querySql.toString());
        result.addAll(dbHelper.preparedStatementExecuteQuery(prepareStatement, new Object[0]));
       
        prepareStatement.close();
        con.close();
        return result;
    }
    
    /**
     * get skill top of the most contacts by size
     * @param size
     * @throws SQLException 
     */
    public List getTopMostSkills(Integer offset,Integer size) throws SQLException {
        if(size == null||size<6){
            size = 6;
        }
        size = size-1;
        offset = offset<0?0:offset;
        Connection con = dbHelper.getConnection();
        String querySql = "select b.\"ts2__Skill_Name__c\" as name, count(distinct c.\"id\") as count  from contact c join ts2__skill__c b   on c.\"sfId\" = b.\"ts2__Contact__c\"   where b.\"ts2__Skill_Name__c\" !='' group by b.\"ts2__Skill_Name__c\"  order by count desc   offset "+offset+" limit "+size;
        String countSqlForNoSkill = "select count(distinct c.\"id\") as count  from contact c join ts2__skill__c b   on c.\"sfId\" = b.\"ts2__Contact__c\"   where b.\"ts2__Skill_Name__c\" ='' or  b.\"ts2__Skill_Name__c\" is null ";
        PreparedStatement prepareStatement =dbHelper.prepareStatement(con, countSqlForNoSkill);
        List<Map> result = new ArrayList<Map>();
        final int noCompanyCount =  dbHelper.preparedStatementExecuteCount(prepareStatement,  new Object[0]);
        result.add(new HashMap(){{put("name", "No Skill");put("count", noCompanyCount);}});
       
        prepareStatement =   dbHelper.prepareStatement(con,querySql.toString());
        result.addAll(dbHelper.preparedStatementExecuteQuery(prepareStatement, new Object[0]));
       
        prepareStatement.close();
        con.close();
        return result;
    }
    
    private String getQueryColumnName(String orginalName,List<String> columnJoinTables,StringBuilder groupBy){
    	if(orginalName.toLowerCase().equals("name")){
    		return "a.\"Name\" as Name,lower(a.\"Name\") as lName";
    	}else if(orginalName.toLowerCase().equals("id")){
    		return " a.\"id\" as id";
    	}else if(orginalName.toLowerCase().equals("title")){
    		return "a.\"Title\" as Title,lower(a.\"Title\") as lTitle";
    	}else if(orginalName.toLowerCase().equals("createdate")){
    		return "to_char(a.\"CreatedDate\",'yyyy-mm-dd') as CreateDate";
    	}else if(orginalName.toLowerCase().equals("company")){
    		columnJoinTables.add(" left join ts2__employment_history__c c on a.\"sfId\" = c.\"ts2__Contact__c\"  ");
    		if(groupBy.length()>0){
    			groupBy.delete(0, groupBy.length());
    		}
    		groupBy.append(" a.\"id\" ");
    		return " string_agg(c.\"ts2__Name__c\",',') as Company,lower(string_agg(c.\"ts2__Name__c\",',')) as lCompany";
    	}else if(orginalName.toLowerCase().equals("skill")){
    		columnJoinTables.add(" left  join ts2__skill__c b on a.\"sfId\" = b.\"ts2__Contact__c\" ");
    		if(groupBy.length()>0){
    			groupBy.delete(0, groupBy.length());
    		}
    		groupBy.append(" a.\"id\" ");
    		return "string_agg(b.\"ts2__Skill_Name__c\",',') as Skill,lower(string_agg(b.\"ts2__Skill_Name__c\",',')) as lSkill";
    	}else if(orginalName.toLowerCase().equals("education")){
    		columnJoinTables.add("  left join ts2__education_history__c d on a.\"sfId\" = d.\"ts2__Contact__c\" ");
    		if(groupBy.length()>0){
    			groupBy.delete(0, groupBy.length());
    		}
    		groupBy.append(" a.\"id\" ");
    		return "string_agg(d.\"ts2__Name__c\",',') as Education,lower(string_agg(d.\"ts2__Name__c\",',')) as lEducation";
    	}
    	return orginalName;
    }
    
    /**
     * @param searchValues
     * @param searchMode
     * @return SearchStatements
     */
    private SearchStatements buildSearchStatements(Connection con,String searchColumns, Map<String, String> searchValues,SearchMode searchMode,
                                                   Integer pageIdx, Integer pageSize,String orderCon) {
        SearchStatements ss = new SearchStatements();
        if(pageIdx < 1){
            pageIdx = 1;
        }
        int offset = (pageIdx -1) * pageSize;

        //the select query  that will query data
        StringBuilder querySql = new StringBuilder();
        //the count query sql that will query the count of data
        StringBuilder countSql = new StringBuilder();
        //the columns
        StringBuilder columnsSql = new StringBuilder();
        // the part of query that build join tables sql
        StringBuilder joinTables = new StringBuilder();
        // the part of query that build join tables sql
        List<String> columnJoinTables = new ArrayList<String>();
        // the part of query that build conditions sql
        StringBuilder conditions = new StringBuilder();
        // the part of query that build group by sql
        StringBuilder groupBy= new StringBuilder();
        // the params will be put in sql
        List values = new ArrayList();
        
        //to test if need to add "where", if true, add ' where 1=1 ', so that will join " and .." condition
        boolean hasCondition = false;
        
        querySql.append(QUERY_SELECT);
        countSql.append(QUERY_COUNT);
        if(searchColumns==null){
            columnsSql.append("a.\"id\" as id,a.\"Name\" as Name,lower(a.\"Name\") as lName,a.\"Title\" as Title,lower(a.\"Title\") as lTitle,to_char(a.\"CreatedDate\",'yyyy-mm-dd') as CreateDate");
        }else{
	        for(String column:searchColumns.split(",")){
	            columnsSql.append(getQueryColumnName(column,columnJoinTables,groupBy));
	            columnsSql.append(",");
	        }
	        columnsSql.deleteCharAt(columnsSql.length()-1);
        }
        querySql.append(columnsSql);
        querySql.append(" from contact a ");
        
        if(searchValues!=null){
           
            // for all search mode, we preform the same condition
            String search = searchValues.get("search");
            if (!Strings.isNullOrEmpty(search)) {
                joinTables.append(" right join (select a_copy.id as id from contact a_copy right join (select ex.id from contact_ex ex where ex.resume_tsv @@ to_tsquery(?)) b on a_copy.id = b.id " + " union "
                                        + " select a_copy1.id as id from contact a_copy1 "
                                        + " where "
                                        + " a_copy1.\"Title\" ilike ? "
                                        + " or a_copy1.\"Name\" ilike ? ) a_ext on a_ext.id = a.id ");
                String value = search;
                String searchTsq = Joiner.on(" & ").join(Splitter.on(" ").omitEmptyStrings().split(value));
                String searchILike = value;
                if (!searchILike.contains("%")) {
                    searchILike = "%" + value + "%";
                }
                values.add(searchTsq);
                values.add(searchILike);
                values.add(searchILike);                
            }

            //keyword mode, use ilike search
            if(searchMode == SearchMode.KEYWORD){

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
                // already in the query
            }else if(searchMode == SearchMode.ADVANCED){
                //add the 'educationNames' filter, and join Education table
                if (searchValues.get("educationNames") != null && !"".equals(searchValues.get("educationNames"))) {
                    hasCondition = true;
                    String value = searchValues.get("educationNames");
                    boolean noEducation = false;
                    if(!"Any Education".equals(value)){
	                    String[] educationNames = value.split(","); 
	                    joinTables.append(" inner join ts2__education_history__c d on a.\"sfId\" = d.\"ts2__Contact__c\" ");
	                    removeDuplicate(columnJoinTables,"ts2__education_history__c");
	                    conditions.append("  and ( d.\"ts2__Name__c\" in ");
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
	                        
	                        if(educationNames[i].equals("No Education")){
	                        	noEducation = true;
	                        	values.add("");
	                        }else{
	                        	values.add(educationNames[i]);
	                        }
	                    }
	                    if(noEducation){
	                    	conditions.append(" or d.\"ts2__Name__c\" is null");
	                    }

                        conditions.append(" )");
                    }
                }
                
                //add the 'companyNames' filter, and join Education table
                if (searchValues.get("companyNames") != null && !"".equals(searchValues.get("companyNames"))) {
                    hasCondition = true;
                    String value = searchValues.get("companyNames");
                    boolean noCompany = false;
                    if(!"Any Company".equals(value)){
	                    String[] companyNames = value.split(","); 
	                    joinTables.append(" inner join ts2__employment_history__c c on a.\"sfId\" = c.\"ts2__Contact__c\" ");
	                    removeDuplicate(columnJoinTables,"ts2__employment_history__c");
	                    conditions.append("  and ( c.\"ts2__Name__c\" in ");
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
	                        if(companyNames[i].equals("No Company")){
	                        	noCompany = true;
	                        	values.add("");
	                        }else{
	                        	values.add(companyNames[i]);
	                        }
	                    }
	                    if(noCompany){
	                    	conditions.append(" or c.\"ts2__Name__c\" is null");
	                    }

                        conditions.append(" ) ");
                    }
                }
                
                //add the 'skillNames' filter, and join Education table
                if (searchValues.get("skillNames") != null && !"".equals(searchValues.get("skillNames"))) {
                    hasCondition = true;
                    String value = searchValues.get("skillNames");
                    boolean noSkill = false;
                    if(!"Any Skill".equals(value)){
	                    String[] skillNames = value.split(","); 
                        joinTables.append(" inner join ts2__skill__c b on a.\"sfId\" = b.\"ts2__Contact__c\" ");
                        removeDuplicate(columnJoinTables,"ts2__skill__c");
                        conditions.append(" and ( b.\"ts2__Skill_Name__c\" in  ");
	                    for(int i = 0; i < skillNames.length; i++){
	                        if(i == 0){
	                            conditions.append("(");
	                        }else{
	                            conditions.append(",");
	                        }
	                        conditions.append("?");
	                        if(i == skillNames.length - 1){
	                            conditions.append(")");
	                        }
	                        if(skillNames[i].equals("No Skill")){
	                        	noSkill = true;
	                        	values.add("");
	                        }else{
	                        	values.add(skillNames[i]);
	                        }
	                    }
	                    if(noSkill){
	                    	conditions.append(" or b.\"ts2__Skill_Name__c\" is null");
	                    }

                        conditions.append(" ) ");
                    }
                }
            }
        }
        
        querySql.append(joinTables);
        countSql.append(joinTables);
        for(String join:columnJoinTables){
        	if(!join.equals("No Join")){
	        	querySql.append(join);
	        	countSql.append(join);
        	}
        }
        if(hasCondition){
            String whereStr = " where 1=1 ";
            querySql.append(whereStr);
            countSql.append(whereStr);
        }
        
        querySql.append(conditions);
        countSql.append(conditions);
        if(!"".equals(groupBy.toString())){
	        querySql.append(" group by "+groupBy);
        }
        
        querySql.append(orderCon);
        querySql.append(" offset ").append(offset).append(" limit ").append(pageSize);
        
        log.debug(querySql);
        // build the statement
        ss.queryStmt = dbHelper.prepareStatement(con,querySql.toString());
        ss.countStmt = dbHelper.prepareStatement(con,countSql.toString());
        ss.values = values.toArray();

        return ss;
    }

    private void removeDuplicate(List<String> columnJoinTables,String tableName){
    	for(int i=0,j=columnJoinTables.size();i<j;i++){
    		if(columnJoinTables.get(i).contains(tableName)){
    			columnJoinTables.set(i, "No Join");
    		}
    	}
    }
}


class SearchStatements {

    PreparedStatement queryStmt;
    PreparedStatement countStmt;
    Object[]          values;

}
