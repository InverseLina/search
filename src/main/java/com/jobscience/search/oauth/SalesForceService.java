package com.jobscience.search.oauth;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Verb;

import com.britesnow.snow.util.JsonUtil;
import com.google.inject.Singleton;

@Singleton
public class SalesForceService {
    private  static final String SF_URL = "https://na15.salesforce.com/services/data/v28.0";
    private  static final String SF_QUERY_URL = SF_URL+"/query";

    public Map<String,String> getloginInfo(String token) throws IOException, JSONException {
    	Map<String,String> result = new HashMap<String,String>();
        OAuthRequest oauth = new OAuthRequest(Verb.GET,SF_URL);
        oauth.addHeader("Authorization", "Bearer "+token);
        oauth.addHeader("X-PrettyPrint", "1");
        Map opts = JsonUtil.toMapAndList(oauth.send().getBody());
        String identityUrl = opts.get("identity").toString();
        oauth = new OAuthRequest(Verb.GET,identityUrl);
        oauth.addHeader("Authorization", "Bearer "+token);
        oauth.addHeader("X-PrettyPrint", "1");
        Map info = JsonUtil.toMapAndList(oauth.send().getBody());
        result.put("userName", (String) info.get("display_name"));
        
        oauth = new OAuthRequest(Verb.GET,SF_QUERY_URL);
        oauth.addHeader("Authorization", "Bearer "+token);
        oauth.addHeader("X-PrettyPrint", "1");
        oauth.addQuerystringParameter("q", "SELECT Name from Organization where id = '"+info.get("organization_id")+"'");
        Map orgInfo = JsonUtil.toMapAndList(oauth.send().getBody());
        JSONArray orgs  = JSONArray.fromObject(orgInfo.get("records"));
        if(orgs.size()==1){
        	JSONObject jo = JSONObject.fromObject(orgs.get(0));
        	 result.put("orgName",(String)jo.get("Name"));
        }
        return result;
    }
}
