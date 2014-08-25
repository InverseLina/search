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
import com.jobscience.search.searchconfig.CustomField;
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
	 * @param org
	 * @return
	 */
	public List<Map> getCustomFields(OrgContext org){
    	String orgName = org.getOrgMap().get("name").toString();
    	List<Map> customFields = new ArrayList<Map>();
    	SearchConfiguration orgSearchConfig = null;
    	List<CustomField> customFieldLists;
    	orgSearchConfig =  searchConfigurationManager.getSearchConfiguration(orgName);
		if(orgSearchConfig != null && orgSearchConfig.getCustomFields() != null){
			customFieldLists = orgSearchConfig.getCustomFields().getFields();
			if(customFieldLists != null){
				for(CustomField field:customFieldLists){
					if(!checkOrgCustomFieldIsValid(orgName, field.getColumnName())){
						continue;
					}
					HashMap fieldMap = new HashMap();
					fieldMap.put("name", field.getName());
					fieldMap.put("label", field.getLabel());
					fieldMap.put("type", field.getType());
					customFields.add(fieldMap);
				}
			}
		}
    	return customFields;
    }
    
	public List<Map> getCustomFieldCompleteData(OrgContext org, String fieldName, String searchText) {
    	String orgName = org.getOrgMap().get("name").toString();
    	List<Map> columnData = new ArrayList<Map>();
    	SearchConfiguration orgSearchConfig = searchConfigurationManager.getSearchConfiguration(orgName);
    	List<CustomField> customFieldLists;
    	if(orgSearchConfig != null && orgSearchConfig.getCustomFields() != null){
        		customFieldLists = orgSearchConfig.getCustomFields().getFields();
        		for(CustomField field:customFieldLists){
        			if(field.getName().equals(fieldName)){
        				columnData = getColumnData(orgName,"contact",field.getColumnName(),searchText,4);
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
        	sql.append(" where ").append(column).append(" like '").append(searchText).append("%' ");
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
