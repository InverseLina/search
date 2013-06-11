(function($){
	var searchDao = app.SearchDaoHandler;
	brite.registerView("SideNav",{parent:"#sidenav-ctn"},{
		create: function(){
			return render("SideNav");
		},
		
		postDisplay: function(){
		  // save serarch mode to the view
			this.searchMode = this.$el.find(".sec.sel").attr("data-search-mode");
		},
		
		events: {
			"click; h2": function(event){
				var view = this;
				var $e = view.$el;
				var $newSec = $(event.target).parent(".sec");
                var oldMode = this.searchMode;
				this.searchMode = $newSec.attr("data-search-mode");

				var $oldSec = view.$el.find(".sec.sel");

				$oldSec.removeClass("sel");
				
				$newSec.addClass("sel");		
				if(this.searchMode=="advanced"){
                    if ($e.find(".AdvancedSearch").size() == 0) {
                        $e.find("div.sec .advanced").html(render("SideNav-advanced-loading"));
                        var companyLimit = app.preference.get("company",6);
                        var educationLimit = app.preference.get("education",6);
                        var skillLimit = app.preference.get("skill",6);
                        searchDao.getAdvancedMenu({companyLimit:companyLimit,educationLimit:educationLimit,skillLimit:skillLimit}).always(function (result) {
                             brite.display("AdvancedSearch", $("div.advanced", $newSec), result);
                       });
                    }
                }
                var mainView = view.$el.bView("MainView");
                if(this.searchMode != oldMode){
                    var query = mainView.contentView.$el.find("input.search-query").val();
                    if (/^\s*[^\s].+[^\s]\s*$/.test(query)) {
                        view.$el.trigger("DO_SEARCH");
                    }else{
                        if(this.searchMode == 'simple'){
                            mainView.contentView.empty();
                        } else{
                           if(!$.isEmptyObject(this.getSearchValues())){
                               view.$el.trigger("DO_SEARCH");
                            }else{
                               mainView.contentView.empty();
                           }
                        }
                    }
                }
            },
			"keypress; input[type='text']": function(event){
				if (event.which === 13){
					this.$el.trigger("DO_SEARCH");
				}
			}
		}, 
		
		getSearchValues: function(){
			var view = this;
			var $e = view.$el;
			if (view.searchMode === "simple"){
				// when simple mode, return nothing.
				return null;
            } else if (view.searchMode === "keyword") {
                var values = {};
                //get input values
                view.$el.find(".keyword-form input[type='text']").each(function () {
                    var $input = $(this);
                    var name = $input.attr("name");
                    var val = $input.val();
                    values[name] = val;
                });
                //get checkbox values
                view.$el.find(".keyword-form input[type='checkbox']:checked").each(function () {
                    var $input = $(this);
                    var name = $input.attr("name");
                    var val = $input.val();
                    values[name] = val;
                });

                return values;
            } else if (view.searchMode === "advanced") {
                var advancedView = $e.bFindComponents("AdvancedSearch");
                if (advancedView.length > 0){
                    return advancedView[0].getSearchValues();
                }else{
                    return {};
                }
            }
        }
	});
	
})(jQuery);