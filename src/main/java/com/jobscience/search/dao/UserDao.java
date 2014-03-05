package com.jobscience.search.dao;

import static com.jobscience.search.Utils.demoSfid;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import net.sf.json.JSONObject;

import org.apache.commons.lang.RandomStringUtils;
import org.jasql.Runner;

import com.britesnow.snow.util.JsonUtil;
import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.britesnow.snow.web.RequestContext;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.organization.OrgContextManager;

@Singleton
public class UserDao {
    @Inject
    private DaoHelper daoHelper;
    @Inject
    private OrgContextManager orgHolder;
    @Inject
    private CurrentRequestContextHolder crh;
    @Named("jss.prod")
    @Inject
    private boolean productMode;

    public static final String selectSql = "select * from \"jss_user\" where sfid = ?";
    public static final String selectByTokenSql = "select * from \"jss_user\" where ctoken = ?";
    public static final String updateSql = "update \"jss_user\" set ctoken = ?, timeout = ?, rtoken=? where sfid = ?";
    public static final String insertSql = "insert into \"jss_user\" (sfid, ctoken, timeout,rtoken) values(?,?,?,?) returning *";

    public Map getCurrentUser(){
       
        RequestContext rc = crh.getCurrentRequestContext();
        if (rc != null) {
            String ctoken = rc.getCookie("ctoken");
            if(ctoken != null){
                return getUserByToken(ctoken);
            }
        }
        return null;
    }
    
    public List<Map> getUserMap(String sfid){
         return daoHelper.executeQuery(orgHolder.getOrgName(), selectSql, sfid);
    }

    public Map getUserByToken(String ctoken) {
        List<Map> users = daoHelper.executeQuery(orgHolder.getOrgName(), selectByTokenSql, ctoken);
        if (users.size() > 0) {
            return users.get(0);
        }else{
            return null;
        }
    }
    public Map getUserByTokenAndOrg(String ctoken, String orgName) {
        List<Map> users = daoHelper.executeQuery(orgName, selectByTokenSql, ctoken);
        if (users.size() > 0) {
            return users.get(0);
        }else{
            return null;
        }
    }

   public void updateCToken(String sfid, String ctoken, long sfTimeout, String rtoken) {
       daoHelper.executeUpdate(orgHolder.getOrgName(), updateSql, ctoken, sfTimeout,sfid,rtoken);
   }
   
   public Map insertUser(String sfid, String ctoken, long sfTimeout, String rtoken) {
       //for now label, sfid should not null
       if (sfid == null) {
           sfid = demoSfid();
       }
       
       Runner runner = daoHelper.openNewOrgRunner(orgHolder.getOrgName());
       Map user = runner.executeWithReturn(insertSql, sfid, ctoken, sfTimeout, rtoken);
       runner.close();
       return user;
   }

   public Map checkAndUpdateUser(int type, String content, String token,
                                    long sfTimeout, String rtoken) {
       String sfid, ctoken;
       if (type == 1) {
           sfid = getSFIDbySF1(content);
       }else {
           sfid = getSFIDbySF2(content);
       }
       if(type == 1){
           ctoken = token;
       }else{
           ctoken = buildCToken(sfid);
       }
       orgHolder.setOrg(ctoken, sfid);
       List<Map> users = getUserMap(sfid);
       if (users.size() > 0) {
           Map user = users.get(0);
           user.put("sfid",sfid);
           user.put("ctoken",ctoken);
           user.put("timeout",sfTimeout);
           user.put("rtoken",rtoken);
           updateCToken(sfid, ctoken, sfTimeout, rtoken);
           return user;
       }else{
           return insertUser(sfid, ctoken, sfTimeout, rtoken);
       }

   }

    public  String getSFIDbySF1(String id) {
        String[] parts = id.split("/");
        int len = parts.length;
        if(len > 3) {
            return parts[len - 2] ;
        }
        throw new IllegalArgumentException("id " + id + " is invalid");
    }
    
    public  String getSFIDbySF2(String signedRequest) {
        JSONObject map = (JSONObject) JsonUtil.toMapAndList(signedRequest);
        String orgId = (String) ((Map)((Map)map.get("context")).get("organization")).get("organizationId");
        System.out.println(signedRequest);
        return orgId;
    }

    public  String buildCToken(String sfid) {
        if (sfid != null) {
            return "SF" + RandomStringUtils.random(32, sfid);
        }else {
            return RandomStringUtils.random(32,"01234567890abcdedfhijklmnopqrst");
        }
    }

}
