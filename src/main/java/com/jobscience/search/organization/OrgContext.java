package com.jobscience.search.organization;

import java.util.Map;

public class OrgContext {

    private Map orgMap;
    private String sfid;
    
    public Map getOrgMap() {
        return orgMap;
    }

    public void setOrgMap(Map orgMap) {
        orgMap.put("schemaname", "\""+orgMap.get("schemaname")+"\"");
        this.orgMap = orgMap;
    }

    public String getSfid() {
        return sfid;
    }

    public void setSfid(String sfid) {
        this.sfid = sfid;
    }
    
}
