(function($){
	var perfSearchDao = app.PerfDaoHandler;
	
	brite.registerView("PerfView",{parent:"body"},{
    // --------- View Interface Implement--------- //
	create: function(data){
			return render("PerfView");
	 }, 
	 postDisplay: function(data){
		 var view = this;
	 },
    // --------- /View Interface Implement--------- //

    // --------- Events--------- //
	 events: {
		 "click;button.go": function(event){
			 var view = this;
	         var $button = $(event.target);
	         $button.attr("disabled","true").addClass("running").html("runing");
	         view.async = true;
	         doSearch.call(view,$button);
	    },
		  "click; .all" : function(event) {
				 var view = this;
					 $("input[type=text]").each(function(){
						 if($(this).val() == ''){
							 alert("Please enter all the value!");
							 return false;
						 }
					 });
					 view.$el.find(".perf-value").empty();
					 var $button = view.$el.find(".go");
					 var $allButton = view.$el.find(".all");
					 $button.attr("disabled","true").addClass("running").html("Runing");
					 $allButton.attr("disabled","true").addClass("running").html("Runing");
					 view.async = false;
					 $button.each(function(){
						 doSearch.call(view,$(this));
					 });
					 $button.removeAttr("disabled").removeClass("running").html("Go");
					 $allButton.removeAttr("disabled").removeClass("running").html("Go All");
		  }
	 }
    // --------- /Events--------- //

	});
    // --------- Private Methods --------- //
	
	function doSearch($button){
		var view = this;
		var $perfItem = $button.closest(".perf-item");
	         var data = {};
	         $perfItem.find("input").each(function(){
	            var $input = $(this);
	            
	            if($input.attr("name") != "q_search"){
	            	var datas = [];
	            	var opt = {};
		        	opt = {name:$.trim($input.val())};
		        	datas.push(opt);
		        	data[$input.attr("name")] = datas; 
	            }else{
	            	data["q_search"] = $input.val(); 
	            }
	           
	         });
	         var searchParameter = getSearchParameter(view,data);
	         var methodName = $perfItem.attr("data-perf-method");
	         if(methodName == 'autocomplete'){
	        	 searchParameter.type = $button.attr("data-type");
	        	 searchParameter.queryString = data["q_search"];
	        	 searchParameter.orderByCount = true;
	         }
	         searchParameter.async = view.async;
	         var perfPromise = perfSearchDao[methodName](searchParameter);
	         
	         perfPromise.done(function(response){
	        	 if(methodName == 'autocomplete'){
	        		 if($button.hasClass("left")){
	        			 $perfItem.find(".left-items").find("[data-perf = 's']").html(response.duration + "ms"); 
	        		 }else{
	        			 $perfItem.find(".right-items").find("[data-perf = 's']").html(response.duration + "ms"); 
	        		 }
	        	 }else{
	        		 $button.closest("div").next("div").find("[data-perf = 'c']").html(response.countDuration + "ms");
		        	 $button.closest("div").next("div").find("[data-perf = 's']").html(response.duration + "ms");
		        	 $button.closest("div").next("div").find("[data-perf = 'match']").html(response.count);
	        	 }
	        	 if(view.async){
	        		 $button.removeAttr("disabled").removeClass("running").html("Go"); 
	        	 }
	         });
	}
	
	function getSearchParameter(view,searchData){
        var result = {};
        result.searchColumns = "contact,company,skill,education,location";
        result.searchValues = JSON.stringify(searchData);
        result.pageIndex = 1;
        result.pageSize =15;
        return result;
	}
	
	// --------- /Private Methods --------- //  
})(jQuery);