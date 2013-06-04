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
        showErrorMessage: function(title, detail){
            var view = this;
            view.$searchInfo.empty();
            var html = render("search-query-error", {title: title, detail:detail});
            view.$searchResult.html(html);

        },
        empty:function() {
            var view = this;
            view.$searchInfo.empty();
            view.$searchResult.html(render("search-empty"));
        },
        loading:function() {
            var view = this;
            view.$searchInfo.empty();
            view.$searchResult.html(render("search-loading"));
        },

		parentEvents: {

            MainView: {
                "SEARCH_RESULT_CHANGE": function (event, result) {
                    var view = this;
                    var html = render("search-items", {items: result.result});
                    view.$searchResult.html(html);
                    var htmlInfo = "Result size: " + result.count + " | Duration: " + result.duration + "ms";
                    htmlInfo += " (c: " + result.countDuration + "ms," + " s: " + result.selectDuration + "ms)";
                    view.$searchInfo.html(htmlInfo);
                    brite.display("Pagination",view.$el.find(".page"), {
                        pageIdx:result.pageIdx,
                        pageSize:result.pageSize,
                        totalCount:result.count,
                        callback:result.callback
                    });
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
	
})(jQuery);