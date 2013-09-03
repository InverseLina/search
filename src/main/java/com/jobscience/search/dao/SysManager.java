package com.jobscience.search.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.db.DBHelper;

@Singleton
public class SysManager {

  @Inject
  private DBHelper dbHelper;
  public void saveOrUpdateEntity(Map<String,String> params) throws SQLException{
    Connection con = dbHelper.getConnection();
    String sql = "";
    if(params.size() > 0 && !"".equals(params.get("oldname"))){
        sql = "update jss_sys.org set name = '"+params.get("name")+"',schemaname = '"+params.get("schemaname")+"',sfid='"+params.get("sfid")+"'  where name = '"+params.get("oldname")+"'";
    }else{
        sql = " insert into jss_sys.org(name,schemaname,sfid) values ('"+params.get("name")+"','"+params.get("schemaname")+"','"+params.get("sfid")+"')";
    }
    PreparedStatement statement = con.prepareStatement(sql);
    statement.executeUpdate();
    statement.close();
    con.close();
  }
  
  public void deleteEntity(String name) throws SQLException{
      Connection con = dbHelper.getConnection();
      PreparedStatement statement = con.prepareStatement("delete from jss_sys.org where name  = '"+name+"'");
      statement.executeUpdate();
      statement.close();
      con.close();
    }
  
  public Object getEntity(String name){
    String sql = "select * from jss_sys.org where name='"+name+"'";
    return dbHelper.executeQuery(sql);    
  }
  
  public List getEntityList(String keyWords){
        String sql = "select * from jss_sys.org";
        return dbHelper.executeQuery(sql);      
    }
}