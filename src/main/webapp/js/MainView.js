(function($){
	
	var searchDao = app.SearchDaoHandler;
	brite.registerView("MainView",{parent:"body"},{
		create: function(data){
			return render("MainView");
	 }, 
	 
	 postDisplay: function(data){
		 var view = this;
		 if(data&&data.type=="admin"){
			 brite.display("Admin");
		 }else{
			 /*brite.display("SideNav",this.$el.find(".sidenav-ctn")).done(function(sideNav){
					view.sideNav = sideNav;
			 });*/
			 brite.display("ContentView",this.$el.find(".contentview-ctn")).done(function(contentView){
			 	 view.contentView = contentView;
			 });
		 }
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
     }
	});


    function doSearch(opts) {
        var view = this;
        var valCount = 0;
        var isValid = true;
        var contentSearchValues = view.contentView.getSearchValues();

        if(contentSearchValues.sort){
            opts = $.extend(opts || {},contentSearchValues.sort);
            delete contentSearchValues.sort;
        }

        var navContectSearchValues = {};
        if(view.sideNav) {
           navContectSearchValues = view.sideNav.getSearchValues();
        }
        var searchValues = $.extend({},contentSearchValues ,navContectSearchValues );
        // just add the "q_"
        var qParams = {};
        
        if(searchValues["radius"]&&!/^\s*\d+(\.\d+)?$/.test(searchValues["radius"])){
       	 view.contentView.showErrorMessage("Wrong Search Query", "radius must be numberic");
            view.$el.trigger("NO_SEARCH");
            return false;
       }

        var errorTxt = [];
        $.each(searchValues, function (key, val) {
            if(!isValid) return;
            if(/Errors$/g.test(key)){
                errorTxt.push(val.join(","));
                isValid = false;
            }
            if(!/^\s*$/.test(val)){
                if(/^\s*[^\s].+[^\s]\s*$/.test(val)||/.*(State).*/.test(key)){
                    qParams["q_" + key] = $.trim(val);
                }else{
                	if(key=="radius"){
                	  qParams["q_" + key] = $.trim(val);
                	}else{
                      isValid = false;
                	}
                }
                valCount++;
            }
        });
        
        //if there has no any search text
        if(valCount == 0 && isValid){
            view.contentView.empty();
            view.$el.trigger("NO_SEARCH");
        	return false;
        }
        if(errorTxt.length > 0) {
            view.contentView.showErrorMessage("Wrong Search Query", errorTxt.join(","));
            view.$el.trigger("NO_SEARCH");
            return false;
        }
        //if has some search text,but less than 3 letters
        if(!isValid){
            view.contentView.showErrorMessage("Wrong Search Query", "Search query needs to be at least 3 character long");
            view.$el.trigger("NO_SEARCH");
            return false;
        }
        qParams=$.extend({},qParams,opts);
        var callback = function(pageIdx, pageSize){
           view.contentView.loading();
           pageIdx = pageIdx||1;
            pageSize = pageSize||30;
            searchDao.search(qParams,"",pageIdx,pageSize,app.preference.columns().join(",")).always(function (result) {
	            result.callback = callback;
	            view.$el.trigger("SEARCH_RESULT_CHANGE", result);
            });
        };
        callback(qParams.pageIdx,qParams.pageSize);

    }
	
})(jQuery);