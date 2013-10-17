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
  <script type="text/javascript">
      [#--var tokenInfo = JSON.parse('${oauthToken!}')||[];--]
      var userInfo = JSON.parse('${loginInfo}');

      var infos = [];
      $.each(userInfo, function(key, value){
          infos.push({key: key, value: $.isPlainObject(value)?JSON.stringify(value):value});
      })
      brite.display("OAuthInfo", "body", {infos: infos});
  </script>
  </body>
</html>