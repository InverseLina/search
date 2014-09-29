package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.param.annotation.WebUser;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.jobscience.search.dao.DaoRwHelper;
import com.jobscience.search.organization.OrgContextManager;

@Singleton
public class PrefWebHandlers {

    @Inject
    private OrgContextManager orgHolder;

    @Inject
    private DaoRwHelper daoRwHelper;

    @Inject
    private WebResponseBuilder webResponseBuilder;

    @WebGet("/perf/get-user-pref")
    public WebResponse getUserPref (@WebUser Map user) throws SQLException {
        try {
            List<Map> prefs = daoRwHelper.executeQuery(orgHolder.getOrgName(), "select * from jss_pref where name = ? and user_id = ?", "filter_order", user.get("id"));
            if (prefs.size() == 1) {
                return webResponseBuilder.success(prefs.get(0));
            } else {
                return webResponseBuilder.success(false);
            }
        } catch (Exception e) {
            return webResponseBuilder.success(false);
        }
    }

    //EndUser use this
    @WebPost("/perf/save-user-pref")
    public WebResponse saveUserPref (@WebUser Map user, @WebParam("value") String value, RequestContext rc) throws SQLException {
        if (user != null && value.trim().length() > 0) {
            try {
                List<Map> prefs = daoRwHelper.executeQuery(orgHolder.getOrgName(),
                        "select * from jss_pref where name = ? and user_id = ?", "filter_order", user.get("id"));
                if (prefs.size() == 0) {
                    daoRwHelper.executeUpdate(orgHolder.getOrgName(),
                            "insert into jss_pref (user_id, name, val_text) values(?,?,?)",user.get("id"),"filter_order", value);
                } else {
                    daoRwHelper.executeUpdate(orgHolder.getOrgName(),
                            "update jss_pref set val_text = ? where id = ?",value, prefs.get(0).get("id") );
                }
                return webResponseBuilder.success(true);
            } catch (Exception e) {
                return webResponseBuilder.success(false);
            }
        }else{
            return webResponseBuilder.fail();
        }

    }
}
