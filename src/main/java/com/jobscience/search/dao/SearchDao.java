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
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.jobscience.search.db.DBHelper;

@Singleton
public class SearchDao {

    static private String QUERY_SELECT = "select distinct ";

    static private String QUERY_COUNT  = "select count (distinct a.id) ";

    private Logger log = Logger.getLogger(SearchDao.class);
    @Inject
    private DBHelper      dbHelper;

    public SearchResult search(String searchColumns,Map<String, String> searchValues,  Integer pageIdx, Integer pageSize,String orderCon) {
        Connection con = dbHelper.getConnection();
        
        //builder statements
        SearchStatements statementAndValues = buildSearchStatements(con,searchColumns,searchValues, pageIdx, pageSize,orderCon);

        //excute query and caculate times
        long start = System.currentTimeMillis();
        List<Map> result = dbHelper.preparedStatementExecuteQuery(statementAndValues.queryStmt, statementAndValues.values);
        long mid = System.currentTimeMillis();
        int count = dbHelper.preparedStatementExecuteCount(statementAndValues.countStmt, statementAndValues.values);
        long end = System.currentTimeMillis();

        SearchResult searchResult = new SearchResult(result, count).setDuration(end - start).setSelectDuration(mid - start).setCountDuration(end - mid);
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
    
    public List getTopAdvancedType(Integer offset,Integer size,String type) throws SQLException {
        if(size == null||size<6){
            size = 5;
        }
        offset = offset < 0 ? 0 : offset;
        Connection con = dbHelper.getConnection();
        String name = getNameExpr(type);
        String table = getTable(type);
        String querySql = " select a.name, count(a.contact) from ( "
                                + " select e."+name+" as name, e.\"ts2__Contact__c\" as contact "
                                + " from "+table+" e  "
                                + " where e."+name+" !='' group by e.\"ts2__Contact__c\", e."+name+") a  "
                                + " group by a.name order by a.count desc offset "
                                + offset
                                + " limit "
                                + size;
        
        PreparedStatement prepareStatement =   dbHelper.prepareStatement(con,querySql.toString());
        List<Map> result = dbHelper.preparedStatementExecuteQuery(prepareStatement, new Object[0]);
        prepareStatement.close();
        con.close();
        return result;
    }
    
    public List<Map> getGroupValuesForAdvanced(Map<String, String> searchValues, String type) throws SQLException {
      //the select query  that will query data
        StringBuilder querySql = new StringBuilder();
        StringBuilder groupBy = new StringBuilder();
        StringBuilder conditions = new StringBuilder();
        StringBuilder searchConditions = new StringBuilder();
        String column = null;
        String baseTableIns = null;
        querySql.append("select result.name, count(*) as count from ( select ");
        boolean hasCondition = false;
        String baseTable = new String();
        List values = new ArrayList();
        StringBuilder joinTables = new StringBuilder();
        
        if(type.equals("company")){
            baseTable = " ts2__employment_history__c ";
            baseTableIns = "c";
            column = " c.\"ts2__Contact__c\" as id, c.\"ts2__Name__c\" as name";
            groupBy.append(" group by c.\"ts2__Contact__c\", c.\"ts2__Name__c\" ");
        }else if(type.equals("education")){
            baseTableIns = "d";
            baseTable = " ts2__education_history__c ";
            groupBy.append(" group by d.\"ts2__Contact__c\", d.\"ts2__Name__c\" ");
            column = " d.\"ts2__Contact__c\" as id, d.\"ts2__Name__c\" as name";
        }else if(type.equals("skill")){
            baseTableIns = "b";
            baseTable = " ts2__skill__c ";
            groupBy.append(" group by b.\"ts2__Contact__c\", b.\"ts2__Skill_Name__c\" ");
            column = " b.\"ts2__Contact__c\" as id, b.\"ts2__Skill_Name__c\" as name";
        }
        
        if (searchValues != null) {
            // for all search mode, we preform the same condition
            String search = searchValues.get("search");
            if (!Strings.isNullOrEmpty(search)) {
                joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__Contact__c\" = a.\"sfId\" ");
                baseTable = " contact ";
                baseTableIns = "a";
                searchConditions.append(getSearchValueJoinTable(search, values,"a"));
            }

            // add the 'educationNames' filter, and join Education table
            if (searchValues.get("educationNames") != null && !"".equals(searchValues.get("educationNames"))) {
                hasCondition = true;
                String value = searchValues.get("educationNames");
                if (!"Any Education".equals(value)) {
                    if(baseTable.indexOf("ts2__education_history__c") == -1 && joinTables.indexOf("ts2__education_history__c") == -1){
                        joinTables.append(" inner join ts2__education_history__c d on ");
                        if(baseTable.indexOf(" contact ") >= 0){
                            joinTables.append("a.\"sfId\" = d.\"ts2__Contact__c\" ");
                        }else{
                            joinTables.append(baseTableIns+".\"ts2__Contact__c\" = d.\"ts2__Contact__c\" ");
                        }
                    }
                    conditions.append(getConditionForThirdNames(value, values, "education"));
                }
            }

            // add the 'companyNames' filter, and join Education table
            if (searchValues.get("companyNames") != null && !"".equals(searchValues.get("companyNames"))) {
                hasCondition = true;
                String value = searchValues.get("companyNames");
                if (!"Any Company".equals(value)) {
                    if(baseTable.indexOf("ts2__employment_history__c") == -1 && joinTables.indexOf("ts2__employment_history__c") == -1){
                        joinTables.append(" inner join ts2__employment_history__c c on ");
                        if(baseTable.indexOf(" contact ") >= 0){
                            joinTables.append("a.\"sfId\" = c.\"ts2__Contact__c\" ");
                        }else{
                            joinTables.append(baseTableIns+".\"ts2__Contact__c\" = c.\"ts2__Contact__c\" ");
                        }
                    }
                    conditions.append(getConditionForThirdNames(value, values, "company"));
                }
            }

            // add the 'skillNames' filter, and join Education table
            if (searchValues.get("skillNames") != null && !"".equals(searchValues.get("skillNames"))) {
                hasCondition = true;
                String value = searchValues.get("skillNames");
                if (!"Any Skill".equals(value)) {
                    if(baseTable.indexOf("ts2__skill__c") == -1 && joinTables.indexOf("ts2__skill__c") == -1){
                        joinTables.append(" inner join ts2__skill__c b on ");
                        if(baseTable.indexOf(" contact ") >= 0){
                            joinTables.append("a.\"sfId\" = b.\"ts2__Contact__c\" ");
                        }else{
                            joinTables.append(baseTableIns+".\"ts2__Contact__c\" = b.\"ts2__Contact__c\" ");
                        }
                    }
                    conditions.append(getConditionForThirdNames(value, values, "skill"));
                }
            }
        }
        
        querySql.append(column);
        querySql.append(" from ");
        querySql.append(baseTable);
        querySql.append(baseTableIns);
        querySql.append(searchConditions);
        querySql.append(joinTables);
        
        if(hasCondition){
            String whereStr = " where 1=1 ";
            querySql.append(whereStr);
        }
        
        querySql.append(conditions);
        if(!"".equals(groupBy.toString())){
            querySql.append(groupBy);
        }
        
        querySql.append(") result where result.name != '' group by result.name order by count desc");
        
        log.debug(querySql);
        Connection con = dbHelper.getConnection();
        PreparedStatement prepareStatement =   dbHelper.prepareStatement(con,querySql.toString());
        List<Map> result = dbHelper.preparedStatementExecuteQuery(prepareStatement, values.toArray());
        prepareStatement.close();
        con.close();
        return result;
    }
    
    private String getQueryColumnName(String orginalName ,List<String> columnJoinTables,StringBuilder groupBy){
    	if(orginalName.toLowerCase().equals("name")){
    		if(groupBy.length()>0){
    			groupBy.append(",");
    		}
    		groupBy.append("a.\"Name\"");
    		return "a.\"Name\" as Name,lower(a.\"Name\") as lName";
    	}else if(orginalName.toLowerCase().equals("id")){
    		return " a.\"id\" as id";
    	}else if(orginalName.toLowerCase().equals("title")){
    		if(groupBy.length()>0){
    			groupBy.append(",");
    		}
    		groupBy.append("a.\"Title\"");
    		return "a.\"Title\" as Title,lower(a.\"Title\") as lTitle";
    	}else if(orginalName.toLowerCase().equals("createdate")){
    		if(groupBy.length()>0){
    			groupBy.append(",");
    		}
    		groupBy.append("a.\"CreatedDate\"");
    		return "to_char(a.\"CreatedDate\",'yyyy-mm-dd') as CreateDate";
    	}else if(orginalName.toLowerCase().equals("company")){
    		columnJoinTables.add(getAdvancedJoinTable("company"));
    		return " string_agg(distinct c.\"ts2__Name__c\",',') as Company,lower(string_agg(c.\"ts2__Name__c\",',')) as lCompany";
    	}else if(orginalName.toLowerCase().equals("skill")){
    		columnJoinTables.add(getAdvancedJoinTable("skill"));
    		return "string_agg(distinct b.\"ts2__Skill_Name__c\",',') as Skill,lower(string_agg(b.\"ts2__Skill_Name__c\",',')) as lSkill";
    	}else if(orginalName.toLowerCase().equals("education")){
    		columnJoinTables.add(getAdvancedJoinTable("education"));
    		return "string_agg(distinct d.\"ts2__Name__c\",',') as Education,lower(string_agg(d.\"ts2__Name__c\",',')) as lEducation";
    	}
    	
    	return orginalName;
    }
    
    
    private String getSearchColumns(String searchColumns,List columnJoinTables,StringBuilder groupBy){
    	StringBuilder columnsSql = new StringBuilder();
    	 if(searchColumns==null){
             columnsSql.append("a.\"id\" as id,a.\"Name\" as Name,lower(a.\"Name\") as lName,a.\"Title\" as Title,lower(a.\"Title\") as lTitle,to_char(a.\"CreatedDate\",'yyyy-mm-dd') as CreateDate");
         }else{
 	        for(String column:searchColumns.split(",")){
 	            columnsSql.append(getQueryColumnName(column,columnJoinTables,groupBy));
 	            columnsSql.append(",");
 	        }
 	        columnsSql.deleteCharAt(columnsSql.length()-1);
         }
    	 return columnsSql.toString();
    }
    
    /**
     * @param searchValues
     * @param searchMode
     * @return SearchStatements
     */
    private SearchStatements buildSearchStatements(Connection con,String searchColumns, Map<String, String> searchValues,
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
        
        querySql.append(QUERY_SELECT);
        countSql.append(QUERY_COUNT);
        querySql.append(getSearchColumns(searchColumns,columnJoinTables,groupBy));
        querySql.append(" from ( select distinct contact.id,contact.\"sfId\",contact.\"Name\",contact.\"LastName\",contact.\"FirstName\",contact.\"Title\",contact.\"CreatedDate\" from contact contact   ");
        countSql.append(" from ( select distinct contact.id,contact.\"sfId\",contact.\"Name\",contact.\"LastName\",contact.\"FirstName\",contact.\"Title\",contact.\"CreatedDate\" from contact contact   ");
        if(searchValues!=null){
           
            // for all search mode, we preform the same condition
            String search = searchValues.get("search");
            String joinSql = getCondtion(search, searchValues,values);
            querySql.append(joinSql);
            countSql.append(joinSql);
        }
        
        querySql.append(joinTables);
        countSql.append(joinTables);
        for(String join:columnJoinTables){
        	if(!join.equals("No Join")){
	        	querySql.append(join);
	        	countSql.append(join);
        	}
        }
        
        
        querySql.append(conditions);
        countSql.append(conditions);
        if(!"".equals(groupBy.toString())){
	        querySql.append(" group by "+groupBy);
        }
        
        querySql.append(orderCon);
        querySql.append(" offset ").append(offset).append(" limit ").append(pageSize);
        
        log.debug(querySql);
        log.debug(countSql);
        
        // build the statement
        ss.queryStmt = dbHelper.prepareStatement(con,querySql.toString());
        ss.countStmt = dbHelper.prepareStatement(con,countSql.toString());
        ss.values = values.toArray();
        return ss;
    }
    
    private String getCondtion(String searchValue, Map<String, String> searchValues, List values){
    	StringBuilder joinSql = new StringBuilder();
    	StringBuilder conditions = new StringBuilder();
    	  
        if(!Strings.isNullOrEmpty(searchValue)){
	        joinSql.append(getSearchValueJoinTable(searchValue, values,"contact"));
        }
        

        if(searchValues!=null){
        	
        	 //add the 'FirstName' filter
            if (searchValues.get("FirstName") != null && !"".equals(searchValues.get("FirstName"))) {
                String value = searchValues.get("FirstName");
                conditions.append(" and contact.\"FirstName\" ilike ? ");
                if(!value.contains("%")){
                    value = "%" + value + "%";
                }
                values.add(value);
            }

            //add the 'LastName' filter
            if (searchValues.get("LastName") != null && !"".equals(searchValues.get("LastName"))) {
                String value = searchValues.get("LastName");
                conditions.append(" and contact.\"LastName\" ilike ? ");
                if(!value.contains("%")){
                    value = "%" + value + "%";
                }
                values.add(value);
            }
            
            
            boolean searchEmployment = false;
            String joinEmployment = "";
            //add the 'Title' filter
            if (searchValues.get("Title") != null && !"".equals(searchValues.get("Title"))) {
                String value = searchValues.get("Title");
                if(searchValues.get("curTitle")!=null){

                    conditions.append(" and contact.\"Title\" ilike ? ");
                    if(!value.contains("%")){
                        value = "%" + value + "%";
                    }
                    values.add(value);
                }else{
                    
                    joinEmployment = " left join (select em.\"ts2__Contact__c\",em.\"ts2__Job_Title__c\" from ts2__employment_history__c em ";
                    conditions.append(" and ( c1.\"ts2__Job_Title__c\" ilike ?  or contact.\"Title\" ilike ? )");
                    if(!value.contains("%")){
                        value = "%" + value + "%";
                    }
                    values.add(value);
                    values.add(value);
                }
            }
            
            
        	
        //add the 'companyNames' filter, and join Education table
        if (searchValues.get("companyNames") != null && !"".equals(searchValues.get("companyNames"))) {
            String value = searchValues.get("companyNames");
            if(!"Any Company".equals(value)){
            	joinSql.append(" join (select em.\"ts2__Contact__c\",em.\"ts2__Job_Title__c\" from ts2__employment_history__c em where em.\"ts2__Name__c\" in ");
                joinSql.append("('"+Joiner.on("','").join(Splitter.on(",").omitEmptyStrings().split(value))+"')");
                searchEmployment = true;
            }
        }
        
        if(!searchEmployment&&!joinEmployment.equals("")){
        	 joinSql.append(joinEmployment);
        }
        
        if(searchEmployment||!joinEmployment.equals("")){
        	joinSql.append(" )  c1 on contact.\"sfId\" = c1.\"ts2__Contact__c\"   ");
        }
        	
	   //add the 'educationNames' filter, and join Education table
        if (searchValues.get("educationNames") != null && !"".equals(searchValues.get("educationNames"))) {
            String value = searchValues.get("educationNames");
            if(!"Any Education".equals(value)){
                joinSql.append(" join (select ed.\"ts2__Contact__c\" from ts2__education_history__c ed where \"ts2__Name__c\" in ");
                joinSql.append("('"+Joiner.on("','").join(Splitter.on(",").omitEmptyStrings().split(value))+"')");
                joinSql.append(" ) ed1 on contact.\"sfId\" = ed1.\"ts2__Contact__c\" ");
            }
        }
       
        
        //add the 'skillNames' filter, and join Education table
        if (searchValues.get("skillNames") != null && !"".equals(searchValues.get("skillNames"))) {
            String value = searchValues.get("skillNames");
            if(!"Any Skill".equals(value)){
                joinSql.append("join (select sk.\"ts2__Contact__c\" from ts2__skill__c sk where sk.\"ts2__Skill_Name__c\" in  ");
                joinSql.append("('"+Joiner.on("','").join(Splitter.on(",").omitEmptyStrings().split(value))+"')");
                joinSql.append(" ) sk1 on contact.\"sfId\" = sk1.\"ts2__Contact__c\" ");
	            }
	        }
        
        joinSql.append(" where 1=1 "+conditions+") a");
       
        }
        
       
        return joinSql.toString();
    }
    
    private String getAdvancedJoinTable(String type){
        StringBuilder joinSql = new StringBuilder();
        
        if(type.equals("company")){
            joinSql.append(" left join ts2__employment_history__c c on a.\"sfId\" = c.\"ts2__Contact__c\" ");
        }else if(type.equals("education")){
            joinSql.append(" left join ts2__education_history__c d on a.\"sfId\" = d.\"ts2__Contact__c\" ");
        }else if(type.equals("skill")){
            joinSql.append(" left join ts2__skill__c b on a.\"sfId\" = b.\"ts2__Contact__c\" ");
        }
        return joinSql.toString();
    }
    
//    private String getAdvancedValueJoinTable(String type){
//        StringBuilder joinSql = new StringBuilder();
//        
//        if(type.equals("company")){
//            joinSql.append(" left join ts2__employment_history__c c on  contact.\"sfId\" = c.\"ts2__Contact__c\" ");
//        }else if(type.equals("education")){
//            joinSql.append(" left join ts2__education_history__c d on  contact.\"sfId\" = d.\"ts2__Contact__c\" ");
//        }else if(type.equals("skill")){
//            joinSql.append(" left join ts2__skill__c b on  contact.\"sfId\" = b.\"ts2__Contact__c\" ");
//        }
//        return joinSql.toString();
//    }
    
    private String getSearchValueJoinTable(String searchValue, List values,String alias){
        StringBuilder joinSql = new StringBuilder();
        joinSql.append(" right join (select a_copy.id as id from contact a_copy right join (select ex.id from contact_ex ex where ex.resume_tsv @@ to_tsquery(?)) b on a_copy.id = b.id " + " union "
                                + " select a_copy1.id as id from contact a_copy1 "
                                + " where "
                                + " a_copy1.\"Title\" ilike ? "
                                + " or a_copy1.\"Name\" ilike ? ) a_ext on a_ext.id = "+alias+".id ");
        String searchTsq = Joiner.on(" & ").join(Splitter.on(" ").omitEmptyStrings().split(searchValue));
        String searchILike = searchValue;
        if (!searchILike.contains("%")) {
            searchILike = "%" + searchValue + "%";
        }
        values.add(searchTsq);
        values.add(searchILike);
        values.add(searchILike); 
        return joinSql.toString();
    }
    
    private String getNameExpr(String type){
        StringBuilder sql = new StringBuilder();
        
        if(type.equals("company")){
            sql.append("\"ts2__Name__c\"");
        }else if(type.equals("education")){
            sql.append("\"ts2__Name__c\"");
        }else if(type.equals("skill")){
            sql.append("\"ts2__Skill_Name__c\"");
        }
        return sql.toString();
    }
    
    private String getTable(String type){
        String table = null;
        
        if(type.equals("company")){
            table = "ts2__employment_history__c";
        }else if(type.equals("education")){
            table = "ts2__education_history__c";
        }else if(type.equals("skill")){
            table = "ts2__skill__c";
        }
        return table;
    }
    
    private String getTableInstance(String type){
        String instance = null;
        if(type.equals("company")){
            instance = "c";
        }else if(type.equals("education")){
            instance = "d";
        }else if(type.equals("skill")){
            instance = "b";
        }
        return instance;
    }
    
    private String getConditionForThirdNames(String namesStr, List values, String type){
        StringBuilder conditions = new StringBuilder();
        String instance = getTableInstance(type);
        String nameExpr = getNameExpr(type);
        conditions.append("  and ( "+instance+"."+nameExpr+" in ");
        conditions.append("('"+ Joiner.on("','").join(Splitter.on(",").omitEmptyStrings().split(namesStr))+"')");
        conditions.append(")");
        return conditions.toString();
    }
    
}


class SearchStatements {

    PreparedStatement queryStmt;
    PreparedStatement countStmt;
    Object[]          values;

}
