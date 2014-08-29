package com.jobscience.search.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.britesnow.snow.util.JsonUtil;
import com.google.inject.Singleton;
import com.jobscience.search.oauth.api.ForceDotComApi;

@Singleton
public class SalesForceCommonService {
    private  static final String SF_URL = "/services/data/v28.0";
    private  static final String SF_QUERY_URL = SF_URL+"/query";

    /**
     * get User info
     * @param token
     * @return
     * @throws IOException
     * @throws JSONException
     */
    public Map<String,String> getloginInfo(ForceDotComApi.ForceDotComToken token) throws IOException, JSONException {
    	Map<String,String> result = new HashMap<String,String>();
    	//----------------  Get the User display name and the user id -----------------//
        OAuthRequest oauth = new OAuthRequest(Verb.GET,token.getInstanceUrl()+SF_URL);
        oauth.addHeader("Authorization", "Bearer "+token.getToken());
        oauth.addHeader("X-PrettyPrint", "1");
        Response res = oauth.send();
        String body = res.getBody();
        Map opts = JsonUtil.toMapAndList(body);
        String identityUrl = opts.get("identity").toString();
        oauth = new OAuthRequest(Verb.GET,identityUrl);
        oauth.addHeader("Authorization", "Bearer "+token.getToken());
        oauth.addHeader("X-PrettyPrint", "1");
        Map info = JsonUtil.toMapAndList(oauth.send().getBody());
        result.put("userName", (String) info.get("display_name"));
        result.put("id", (String) info.get("id"));
        result.put("organization_id", (String) info.get("organization_id"));
        result.put("user_id", (String) info.get("user_id"));
        //----------------  /Get the User display name and the user id -----------------//
        
        //-------------------------  Get Org name for login user ----------------------//
        oauth = new OAuthRequest(Verb.GET,token.getInstanceUrl()+SF_QUERY_URL);
        oauth.addHeader("Authorization", "Bearer "+token.getToken());
        oauth.addHeader("X-PrettyPrint", "1");
        oauth.addQuerystringParameter("q", "SELECT Name from Organization where id = '"+info.get("organization_id")+"'");
        Map orgInfo = JsonUtil.toMapAndList(oauth.send().getBody());
        JSONArray orgs  = JSONArray.fromObject(orgInfo.get("records"));
        if(orgs.size() == 1){
        	JSONObject jo = JSONObject.fromObject(orgs.get(0));
        	 result.put("orgName",(String)jo.get("Name"));
        }
        //-------------------------  /Get Org name for login user ----------------------//
        return result;
    }

    public Map<String,String> getFullLoginInfo(String token, String instanceUrl) throws IOException, JSONException {
    	//----------------  Get the User display name and the user id -----------------//
        OAuthRequest oauth = new OAuthRequest(Verb.GET,instanceUrl+SF_URL);
        oauth.addHeader("Authorization", "Bearer "+token );
        oauth.addHeader("X-PrettyPrint", "1");
        Response res = oauth.send();
        String body = res.getBody();
        Map opts = JsonUtil.toMapAndList(body);
        String identityUrl = opts.get("identity").toString();
        oauth = new OAuthRequest(Verb.GET,identityUrl);
        oauth.addHeader("Authorization", "Bearer "+token );
        oauth.addHeader("X-PrettyPrint", "1");
        Map info = JsonUtil.toMapAndList(oauth.send().getBody());
        //----------------  /Get the User display name and the user id -----------------//

        //-------------------------  Get Org name for login user ----------------------//
        oauth = new OAuthRequest(Verb.GET,instanceUrl+SF_QUERY_URL);
        oauth.addHeader("Authorization", "Bearer "+token );
        oauth.addHeader("X-PrettyPrint", "1");
        oauth.addQuerystringParameter("q", "SELECT Name from Organization where id = '"+info.get("organization_id")+"'");
        Map orgInfo = JsonUtil.toMapAndList(oauth.send().getBody());
        JSONArray orgs  = JSONArray.fromObject(orgInfo.get("records"));
        if(orgs.size() == 1){
        	JSONObject jo = JSONObject.fromObject(orgs.get(0));
        	 info.put("orgName",(String)jo.get("Name"));
        }
        //-------------------------  /Get Org name for login user ----------------------//
        return info;
    }
}
