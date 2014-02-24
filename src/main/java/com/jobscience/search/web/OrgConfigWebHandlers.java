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
import com.jobscience.search.CurrentOrgHolder;
import com.jobscience.search.dao.DaoHelper;
import com.jobscience.search.dao.OrgConfigDao;
import com.jobscience.search.oauth.ForceDotComApiManager;

@Singleton
public class OrgConfigWebHandlers {

  @Inject
  private OrgConfigDao orgConfigDao;
  @Inject
  private DaoHelper daoHelper;
  @Inject
  private CurrentOrgHolder currentOrgHolder;
  @Inject
  private ForceDotComApiManager forceDotComApiManager;

  @Inject
  private WebResponseBuilder webResponseBuilder;
  
  @WebPost("/org/save")
  public WebResponse saveEntity(@WebParam("name")String name,@WebParam("schemaname")String schemaname,
                                @WebParam("sfid")String sfid,@WebParam("orgId")String orgId,@WebParam("id")String id) throws SQLException{
      Map<String,String> params = new HashMap<String,String>();
      params.put("name", name);
      params.put("id", id);
      params.put("schemaname", schemaname);
      params.put("sfid", sfid);
      orgConfigDao.saveOrUpdateOrg(params);
      daoHelper.updateDataSource(name);
      try{
          currentOrgHolder.updateSchema();
          forceDotComApiManager.clearForceDotComApi(currentOrgHolder.getId());
      }catch(Exception e){
      }
      return webResponseBuilder.success();
  }
  
  @WebGet("/org/get/{id}")
  public WebResponse getEntity(@WebParam("id")String id) throws SQLException{
      return webResponseBuilder.success(orgConfigDao.getOrg(id));
  }
  
  @WebPost("/org/del/{id}")
  public WebResponse delEntity(@WebParam("id")String id) throws SQLException{
      orgConfigDao.deleteOrg(id);
      return webResponseBuilder.success();
  }
  
  @WebGet("/org/list")
  public WebResponse listEntity(){
      List list = orgConfigDao.getOrgsList(null);
      return webResponseBuilder.success(list);
  }
}