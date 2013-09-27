(function($){
	var searchDao = app.SearchDaoHandler;
	brite.registerView("MainView",{parent:"body"},{
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
/*     getSearchValues: function(){
         var key, view = this;
         var result = {};
         var searchData = result.searchValues = {};
         var contentSearchValues = view.contentView.getSearchValues();
//         console.log(contentSearchValues);
         result.searchColumns = app.preference.columns().join(",");
         if(contentSearchValues.sort){
             result.orderBy = contentSearchValues.sort.column;
             result.orderType =  !!(contentSearchValues.sort.order === "asc");
         }
         if(!/^\s*$/.test(contentSearchValues.search)){
             searchData.q_search = $.trim(contentSearchValues.search)
         }

         if(view._searchValues){
             for(key in view._searchValues){
                 if(key == "q_contacts"){
                     searchData[key] = view._searchValues[key];
                 }else{
                     searchData[key] = {values: view._searchValues[key]}
                 }
             }
         }
         result.searchValues = JSON.stringify(searchData);
         result.pageIndex = view.contentView.pageIdx || 1;
         result.pageSize = view.contentView.pageSize || 15;
//         console.log(result);
         return result;
     },*/
	 
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
	 	
	 	"DO_SEARCH": function(event,opts){
	 		doSearch.call(this,opts);	
	 	},
	 	"btap;.config":function(event){
	 		if(app.cookie("login")!="true"){
	 		brite.display("LoginModal");
	 		}else{
        		window.location.href="/admin";
	 		}
	 	}
	 },
     docEvents: {
         "DO_SET_COLUMNS":function(event, extra){
             var view = this;
             if(extra.columns && extra.columns.length > 0){
                 app.preference.columns(extra.columns);
                 view.$el.trigger("DO_SEARCH");
             }
         },
         "SEARCH_PARAMS_CHANGE":function(event, extra){
/*             var view = this;
             var result = {};
             view.contentView.$el.find("th").each(function(idx, th){
                 var type = $(th).closest("th").attr("data-column");
                 if(type == "name"){
                     type = "contact";
                 }
                 if(type=="company"){
                	 type = "companie";
                 }
                 type = "q_" + type + "s";
                 var data = [], val;
                 $(th).find(".selectedItems .item").each(function(index, item){
                       val = $(item).data("value");
                     if(val){
                         data.push(val);
                     }
                 });
//                 console.log(data);
                 if(data.length > 0 || !$.isEmptyObject(data)){
                     result[type] = data;
                 }
             });
             view._searchValues = result;
//             console.log(view._searchValues);*/
         }
     }
	});

    function doSearch(opts) {
        var view = this;
        opts = opts|| {};
        var search = opts.search;
        var callback = function(pageIdx, pageSize){
            view.contentView.loading();
            view.contentView.restoreSearchParam();
            var searchParameter = app.ParamsControl.getParamsForSearch();
//            qParams.pageIndex = pageIdx||qParams.pageIndex;
//            qParams.pageSize =  pageSize||qParams.pageSize;
//            var qParams=view.getSearchValues();
            searchParameter.pageIndex = pageIdx||1;
            if(search && search != ""){
              var searchValues = JSON.parse(searchParameter.searchValues);
              searchValues.q_search = $.trim(search); 
              searchParameter.searchValues = JSON.stringify(searchValues);
            }
            
            searchDao.search(searchParameter).always(function (result) {
	            result.callback = callback;
	            result.q_search = search;
	            view.$el.trigger("SEARCH_RESULT_CHANGE", result);
            });
        };
        callback();

    }
	
})(jQuery);