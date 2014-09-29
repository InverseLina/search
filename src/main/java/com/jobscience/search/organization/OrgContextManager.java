package com.jobscience.search.organization;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.britesnow.snow.web.RequestContext;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.dao.DaoRwHelper;
import com.jobscience.search.dao.OrgConfigDao;
import com.jobscience.search.exception.OrganizationNotSelectException;

@Singleton
public class OrgContextManager {
	
    @Inject
    private CurrentRequestContextHolder crh;
    @Inject
    private DaoRwHelper daoRwHelper;
    @Inject
    private OrgConfigDao orgConfigDao;
    @Inject
    private DBSetupManager dbSetupManager;
    
    private Cache<String, OrgContext> orgCacheByToken;
    private Cache<String, OrgContext> orgCacheByorgName;
    
    @Named("jss.prod")
    @Inject
    private boolean productMode;

    public String getOrgName() {
        return (String)getFieldValue("name");
    }
    
    public void updateSchema(){
        final String orgName = getOrgName();
    	List<Map> orgs = orgConfigDao.getOrgByName(orgName);
    	if(orgs.size() == 1){
    	    OrgContext orgContext = null;
            try {
                orgContext = orgCacheByorgName.get(orgName,new Callable<OrgContext>() {
                    @Override
                    public OrgContext call() throws Exception {
                        return loadOrgContext(orgName);
                    }
                });
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            if(orgContext != null){
        	    orgContext.setOrgMap(orgs.get(0));
        	    orgCacheByorgName.put(getOrgName(),orgContext);
            }
    	}
    }
    
    public String getSchemaName(){
        String orgName = getOrgName();
    	String schemaname = orgCacheByorgName.getIfPresent(orgName).getOrgMap().get("schemaname").toString();
    	if(schemaname == null){
	    	List<Map> orgs = orgConfigDao.getOrgByName(getOrgName());
	    	if(orgs.size() == 1){
	    		schemaname = orgs.get(0).get("schemaname").toString();
	    	}
    	}
    	return schemaname;
    }

    public OrgContextManager() {
        orgCacheByToken = CacheBuilder.newBuilder().expireAfterAccess(8, TimeUnit.MINUTES)
               .maximumSize(100).build(new CacheLoader<String, OrgContext>() {
                   @Override
                   public OrgContext load(String token) throws Exception {return null;}
               });
        orgCacheByorgName = CacheBuilder.newBuilder().expireAfterAccess(8, TimeUnit.MINUTES)
                .maximumSize(100).build(new CacheLoader<String, OrgContext>() {
                    @Override
                    public OrgContext load(String orgName) throws Exception {
                        OrgContext orgContext = new OrgContext();
                        orgContext.setOrgMap(loadOrg(orgName));
                        orgContext.setSfid(loadOrgSfid(orgName));
                        return orgContext;
                    }
                });
    }

    public Integer getId(){
        return (Integer) getFieldValue("id");
    }

    public OrgContext getCurrentOrg() {
        OrgContext orgContext = null;
        if (crh != null) {
            RequestContext rc = crh.getCurrentRequestContext();
            if (rc != null) {
                final String orgName = rc.getCookie("org");
                if (productMode || orgName == null) {
                    String ctoken = rc.getCookie("ctoken");
                    if (ctoken != null) {
                        orgContext = getOrg(ctoken);
                    }

                } else {
                    if (orgName != null) {
                        try {
                            orgContext = orgCacheByorgName.get(orgName,new Callable<OrgContext>() {
                                @Override
                                public OrgContext call() throws Exception {
                                    return loadOrgContext(orgName);
                                }
                            });
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        if(orgContext == null){
            OrganizationNotSelectException e = new OrganizationNotSelectException();
            throw e;
        }
        return orgContext;
    }

    /**
     * Set the salesforce token and the related org info into cookie
     * @param ctoken
     * @param sfid
     */
    public void setOrg(String ctoken, String sfid) {
        List<Map> list = daoRwHelper.executeQuery(daoRwHelper.datasourceManager.newSysRunner(), "select * from org where sfid = ?", sfid);
        if (list.size() > 0) {
            String orgName = (String) list.get(0).get("name");
            OrgContext orgContext = new OrgContext();
            orgContext.setOrgMap(list.get(0));
            orgContext.setSfid(loadOrgSfid(orgName));
            orgCacheByToken.put(ctoken, orgContext);
        }
        if (crh.getCurrentRequestContext() != null) {
            crh.getCurrentRequestContext().setCookie("ctoken", ctoken,true);
        }
    }

    public OrgContext getOrgContext(final String orgName){
        try {
            return orgCacheByToken.get(orgName,new Callable<OrgContext>() {
                @Override
                public OrgContext call() throws Exception {
                    return loadOrgContext(orgName);
                }
            });
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Object getFieldValue(String fieldName){
        Map map = getCurrentOrg().getOrgMap();
        if (map != null) {
            return map.get(fieldName);
        }
        OrganizationNotSelectException e = new OrganizationNotSelectException();
        throw e;
    }
    
    private OrgContext getOrg(String ctoken) {
        return orgCacheByToken.getIfPresent(ctoken);
    }
    
    private OrgContext loadOrgContext(String orgName){
        OrgContext orgContext = new OrgContext();
        orgContext.setOrgMap(loadOrg(orgName));
        orgContext.setSfid(loadOrgSfid(orgName));
        return orgContext;
    }
    
    private Map loadOrg(String orgName){
        List<Map> list = daoRwHelper.executeQuery(daoRwHelper.datasourceManager.newSysRunner(),
                "select * from org where name = ?", orgName);
        Map map = null;
        if (list.size() > 0) {
            map = list.get(0);
        }
        return map;
    }
    
    private String loadOrgSfid(String orgName){
        String sfid = null;
        if(dbSetupManager.hasOrgTable(orgName, "recordtype")){
            List<Map> list = daoRwHelper.executeQuery(orgName,
                    "select sfid from recordtype where recordtype.sobjecttype='Contact' "
                    +" and recordtype.name='Candidate' and recordtype.namespaceprefix='ts2'");
            if (list.size() > 0) {
                sfid = (String) list.get(0).get("sfid");
            }
        }
        return sfid;
    }
}
