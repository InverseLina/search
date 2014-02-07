package com.jobscience.search;

import javax.inject.Inject;

import com.britesnow.snow.web.AbortWithHttpRedirectException;
import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.britesnow.snow.web.RequestContext;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.dao.DaoHelper;
import com.jobscience.search.dao.OrgConfigDao;
import com.jobscience.search.exception.OrganizationNotSelectException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class CurrentOrgHolder {
    @Inject
    private CurrentRequestContextHolder crh;
    @Inject
    private DaoHelper daoHelper;

    @Inject
    private OrgConfigDao orgConfigDao;
    private Cache<String, Map> cache;
    private Map<String, Map> orgMap = new HashMap<String, Map>();
    @Named("jss.prod")
    @Inject
    private boolean productMode;

    public String getOrgName() {
        return (String)getFieldValue("name");
    }
    
    public void updateSchema(){
    	List<Map> orgs = orgConfigDao.getOrgByName(getOrgName());
    	if(orgs.size()==1){
    		orgMap.put(getOrgName(), orgs.get(0));
    	}
    }
    
    public String getSchemaName(){
    	String schemaname =orgMap.get(getOrgName()).get("schemaname").toString();
    	if(schemaname==null){
	    	List<Map> orgs = orgConfigDao.getOrgByName(getOrgName());
	    	if(orgs.size()==1){
	    		schemaname = orgs.get(0).get("schemaname").toString();
	    	}
    	}
    	return schemaname;
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
                            List<Map> list = daoHelper.executeQuery(daoHelper.openNewSysRunner(),
                                    "select * from org where name = ?",
                                    orgName);
                            if (list.size() > 0){
                                map = list.get(0);
                            }
                            orgMap.put(orgName, map);
                        }
                        if(map.get("sfid") != null&&((String)map.get("sfid")).length()>0){
                              //forece sf1 test
                            rc.removeCookie("org");
                            throw new AbortWithHttpRedirectException("/sf1");
                        }
                    }
                }
            }
            if (map != null) {
                return map.get(fieldName);
            }
        }

        OrganizationNotSelectException e = new OrganizationNotSelectException();
        throw e;
    }

    private Map getOrg(String ctoken) {
        return cache.getIfPresent(ctoken);
    }
    
    /**
     * Set the salesforce token and the related org info into cookie
     * @param ctoken
     * @param sfid
     */
    public void setOrg(String ctoken, String sfid) {
        List<Map> list = daoHelper.executeQuery(daoHelper.openNewSysRunner(), "select * from org where sfid = ?", sfid);
        if (list.size() > 0) {
            cache.put(ctoken, list.get(0));
        }
        if (crh.getCurrentRequestContext() != null) {
            crh.getCurrentRequestContext().setCookie("ctoken", ctoken);
        }
    }

}
