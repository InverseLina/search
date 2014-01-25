(function($){
	var searchDao = app.SearchDaoHandler;
	brite.registerView("MainView",{parent:"body"},{
    // --------- View Interface Implement--------- //
	 create: function(data){
		return render("MainView",{contextPath:contextPath});
	 },
	 postDisplay: function(data){
		 var view = this;
		 view.$el.find(".config").removeClass("hide");
		 view.$el.find(".home").addClass("hide");
		 
		 if(app.cookie("userName")){
	          var userName = app.cookie("userName");
	          if(userName) {
	              userName = ":"+ userName
	          }else{
	              userName="";
	          }
	          var $sfInfo =view.$el.find(".sf-info");
	    	  $sfInfo.html((app.cookie("org")||" ")+userName.replace(/"/g,""));
	      }
		 
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
		 app.MainView = view;
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
	 	"click;.config":function(event){
	 		if(app.cookie("login")!="true"){
	 			brite.display("LoginModal");
	 		}else{
        		window.location.href=contextPath+"/admin";
	 		}
	 	     event.preventDefault();
	         event.stopPropagation();
	 	},
	 	"click;.clear-all":function(event){
	 		 clearSearch.call(this);
	 	},
	 	"CHECK_CLEAR_BTN":function(){
	 		 var view = this;
	 		 var filters = app.ParamsControl.getFilterParams();
	         var hasFilter = false;
	         for (var key in filters) {
	             if(filters[key].length > 0) {
	                 hasFilter = true;
	                 break;
	             }
	         }
	         if($.trim(view.contentView.$el.find(".search-form .search-input").val())||hasFilter){
	        	view.$el.find(".clear-all").prop("disabled",false);
	         }else{
	        	 view.$el.find(".clear-all").prop("disabled",true);
	         }
	 	}
	 },
    // --------- /Events--------- //


    // --------- Document Events--------- //
     docEvents: {
         "DO_SET_COLUMNS":function(event, extra){
             var columns = ["contact","company","skill","education","location"];
             var colStr, view = this;
             if(extra.columns && extra.columns.length > 0){
                 colStr = extra.columns.join(",");
                 $.each(columns, function(idx, column){
                     if(colStr.indexOf(column)<0){
                         app.ParamsControl.remove(column=="contact"?"Contact":column);
                     }
                 });
                 app.preference.columns(extra.columns);
                 view.$el.trigger("DO_SEARCH");
             }
         },
         "DO_SEARCH": function(event,opts){
             doSearch.call(this,opts);
             event.preventDefault()
         },
         "SEARCH_QUERY_CHANGE":function(event){
        	 this.$el.trigger("CHECK_CLEAR_BTN");
         }
     }
    // --------- /Document Events--------- //
	});

    // --------- Private Methods--------- //
    function doSearch(opts) {
        var view = this;
        var $e = view.$el;
        opts = opts || {};
        var search = opts.search;
        var searchParameter = app.ParamsControl.getParamsForSearch({search: search});
        var searchKey = app.ParamsControl.getQuery();
        var filters = app.ParamsControl.getFilterParams();
        if($.trim(searchKey).length>0&&$.trim(searchKey).length < 3 ){
            view.contentView.dataGridView.showContentMessage("lessword");
            var labelAssigned = app.buildPathInfo().labelAssigned;
            if(labelAssigned){
                view.$el.trigger("CHANGE_TO_FAV_VIEW");
            }
            return;
        }

        view.contentView.dataGridView.showContentMessage("loading");
        view.contentView.dataGridView.restoreSearchParam();


        searchParameter.pageIndex = opts.pageIdx || 1;

        searchDao.search(searchParameter).always(function (result) {
        	view.$el.trigger("CHECK_CLEAR_BTN");
            view.$el.trigger("SEARCH_RESULT_CHANGE", result);
        });
        
    }
    
    function clearSearch(){
    	var view = this;
	 	var $el = view.contentView.$el;
	 	app.ParamsControl.clear();
	 	view.$el.find(".contentview-ctn").bEmpty();
	 	brite.display("ContentView",this.$el.find(".contentview-ctn")).done(function(contentView){
			view.contentView = contentView;
			view.$el.find(".clear-all").prop("disabled",true);;
		});
    }
    
    // --------- /Private Methods--------- //
	
})(jQuery);