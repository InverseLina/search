package com.jobscience.search.dao;

import java.util.Date;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.organization.OrgContext;

@Singleton
public class SearchLogDao {

    @Inject
    private DaoRwHelper daoRwHelper;
    @Inject
    private DBSetupManager dbSetupManager;
    private String INSERT_SQL = "INSERT INTO jss_searchlog(user_id,date,search,perfcount,perffetch) values(?,?,?,?,?)";

    public void addSearchLog(String search,Long perfCount,Long perfFetch,Long userId,OrgContext org){
        if(!dbSetupManager.checkOrgExtra((String)org.getOrgMap().get("name")).contains("searchlog,")){
            daoRwHelper.executeUpdate(org.getOrgMap().get("name").toString(), INSERT_SQL,userId,new Date(),search,perfCount,perfFetch);   
        }
    }
    
}
