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
        var searchValues = $.extend({}, view.contentView.getSearchValues(), view.sideNav.getSearchValues());
        // just add the "q_"
        var qParams = {searchMode: view.sideNav.searchMode};
        $.each(searchValues, function (key, val) {
            qParams["q_" + key] = val;
        });

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