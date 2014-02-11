package com.jobscience.search.dao;

import java.util.Date;
import java.util.Map;

import org.jasql.Runner;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SearchLogDao {

    @Inject
    private DaoHelper daoHelper;
    @Inject
    private DBSetupManager dbSetupManager;
    private String INSERT_SQL = "INSERT INTO searchlog(user_id,date,search,perfcount,perffetch) values(?,?,?,?,?)";
    
    public void addSearchLog(String search,Long perfCount,Long perfFetch,Long userId,Map org){
        if(dbSetupManager.checkOrgExtra((String)org.get("name")).contains("searchlog,")){
            Runner runner = daoHelper.openNewOrgRunner((String)org.get("name"));
            daoHelper.executeUpdate(runner, INSERT_SQL,userId,new Date(),search,perfCount,perfFetch);   
        }
    }
    
}
