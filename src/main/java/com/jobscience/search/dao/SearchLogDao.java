package com.jobscience.search.dao;

import java.util.Date;

import org.josql.Runner;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.CurrentOrgHolder;

@Singleton
public class SearchLogDao {

    @Inject
    private DaoHelper daoHelper;
    @Inject
    private CurrentOrgHolder orgHolder;
    @Inject
    private DBSetupManager dbSetupManager;
    private String INSERT_SQL = "INSERT INTO searchlog(user_id,date,search,perfcount,perffetch) values(?,?,?,?,?)";
    
    public void addSearchLog(String search,Long perfCount,Long perfFetch,Long userId){
        if(dbSetupManager.checkOrgExtra(orgHolder.getOrgName()).contains("searchlog,")){
            Runner runner = daoHelper.openNewOrgRunner(orgHolder.getOrgName());
            daoHelper.executeUpdate(runner, INSERT_SQL,userId,new Date(),search,perfCount,perfFetch);   
        }
    }
    
}
