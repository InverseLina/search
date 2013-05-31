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
	 },
	 getSearchMode:function(){
	   var view = this;
	   console.log(view.$el.bFindComponents("SideNav")[0]);
	   var searchMode = view.$el.bFindComponents("SideNav")[0].searchMode;
	   
	   return searchMode;
	 }
	});


    function doSearch() {
        var view = this;
        var valCount = 0;
        var isValid = true;
        var contentSearchValues = view.contentView.getSearchValues();
        var navContectSearchValues = view.sideNav.getSearchValues();
        var searchValues = $.extend({},contentSearchValues ,navContectSearchValues );
        // just add the "q_"
        var qParams = {searchMode: view.sideNav.searchMode};
        console.log(qParams);
        $.each(searchValues, function (key, val) {
            if(!/^\s*$/.test(val)){
                if(/^\s*[^\s].+[^\s]\s*$/.test(val)){
                    qParams["q_" + key] = val;
                }else{
                    isValid = false;
                }
                valCount++;
            }
        });
        
        //if there has no any search text
        if(valCount == 0 && isValid){
            view.contentView.showErrorMessage("Not Search Keyword", "please enter some keywords to search");
        	return false;
        }
        //if has some search text,but less than 3 letters
        if(!isValid){
            view.contentView.showErrorMessage("Wrong Search Query", "Search query needs to be at least 3 character long");
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