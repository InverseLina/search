package com.jobscience.search.oauth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.scribe.builder.ServiceBuilder;
import org.scribe.oauth.OAuthService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.ConfigManager;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.dao.DaoHelper;
import com.jobscience.search.exception.OAuthConfigBuildException;
import com.jobscience.search.organization.OrgContextManager;

@Singleton
public class OAuthHelper {
    @Inject
    private ConfigManager configManager;
    @Inject
    private DaoHelper daoHelper;
    @Inject
    private DBSetupManager dbSetupManager;
    @Inject 
    private ForceDotComApiManager forceDotComApiManager;
    
    @Inject 
    private OrgContextManager currentOrgHolder;

    private volatile String apiKey = null;
    private volatile String apiSecret = null;
    private volatile String callbackUrl = null;
    
    private Logger log = Logger.getLogger(getClass());

    /**
     * Get the auth service for salesforce,
     * First get the app conifg for current org,if not existed,
     * would use global app config(snow.properties) as default
     * @return
     */
    public OAuthService  getService(){
    	List<Map> list = new ArrayList();
	    if(dbSetupManager.checkSysTables().contains("jss_config")){
	        String sql = "select * from config  where org_id = -1 ";
	        List<Map> configList = daoHelper.executeQuery(daoHelper.openNewSysRunner(), sql);
	        if (configList != null && configList.size() > 0) {
	            list = configList;
	        }
    	}
        if(list != null && list.size() > 0){
            apiKey = configManager.getConfig("saleforce.apiKey", currentOrgHolder.getId());
            apiSecret = configManager.getConfig("saleforce.apiSecret", currentOrgHolder.getId());
            callbackUrl = configManager.getConfig("saleforce.callBackUrl", currentOrgHolder.getId());
            ServiceBuilder builder = null;
            OAuthService servcie = null;
            try{
                builder = new ServiceBuilder().provider(forceDotComApiManager.getForceDotComApi(currentOrgHolder.getId())).apiKey(apiKey).apiSecret(apiSecret).callback(callbackUrl);
                servcie = builder.build();
            }catch(Exception e){
                log.error(e.getMessage());
                throw new OAuthConfigBuildException(e.getMessage());
            }
            return servcie;
        }
        return null;
    }

    public String getApiKey() {
        if (apiKey == null) {
            getService();
        }
        return apiKey;
    }

    public String getApiSecret() {
        if (callbackUrl == null) {
            getService();
        }
        return apiSecret;
    }

    public String getCallbackUrl() {
        if (callbackUrl == null) {
            getService();
        }
        return callbackUrl;
    }
}
