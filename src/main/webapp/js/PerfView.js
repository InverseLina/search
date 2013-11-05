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
		 "click; .btn1,.btn2" : function(event) {
			 var view = this;
			 var keyword = $(event.currentTarget).closest("div").find("input[name='common']").val();
			 if(keyword == null || keyword == ''){
					alert("Please enter the word"); 
					return false;
				 }
			 var $result = $(event.currentTarget).closest("div");
			 $(event.currentTarget).attr("disabled","true").html("runing");
			 doPrefSearch.call(view,{search:keyword},$result);
		  },
		  "click; .btn3,.btn4" : function(event) {
				 var view = this;
				 var keyword = $(event.currentTarget).closest("div").find("input[name='common']").val();
				 if(keyword == null || keyword == ''){
					alert("Please enter the word"); 
					return false;
				 }
				 var employer = $(event.currentTarget).closest("div").find("input[name='common1']").val();
				 if(keyword == null || keyword == ''){
						alert("Please enter the word"); 
						return false;
					 }
				 var $result = $(event.currentTarget).closest("div");
				 $(event.currentTarget).attr("disabled","true").html("runing");
				 doPrefSearch.call(view,{search:keyword,employer:employer},$result);
		  },
		  "click; .searchAuto" : function(event) {
				 var view = this;
				 var keyword = $(event.currentTarget).closest("div").find("input[name='common']").val();
				 if(keyword == null || keyword == ''){
						alert("Please enter the word"); 
						return false;
					 }
				 var type = $(event.currentTarget).attr("data-type");
				 var $result = $(event.currentTarget).closest("div");
				 $(event.currentTarget).attr("disabled","true").html("runing");
				 getPerfGroupValuesForAdvanced.call(view,{keyword:keyword,type:type},$result);
		  },
		  "click; .all" : function(event) {
				 var view = this;
					 $("input[type=text]").each(function(){
						 if($(this).val() == ''){
							 alert("Please enter all the value!");
							 return false;
						 }
					 });	
				    view.$el.find(".btn").attr("disabled","true").html("runing");
				    doGoAll.call(view);
		  }
	 }
    // --------- /Events--------- //

	});
    // --------- Private Methods --------- //
	function doPrefSearch(opts,$result) {
		var dfd = $.Deferred();
        var view = this;
        opts = opts || {};
        var search = opts.search;
        var employer = opts.employer;
        
        var searchData = {};
        if(!/^\s*$/.test(search)){
            searchData.q_search = $.trim(search);
        }
        var data = [];
        if(employer != null && !/^\s*$/.test(employer)){
        	var opt = {};
        	
        	opt = {name:$.trim(employer)};
        	data.push(opt);
            searchData["q_companies"]= data;
        }
        var searchParameter = getSearchParameter(view,searchData);
        searchParameter.pageIndex = opts.pageIdx || 1;
        perfSearchDao.perfSearch(searchParameter).always(function (result) {
        	$result.find(".results").html("c:"+result.countDuration + "ms s:" + result.duration + "ms matches:" +result.count);
        	$result.find("button").removeAttr("disabled").html("Go");
        	dfd.resolve();
        });
        return dfd.promise();
    }
    
	function getPerfGroupValuesForAdvanced(opts,$result) {
		var dfd = $.Deferred();
		var view = this;
		 var type = opts.type;
		 var searchCond = {
	             "type":type,
	              queryString: opts.keyword,
	              orderByCount: true
	         };
	 perfSearchDao.getPerfGroupValuesForAdvanced(searchCond).done(function(data){
		 console.log(data);
		 $result.find(".results").html("s:" + data.duration+" ms ");
    	 $result.find("button").removeAttr("disabled").html("Go");
    	 dfd.resolve();
	    });
	 return dfd.promise();
	}
	
	
	function doGoAll() {
		var view = this;
		var $result = view.$el.find(".btn1").closest("div");
		 var keyword = $result.find("input[name='common']").val();
		 doPrefSearch.call(view,{search:keyword},$result).done(function(){
			 $result = view.$el.find(".btn2").closest("div");
			 keyword = $result.find("input[name='common']").val();
			 doPrefSearch.call(view,{search:keyword},$result).done(function(){
				 $result = view.$el.find(".btn3").closest("div");
				 keyword = $result.find("input[name='common']").val();
				 var employer = $result.find("input[name='common1']").val();
				 doPrefSearch.call(view,{search:keyword,employer:employer},$result).done(function(){
					 $result = view.$el.find(".btn4").closest("div");
					 keyword = $result.find("input[name='common']").val();
					 employer = $result.find("input[name='common1']").val();
					 doPrefSearch.call(view,{search:keyword,employer:employer},$result).done(function(){
						 $result = view.$el.find(".btn5");
						 keyword = $result.closest("div").find("input[name='common']").val();
						 var type = $result.attr("data-type");
						 getPerfGroupValuesForAdvanced.call(view,{keyword:keyword,type:type},$result.closest("div")).done(function(){
							 $result = view.$el.find(".btn6");
							 keyword = $result.closest("div").find("input[name='common']").val();
							 var type = $result.attr("data-type");
							 getPerfGroupValuesForAdvanced.call(view,{keyword:keyword,type:type},$result.closest("div")).done(function(){
								 $result = view.$el.find(".btn7");
								 keyword = $result.closest("div").find("input[name='common']").val();
								 var type = $result.attr("data-type");
								 getPerfGroupValuesForAdvanced.call(view,{keyword:keyword,type:type},$result.closest("div")).done(function(){
									 $result = view.$el.find(".btn8");
									 keyword = $result.closest("div").find("input[name='common']").val();
									 var type = $result.attr("data-type");
									 getPerfGroupValuesForAdvanced.call(view,{keyword:keyword,type:type},$result.closest("div")).done(function(){
										 $result = view.$el.find(".btn9");
										 keyword = $result.closest("div").find("input[name='common']").val();
										 var type = $result.attr("data-type");
										 getPerfGroupValuesForAdvanced.call(view,{keyword:keyword,type:type},$result.closest("div")).done(function(){
											 $result = view.$el.find(".btn10");
											 keyword = $result.closest("div").find("input[name='common']").val();
											 var type = $result.attr("data-type");
											 getPerfGroupValuesForAdvanced.call(view,{keyword:keyword,type:type},$result.closest("div")).done(function(){
												 view.$el.find(".all").removeAttr("disabled").html("Go  All");
											 });
										 });
									 });
								 });
							 });
						 });
					 });
				 });
			 });
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