package com.jobscience.search.dao;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.searchconfig.SearchConfigurationManager;

@Singleton
public class SearchConfigurationDao {
    public static final String COL_NAME = "searchconfig";

    @Inject
    private DaoHelper daoHelper;
    @Inject
    private DatasourceManager datasourceManager;
    @Inject
    private SearchConfigurationManager searchConfigurationManager;

    public List getSearchConfig() {
        List<Map> result = daoHelper.executeQuery(datasourceManager.newSysRunner(),
                "select val_text from config where name = ? and org_id is null", COL_NAME);
        return result;
    }

    public void resetSearchConfig() {
        daoHelper.executeUpdate(datasourceManager.newSysRunner(),
                "delete  from config where name = ? and org_id is null", COL_NAME);
        searchConfigurationManager.updateCache(null);
    }

    public void saveSearchConfig(String content) {
        List list = daoHelper.executeQuery(datasourceManager.newSysRunner(),
                "select 1 from config where name = ? and org_id is null ", COL_NAME);
        if (list.size() == 0) {
            daoHelper.create(datasourceManager.newSysRunner(),
                    "insert into config (name, val_text) values(?,?) returning id", COL_NAME, content);
        } else {
            daoHelper.executeUpdate(datasourceManager.newSysRunner(),
                    "update config set val_text = ?  where name = ? and org_id is null", content, COL_NAME);
        }
        searchConfigurationManager.updateCache(null);
    }

    public void resetOrgSearchConfig(String orgName) {
        daoHelper.executeUpdate(datasourceManager.newSysRunner(),
                "delete  from config where name = ? and org_id in (select id from org where name = ?)", COL_NAME, orgName);
        searchConfigurationManager.updateCache(orgName);
    }

    public void saveOrgSearchConfig(String orgName, String content) {
        List<Map> list = daoHelper.executeQuery(datasourceManager.newSysRunner(),
                "select 1 from config where org_id in (select id from org where name = ?) and name = ?", orgName, COL_NAME);
        if(list.size() == 0){
            daoHelper.create(datasourceManager.newSysRunner(),
                    "insert into config (org_id, name, val_text) values((select id from org where name = ?), ?, ?) returning id",
                    orgName, COL_NAME, content);
        }else{
            daoHelper.executeUpdate(datasourceManager.newSysRunner(),
                    "update config set val_text = ? where  name = ? and org_id in " +
                            "(select id from org where name = ?)", content, COL_NAME, orgName);
        }
        searchConfigurationManager.updateCache(orgName);
    }
}
