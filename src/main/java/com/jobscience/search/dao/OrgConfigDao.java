package com.jobscience.search.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class OrgConfigDao {

  @Inject
  private DaoRwHelper daoRwHelper;

  public Integer saveOrUpdateOrg(Map<String,String> params) throws SQLException{
      String sql;
      if(params.size() > 0 && !"".equals(params.get("id")) && params.get("id") != null) {
          Integer id = Integer.valueOf(params.get("id"));
          sql = "update org set name = ? ,schemaname = ? ,sfid=?  where id = ?";
          daoRwHelper.executeUpdate(daoRwHelper.newSysRunner(), sql, params.get("name"),
                  params.get("schemaname"), params.get("sfid"), id);
          return id;
          
      }else{
          if(params.containsKey("id")){
              params.remove("id");
          }
          Map result = (Map) daoRwHelper.insert(daoRwHelper.newSysRunner(), "org", params);
          return (Integer) result.get("id");
      }
  }
  
  public void deleteOrg(String id) throws SQLException{
      daoRwHelper.executeUpdate(daoRwHelper.newSysRunner(), "delete from org where id  = "+id);
  }

  public Object getOrg(String id){
      String sql = "select * from org where id="+id;
      return daoRwHelper.executeQuery(daoRwHelper.newSysRunner(), sql);
  }
  
  public List<Map> getOrgByName(String name){
      String sql = "select * from org where name='"+name+"'";
      return daoRwHelper.executeQuery(daoRwHelper.newSysRunner(), sql);
  }
  
  public List<Map> getOrgNameById(int orgId){
      String sql = "select name from org where id='"+orgId+"'";
      return daoRwHelper.executeQuery(daoRwHelper.newSysRunner(), sql);
  }
  
  public List getOrgsList(String keyWords){
      String sql = "select * from org";
      return daoRwHelper.executeQuery(daoRwHelper.newSysRunner(), sql);
  }
}