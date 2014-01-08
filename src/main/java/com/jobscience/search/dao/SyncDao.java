package com.jobscience.search.dao;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.CurrentOrgHolder;

@Singleton
public class SyncDao {
    
    @Inject
    private DaoHelper daoHelper;
    @Inject
    private CurrentOrgHolder orgHolder;
    
    public  List<Map> getTablesByOrg(String schemaName){
        if(schemaName == null){
            schemaName = orgHolder.getSchemaName();
        }
        
        List<Map> list = daoHelper.executeQuery(daoHelper.openDefaultRunner(), "select table_name as name from information_schema.tables" +
                " where table_schema= ? and table_type='BASE TABLE'", schemaName);
        return list;
    }
    
    public  List<Map> getFields(String tableName){
        List<Map> list = daoHelper.executeQuery(daoHelper.openDefaultRunner(), "select column_name as name, data_type as type, character_maximum_length as length from information_schema.columns where table_name= ? ", tableName);
        return list;
    }
    
    public  List<Map> getData(String tableName, int pageIndex, int pageSize){
        List<Map> list = daoHelper.executeQuery(daoHelper.openNewOrgRunner(orgHolder.getOrgName()), "select * from "+tableName+" offset "+pageIndex+" limit "+pageSize);
        return list;
    }
    
    public  List<Map> getAllData(String tableName){
        List<Map> list = daoHelper.executeQuery(daoHelper.openDefaultRunner(), "select * from "+tableName);
        return list;
    }
}
