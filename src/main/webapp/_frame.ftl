<!doctype html>
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="${_r.contextPath}/bootstrap/css/bootstrap.css">
    <link rel="stylesheet" type="text/css" href="${_r.contextPath}/bootstrap/css/bootstrap-glyphicons.css">
    <title>JobScience Search (Demo)</title>
    [@webBundle path="${_r.contextPath}/css/" type="css" /]
    [@webBundle path="${_r.contextPath}/js/" type="js" /]
      <!--[if IE 8]>
      <link rel="stylesheet" type="text/css" href="${_r.contextPath}/hack/ie8_hack.css">
      <![endif]-->
    
    [#if signedRequestJson??]
    
    <link rel="stylesheet" type="text/css" href="/canvassdk/css/canvas.css" />
    <!-- Include all the canvas JS dependencies in one file -->
    <script type="text/javascript" src="/canvassdk/js/canvas-all.js"></script>
    <!-- Third part libraries, substitute with your own -->
    <script type="text/javascript" src="/canvassdk/js/json2.js"></script>
    <script>

      Sfdc.canvas(function() {
        var sr = JSON.parse('${signedRequestJson}');
        // Save the token
        Sfdc.canvas.oauth.token(sr.oauthToken);
        Sfdc.canvas.client.resize(sr.client,{height : "800px", width : "100%"});
      });
    </script>
    [/#if]
    
  </head>
  
  <body>



    <div class="mainContent">
    [@includeFrameContent /]
    </div>
  </body>
</html>