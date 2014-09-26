package com.jobscience.search.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.base.Strings;
import com.google.inject.Singleton;
import com.jobscience.search.organization.OrgContext;
import com.jobscience.search.searchconfig.Filter;
import com.jobscience.search.searchconfig.SearchConfiguration;
import com.jobscience.search.searchconfig.SearchConfigurationManager;

@Singleton
public class CustomFieldsDao {
	
	@Inject
    private OrgConfigDao orgConfigDao;
	
	@Inject
    private DaoHelper daoHelper;
	
	@Inject
    private DatasourceManager datasourceManager;
	
	@Inject
	private SearchConfigurationManager searchConfigurationManager;

	/**
	 * get the org customFields 
	 * @param org
	 * @return
	 */
	public List<Map> getCustomFields(OrgContext org){
    	String orgName = org.getOrgMap().get("name").toString();
    	List<Map> customFields = new ArrayList<Map>();
    	SearchConfiguration orgSearchConfig = null;
    	List<Filter> filterLists;
    	orgSearchConfig =  searchConfigurationManager.getSearchConfiguration(orgName);
		if(orgSearchConfig != null){
			filterLists = orgSearchConfig.getCustomFilters();
			if(filterLists != null){
				for(Filter filter : filterLists){
					if(!checkOrgCustomFieldIsValid(orgName, filter.getFilterField().getColumn())){
						continue;
					}
					HashMap fieldMap = new HashMap();
					fieldMap.put("name", filter.getName());
					fieldMap.put("label", filter.getTitle());
					fieldMap.put("type", filter.getFilterType().value());
					fieldMap.put("bg_color", filter.getBg_color());
					customFields.add(fieldMap);
				}
			}
		}
    	return customFields;
    }
    
	/**
	 * get the autoCompleteData for customField column and field type is String
	 * @param org
	 * @param fieldName
	 * @param searchText
	 * @return
	 */
	public List<Map> getCustomFieldCompleteData(OrgContext org, String fieldName, String searchText) {
    	String orgName = org.getOrgMap().get("name").toString();
    	List<Map> columnData = new ArrayList<Map>();
    	SearchConfiguration orgSearchConfig = searchConfigurationManager.getSearchConfiguration(orgName);
    	List<Filter> customFilterLists = orgSearchConfig.getCustomFilters();
    	if(orgSearchConfig != null && customFilterLists.size() > 0){
        		for(Filter customFilter : customFilterLists){
        			if(customFilter.getName().equals(fieldName)){
        				columnData = getColumnData(orgName,"contact",customFilter.getFilterField().getColumn(),searchText,4);
        				break;
        			}
        		}
        	}
    	return columnData;
    }
	
	private List<Map> getColumnData(String orgName, String table, String column, String searchText, int limit){
		StringBuilder sql = new StringBuilder();
        sql.append("select distinct ").append(column).append(" as value from ").append(table);
        if(!Strings.isNullOrEmpty(searchText)){
        	sql.append(" where ").append(column).append(" ilike '").append(searchText).append("%' ");
        }
        if(limit <= 0){
        	limit = 4;
        }
        sql.append(" limit ").append(limit);
        List<Map> data = daoHelper.executeQuery(datasourceManager.newOrgRunner(orgName),sql.toString());
        return data;
    }
	
	private boolean checkOrgCustomFieldIsValid(String orgName, String columnName) {
    	List<Map> orgs = orgConfigDao.getOrgByName(orgName);
        String schemaname = "";
        if (orgs.size() == 1) {
            schemaname = orgs.get(0).get("schemaname").toString();
        }else{
        	return false;
        }
    	try {
			if(checkColumn(columnName,"contact",schemaname)){
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
    	return false;
    }

	/**
     * check a table have a column or not
     * 
     * @param columnName
     * @param table
     * @param schemaName
     * @return
     * @throws SQLException
     */
    private boolean checkColumn(String columnName, String table, String schemaName)
            throws SQLException {
        boolean result = false;
        List list = daoHelper.executeQuery(datasourceManager.newRunner(),
                " select 1 from information_schema.columns "
                        + " where table_name =? and table_schema=?  and column_name=? ", table,
                schemaName, columnName);
        if (list.size() > 0) {
            result = true;
        }
        return result;
    }

}
