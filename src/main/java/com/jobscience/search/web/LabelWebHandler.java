package com.jobscience.search.web;

import java.util.Map;

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
    public WebResponse addLabel(@WebUser OAuthToken token,@WebParam("name")String name){
        Map user = userDao.getUserByToken(token.getToken());
        if(user!=null){
            labelDao.addLabel(Long.parseLong(user.get("id").toString()), name);
            return WebResponse.success();
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
    public WebResponse getLabels(@WebUser OAuthToken token){
        Map user = userDao.getUserByToken(token.getToken());
        if(user!=null){
            return WebResponse.success(labelDao.getLabelForUser(Long.parseLong(user.get("id").toString())));
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
}
