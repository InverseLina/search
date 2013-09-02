package com.jobscience.search;

import javax.inject.Inject;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.britesnow.snow.web.RequestContext;
import com.google.inject.Singleton;
import com.jobscience.search.db.DBHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class CurrentOrgHolder {

    @Inject
    private CurrentRequestContextHolder crh;
    @Inject
    private DBHelper dbHelper;
    private Map<String, Map> orgMap = new HashMap<String, Map>();

    public String getSchema() {
        if (crh != null) {
            RequestContext rc = crh.getCurrentRequestContext();
            if (rc != null) {
                String orgName = rc.getCookie("org");
                if (orgName != null) {
                    if (orgMap.size() == 0) {
                        List<Map> list = dbHelper.executeQuery("select * from jss_sys.org");
                        for (Map map : list) {
                            orgMap.put((String) map.get("name"), map);
                        }
                    }


                    Map map = orgMap.get(orgName);
                    if (map != null) {
                        return (String) map.get("shemaname");
                    }
                }
            }
        }
        //return null;
        throw new IllegalArgumentException("current org name is null");
    }

}
