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
                        pageNo:result.pageNo,
                        pageSize:result.pageSize,
                        totalCount:result.count,
                        callback:result.callback
                    })
                }
            }
		},
		
		getSearchValues: function(){
			var val = this.$searchInput.val();
			if (val){
				return {
					search: val
				}
			}else{
				return null;
			}
		}
	});
	
})(jQuery);