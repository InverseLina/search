<!doctype html>
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="${_r.contextPath}/bootstrap/css/bootstrap.css">
    <title>JobScience Search (Demo)</title>
    <script type="text/javascript">
    var jssVersion = "${JSS_VERSION}";
	  var contextPath = "${_r.contextPath}";
    if(contextPath=="/"){contextPath="";};
	 </script> 
    
    [@webBundle path="/css/" type="css" /]
    [@webBundle path="/js/" type="js" /]
      <!--[if lt IE 9]>
      <link rel="stylesheet" type="text/css" href="${_r.contextPath}/hack/ie8_hack.css">
      <![endif]-->

    [#if signedRequestJson??]
    
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
    <script>
      [#if orgConfigs??]
     	var app = app||{};
        app.orgInfo = JSON.parse('${orgConfigs}');
      [/#if]
    </script>
    <script type="text/javascript">
		var app = app || {};
		app.uiFlags = {};
		[#if (_r.rc.paramMap.header)?? && _r.rc.paramMap.header == "false"]
			app.uiFlags.showHeader = false;
		[#else]
			app.uiFlags.showHeader = true;
		[/#if]
	</script>
  </head>
  
  <body>



    <div class="mainContent">
    [@includeFrameContent /]
    [#if errorCode??]
        <script type="text/javascript">
            app.startError = {errorCode: "${errorCode}", errorMessage: "${errorMessage}"}
        </script>
    [/#if]
    </div>
  </body>
</html>