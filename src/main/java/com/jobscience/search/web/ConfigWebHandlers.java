package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.param.annotation.WebModel;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.param.annotation.WebPath;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.dao.ConfigManager;

@Singleton
public class ConfigWebHandlers {

	@Inject
	private ConfigManager configManager;
	
	
	@WebPost("/config/save")
	public WebResponse saveConfig(@WebParam("local_distance")String distance,@WebParam("local_date")String date,
			@WebParam("action_add_to_sourcing")String addToSourcing,@WebParam("action_favorite")String favorite) throws SQLException{
		Map<String,String> params = new HashMap<String,String>();
		params.put("local_distance", distance);
		params.put("local_date", date);
		params.put("action_add_to_sourcing", addToSourcing);
		params.put("action_favorite", favorite);
		
		configManager.saveOrUpdateConfig(params);
		return WebResponse.success();
	}
	
	@WebGet("/config/get/{name}")
	public WebResponse getConfig(@WebPath(2)String name) throws SQLException{
		return WebResponse.success(configManager.getConfig(name));
	}
	
	@WebModelHandler(startsWith="/admin")
	public void admin(@WebModel Map m,RequestContext rc){
		m.put("login", rc.getCookie("login")==null?false:Boolean.parseBoolean(rc.getCookie("login")));
	}
	
	@WebPost("/admin/validate")
	public WebResponse doValidate(@Named("jss.sysadmin.pwd")String configPassword, RequestContext rc,@WebParam("password") String password) throws SQLException{
		
		if(configPassword.equals(password)){
			rc.setCookie("login", true);
			return WebResponse.success();
		}else{
			rc.setCookie("login", false);
			return WebResponse.fail();
		}
	}
	
}
