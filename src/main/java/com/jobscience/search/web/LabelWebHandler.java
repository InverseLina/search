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

    @Inject
    private WebResponseBuilder webResponseBuilder;
    
    @WebPost("/addLabel")
    public WebResponse addLabel(@WebUser Map token,@WebParam("name")String name, RequestContext rc){
        String ctoken = rc.getCookie("ctoken");
        if (ctoken != null) {
            Map user = userDao.getUserByToken(ctoken);
            if (user != null) {
                List list = labelDao.getLabelByName(name, user.get("id"),orgHolder.getCurrentOrg());
                if (list != null && list.size() > 0) {
                    return webResponseBuilder.success(String.format("label of name %s has exits", name));
                }
                Object id = labelDao.addLabel((String)user.get("sfid"), name,orgHolder.getCurrentOrg());
                return webResponseBuilder.success(id);
            }
        }
        return webResponseBuilder.fail();
    }
    
    @WebPost("/deleteLabel")
    public WebResponse deleteLabel(@WebParam("id")Long id){
        labelDao.deleteLabel(id,orgHolder.getCurrentOrg());
        return webResponseBuilder.success();
    }
    
    @WebPost("/updateLabel")
    public WebResponse updateLabel(@WebParam("id")Long labelId,@WebParam("name")String name){
        labelDao.updateLabel(labelId, name,orgHolder.getCurrentOrg());
        return webResponseBuilder.success();
    }
    
    @WebGet("/getLabels")
    public WebResponse getLabels(@WebUser  Map token, RequestContext rc) {
        String ctoken = rc.getCookie("ctoken");
        if (ctoken != null) {
            Map user = userDao.getUserByToken(ctoken);
            if (user == null) {
                userDao.insertUser(null, ctoken, 0l, null);
                user = userDao.getUserByToken(ctoken);
            }
            return webResponseBuilder.success(labelDao.getLabelForUser(Long.parseLong(user.get("id").toString()),
                    orgHolder.getCurrentOrg()));
        }
        return webResponseBuilder.fail();
    }

    @WebGet("/getLabelByName")
    public WebResponse getLabel(@WebUser Map token, @WebParam("name") String name, RequestContext rc) {
        String ctoken = rc.getCookie("ctoken");
        if (ctoken != null) {
            Map user = userDao.getUserByToken(ctoken);
            if (user != null) {
                return webResponseBuilder.success(labelDao.getLabelByName(name,
                        user.get("id"),orgHolder.getCurrentOrg()));
            }
        }
        return webResponseBuilder.fail();
    }
    
    @WebGet("/getLabel")
    public  WebResponse  getLabel(@WebParam("id")Long labelId){
        return webResponseBuilder.success(labelDao.getLabelForById(labelId,orgHolder.getCurrentOrg()));
    }
    
    @WebPost("/assignLabelToContact")
    public WebResponse assignLabelToContact(@WebParam("contactId")Long contactId,@WebParam("labelId")Long labelId){
       labelDao.assignLabelToContact(contactId, labelId,orgHolder.getCurrentOrg());
       return webResponseBuilder.success();
    }
    
    @WebPost("/unAssignLabelFromContact")
    public WebResponse unAssignLabelFromContact(@WebParam("contactId")Long contactId,@WebParam("labelId")Long labelId){
        labelDao.unAssignLabelFromContact(contactId, labelId,orgHolder.getCurrentOrg());
        return webResponseBuilder.success();
    }
    
    @WebGet("/getLabelStatus")
    public WebResponse getLabelStatus(@WebParam("contactIds")String contactIds,@WebParam("labelId")Long labelId){
        if(contactIds !=null &&contactIds.startsWith("[")&&contactIds.endsWith("]")){
            if(!contactIds.equals("[]")){
                contactIds = contactIds.substring(1,contactIds.length()-1);
                return webResponseBuilder.success(labelDao.getLabelStatus(contactIds, labelId,orgHolder.getCurrentOrg()));
            }

        }
        return webResponseBuilder.success(labelDao.getLabelStatus(contactIds, labelId,orgHolder.getCurrentOrg()));

    }
}
