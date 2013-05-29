package com.jobscience.search.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.base.Throwables;
import com.jobscience.search.db.DBHelper;

@Singleton
public class SearchDao {

//    static private String QUERY_SELECT = "select \"Name\", \"id\", \"Title\"" + " from contact where resume_tsv @@ to_tsquery(?) or \"Title\" ilike ? limit 30";
//
//    static private String QUERY_COUNT  = "select count (id)" + " from contact where resume_tsv @@ to_tsquery(?) or \"Title\" ilike ?";

    @Inject
    private DBHelper      dbHelper;

    public SearchResult search(Map<String, String> searchValues, String searchMode, Integer pageIdx, Integer pageSize) {
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

    private SearchStatements buildSearchStatements(Connection con, Map<String, String> searchValues,String searchMode,
                                                   Integer pageIdx, Integer pageSize) {
        SearchStatements ss = new SearchStatements();
        int offset = (pageIdx -1)* pageSize;

        // build the statement
        ss.queryStmt = dbHelper.prepareStatement(con,buildSql(searchValues, searchMode, offset, pageSize));
        ss.countStmt = dbHelper.prepareStatement(con,buildSql(searchValues, searchMode, -1,-1));

       /* // build the values
        String search = searchValues.get("search");

        String searchILike = search;
        if (!search.contains("%")) {
            searchILike = "%" + search + "%";
        }

        String searchTq = Joiner.on(" & ").join(Splitter.on(" ").omitEmptyStrings().split(search));*/
        ss.values = new Object[] {};

        return ss;
    }
    
    /**
     * @param searchValues
     * @param searchMode
     * @param offset -1, for count 0 for list,1 for count
     * @param limit
     * @return
     */
    public String buildSql( Map<String, String> searchValues,String searchMode,int offset, int limit){
    	StringBuffer sb = new StringBuffer();
    	if(offset>=0){
    		sb.append("select \"Name\", \"id\", \"Title\"" + " from contact where 1=1 ");
    	}else{
    		sb.append("select count(id) from contact where 1=1 ");
    	}
    	if(searchValues!=null){
	    	if("keyword".equals(searchMode)){
	    		for(String key:searchValues.keySet()){
	    			if("search".equals(key)&&searchValues.get(key)!=null&&!"".equals(searchValues.get(key))){
	    				sb.append(" and ( \"Name\" ilike '%"+searchValues.get(key)+"%' or \"Title\" ilike '%"+searchValues.get(key)+"%')");
	    				continue;
	    			}
	    			sb.append(" and \""+covertToColumnName(key)+"\" ilike '%"+searchValues.get(key)+"%'");
	    		}
	    	}else if("simple".equals(searchMode)){
	    		sb.append(" and resume_tsv @@ to_tsquery('"+searchValues.get("search")+"') or \"Title\" ilike '%"+searchValues.get("search")+"%'");
	    		 sb.append("  or \"Name\" ilike '%"+searchValues.get("search")+"%'");
	    	}
    	}
    	if(offset>=0){
    		sb.append(" offset ").append(offset).append(" limit ").append(limit);
    	}
    	
    	System.out.println(sb);
    	return sb.toString();
    }
    
    private String covertToColumnName(String src){
    	if(src.toLowerCase().equals("firstname")){
    		return "FirstName";
    	}else if (src.toLowerCase().equals("lastname")){
    		return "LastName";
    	}else{
    		return src.substring(0,1).toUpperCase()+src.substring(1).toLowerCase();
    	}
    }
}


class SearchStatements {

    PreparedStatement queryStmt;
    PreparedStatement countStmt;
    Object[]          values;

}
