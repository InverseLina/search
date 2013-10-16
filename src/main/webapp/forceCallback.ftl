<!DOCTYPE html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>JSS Callback</title>
    
    <link rel="stylesheet" type="text/css" href="${_r.contextPath}/bootstrap/css/bootstrap.css" />
    [@webBundle path="/js/" type="js" /]
    
    [#-- Global Initialization --] 
    <script type="text/javascript">
      // set the contextPath as a javascript global variable
      var contextPath = "${_r.contextPath}";
      // set the default to load the template
      brite.defaultComponentConfig.loadTmpl = true;
    </script>
    [#-- /Global Initialization --] 
  </head>

  <body>
  [#if oauthToken??]
       <div>
           show login info.
       </div>
  <script type="text/javascript">
      var tokenInfo = JSON.parse('${oauthToken}');
      var userInfo = JSON.parse('${loginInfo}');

      var infos = [];
      $.each(tokenInfo, function(key, value){
          infos.push({key: key, value: value});
      })
      $.each(userInfo, function(key, value){
          infos.push({key: key, value: value});
      })
      console.log(infos);
      brite.display("OAuthInfo", "body", {infos: infos});
  </script>
   [#else]
   <div >
       login success
   </div>

   <script type="text/javascript">
       window.location.href = contextPath + "/";
   </script>
   [/#if]
  </body>
</html>