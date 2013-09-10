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
		 }else if(data&&data.type=="organization"){
			 brite.display("Organization");
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
         },
         "SEARCH_RESULT_CHANGE": function () {
             var view = this;
             setTimeout(function () {
                 restoreSearchParam.call(view);
             }, 200);
         }
     }
	});


    function doSearch(opts) {
        var view = this;

        var callback = function(pageIdx, pageSize){
            view.contentView.loading();
            restoreSearchParam.call(view);
            var searchParameter = app.ParamsControl.getParamsForSearch();
//            qParams.pageIndex = pageIdx||qParams.pageIndex;
//            qParams.pageSize =  pageSize||qParams.pageSize;
//            var qParams=view.getSearchValues();
            searchParameter.pageIndex = pageIdx||1;
            searchDao.search(searchParameter).always(function (result) {
	            result.callback = callback;
	            view.$el.trigger("SEARCH_RESULT_CHANGE", result);
            });
        };
        callback();

    }

    function restoreSearchParam(){
        var key, dataName, data, displayName, $html, $th, view = this;

        if(view.$el.find("table th .selectedItems .item").length > 0){
            return;
        }
        var result = app.ParamsControl.getFilterParams() || {};

        for(key in result){
            dataName = key;
            if(key == "Contact"){
                dataName = "contact";
            }
            $th = view.contentView.$el.find("table thead th[data-column='{0}']".format(dataName));
            var data = result[key];
            if (data && data.length > 0) {
                $.each(data, function (index, val) {
                    /* if (dataName == "contact") {
                     displayName = app.getContactDisplayName(val.value)  ;
                     } else {
                     displayName = val;
                     }*/
                    $html = $(render("search-items-header-add-item", {name: val.name}));
                    $th.find(".addFilter").before($html);
                });
                $th.find(".addFilter").hide();
            }
        }

    }
	
})(jQuery);