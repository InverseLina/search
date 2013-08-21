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

     getSearchValues: function(){
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
         },
         "SEARCH_PARAMS_CHANGE":function(event, extra){
             var view = this;
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
//             console.log(view._searchValues);
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
        /*var valCount = 0;
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
                if(/^\s*[^\s].+[^\s]\s*$/.test(val)||/.*(State).*//*.test(key)){
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
        }*/

        var callback = function(){
            view.contentView.loading();
            restoreSearchParam.call(view);
//            qParams.pageIndex = pageIdx||qParams.pageIndex;
//            qParams.pageSize =  pageSize||qParams.pageSize;
//            var qParams=view.getSearchValues();
            searchDao.search(view.getSearchValues()).always(function (result) {
	            result.callback = callback;
	            view.$el.trigger("SEARCH_RESULT_CHANGE", result);
            });
        };
        callback();

    }

    function restoreSearchParam(){
        var key, dataName, data, displayName, $html, $th, view = this;
        var result = view._searchValues || {};
        if(view.$el.find("table th .selectedItems .item").length > 0){
            return;
        }
        for(key in result){
            if(key == "q_companies"){
                dataName = "company"
            }else{
                dataName = key.substring(2, key.length-1);
            }
            $th = view.contentView.$el.find("table thead th[data-column='{0}']".format(dataName));
            var data = result[key];
            $.each(data, function (index, val) {
                if (dataName == "contact") {
                    displayName = (val.firstName||"")  + " " + (val.lastName||"")  ;
                } else {
                    displayName = val;
                }
                $html = $(render("search-items-header-add-item", {name: displayName}));
                $html.data("value", val);
                $th.find(".addFilter").before($html);
            });
            $th.find(".addFilter").hide();
        }

    }
	
})(jQuery);