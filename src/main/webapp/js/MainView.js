(function($){
	
	brite.registerView("MainView",{parent:"body"},{
		create: function(){
			console.log("...");
			return render("MainView");
	 }, 
	 
	 postDisplay: function(){
     var view = this;
		 brite.display("SideNav",this.$el.find(".sidenav-ctn")).done(function(sideNav){
				view.sideNav = sideNav;
		 });
		 brite.display("ContentView",this.$el.find(".contentview-ctn")).done(function(contentView){
		 	 view.contentView = contentView;
		 });
	 },
	 
	 events: {
	 	"click; [data-action='DO_SEARCH']": function(){
	 		var view = this;
	 		view.$el.trigger("DO_SEARCH");
	 	},
	 	
	 	"DO_SEARCH": function(){
	 		doSearch.call(this);	
	 	}
	 }
	});


    function doSearch() {
        var view = this;
        var isValid = false;
        var contentSearchValues = view.contentView.getSearchValues();
        var navContectSearchValues = view.sideNav.getSearchValues();
        var searchValues = $.extend({},contentSearchValues ,navContectSearchValues );
        // just add the "q_"
        var qParams = {searchMode: view.sideNav.searchMode};
        $.each(searchValues, function (key, val) {
            qParams["q_" + key] = val;
            isValid = true;
        });
        
        //if there has no any search text
        if(contentSearchValues==null&&navContectSearchValues==null){
        	alert("please enter some keywords to search");
        	return false;
        }
        //if has some search text,but less than 3 letters
        if(!isValid){
        	return false;
        }
        
        var callback = function(pageIdx, pageSize){
            qParams.pageIdx = pageIdx||1;
            qParams.pageSize = pageSize||30;
            $.ajax({
                url: "search",
                type: "GET",
                data: qParams,
                dataType: "json"
            }).always(function (data) {
	            var result = data.result;
	            result.callback = callback;
	            view.$el.trigger("SEARCH_RESULT_CHANGE", result);
            });
        };
        callback();

    }
	
})(jQuery);