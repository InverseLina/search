var app = app || {};
(function($){
	
	app.PerfDaoHandler = {};

	   app.PerfDaoHandler.search = function(data){
	      var dfd = $.Deferred();
	      $.ajax({
				url:"/search",
				type:"Get",
				dataType:'json',
				data : data,
				async : data.async
	  	  }).done(function(data){
	  		if(data.success === true){
                dfd.resolve(data.result);
            }else{
            	dfd.fail(response);
                $(document).trigger("ERROR_PROCESS", data);
            }
	  	  });
	      return dfd.promise();
	   }
	
	   app.PerfDaoHandler.autocomplete = function(data){
		      var dfd = $.Deferred(); 
		      $.ajax({
					url:"/autocomplete",
					type:"Get",
					dataType:'json',
					data : data,
					async : data.async
		  	  }).done(function(data){
		  		if(data.success === true){
	                dfd.resolve(data.result);
	            }else{
	            	dfd.fail(response);
	                $(document).trigger("ERROR_PROCESS", data);
	            }
		  	  });
		      return dfd.promise();
		   }
	   
})(jQuery);