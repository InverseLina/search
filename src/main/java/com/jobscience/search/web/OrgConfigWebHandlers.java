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
import com.jobscience.search.auth.RequireAdmin;
import com.jobscience.search.dao.DaoRwHelper;
import com.jobscience.search.dao.OrgConfigDao;
import com.jobscience.search.oauth.ForceDotComApiManager;
import com.jobscience.search.organization.OrgContextManager;

@Singleton
public class OrgConfigWebHandlers {

  @Inject
  private OrgConfigDao orgConfigDao;
  @Inject
  private OrgContextManager currentOrgHolder;
  @Inject
  private ForceDotComApiManager forceDotComApiManager;
  @Inject
  private DaoRwHelper daoRwHelper;
  @Inject
  private WebResponseBuilder webResponseBuilder;
  
  @WebPost("/org/save")
  @RequireAdmin
  public WebResponse saveEntity(@WebParam("name")String name,@WebParam("schemaname")String schemaname,
                                @WebParam("sfid")String sfid,@WebParam("orgId")String orgId,@WebParam("id")String id) throws SQLException{
      Map<String,String> params = new HashMap<String,String>();
      params.put("name", name);
      params.put("id", id);
      params.put("schemaname", schemaname);
      params.put("sfid", sfid);
      Integer oId = orgConfigDao.saveOrUpdateOrg(params);
      daoRwHelper.datasourceManager.updateDB(name);
      try{
          currentOrgHolder.updateSchema();
          forceDotComApiManager.clearForceDotComApi(currentOrgHolder.getId());
      }catch(Exception e){
      }
      return webResponseBuilder.success(oId);
  }
  
  @WebGet("/org/get/{id}")
  @RequireAdmin
  public WebResponse getEntity(@WebParam("id")String id) throws SQLException{
      return webResponseBuilder.success(orgConfigDao.getOrg(id));
  }
  
  @WebPost("/org/del/{id}")
  @RequireAdmin
  public WebResponse delEntity(@WebParam("id")String id) throws SQLException{
      orgConfigDao.deleteOrg(id);
      return webResponseBuilder.success();
  }
  
  @WebGet("/org/list")
  @RequireAdmin
  public WebResponse listEntity(){
      List list = orgConfigDao.getOrgsList(null);
      return webResponseBuilder.success(list);
  }
}