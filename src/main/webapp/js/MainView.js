(function($){
	
	var searchDao = app.SearchDaoHandler;
	brite.registerView("MainView",{parent:"body"},{
		create: function(){
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
	 	//should trim the search value when focus out
        'focusout; input[type="text"]':function(event){
            var $target = $(event.currentTarget);
            $target.val($.trim($target.val()));
        },
	 	
	 	"DO_SEARCH": function(event,opts){
	 		doSearch.call(this,opts);	
	 	}
	 },
     docEvents: {
         "DO_SET_COLUMNS":function(event, extra){
             var view = this;
             if(extra.columns && extra.columns.length > 0){
                 app.preference.columns(extra.columns);
                 view.$el.trigger("DO_SEARCH");
             }
         }
     },
	 getSearchMode:function(){
	   var view = this;
	   var searchMode = view.$el.bFindComponents("SideNav")[0].searchMode;
	   
	   return searchMode;
	 }
	});


    function doSearch(opts) {
        var view = this;
        var valCount = 0;
        var isValid = true;
        var contentSearchValues = view.contentView.getSearchValues();
        var navContectSearchValues = {};
        if(view.sideNav) {
           navContectSearchValues = view.sideNav.getSearchValues();
        }
        var searchValues = $.extend({},contentSearchValues ,navContectSearchValues );
        // just add the "q_"
        var qParams = {};
        $.each(searchValues, function (key, val) {
            if(!/^\s*$/.test(val)){
                if(/^\s*[^\s].+[^\s]\s*$/.test(val)){
                    qParams["q_" + key] = $.trim(val);
                }else{
                    isValid = false;
                }
                valCount++;
            }
        });
        
        //if there has no any search text
        if(valCount == 0 && isValid){
            view.contentView.empty();
        	return false;
        }
        //if has some search text,but less than 3 letters
        if(!isValid){
            view.contentView.showErrorMessage("Wrong Search Query", "Search query needs to be at least 3 character long");
            return false;
        }
        qParams=$.extend({},qParams,opts);
        var callback = function(pageIdx, pageSize){
            view.contentView.loading();
           pageIdx = pageIdx||1;
            pageSize = pageSize||30;
            searchDao.search(qParams,view.sideNav.searchMode,pageIdx,pageSize,app.preference.columns().join(",")).always(function (result) {
	            result.callback = callback;
	            view.$el.trigger("SEARCH_RESULT_CHANGE", result);
            });
        };
        callback(qParams.pageIdx,qParams.pageSize);

    }
	
})(jQuery);