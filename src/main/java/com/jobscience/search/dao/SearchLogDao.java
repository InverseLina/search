package com.jobscience.search.dao;

import java.util.Date;

import org.jasql.Runner;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.organization.OrgContext;

@Singleton
public class SearchLogDao {

    @Inject
    private DaoHelper daoHelper;
    @Inject
    private DBSetupManager dbSetupManager;
    private String INSERT_SQL = "INSERT INTO jss_searchlog(user_id,date,search,perfcount,perffetch) values(?,?,?,?,?)";
    @Inject
    private DatasourceManager datasourceManager;

    public void addSearchLog(String search,Long perfCount,Long perfFetch,Long userId,OrgContext org){
        if(dbSetupManager.checkOrgExtra((String)org.getOrgMap().get("name")).contains("searchlog,")){
            Runner runner = datasourceManager.newOrgRunner((String)org.getOrgMap().get("name"));
            daoHelper.executeUpdate(runner, INSERT_SQL,userId,new Date(),search,perfCount,perfFetch);   
        }
    }
    
}
