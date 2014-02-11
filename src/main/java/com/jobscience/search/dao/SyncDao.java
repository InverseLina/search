package com.jobscience.search.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jasql.Runner;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SyncDao {
    
    @Inject
    private DaoHelper daoHelper;
    
    public  List<Map> getTablesByOrg(String schemaName,Map org){
        if(schemaName == null){
            schemaName = (String)org.get("schemaname");
        }
        
        List<Map> list = daoHelper.executeQuery(daoHelper.openDefaultRunner(), "select table_name as name from information_schema.tables" +
                " where table_schema= ? and table_type='BASE TABLE'", schemaName);
        return list;
    }
    
    public  List<Map> getFields(String tableName){
        List<Map> list = daoHelper.executeQuery(daoHelper.openDefaultRunner(), "select column_name as name, data_type as type, character_maximum_length as length from information_schema.columns where table_name= ? ", tableName);
        return list;
    }
    
    public  List<Map> getData(String tableName, int pageIndex, int pageSize,Map org){
        List<Map> list = daoHelper.executeQuery(daoHelper.openNewOrgRunner((String)org.get("name")), "select * from "+tableName+" offset "+pageIndex+" limit "+pageSize);
        return list;
    }
    
    public  List<Map> getAllData(String tableName){
        List<Map> list = daoHelper.executeQuery(daoHelper.openDefaultRunner(), "select * from "+tableName);
        return list;
    }
    
    public void syncFromSF(String tableName, List<Map> data,Map org){
        if(data==null||data.size()==0){
            return ;
        }
        StringBuilder columns = new StringBuilder();
        List<String> columnsList = new ArrayList<String>();
        for(Object key: data.get(0).keySet()){
            if(!"id".equals((String)key)){
                columns.append(",\"").append(key).append("\"");
                columnsList.add(key.toString());
            }
        }
        columns.delete(0, 1);
        
        String prefix = "insert into "+tableName+"("+columns+") values(";
        StringBuilder sql =new StringBuilder();
        Runner runner = daoHelper.openNewOrgRunner((String)org.get("name"));
        int time = 0;
        for(Map d:data){
            if(time%99==0){
                System.out.println(sql);
                runner.executeUpdate(sql.toString());
                sql = new StringBuilder();
            }
            sql.append(prefix);
            for(String column:columnsList){
                sql.append(wrapValue(d.get(column))+",");
            }
            sql.delete(sql.length()-1, sql.length()).append(");");
        }
        
        runner.executeUpdate(sql.toString());
    }
    
    private String wrapValue(Object value) {
        if (value == null||value.equals("null") ||
            value.equals("true") || value.equals("false")||
            !(value instanceof String)) {
            return value+"";
        }
        return "\'"+value.toString().replaceAll("\'", "\'\'")+"'";
    }
    public static void main(String[] args) {
        System.out.println("ss"+null);
    }
}
