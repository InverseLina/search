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
import com.jobscience.search.dao.SysManager;

@Singleton
public class SysWebHandlers {

  @Inject
  private SysManager sysManager;
  
  @WebPost("/sys/save")
  public WebResponse saveEntity(@WebParam("name")String name,@WebParam("schemaname")String schemaname,@WebParam("sfid")String sfid,@WebParam("oldname")String oldname) throws SQLException{
    Map<String,String> params = new HashMap<String,String>();
    params.put("name", name);
    params.put("oldname", oldname);
    params.put("schemaname", schemaname);
    params.put("sfid", sfid);
    sysManager.saveOrUpdateEntity(params);
    return WebResponse.success();
  }
  
  @WebGet("/sys/get/{name}")
  public WebResponse getEntity(@WebParam("name")String name) throws SQLException{
    return WebResponse.success(sysManager.getEntity(name));
  }
  
  @WebPost("/sys/del/{name}")
    public WebResponse delEntity(@WebParam("name")String name) throws SQLException{
      sysManager.deleteEntity(name);
        return WebResponse.success();
    }
  
  @WebGet("/sys/list")
    public WebResponse listEntity(){
        List list = sysManager.getEntityList(null);
        return WebResponse.success(list);
    }
  
}