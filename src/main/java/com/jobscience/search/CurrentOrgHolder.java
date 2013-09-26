package com.jobscience.search;

import javax.inject.Inject;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.britesnow.snow.web.RequestContext;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.db.DBHelper;
import com.jobscience.search.db.DataSourceManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class CurrentOrgHolder {

   
    @Inject
    private CurrentRequestContextHolder crh;
    @Inject
    private DBHelper dbHelper;
    @Inject
    private DataSourceManager dm;

    private Cache<String, Map> cache;

    private Map<String, Map> orgMap = new HashMap<String, Map>();

    @Named("jss.prod")
    @Inject
    private boolean productMode;

    public String getOrgName() {
        return (String)getFieldValue("name");
    }

    public CurrentOrgHolder() {
        cache = CacheBuilder.newBuilder().expireAfterAccess(8, TimeUnit.MINUTES)
                .maximumSize(100).build(new CacheLoader<String, Map>() {
                    @Override
                    public Map load(String ctoken) throws Exception {
                        return null;
                    }
                });
    }

    /*
      public String getSchema() {
            return (String) getFieldValue("schemaname");
        }
       */
    public Integer getId(){
        return (Integer) getFieldValue("id");
    }

    protected Object getFieldValue(String fieldName){
        
        Map map = null;
        if (crh != null) {
            RequestContext rc = crh.getCurrentRequestContext();
            if (rc != null) {
                String orgName = rc.getCookie("org");
                if (productMode || orgName == null) {
                    String ctoken = rc.getCookie("ctoken");
                    if (ctoken != null) {
                        map = getOrg(ctoken);
                    }

                } else {
                    if (orgName != null) {
                        map = orgMap.get(orgName);
                        if (map == null) {
                            List<Map> list = dbHelper.executeQuery(dm.getSysDataSource(), "select * from org where name = ?", orgName);
                            if (list.size() > 0)
                                map = list.get(0);
                            orgMap.put(orgName, map);
                        }
                    }
                }
            }
            if (map != null) {
                return map.get(fieldName);
            }
        }

        OrganizationNotSelectException e = new OrganizationNotSelectException();
//        log.warn("current org name is null", e);
        throw e;
    }

    private Map getOrg(String ctoken) {
        return cache.getIfPresent(ctoken);
    }

    public void setOrg(String ctoken, String sfid) {
        List<Map> list = dbHelper.executeQuery(dm.getSysDataSource(), "select * from org where sfid = ?", sfid);
        if (list.size() > 0) {
            cache.put(ctoken, list.get(0));
        }
        if (crh.getCurrentRequestContext() != null) {
            crh.getCurrentRequestContext().setCookie("ctoken", ctoken);
        }
    }

}
