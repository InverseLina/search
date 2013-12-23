package com.jobscience.search.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;
import java.util.Map;

@Singleton
public class SearchConfigurationDao {
    public static final String COL_NAME = "searchconfig";

    @Inject
    private DaoHelper daoHelper;


    public List getSearchConfig() {
        List<Map> result = daoHelper.executeQuery(daoHelper.openNewSysRunner(),
                "select val_text from config where name = ? and org_id is null", COL_NAME);
        return result;
    }

    public void resetSearchConfig() {
        daoHelper.executeUpdate(daoHelper.openNewSysRunner(),
                "delete  from config where name = ? and org_id is null", COL_NAME);
    }

    public void saveSearchConfig(String content) {
        List list = daoHelper.executeQuery(daoHelper.openNewSysRunner(),
                "select 1 from config where name = ? and org_id is null ", COL_NAME);
        if (list.size() == 0) {
            daoHelper.insert(daoHelper.openNewSysRunner(),
                    "insert into config (name, val_text) values(?,?)", COL_NAME, content);
        } else {
            daoHelper.executeUpdate(daoHelper.openNewSysRunner(),
                    "update config set val_text = ?  where name = ? and org_id is null", content, COL_NAME);
        }
    }

    public void resetOrgSearchConfig(String orgName) {
        daoHelper.executeUpdate(daoHelper.openNewSysRunner(),
                "delete  from config where name = ? and org_id in (select id from org where name = ?)", COL_NAME, orgName);
    }

    public void saveOrgSearchConfig(String orgName, String content) {
        List<Map> list = daoHelper.executeQuery(daoHelper.openNewSysRunner(),
                "select 1 from config where org_id in (select id from org where name = ?) and name = ?", orgName, COL_NAME);
        if(list.size() == 0){
            daoHelper.insert(daoHelper.openNewSysRunner(),
                    "insert into config (org_id, name, val_text) values((select id from org where name = ?), ?, ?)",
                    orgName, COL_NAME, content);
        }else{
            daoHelper.executeUpdate(daoHelper.openNewSysRunner(),
                    "update config set val_text = ? where  name = ? and org_id in " +
                            "(select id from org where name = ?)", content, COL_NAME, orgName);
        }
    }
}
