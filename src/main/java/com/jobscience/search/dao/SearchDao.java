package com.jobscience.search.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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

    static private String QUERY_SELECT = "select \"Name\", \"id\", \"Title\"" + " from contact where resume_tsv @@ to_tsquery(?) or \"Title\" ilike ? limit 30";

    static private String QUERY_COUNT  = "select count (id)" + " from contact where resume_tsv @@ to_tsquery(?) or \"Title\" ilike ?";

    @Inject
    private DBHelper      dbHelper;

    public SearchResult search(Map<String, String> searchValues) {
        Connection con = dbHelper.getConnection();
        SearchStatements statementAndValues = buildSearchStatements(con,searchValues);

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
        
        return searchResult;
    }

    private SearchStatements buildSearchStatements(Connection con, Map<String, String> searchValues) {
        SearchStatements ss = new SearchStatements();

        // build the statement
        ss.queryStmt = dbHelper.prepareStatement(con,QUERY_SELECT);
        ss.countStmt = dbHelper.prepareStatement(con,QUERY_COUNT);

        // build the values
        String search = searchValues.get("search");

        String searchILike = search;
        if (!search.contains("%")) {
            searchILike = "%" + search + "%";
        }

        String searchTq = Joiner.on(" & ").join(Splitter.on(" ").omitEmptyStrings().split(search));
        ss.values = new Object[] { searchTq, searchILike };

        return ss;
    }
}

class SearchStatements {

    PreparedStatement queryStmt;
    PreparedStatement countStmt;
    Object[]          values;

}
