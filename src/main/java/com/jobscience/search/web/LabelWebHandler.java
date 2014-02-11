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
import com.jobscience.search.CurrentOrgHolder;
import com.jobscience.search.dao.LabelDao;
import com.jobscience.search.dao.UserDao;
 
@Singleton
public class LabelWebHandler {

    @Inject
    private LabelDao labelDao;
    @Inject
    private UserDao userDao;
    @Inject
    private CurrentOrgHolder orgHolder;
    
    @WebPost("/addLabel")
    public WebResponse addLabel(@WebUser Map token,@WebParam("name")String name, RequestContext rc){
        String ctoken = rc.getCookie("ctoken");
        if (ctoken != null) {
            Map user = userDao.getUserByToken(ctoken);
            if (user != null) {
                List list = labelDao.getLabelByName(name, user.get("id"),orgHolder.getCurrentOrg());
                if (list != null && list.size() > 0) {
                    return WebResponse.success(String.format("label of name %s has exits", name));
                }
                Object id = labelDao.addLabel((String)user.get("sfid"), name,orgHolder.getCurrentOrg());
                return WebResponse.success(id);
            }
        }
        return WebResponse.fail();
    }
    
    @WebPost("/deleteLabel")
    public WebResponse deleteLabel(@WebParam("id")Long id){
        labelDao.deleteLabel(id,orgHolder.getCurrentOrg());
        return WebResponse.success();
    }
    
    @WebPost("/updateLabel")
    public WebResponse updateLabel(@WebParam("id")Long labelId,@WebParam("name")String name){
        labelDao.updateLabel(labelId, name,orgHolder.getCurrentOrg());
        return WebResponse.success();
    }
    
    @WebGet("/getLabels")
    public WebResponse getLabels(@WebUser  Map token, RequestContext rc) {
        String ctoken = rc.getCookie("ctoken");
        if (ctoken != null) {
            Map user = userDao.getUserByToken(ctoken);
            if (user == null) {
                userDao.insertUser(null, ctoken);
                user = userDao.getUserByToken(ctoken);
            }
            return WebResponse.success(labelDao.getLabelForUser(Long.parseLong(user.get("id").toString()),orgHolder.getCurrentOrg()));
        }
        return WebResponse.fail();
    }

    @WebGet("/getLabelByName")
    public WebResponse getLabel(@WebUser Map token, @WebParam("name") String name, RequestContext rc) {
        String ctoken = rc.getCookie("ctoken");
        if (ctoken != null) {
            Map user = userDao.getUserByToken(ctoken);
            if (user != null) {
                return WebResponse.success(labelDao.getLabelByName(name, user.get("id"),orgHolder.getCurrentOrg()));
            }
        }
        return WebResponse.fail();
    }
    
    @WebGet("/getLabel")
    public  WebResponse  getLabel(@WebParam("id")Long labelId){
        return WebResponse.success(labelDao.getLabelForById(labelId,orgHolder.getCurrentOrg()));
    }
    
    @WebPost("/assignLabelToContact")
    public WebResponse assignLabelToContact(@WebParam("contactId")Long contactId,@WebParam("labelId")Long labelId){
       labelDao.assignLabelToContact(contactId, labelId,orgHolder.getCurrentOrg());
       return WebResponse.success();
    }
    
    @WebPost("/unAssignLabelFromContact")
    public WebResponse unAssignLabelFromContact(@WebParam("contactId")Long contactId,@WebParam("labelId")Long labelId){
        labelDao.unAssignLabelFromContact(contactId, labelId,orgHolder.getCurrentOrg());
        return WebResponse.success();
    }
    
    @WebGet("/getLabelStatus")
    public WebResponse getLabelStatus(@WebParam("contactIds")String contactIds,@WebParam("labelId")Long labelId){
        if(contactIds !=null &&contactIds.startsWith("[")&&contactIds.endsWith("]")){
            if(!contactIds.equals("[]")){
                contactIds = contactIds.substring(1,contactIds.length()-1);
                return WebResponse.success(labelDao.getLabelStatus(contactIds, labelId,orgHolder.getCurrentOrg()));
            }

        }
        return WebResponse.success(labelDao.getLabelStatus(contactIds, labelId,orgHolder.getCurrentOrg()));

    }
}
