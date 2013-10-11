package com.jobscience.search.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.db.DBHelper;
import com.jobscience.search.db.DataSourceManager;

@Singleton
public class OrgConfigDao {

  @Inject
  private DBHelper dbHelper;
    @Inject
    private DataSourceManager dm;

  public void saveOrUpdateEntity(Map<String,String> params) throws SQLException{
    Connection con = dbHelper.getConnection(dm.getSysDataSource());
    String sql = "";
    if(params.size() > 0 && !"".equals(params.get("id"))&&params.get("id")!=null){
        sql = "update org set name = '"+params.get("name")+"',schemaname = '"+params.get("schemaname")+"',sfid='"+params.get("sfid")+"'  where id = "+params.get("id");
    }else{
        sql = " insert into org(name,schemaname,sfid) values ('"+params.get("name")+"','"+params.get("schemaname")+"','"+params.get("sfid")+"')";
    }
    PreparedStatement statement = con.prepareStatement(sql);
    statement.executeUpdate();
    statement.close();
    con.close();
  }
  
  public void deleteEntity(String id) throws SQLException{
      Connection con = dbHelper.getConnection(dm.getSysDataSource());
      PreparedStatement statement = con.prepareStatement("delete from org where id  = "+id);
      statement.executeUpdate();
      statement.close();
      con.close();
    }
  
  public Object getEntity(String id){
    String sql = "select * from org where id="+id;
    return dbHelper.executeQuery(dm.getSysDataSource(), sql);
  }
  
  public List<Map> getOrgByName(String name){
	    String sql = "select * from org where name='"+name+"'";
	    return dbHelper.executeQuery(dm.getSysDataSource(), sql);
 }
  
  public List getEntityList(String keyWords){
        String sql = "select * from org";
        return dbHelper.executeQuery(dm.getSysDataSource(), sql);
    }
}