(function($){
	var searchDao = app.SearchDaoHandler;
	brite.registerView("MainView",{parent:"body"},{
    // --------- View Interface Implement--------- //
	 create: function(data){
			return render("MainView");
	 },
	 postDisplay: function(data){
		 var view = this;
		 view.$el.find(".config").removeClass("hide");
		 view.$el.find(".home").addClass("hide");
		 if(data&&data.type=="admin"){
			 brite.display("AdminMainView");
		 }else{
			 /*brite.display("SideNav",this.$el.find(".sidenav-ctn")).done(function(sideNav){
					view.sideNav = sideNav;
			 });*/
			 brite.display("ContentView",this.$el.find(".contentview-ctn")).done(function(contentView){
			 	 view.contentView = contentView;
			 });
		 }
	 },
    // --------- /View Interface Implement--------- //

    // --------- Events--------- //
	 events: {
	 	"click; [data-action='DO_SEARCH']": function(e){
	 		var view = this;
	 		var $this = $(e.currentTarget);
	 		var search;
	 		// if click by the search icon
	 		if($this.hasClass("glyphicon-search")){
	 		  var $input = $this.closest(".input-wrapper").find("input[type='text']");
	 		  search = $input.val();
	 		}
	 		view.$el.trigger("DO_SEARCH",{search:search});
	 	},
	 	//should trim the search value when focus out
        'focusout; input[type="text"]':function(event){
            var $target = $(event.currentTarget);
            $target.val($.trim($target.val()));
        },
	 	"btap;.config":function(event){
	 		if(app.cookie("login")!="true"){
	 		brite.display("LoginModal");
	 		}else{
        		window.location.href="/admin";
	 		}
	 	}
	 },
    // --------- /Events--------- //


    // --------- Document Events--------- //
     docEvents: {
         "DO_SET_COLUMNS":function(event, extra){
             var view = this;
             if(extra.columns && extra.columns.length > 0){
                 app.preference.columns(extra.columns);
                 view.$el.trigger("DO_SEARCH");
             }
         },
         "DO_SEARCH": function(event,opts){
             doSearch.call(this,opts);
         }
     }
    // --------- /Document Events--------- //
	});

    // --------- Private Methods--------- //
    function doSearch(opts) {
        var searchValues, view = this;
        opts = opts|| {};
        var search = opts.search;

        var callback = function(pageIdx, pageSize){
            view.contentView.loading();
            view.contentView.restoreSearchParam();
            var searchParameter = app.ParamsControl.getParamsForSearch(search);
            searchParameter.pageIndex = pageIdx||1;

            searchDao.search(searchParameter).always(function (result) {
	            result.callback = callback;
	            view.$el.trigger("SEARCH_RESULT_CHANGE", result);
            });
        };
        callback();

    }
    // --------- /Private Methods--------- //
	
})(jQuery);