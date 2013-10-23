package com.jobscience.search.web;

import java.util.List;
import java.util.Map;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.param.annotation.WebUser;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.LabelDao;
import com.jobscience.search.dao.UserDao;
 
@Singleton
public class LabelWebHandler {

    @Inject
    private LabelDao labelDao;
    @Inject
    private UserDao userDao;
    
    @WebPost("/addLabel")
    public WebResponse addLabel(@WebUser OAuthToken token,@WebParam("name")String name, RequestContext rc){
        String ctoken = rc.getCookie("ctoken");
        if (ctoken != null) {
            Map user = userDao.getUserByToken(ctoken);
            if (user != null) {
                List list = labelDao.getLabelByName(name, user.get("id"));
                if (list != null && list.size() > 0) {
                    return WebResponse.success(String.format("label of name %s has exits", name));
                }
                Long id = labelDao.addLabel(Long.parseLong(user.get("id").toString()), name);
                return WebResponse.success(id);
            }
        }
        return WebResponse.fail();
    }
    
    @WebPost("/deleteLabel")
    public WebResponse deleteLabel(@WebParam("id")Long id){
        labelDao.deleteLabel(id);
        return WebResponse.success();
    }
    
    @WebPost("/updateLabel")
    public WebResponse updateLabel(@WebParam("id")Long labelId,@WebParam("name")String name){
        labelDao.updateLabel(labelId, name);
        return WebResponse.success();
    }
    
    @WebGet("/getLabels")
    public WebResponse getLabels(@WebUser OAuthToken token, RequestContext rc) {
        String ctoken = rc.getCookie("ctoken");
        if (ctoken != null) {
            Map user = userDao.getUserByToken(ctoken);
            if (user != null) {
                return WebResponse.success(labelDao.getLabelForUser(Long.parseLong(user.get("id").toString())));
            }
        }
        return WebResponse.fail();
    }

    @WebGet("/getLabelByName")
    public WebResponse getLabel(@WebUser OAuthToken token, @WebParam("name") String name, RequestContext rc) {
        String ctoken = rc.getCookie("ctoken");
        if (ctoken != null) {
            Map user = userDao.getUserByToken(ctoken);
            if (user != null) {
                return WebResponse.success(labelDao.getLabelByName(name, user.get("id")));
            }
        }
        return WebResponse.fail();
    }
    
    @WebGet("/getLabel")
    public  WebResponse  getLabel(@WebParam("id")Long labelId){
        return WebResponse.success(labelDao.getLabelForById(labelId));
    }
    
    @WebPost("/assignLabelToContact")
    public WebResponse assignLabelToContact(@WebParam("contactId")Long contactId,@WebParam("labelId")Long labelId){
       labelDao.assignLabelToContact(contactId, labelId);
       return WebResponse.success();
    }
    
    @WebPost("/unAssignLabelFromContact")
    public WebResponse unAssignLabelFromContact(@WebParam("contactId")Long contactId,@WebParam("labelId")Long labelId){
        labelDao.unAssignLabelFromContact(contactId, labelId);
        return WebResponse.success();
    }
    
    @WebGet("/getLabelStatus")
    public WebResponse getLabelStatus(@WebParam("contactIds")String contactIds,@WebParam("labelId")Long labelId){
        if(contactIds !=null &&contactIds.startsWith("[")&&contactIds.endsWith("]")){
            if(!contactIds.equals("[]")){
                contactIds = contactIds.substring(1,contactIds.length()-1);
                return WebResponse.success(labelDao.getLabelStatus(contactIds, labelId));
            }

        }
        return WebResponse.success(labelDao.getLabelStatus(contactIds, labelId));

    }
}
