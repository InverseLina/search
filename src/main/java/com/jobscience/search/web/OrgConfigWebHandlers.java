package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.OrgConfigDao;

@Singleton
public class OrgConfigWebHandlers {

  @Inject
  private OrgConfigDao orgConfigDao;
  
  @WebPost("/org/save")
  public WebResponse saveEntity(@WebParam("name")String name,@WebParam("schemaname")String schemaname,@WebParam("sfid")String sfid,@WebParam("id")String id) throws SQLException{
    Map<String,String> params = new HashMap<String,String>();
    params.put("name", name);
    params.put("id", id);
    params.put("schemaname", schemaname);
    params.put("sfid", sfid);
    orgConfigDao.saveOrUpdateEntity(params);
    return WebResponse.success();
  }
  
  @WebGet("/org/get/{id}")
  public WebResponse getEntity(@WebParam("id")String id) throws SQLException{
    return WebResponse.success(orgConfigDao.getEntity(id));
  }
  
  @WebPost("/org/del/{id}")
    public WebResponse delEntity(@WebParam("id")String id) throws SQLException{
      orgConfigDao.deleteEntity(id);
        return WebResponse.success();
    }
  
  @WebGet("/org/list")
    public WebResponse listEntity(){
        List list = orgConfigDao.getEntityList(null);
        return WebResponse.success(list);
    }
  
}