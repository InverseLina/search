package com.jobscience.search;

import javax.inject.Inject;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.britesnow.snow.web.RequestContext;
import com.google.inject.Singleton;
import com.jobscience.search.db.DBHelper;
import com.jobscience.search.db.DataSourceManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class CurrentOrgHolder {

    private org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CurrentOrgHolder.class);
    @Inject
    private CurrentRequestContextHolder crh;
    @Inject
    private DBHelper dbHelper;
    @Inject
    private DataSourceManager dm;
    private Map<String, Map> orgMap = new HashMap<String, Map>();

    public String getOrgName() {
        return (String)getFieldValue("name");
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
        if (crh != null) {
            RequestContext rc = crh.getCurrentRequestContext();
            if (rc != null) {
                String orgName = rc.getCookie("org");
                if (orgName != null) {
                    if (orgMap.size() == 0) {
                        List<Map> list = dbHelper.executeQuery(dm.getSysDataSource(),"select * from jss_sys.org");
                        for (Map map : list) {
                            orgMap.put((String) map.get("name"), map);
                        }
                    }


                    Map map = orgMap.get(orgName);
                    if (map != null) {
                        return  map.get(fieldName);
                    }
                }
            }
        }
        OrganizationNotSelectException e = new OrganizationNotSelectException();
        log.warn("current org name is null", e);
        throw e;
    }

}
