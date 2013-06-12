(function($){

	brite.registerView("ContentView",{parent:"#contentview-ctn"},{
		create: function(){
			return render("ContentView");
		},

		postDisplay: function(){
			var view = this;
			view.$searchInput = view.$el.find(".search-input");
			view.$searchResult = view.$el.find(".search-result");
			view.$searchInfo = view.$el.find(".search-info");
			view.tableOrderColumn = null;
			view.tableOrderType = null;

			view.$searchInput.on("keypress",function(event){
				if (event.which === 13){
					view.$el.trigger("DO_SEARCH");
				}
			});
            view.empty();
		},
		events:{
			"btap; table th[data-column]":function(event){
				var view = this;
				var $th = $(event.currentTarget);
				var $desc = $(".desc",$th);
				var $asc = $(".asc",$th);
				var column = $th.attr("data-column");
				view.tableOrderColumn = column;
				var bPagination = view.$el.bComponent("ContentView");
				var pageIdx = bPagination.pageIdx||1;
				var pageSize = bPagination.pageSize||30;
				if($asc.is(":hidden")){
					$(".desc,.asc",$th.parent()).hide();
					$asc.show();
					view.tableOrderType = "asc";
					view.$el.bComponent("MainView").$el.trigger("DO_SEARCH",{column:column,order:"asc",pageIdx:pageIdx,pageSize:pageSize});
				}else{
					$(".desc,.asc",$th.parent()).hide();
					view.tableOrderType = "desc";
					$desc.show();
					view.$el.bComponent("MainView").$el.trigger("DO_SEARCH",{column:column,order:"desc",pageIdx:pageIdx,pageSize:pageSize});
				}
			},
            "btap;div.btnPopupColumns":function(event){
                view = this;
                var pos = $(event.currentTarget).offset();
                brite.display("SelectColumns", "body", {top: pos.top  + $(event.currentTarget).outerHeight() + 10,
                    left: pos.left -5});
			},

			"keypress;.search-input":function(event){
			  var view = this;
			  if (event.which === 13){
                  view.$el.trigger("DO_SEARCH");
                }
			}
		},
        showErrorMessage: function(title, detail){
            var view = this;
            view.$searchInfo.empty();
            var html = render("search-query-error", {title: title, detail:detail,colWidth:getColWidth.call(view)});
            view.$searchResult.find(".tableContainer").html(html);
            view.$searchResult.find(".page").empty();

        },
        empty:function() {
            var view = this;
            view.$searchInfo.empty();
            view.$searchResult.find(".tableContainer").html(render("search-empty",{colWidth:getColWidth.call(view)}));
            view.$searchResult.find(".page").empty();
        },
        loading:function() {
            var view = this;
            view.$searchInfo.empty();
            view.$searchResult.find(".page").empty();
            view.$searchResult.find(".tableContainer").html(render("search-loading"));
        },

		parentEvents: {

            MainView: {
                "SEARCH_RESULT_CHANGE": function (event, result) {
                    var view = this;
                    var $e = view.$el;
                    var html;
                    var htmlInfo = "Result size: " + result.count + " | Duration: " + result.duration + "ms";
                    htmlInfo += " (c: " + result.countDuration + "ms," + " s: " + result.selectDuration + "ms)";

                    view.$searchInfo.html(htmlInfo);
                    if(result.count > 0){


                        html = render("search-items", {items: buildResult(result.result),
                            colWidth:getColWidth.call(view)});
                        view.$searchResult.find(".tableContainer").html(html);
                        
                        //show desc/asc
                        if(view.tableOrderColumn && view.tableOrderType){
                          $e.find("table th[data-column='"+view.tableOrderColumn+"']").find("."+view.tableOrderType).show();
                        }
                        fixColWidth.call(view);
                        
                        brite.display("Pagination",view.$el.find(".page"), {
                            pageIdx:result.pageIdx,
                            pageSize:result.pageSize,
                            totalCount:result.count,
                            callback:result.callback
                        });
                    }else {
                        view.$searchResult.find(".tableContainer").html(render("search-query-notfound",{colWidth:getColWidth.call(view)}));
                        view.$searchResult.find(".page").empty();
                    }
                }
            }
		},
		
		getSearchValues: function(){
			var val = this.$searchInput.val();
			var searchMode = this.$el.bView("MainView").getSearchMode();
            return {
                search: val
            }
		}
	});

    function buildResult(items){
       var result = [];
       var item;
        var columns = app.preference.columns();
        var colLen = columns.length;
       for(var i = 0; i < items.length; i++){
            item = [];
           for(var j = 0; j < columns.length; j++) {
               item.push({name:columns[j], value: items[i][columns[j]], notLast:colLen - j >1});
           }
           result.push({row:item});
       }
        return result;
    }

    function getColWidth(){
        var view = this;
        var colLen = app.preference.columns().length;
        return parseInt((view.$searchResult.innerWidth()-30)/colLen)-2;
//        return 100/colLen;
    }

    function fixColWidth(){
        var view = this;
        var colWidth;
        var colName;
        var columns = app.preference.columns();
        var colLen = columns.length;
        var tableWidth = view.$el.find(".tableContainer").width() - 20;
        if($.inArray("id", columns)>=0){
            tableWidth = tableWidth - 80;
            colLen --;
        }
        if($.inArray("createdate", columns)>=0){
            tableWidth = tableWidth - 110;
            colLen --;
        }

        colWidth = tableWidth / colLen;
        var realWidth;

        var $body = view.$el.find("tbody");
        var $head = view.$el.find("thead");
        $head.find("th").each(function(idx, item){
            colName = $(item).attr("data-column");
            if(colName == "id" ){
                realWidth = 80;
            }else if(colName == "createdate"){
                realWidth = 110;
            }else {
                realWidth = colWidth;
            }
            $(item).css({width: realWidth, "max-width": realWidth, "min-width": realWidth});
            $body.find("td[data-column='" + colName + "']").css(
                {width: realWidth, "max-width": realWidth, "min-width":realWidth});
        })
    }
	
})(jQuery);