var app = app || {};
(function($){
	
	app.PerfDaoHandler = {};

	   app.PerfDaoHandler.search = function(data){
	      var dfd = $.Deferred();
	      $.ajax({
				url:contextPath+"/perf/search",
				type:"Get",
				dataType:'json',
				data : data
	  	  }).done(function(data){
	  		if(data.success === true){
                dfd.resolve(data.result);
            }else{
            	dfd.fail(data);
            }
	  	  });
	      return dfd.promise();
	   }
	
	   app.PerfDaoHandler.autocomplete = function(data){
		      var dfd = $.Deferred(); 
		      $.ajax({
					url:contextPath+"/perf/autocomplete",
					type:"Get",
					dataType:'json',
					data : data
		  	  }).done(function(data){
		  		if(data.success === true){
	                dfd.resolve(data.result);
	            }else{
	            	dfd.fail(data);
	            }
		  	  });
		      return dfd.promise();
		   }
	   
})(jQuery);