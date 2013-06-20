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

    static private String QUERY_COUNT  = "select count (distinct a.id)" + " from contact a ";

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
        //the columns
        StringBuilder columnsSql = new StringBuilder();
        // the part of query that build join tables sql
        StringBuilder joinTables = new StringBuilder();
        // the part of query that build conditions sql
        StringBuilder conditions = new StringBuilder();
        // the part of query that build group by sql
        StringBuilder groupBy= new StringBuilder();
        // the params will be put in sql
        List values = new ArrayList();
        
        //to test if need to add "where", if true, add ' where 1=1 ', so that will join " and .." condition
        boolean hasCondition = false;
        querySql.append("select result.name, count(*) as count from ( ");
        querySql.append(QUERY_SELECT);
        columnsSql.append("a.\"sfId\" as id, ");
        querySql.append(columnsSql);
        String companyTable = getAdvancedJoinTable("company");
        String educationTable = getAdvancedJoinTable("education");
        String skillTable = getAdvancedJoinTable("skill");
        querySql.append(getNameExprForNothing(type));
        querySql.append(" from contact a ");
        
        if (searchValues != null) {

            // for all search mode, we preform the same condition
            String search = searchValues.get("search");
            if (!Strings.isNullOrEmpty(search)) {
                querySql.append(getSearchValueJoinTable(search, values));
            }

            // add the 'educationNames' filter, and join Education table
            if (searchValues.get("educationNames") != null && !"".equals(searchValues.get("educationNames"))) {
                hasCondition = true;
                String value = searchValues.get("educationNames");
                if (!"Any Education".equals(value)) {
                    joinTables.append(getAdvancedJoinTable("education"));
                    conditions.append(getConditionForThirdNames(value, values, "education"));
                }
            }

            // add the 'companyNames' filter, and join Education table
            if (searchValues.get("companyNames") != null && !"".equals(searchValues.get("companyNames"))) {
                hasCondition = true;
                String value = searchValues.get("companyNames");
                if (!"Any Company".equals(value)) {
                    joinTables.append(getAdvancedJoinTable("company"));
                    conditions.append(getConditionForThirdNames(value, values, "company"));
                }
            }

            // add the 'skillNames' filter, and join Education table
            if (searchValues.get("skillNames") != null && !"".equals(searchValues.get("skillNames"))) {
                hasCondition = true;
                String value = searchValues.get("skillNames");
                if (!"Any Skill".equals(value)) {
                    joinTables.append(getAdvancedJoinTable("skill"));
                    conditions.append(getConditionForThirdNames(value, values, "skill"));
                }
            }
        }
        
        querySql.append(joinTables);
        if(type.equals("company")){
            if(joinTables.indexOf("ts2__employment_history__c") == -1){
                querySql.append(companyTable);
            }
            groupBy.append(" group by a.\"sfId\", c.\"ts2__Name__c\" ");
        }else if(type.equals("education")){
            if(joinTables.indexOf("ts2__education_history__c") == -1){
                querySql.append(educationTable);
            }
            groupBy.append(" group by a.\"sfId\", d.\"ts2__Name__c\" ");
        }else if(type.equals("skill")){
            if(joinTables.indexOf("ts2__skill__c") == -1){
                querySql.append(skillTable);
            }
            groupBy.append(" group by a.\"sfId\", b.\"ts2__Skill_Name__c\" ");
        }
        
        if(hasCondition){
            String whereStr = " where 1=1 ";
            querySql.append(whereStr);
        }
        
        querySql.append(conditions);
        if(!"".equals(groupBy.toString())){
            querySql.append(groupBy);
        }
        
        querySql.append(") result group by result.name order by count desc");
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
    		return "a.\"Name\" as Name,lower(a.\"Name\") as lName";
    	}else if(orginalName.toLowerCase().equals("id")){
    		return " a.\"id\" as id";
    	}else if(orginalName.toLowerCase().equals("title")){
    		return "a.\"Title\" as Title,lower(a.\"Title\") as lTitle";
    	}else if(orginalName.toLowerCase().equals("createdate")){
    		return "to_char(a.\"CreatedDate\",'yyyy-mm-dd') as CreateDate";
    	}else if(orginalName.toLowerCase().equals("company")){
    		columnJoinTables.add(getAdvancedJoinTable("company"));
    		if(groupBy.length()>0){
    			groupBy.delete(0, groupBy.length());
    		}
    		groupBy.append(" a.\"id\" ");
    		return " string_agg(distinct c.\"ts2__Name__c\",',') as Company,lower(string_agg(c.\"ts2__Name__c\",',')) as lCompany";
    	}else if(orginalName.toLowerCase().equals("skill")){
    		columnJoinTables.add(getAdvancedJoinTable("skill"));
    		if(groupBy.length()>0){
    			groupBy.delete(0, groupBy.length());
    		}
    		groupBy.append(" a.\"id\" ");
    		return "string_agg(distinct b.\"ts2__Skill_Name__c\",',') as Skill,lower(string_agg(b.\"ts2__Skill_Name__c\",',')) as lSkill";
    	}else if(orginalName.toLowerCase().equals("education")){
    		columnJoinTables.add(getAdvancedJoinTable("education"));
    		if(groupBy.length()>0){
    			groupBy.delete(0, groupBy.length());
    		}
    		groupBy.append(" a.\"id\" ");
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
    
    private boolean renderSearchCondition(Map<String, String> searchValues, List values, StringBuilder conditions,StringBuilder joinTables,List<String> columnJoinTables ){

    	boolean hasCondition = false;
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
        	hasCondition = true;
            String value = searchValues.get("Title");
            if(searchValues.get("curTitle")!=null){

                conditions.append(" and a.\"Title\" ilike ? ");
                if(!value.contains("%")){
                    value = "%" + value + "%";
                }
                values.add(value);
            }else{
                searchEmployment = true;
                removeDuplicate(columnJoinTables,"ts2__employment_history__c");
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
            	 removeDuplicate(columnJoinTables,"ts2__employment_history__c");
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
        
    

        //add the 'educationNames' filter, and join Education table
        if (searchValues.get("educationNames") != null && !"".equals(searchValues.get("educationNames"))) {
        	hasCondition = true;
            String value = searchValues.get("educationNames");
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
                    
                    values.add(educationNames[i]);
                }
                

                conditions.append(" )");
            }
        }
        
        //add the 'companyNames' filter, and join Education table
        if (searchValues.get("companyNames") != null && !"".equals(searchValues.get("companyNames"))) {
        	hasCondition = true;
            String value = searchValues.get("companyNames");
            if(!"Any Company".equals(value)){
                String[] companyNames = value.split(","); 
                if(!searchEmployment){
	                joinTables.append(" inner join ts2__employment_history__c c on a.\"sfId\" = c.\"ts2__Contact__c\" ");
	                removeDuplicate(columnJoinTables,"ts2__employment_history__c");
                }
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
                    values.add(companyNames[i]);
                }

                conditions.append(" ) ");
            }
        }
        
        //add the 'skillNames' filter, and join Education table
        if (searchValues.get("skillNames") != null && !"".equals(searchValues.get("skillNames"))) {
        	hasCondition = true;
            String value = searchValues.get("skillNames");
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
                    values.add(skillNames[i]);
                }

                conditions.append(" ) ");
            }
        }
        return hasCondition;
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
        StringBuilder groupBy= new StringBuilder();
        // the params will be put in sql
        List values = new ArrayList();
        
        //to test if need to add "where", if true, add ' where 1=1 ', so that will join " and .." condition
        boolean hasCondition = false;
        
        querySql.append(QUERY_SELECT);
        countSql.append(QUERY_COUNT);
        querySql.append(getSearchColumns(searchColumns,columnJoinTables,groupBy));
        querySql.append(" from contact a ");
        
        if(searchValues!=null){
           
            // for all search mode, we preform the same condition
            String search = searchValues.get("search");
            if (!Strings.isNullOrEmpty(search)) {
                String joinSql = getSearchValueJoinTable(search, values);
                querySql.append(joinSql);
                countSql.append(joinSql);
            }

            hasCondition = renderSearchCondition(searchValues,values,conditions,joinTables,columnJoinTables );
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
    
    private String getSearchValueJoinTable(String searchValue, List values){
        StringBuilder joinSql = new StringBuilder();
        joinSql.append(" right join (select a_copy.id as id from contact a_copy right join (select ex.id from contact_ex ex where ex.resume_tsv @@ to_tsquery(?)) b on a_copy.id = b.id " + " union "
                                + " select a_copy1.id as id from contact a_copy1 "
                                + " where "
                                + " a_copy1.\"Title\" ilike ? "
                                + " or a_copy1.\"Name\" ilike ? ) a_ext on a_ext.id = a.id ");
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
    
    private String getNameExprForNothing(String type){
        StringBuilder sql = new StringBuilder();
        String name = getNameExpr(type);
        String instance = getTableInstance(type);
        String label = getLabel(type);
        sql.append(" case when "+instance+"."+name+" is null then 'No "+label+"' when "+instance+"."+name+" = '' then 'No "+label+"'  else "+instance+"."+name+" end  as name ");
        return sql.toString();
    }
    
    private String getLabel(String type){
        String label = null;
        
        if(type.equals("company")){
            label = "Company";
        }else if(type.equals("education")){
            label = "Educations";
        }else if(type.equals("skill")){
            label = "Skills";
        }
        return label;
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
        String[] names = namesStr.split(",");
        String instance = getTableInstance(type);
        String nameExpr = getNameExpr(type);
        conditions.append("  and ( "+instance+"."+nameExpr+" in ");
        for (int i = 0; i < names.length; i++) {
            if (i == 0) {
                conditions.append("(");
            } else {
                conditions.append(",");
            }
            conditions.append("?");
            if (i == names.length - 1) {
                conditions.append(")");
            }
           values.add(names[i]);
        }
        conditions.append(" )");
        return conditions.toString();
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
