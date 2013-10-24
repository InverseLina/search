package com.jobscience.search.web;

import java.sql.SQLException;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.MultiplierManager;

@Singleton
public class MultiplierWebHandler {

    @Inject
    private MultiplierManager multiplierManager;
    
    @WebPost("/multiplyData")
    public WebResponse multiplyData(@WebParam("orgName")String orgName,
            @WebParam("times")Integer times) throws SQLException{
        multiplierManager.multiplyData(times, orgName);
        return WebResponse.success();
    }
    
    @WebGet("/getMultiplyStatus")
    public WebResponse getStatus(){
        return WebResponse.success(multiplierManager.getStatus());
    }
    
}
