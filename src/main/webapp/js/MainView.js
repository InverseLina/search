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
	 		
	 		if(search == null || search.length < 3){
	 			showSearchErrorMesage.call(view);
	 		}else{
	 			view.$el.trigger("DO_SEARCH",{search:search});
	 		}
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
         }
     }
    // --------- /Document Events--------- //
	});

    // --------- Private Methods--------- //
    function doSearch(opts) {
        var view = this;
        opts = opts || {};
        var search = opts.search;

        view.contentView.loading();
        view.contentView.restoreSearchParam();
        var searchParameter = app.ParamsControl.getParamsForSearch({search: search});
        searchParameter.pageIndex = opts.pageIdx || 1;

        searchDao.search(searchParameter).always(function (result) {
            view.$el.trigger("SEARCH_RESULT_CHANGE", result);
        });

    }
    
    function getColWidth() {
        var view = this;
        var colLen = app.preference.columns().length;
        //        return parseInt((view.$searchResult.innerWidth()-30)/colLen)-2;
        return 100 / colLen;
      }

      function fixColWidth() {
        var view = this;
        var colWidth;
        var colName;
        var columns = app.preference.columns();
        var colLen = columns.length;
        var tableWidth = view.$el.find(".tableContainer").width() - 20;
          var excludes = ["id", "CreatedDate","title","email", "resume"];
        if ($.inArray("id", columns) >= 0) {
          tableWidth = tableWidth - 80;
          colLen--;
        }
        if ($.inArray("CreatedDate", columns) >= 0) {
          tableWidth = tableWidth - 110;
          colLen--;
        }
        if ($.inArray("resume", columns) >= 0) {
            tableWidth = tableWidth - 65;
            colLen--;
          }
        //checkbox
        tableWidth = tableWidth - 30;
        tableWidth = tableWidth - 32;
        if (colLen != 0) {
          colWidth = tableWidth / colLen;
        } else {
          colWidth = tableWidth;
        }
        var realWidth;

        var $body = view.$el.find("tbody");
        var $head = view.$el.find("thead");
        var tlen = $head.find("th").length - 1;

        $head.find("th").each(function(idx, item) {
          var $item  = $(item);
          colName = $item.attr("data-column");
          if (colName == "id") {
            realWidth = 80;
            if (idx == tlen) {
              realWidth = colWidth + 80;
            }
          } else if (colName == "CreatedDate") {
            realWidth = 110;
            if (idx == tlen) {
              realWidth = colWidth + 110;
            }
          } else if ($item.hasClass("checkboxCol")) {
            realWidth = 30;
          } else if ($item.hasClass("favLabel")) {
            realWidth = 32;
          } else if (colName=="resume") {
            realWidth = 65;
          } else {
            realWidth = colWidth;
          }
          if (idx == tlen) {
              $item.css({
              width : realWidth + 50,
              "max-width" : realWidth + 50,
              "min-width" : realWidth
            });
          } else {
              $item.css({
              width : realWidth,
              "max-width" : realWidth,
              "min-width" : realWidth
            });
          }
          $body.find("td[data-column='" + colName + "']").css({
            width : realWidth,
            "max-width" : realWidth,
            "min-width" : realWidth
          });
            //fix for ie
            $body.find("td[data-column='" + colName + "'] > span").css({
                width : Math.floor(realWidth - 4),
            });
            //hide filter
            if($.inArray(colName, excludes) >= 0){
                $item.find(".addFilter").hide();
            }

        })
      }
    
    function showSearchErrorMesage(){
  	  var view = this;
     var $searchResult = view.$el.find(".search-result");
     view.$el.find(".search-info").empty();
        $searchResult.find(".tableContainer").html(render("search-query-less-words", {
          colWidth : getColWidth.call(view),
          labelAssigned: app.buildPathInfo().labelAssigned
        }));
        $searchResult.find(".page").empty();
        fixColWidth.call(view);
        view.contentView.restoreSearchParam();
   }
    // --------- /Private Methods--------- //
	
})(jQuery);