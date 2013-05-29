package com.jobscience.search.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.jobscience.search.db.DBHelper;

@Singleton
public class SearchDao {

    static private String QUERY_SELECT = "select distinct a.\"Name\", a.\"id\", a.\"Title\"" + " from contact a ";

    static private String QUERY_COUNT  = "select count (distinct a.id)" + " from contact a ";

    @Inject
    private DBHelper      dbHelper;

    public SearchResult search(Map<String, String> searchValues, SearchMode searchMode, Integer pageIdx, Integer pageSize) {
        Connection con = dbHelper.getConnection();
        SearchStatements statementAndValues = buildSearchStatements(con,searchValues,searchMode, pageIdx, pageSize);


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
     * @param searchValues
     * @param searchMode
     * @return SearchStatements
     */
    private SearchStatements buildSearchStatements(Connection con, Map<String, String> searchValues,SearchMode searchMode,
                                                   Integer pageIdx, Integer pageSize) {
        SearchStatements ss = new SearchStatements();
        if(pageIdx < 0){
            pageIdx = 0;
        }
        int offset = (pageIdx -1) * pageSize;

        StringBuilder querySql = new StringBuilder();
        StringBuilder countSql = new StringBuilder();
        StringBuilder joinTables = new StringBuilder();
        StringBuilder conditions = new StringBuilder();
        List values = new ArrayList();
        
        boolean hasCondition = false;
        
        querySql.append(QUERY_SELECT);
        countSql.append(QUERY_COUNT);
        
        if(searchValues!=null){
            if(searchMode == SearchMode.KEYWORD){
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

                if (searchValues.get("FirstName") != null && !"".equals(searchValues.get("FirstName"))) {
                    hasCondition = true;
                    String value = searchValues.get("FirstName");
                    conditions.append(" and a.\"FirstName\" ilike ? ");
                    if(!value.contains("%")){
                        value = "%" + value + "%";
                    }
                    values.add(value);
                }

                if (searchValues.get("LastName") != null && !"".equals(searchValues.get("LastName"))) {
                    hasCondition = true;
                    String value = searchValues.get("LastName");
                    conditions.append(" and a.\"LastName\" ilike ? ");
                    if(!value.contains("%")){
                        value = "%" + value + "%";
                    }
                    values.add(value);
                }

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

                if (searchValues.get("Employment") != null && !"".equals(searchValues.get("Employment"))) {
                    hasCondition = true;
                    String value = searchValues.get("Employment");
                    joinTables.append(" inner join ts2__employment_history__c c on a.\"sfId\" = c.\"ts2__Contact__c\" ");
                    conditions.append(" and ( c.\"ts2__Name__c\" ilike ? or c.\"ts2__Job_Title__c\" ilike ?) ");
                    if(!value.contains("%")){
                        value = "%" + value + "%";
                    }
                    values.add(value);
                    values.add(value);
                }

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
        
        System.out.println(querySql);
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
