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

			view.$searchInput.on("keypress",function(event){
				if (event.which === 13){
					view.$el.trigger("DO_SEARCH");
				}
			});
            view.empty();
		},
		events:{
			"btap;.tableHeader .row>div[data-column]":function(event){
				var view = this;
				var $th = $(event.currentTarget);
				var $desc = $(".desc",$th);
				var $asc = $(".asc",$th);
				var column = $th.attr("data-column");
				var bPagination = view.$el.bComponent("ContentView");
				var pageIdx = bPagination.pageIdx||1;
				var pageSize = bPagination.pageSize||30;
				if($asc.is(":hidden")){
					$(".desc,.asc",$th.parent()).hide();
					$asc.show();
					view.$el.bComponent("MainView").$el.trigger("DO_SEARCH",{column:column,order:"asc",pageIdx:pageIdx,pageSize:pageSize});
				}else{
					$(".desc,.asc",$th.parent()).hide();
					$desc.show();
					view.$el.bComponent("MainView").$el.trigger("DO_SEARCH",{column:column,order:"desc",pageIdx:pageIdx,pageSize:pageSize});
				}
			},
            "btap;.tableHeader span[data-action='popupColumns']":function(event){
                brite.display("SelectColumns", "body", {top: event.pageY, left: event.pageX});
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
            var html = render("search-query-error", {title: title, detail:detail});
            view.$el.find(".tableBody",view.$searchResult).html(html);

        },
        empty:function() {
            var view = this;
            view.$searchInfo.empty();
            view.$searchResult.html(render("search-empty"));
        },
        loading:function() {
            var view = this;
            view.$searchInfo.empty();
            view.$el.find(".tableBody",view.$searchResult).html(render("search-loading"));
        },

		parentEvents: {

            MainView: {
                "SEARCH_RESULT_CHANGE": function (event, result) {
                    var view = this;
                    var html;
                    var htmlInfo = "Result size: " + result.count + " | Duration: " + result.duration + "ms";
                    htmlInfo += " (c: " + result.countDuration + "ms," + " s: " + result.selectDuration + "ms)";
                    view.$searchInfo.html(htmlInfo);
                    if(result.count > 0){
                        html = render("search-items", {items: buildResult(result.result)});
                        view.$searchResult.html(html);
                        brite.display("Pagination",view.$el.find(".page"), {
                            pageIdx:result.pageIdx,
                            pageSize:result.pageSize,
                            totalCount:result.count,
                            callback:result.callback
                        });
                        view.$el.find(".table .row>div").css({width: 100/app.preference.columns().length + "%"})
                    }else {
                        view.$searchResult.html(render("search-query-notfound"));
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
       for(var i = 0; i < items.length; i++){
            item = [];
//           console.log(items[i]);
           for(var j = 0; j < columns.length; j++) {
               item.push(items[i][columns[j]]);
           }
           result.push({row:item});
       }
//        console.log(result);
        return result;
    }
	
})(jQuery);