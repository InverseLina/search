package com.jobscience.search.web;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.auth.AuthRequest;
import com.britesnow.snow.web.auth.AuthToken;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.param.annotation.WebModel;
import com.britesnow.snow.web.param.annotation.WebUser;
import com.google.inject.Singleton;

import java.util.Map;

@Singleton
public class AppAuthRequest implements AuthRequest {

    @Override
    public AuthToken authRequest(RequestContext rc) {
        OAuthToken token = OAuthToken.fromCookie(rc);
        if (token != null) {
            AuthToken<OAuthToken> at = new AuthToken<OAuthToken>();
            at.setUser(token);
            return at;
        }else{
            return null;
        }
    }

    @WebModelHandler(startsWith = "/")
    public void home(@WebModel Map m, @WebUser OAuthToken user, RequestContext rc) {
        String orgName = rc.getParam("org");
        rc.setCookie("org", orgName);
        m.put("user", user);
    }

/*    @WebModelHandler(startsWith = "/logout")
    public void logout(@WebModel Map m, @WebUser User user, RequestContext rc) {
        if (user != null) {
            //remove cookie
//            for(Cookie c : rc.getReq().getCookies()){
//                String userToken = "userToken";
//                String userId = "userId";
//                if(userToken.equals(c.getName()) || userId.equals(c.getName())){
//                    c.setPath("/");
//                    c.setMaxAge(0);
//                    rc.getRes().addCookie(c);
//                }
//            }
        }
    }*/

/*    @WebActionHandler
    public Object login(@WebParam("userId") Long userId, @WebParam("username") String username,
                            @WebParam("password") String password, RequestContext rc) {
        User user = userDao.getUser(username);

        if (user == null) {
            user = new User();
            user.setUsername(username);
            user.setPassword(password);
            userDao.save(user);
            return user;
        } else if (authentication(user, password)) {
            setUserToSession(rc, user);
            return user;
        }
        return "null";
    }*/

    // --------- Private Helpers --------- //
    // store the user in the session. If user == null, then, remove it.
/*    private void setUserToSession(RequestContext rc, User user) {
        // TODO: need to implement session less login (to easy loadbalancing)
        if (user != null) {
            String userToken = Hashing.sha1().hashString(user.getUsername() + user.getId()).toString();
            rc.setCookie("userToken", userToken);
            rc.setCookie("userId", user.getId());
            //
        }
    }

    private boolean authentication(User user, String password) {
        if (user != null && user.getPassword() != null && user.getPassword().equals(password)) {
            return true;
        } else {
            return false;
        }
    }*/
    // --------- /Private Helpers --------- //
}