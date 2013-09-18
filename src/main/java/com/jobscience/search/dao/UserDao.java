package com.jobscience.search.dao;

import com.britesnow.snow.util.JsonUtil;
import com.google.inject.Singleton;
import com.jobscience.search.CurrentOrgHolder;
import com.jobscience.search.db.DBHelper;
import net.sf.json.JSONObject;
import org.apache.commons.lang.RandomStringUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@Singleton
public class UserDao {
    @Inject
    private DBHelper dbHelper;
    @Inject
    private CurrentOrgHolder orgHolder;


    public static final String selectSql = "select * from \"user\" where sfid = ?";
    public static final String selectByTokenSql = "select * from \"user\" where ctoken = ?";
    public static final String updateSql = "update \"user\" set ctoken = ? where sfid = ?";
    public static final String insertSql = "insert into \"user\" (sfid, ctoken) values(?,?)";

   public List<Map> getUserMap(String sfid){
         return dbHelper.executeQuery(orgHolder.getOrgName(), selectSql, sfid);
   }

    public Map getUserByToken(String ctoken) {
        List<Map> users = dbHelper.executeQuery(orgHolder.getOrgName(), selectByTokenSql, ctoken);
        if (users.size() > 0) {
            return users.get(0);
        }else{
            return null;
        }
    }

   public void updateCToken(String sfid, String CToken) {
       dbHelper.executeUpdate(orgHolder.getOrgName(), updateSql, CToken, sfid);
   }
   public void insertUser(String sfid, String CToken) {
       dbHelper.executeUpdate(orgHolder.getOrgName(), insertSql, sfid, CToken);
   }

   public String checkAndUpdateUser(int type, String content) {
       String sfid, ctoken;
       if (type == 1) {
           sfid = getSFIDbySF1(content);
       }else {
           sfid = getSFIDbySF2(content);
       }
       ctoken = buildCToken(sfid);
       orgHolder.setOrg(ctoken, sfid);
       List<Map> users = getUserMap(sfid);
       if (users.size() > 0) {
           updateCToken(sfid, ctoken);
       }else{
           insertUser(sfid, ctoken);
       }

       return ctoken;
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
        //String userId = (String) ((Map)((Map)map.get("context")).get("user")).get("userId");
        return orgId;
    }

    public  String buildCToken(String sfid) {
        if (sfid != null) {
            return RandomStringUtils.random(32, sfid);
        }else {
            return RandomStringUtils.random(32,"01234567890abcdedfhijklmnopqrst");
        }

    }

}
