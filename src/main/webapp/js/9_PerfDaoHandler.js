var app = app || {};
(function($){
	
	app.PerfDaoHandler = {};

	   app.PerfDaoHandler.search = function(data){
          return app.getJsonData(contextPath+"/perf/search", data);
	   }
	
	   app.PerfDaoHandler.autocomplete = function(data){
           return app.getJsonData(contextPath+"/perf/autocomplete", data);
		}

    app.PerfDaoHandler.checkStatus = function(){
        return app.getJsonData(contextPath+"/perf/checkStatus");
    }
	   
})(jQuery);