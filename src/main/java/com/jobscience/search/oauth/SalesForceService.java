package com.jobscience.search.oauth;

import com.google.inject.Singleton;

@Singleton
public class SalesForceService {
 //   private  static final String SF_URL = "https://na15.salesforce.com/services/data/v28.0";
 //   private  static final String BASE_URL = "https://na15.salesforce.com";
 //   private  static final String SF_QUERY_URL = SF_URL+"/query";
/*    @Inject
    private DaoRegistry daoRegistry;
    
    
    *//**
     * @return JSONArray results
     *         Long result_count
     * @throws IOException 
     * @throws JSONException
     *//*
    public Map listContacts(String token, int pageIndex, int pageSize) throws IOException, JSONException {
        Map resultMap = new HashMap();
        String sql = "SELECT name, title FROM Contact LIMIT 100";
        OAuthRequest oauth = new OAuthRequest(Verb.GET,SF_QUERY_URL);
        oauth.addHeader("Authorization", "Bearer "+token);
        oauth.addHeader("X-PrettyPrint", "1");
        oauth.addQuerystringParameter("q", sql);
        Map opts = JsonUtil.toMapAndList(oauth.send().getBody());
        resultMap.put("result", opts.get("records"));
        List list = (JSONArray) opts.get("records");
        IDao<LabelRel> lableDao = daoRegistry.getDao(LabelRel.class);
        for (Object o : list) {
            Map map = (Map) o;
            Map attrs = (Map)map.get("attributes");
            String url = (String)attrs.get("url");
            List<LabelRel> rels = lableDao.search("from LabelRel rel where rel.contactId = ?", url);
            map.put("labels", rels);
        }

        resultMap.put("result_count", 20);
        return resultMap;
    }*/

}
