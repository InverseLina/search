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
       List<Map> users = getUserMap(sfid);
       if (users.size() > 0) {
           Map user = users.get(0);
           updateCToken(sfid, ctoken);
       }else{
           insertUser(sfid, ctoken);
       }
       return ctoken;
   }

    public static String getSFIDbySF1(String id) {
        String[] parts = id.split("/");
        int len = parts.length;
        if(len > 3) {
            return parts[len - 2] + "-" + parts[len - 1];
        }
        throw new IllegalArgumentException("id " + id + " is invalid");
    }
    public static String getSFIDbySF2(String signedRequest) {
        JSONObject map = (JSONObject) JsonUtil.toMapAndList(signedRequest);
        String orgId = (String) ((Map)((Map)map.get("context")).get("organization")).get("organizationId");
        String userId = (String) ((Map)((Map)map.get("context")).get("user")).get("userId");
        return orgId + "-" + userId;
    }

    public static String buildCToken(String sfid) {
        if (sfid != null) {
            return RandomStringUtils.random(32, sfid);
        }else {
            return RandomStringUtils.random(32);
        }

    }

/*    public static void main(String[] args) {
        System.out.println(getSFIDbySF2("{\"client\":{\"instanceId\":\"woofgl:localhost_app:\",\"targetOrigin\":\"https://na15.salesforce.com\",\"instanceUrl\":\"https://na15.salesforce.com\",\"oauthToken\":\"00Di0000000auCe!ARwAQNXqe22qz6U_MXVGZTihQtskXUY.QyqVX9Wi6pyviiZxcOFlPBLnCB._MqKSBjfwKOIoWIIl09OCSB7F6TRqTL1.aa.Y\"},\"issuedAt\":null,\"userId\":\"005i0000000Pq1t\",\"context\":{\"application\":{\"name\":\"localhost app\",\"version\":\"1.0\",\"authType\":\"SIGNED_REQUEST\",\"namespace\":\"woofgl\",\"developerName\":\"localhost_app\",\"applicationId\":\"06Pi000000004ES\",\"canvasUrl\":\"https://localhost:8443/sf2\"},\"user\":{\"language\":\"en_US\",\"timeZone\":\"America/Los_Angeles\",\"locale\":\"en_US\",\"fullName\":\"weifeng liang\",\"networkId\":null,\"userId\":\"005i0000000Pq1tAAC\",\"userName\":\"woofgl@qq.com\",\"email\":\"woofgl@qq.com\",\"profileId\":\"00ei0000000xUGp\",\"isDefaultNetwork\":true,\"profilePhotoUrl\":\"https://c.na15.content.force.com/profilephoto/005/F\",\"userType\":\"STANDARD\",\"firstName\":\"weifeng\",\"lastName\":\"liang\",\"siteUrl\":null,\"currencyISOCode\":\"USD\",\"profileThumbnailUrl\":\"https://c.na15.content.force.com/profilephoto/005/T\",\"roleId\":null,\"siteUrlPrefix\":null,\"accessibilityModeEnabled\":false},\"organization\":{\"name\":\"xxx\",\"organizationId\":\"00Di0000000auCeEAI\",\"namespacePrefix\":\"woofgl\",\"multicurrencyEnabled\":false,\"currencyIsoCode\":\"USD\"},\"environment\":{\"parameters\":{},\"dimensions\":{\"width\":\"800px\",\"height\":\"900px\",\"maxHeight\":\"2000px\",\"maxWidth\":\"1000px\"},\"locationUrl\":\"https://na15.salesforce.com/_ui/platform/connect/ui/CanvasPreviewerUi?retURL=%2Fui%2Fsetup%2FSetup%3Fsetupid%3DStudio&setupid=CanvasPreviewerUi\",\"displayLocation\":null,\"uiTheme\":\"Theme3\",\"version\":{\"api\":\"28.0\",\"season\":\"SUMMER\"}},\"links\":{\"loginUrl\":\"https://login.salesforce.com/\",\"chatterFeedItemsUrl\":\"/services/data/v28.0/chatter/feed-items\",\"chatterFeedsUrl\":\"/services/data/v28.0/chatter/feeds\",\"chatterGroupsUrl\":\"/services/data/v28.0/chatter/groups\",\"chatterUsersUrl\":\"/services/data/v28.0/chatter/users\",\"enterpriseUrl\":\"/services/Soap/c/28.0/00Di0000000auCe\",\"metadataUrl\":\"/services/Soap/m/28.0/00Di0000000auCe\",\"partnerUrl\":\"/services/Soap/u/28.0/00Di0000000auCe\",\"queryUrl\":\"/services/data/v28.0/query/\",\"recentItemsUrl\":\"/services/data/v28.0/recent/\",\"restUrl\":\"/services/data/v28.0/\",\"searchUrl\":\"/services/data/v28.0/search/\",\"sobjectUrl\":\"/services/data/v28.0/sobjects/\",\"userUrl\":\"/005i0000000Pq1tAAC\"}},\"algorithm\":\"HMACSHA256\"}"));
        System.out.println(getSFIDbySF1("https://login.salesforce.com/id/00Di0000000auCeEAI/005i0000000Pq1tAAC"));
    }*/
}
