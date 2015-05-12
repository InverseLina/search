package com.jobscience.search.web;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;

import com.jobscience.search.exception.InjectException;
import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.britesnow.snow.util.ObjectUtil;
import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.WebRequestType;
import com.britesnow.snow.web.auth.AuthRequest;
import com.britesnow.snow.web.auth.AuthToken;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.param.annotation.WebModel;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.param.annotation.WebUser;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.hash.Hashing;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.AppConfig;
import com.jobscience.search.auth.AuthCode;
import com.jobscience.search.auth.AuthException;
import com.jobscience.search.canvas.SignedRequest;
import com.jobscience.search.dao.ConfigManager;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.dao.UserDao;
import com.jobscience.search.organization.OrgContextManager;

@Singleton
public class AppAuthRequest implements AuthRequest {
	@Inject
	private UserDao             userDao;
	@Inject
	OrgContextManager            orgHolder;
	@Inject
	private ConfigManager       configManager;
	@Inject
	private DBSetupManager      dbSetupManager;

	@Inject(optional = true)
	@Named("jss.passcode")
	private String passCode;

	@Inject(optional = true)
	@Named("jss.sysadmin.pwd")
	private String configPassword;

	@Inject(optional = true)
	@Named("force.ssl")
	private boolean sslFlag;

	@Inject
	private WebResponseBuilder webResponseBuilder;

	private static final Logger log = LoggerFactory.getLogger(AppAuthRequest.class);

	static private final String COOKIE_ORG_USER_TOKEN = "ctoken";
	static private final String COOKIE_ORG = "org";
	static private final String COOKIE_ADMIN_TOKEN = "atoken";
	static private final String COOKIE_PASSCODE = "pcode";

	static private final String SESSION_EXPIRE_DURATION = "sessionExpireDuration";

	private final Cache<String, Map> userCache;

	public AppAuthRequest() {
		userCache = CacheBuilder.newBuilder().expireAfterAccess(8, TimeUnit.MINUTES)
				.maximumSize(100).build(new CacheLoader<String, Map>() {
					@Override
					public Map load(String ctoken) throws Exception {
						return userDao.getUserByToken(ctoken);
					}
				});
	}

	@Override
	public AuthToken authRequest(RequestContext rc) {
		String servletPath = rc.getReq().getServletPath();
		if("/getAutoCompleteData".equals(servletPath) || "/searchuiconfig".equals(servletPath)){
			Enumeration rnames=rc.getReq().getParameterNames();
			for (Enumeration e = rnames ; e.hasMoreElements() ;) {
				String thisName=e.nextElement().toString();
				String thisValue=rc.getReq().getParameter(thisName);
				if(thisName != null && checkXPath(thisName)){
					throw new InjectException("The input should not contains any metacharacters which may attack the project");
				}
				if(!"searchValues".equals(thisName) && !"value".equals(thisName) && !"configsJson".equals(thisName)){
					if(thisValue != null && checkXPath(thisValue)){
						throw new InjectException("The input should not contains any metacharacters which may attack the project");
					}
				}
			}
		}
		WebRequestType wrt = rc.getWebRequestType();
		AuthToken authToken = null;
		switch(wrt){
			// All the dynamic resources, we need to auth
			case WEB_RESOURCE:
			case WEB_REST:
			case WEB_TEMPLATE:
				String orgName = rc.getParam("org");
				if (orgName != null) {
					setCookie(rc, COOKIE_ORG, orgName, 524160f);
				}
				return authWebRequest(rc);
			// static files and generated files (.less, webbundle) we do not need to auth.
			case GENERATED_ASSET:
			case STATIC_FILE:break;
		}
		return authToken;
	}

	@WebPost("/passcode")
	public WebResponse passcode(@WebParam("passcode") String code ,RequestContext rc) {
		if (passCode != null && passCode.length() > 0 && passCode.equals(code)) {
			String codeSha1 = sha1(code);
			rc.setCookie(COOKIE_PASSCODE, codeSha1, true);
			return webResponseBuilder.success(true);
		}else{
			rc.removeCookie(COOKIE_PASSCODE);
		}
		return webResponseBuilder.fail(new AuthException(AuthCode.NO_PASSCODE));
	}

	@WebPost("/admin-login")
	public WebResponse adminLogin(RequestContext rc,
							@WebParam("password") String password) throws SQLException {
		if (configPassword.equals(password)) {
			String passwordSha1 = sha1(password);

			setCookie(rc, COOKIE_ADMIN_TOKEN, passwordSha1, 30f);

			return webResponseBuilder.success();
		} else {
			rc.removeCookie(COOKIE_ADMIN_TOKEN);
		}
		return webResponseBuilder.fail();
	}

	@WebModelHandler(startsWith = "/")
	public void home(@WebModel Map m, @WebUser Map user, RequestContext rc) {
		m.put("JSS_VERSION", AppConfig.JSS_VERSION);
		if (!rc.getPathInfo().startsWith("/admin")){
			String orgName = null;
			boolean isSysSchemaExist = dbSetupManager.checkSysTables().contains("config");
			m.put("sys_schema", isSysSchemaExist);
			try{
				orgName = orgHolder.getOrgName();
			}catch(Exception e){
				log.warn("NO_ORG");
			}

			if(orgName == null){
				rc.getWebModel().put("errorCode", "NO_ORG");
				rc.getWebModel().put("errorMessage", "No organization selected, please, authenticate via SalesForce.com");
				rc.getWebModel().put("success", "false");
			}else{
				if (orgName != null) {
					m.put("user", user);
				}
				// check org is set or not
				try {
					Map configMap = configManager.getOrgInfo(orgHolder.getId());
					configMap.put("instanceUrl", rc.getCookie("instanceUrl"));
					m.put("orgConfigs", JSONObject.fromObject(configMap).toString());
				} catch (Exception e) {
					rc.removeCookie(COOKIE_ORG_USER_TOKEN);
				}
			}
		}else{
			//FIXME: for now do check here, cause the expection catcher just for rest methods.
			boolean isAdmin = false;
			if(user != null && user.containsKey("isAdmin")){
				isAdmin = (Boolean) user.get("isAdmin");
			}

			if(!isAdmin){
				rc.getWebModel().put("errorCode", AuthCode.NO_ADMIN_ACCESS.toString());
				rc.getWebModel().put("errorMessage", "You have no privaliges to access for admin resources");
				rc.getWebModel().put("success", "false");
			}
		}
	}

	public void updateCache(Map user){
		userCache.put((String)user.get(COOKIE_ORG_USER_TOKEN), user);
	}

	private AuthToken authWebRequest(RequestContext rc){
		rc.getRes().setHeader("P3P", "CP=\"IDC DSP COR CURa ADMa OUR IND PHY ONL COM STA\"");
		AuthToken authToken = null;
		String path = rc.getPathInfo();
		String contextPath = rc.getContextPath();
		if(path.equals("/admin/")){
			String atoken = rc.getCookie(COOKIE_ADMIN_TOKEN);
			if(!Strings.isNullOrEmpty(atoken) && atoken.equals(sha1(configPassword))){
				Map adminUser = new HashMap();
				adminUser.put("isAdmin", true);
				authToken = new AuthToken();
				authToken.setUser(adminUser);
			}else{
				rc.removeCookie(COOKIE_ADMIN_TOKEN);
			}
		}else if(path.equals("/sf-canvas")){
			String signedRequest = rc.getParam("signed_request");

			if (signedRequest != null) {
				Integer orgId = null;
				try{
					orgId = orgHolder.getId();
				}catch(Exception e){
					e.printStackTrace();
					rc.getWebModel().put("errorCode", "ERROR_ORG");
					rc.getWebModel().put("errorMessage", "Organization is not correct, Please enter correct organization");
					rc.getWebModel().put("success", "false");
				}
				if (orgId != null) {
					String canvasappSecretStr = configManager.getConfig("canvasapp_secret", orgId);
					try {
						String signedRequestJson = SignedRequest.verifyAndDecodeAsJson(signedRequest, canvasappSecretStr);
						rc.getWebModel().put("signedRequestJson", signedRequestJson);
						Map userMap = userDao.checkAndUpdateUser(2, signedRequestJson, null, 0, null);

						float expiration = ObjectUtil.getValue(configManager.getConfig(SESSION_EXPIRE_DURATION, orgHolder.getId()),Float.class,120f);
						setCookie(rc, COOKIE_ORG_USER_TOKEN, (String)userMap.get("ctoken"), expiration);

						updateCache(userMap);
						String sfid = (String)orgHolder.getCurrentOrg().getOrgMap().get("sfid");
						if(!Strings.isNullOrEmpty(sfid)&&!userDao.getSFIDbySF2(signedRequestJson).startsWith(sfid)){
							rc.getWebModel().put("errorCode", "CANVAS_AUTH_ERROR");
							rc.getWebModel().put("errorMessage", "Cannot Access Org SFID from SFCanvas does not match JSS Org SFID");
							rc.getWebModel().put("success", "false");
						}else{
							Map user = getUserFromCToken(rc);
							authToken = new AuthToken();
							authToken.setUser(user);
						}
					} catch (Exception e) {
						rc.getWebModel().put("errorCode", "CANVAS_AUTH_ERROR");
						rc.getWebModel().put("errorMessage", "The app secret might be incorrect, Make sure you have correct secret");
						rc.getWebModel().put("success", "false");
					}
				}
			}else{
				rc.getWebModel().put("errorCode", "CANVAS_AUTH_ERROR");
				rc.getWebModel().put("errorMessage", "The signedRequest can't be empty.");
				rc.getWebModel().put("success", "false");
			}
		}else if(path.equals(contextPath + "/") || path.equals(rc.getContextPath())){
			if (passCode != null && passCode.length() > 0 ) {
				String pcode = rc.getCookie(COOKIE_PASSCODE);
				if (pcode == null || !pcode.equals(sha1(passCode))) {
					rc.getWebModel().put("errorCode", AuthCode.NO_PASSCODE.toString());
					rc.getWebModel().put("errorMessage", "No passcode exists or incorrect");
					rc.getWebModel().put("success", "false");
					return null;
				}

				Map user = getOrCreateUserFromCToken(rc);

				authToken = new AuthToken();
				authToken.setUser(user);
			}


		}else{
			try {
				Map user = getUserFromCToken(rc);

				if(user == null){
					rc.removeCookie(COOKIE_ORG_USER_TOKEN);
				}

				String atoken = rc.getCookie(COOKIE_ADMIN_TOKEN);
				if(atoken != null){
					if(user == null){
						user = new HashMap();
					}
					user.put("isAdmin", true);

					setCookie(rc, COOKIE_ADMIN_TOKEN, atoken, 30f);
				}else{
					rc.removeCookie(COOKIE_ADMIN_TOKEN);
				}

				if(user != null){
					authToken = new AuthToken();
					authToken.setUser(user);
				}

			} catch (Exception e) {
				rc.removeCookie(COOKIE_ORG_USER_TOKEN);
				log.warn("Does not have user token");
			}
		}
		return authToken;
	}

	private Map getOrCreateUserFromCToken(RequestContext rc){
		Map user = getUserFromCToken(rc);

		try{
			if(user == null){
				String ctoken = userDao.buildCToken(null);
				userDao.insertUser(null, ctoken, 0l, null);

				float expiration = ObjectUtil.getValue(configManager.getConfig(SESSION_EXPIRE_DURATION, orgHolder.getId()),Float.class,120f);
				setCookie(rc, COOKIE_ORG_USER_TOKEN, ctoken, expiration);

				user = userDao.getUserByToken(ctoken);
				updateCache(user);
			}
		}catch(Exception e){
			rc.getWebModel().put("errorCode", "NO_ORG");
			rc.getWebModel().put("errorMessage", "Organization is not correct, Please enter correct organization");
			rc.getWebModel().put("success", "false");
		}
		return user;
	}

	private Map getUserFromCToken(RequestContext rc){
		Map user = null;
		String ctoken = rc.getCookie(COOKIE_ORG_USER_TOKEN);

		if (ctoken != null) {
			user = userCache.getIfPresent(ctoken);

			if (user == null) {
				String orgName = null;
				try {
					orgName = orgHolder.getOrgName();
					if (orgName != null) {
						dbSetupManager.checkOrgExtra(orgHolder.getOrgName()).contains("jss_user");
					}
					user = userDao.getUserByToken(ctoken);
				} catch (Exception e) {
					rc.getWebModel().put("errorCode", "NO_ORG");
					rc.getWebModel().put("errorMessage", "Organization is not correct, Please enter correct organization");
					rc.getWebModel().put("success", "false");
				}
			}

		}

		if(user != null){
			float expiration = ObjectUtil.getValue(configManager.getConfig(SESSION_EXPIRE_DURATION, orgHolder.getId()),Float.class,120f);
			setCookie(rc, COOKIE_ORG_USER_TOKEN, ctoken, expiration);
		}

		return user;
	}

	private void setCookie(RequestContext rc, String name, String value, float expire){
		if(sslFlag){
			if(expire > 0) {
				rc.getRes().addHeader("Set-Cookie",name+"="+value+";Max-Age="+(int) (expire * 60)+"path=/;Secure;HttpOnly");
			}else{
				rc.getRes().addHeader("Set-Cookie",name+"="+value+";Max-Age="+120 * 60+"path=/;Secure;HttpOnly");
			}
		}else{
			Cookie cookie = new Cookie(name,value);
			cookie.setPath("/");
			if(expire > 0) {
				cookie.setMaxAge((int) (expire * 60));
			}else{
				cookie.setMaxAge(120 * 60);
			}
			rc.getRes().addCookie(cookie);
		}

	}

	static String sha1(String txt){
		return Hashing.sha1().hashString(txt, Charsets.UTF_8).toString();
	}


	private boolean checkXPath(String queryString){
		boolean flag = false;
		String[] strings = {"\'","@","and","(",")","/","\"","=","[","]"};
		for(String s:strings){
			if(queryString.contains(s)){
				flag = true;
			}
		}
		return  flag;
	}

}