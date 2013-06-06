(function($){
	var searchDao = app.SearchDaoHandler;
	brite.registerView("SideNav",{parent:"#sidenav-ctn"},{
		create: function(){
			return render("SideNav");
		},
		
		postDisplay: function(){
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
                        searchDao.getAdvancedMenu({type:"company",limit:app.preference.get("company",5)}).always(function (result) {
                             var companies = result.companies;
                             searchDao.getAdvancedMenu({type:"education",limit:app.preference.get("education",5)}).always(function (res) {
                                 var educations = res.educations;
                                 brite.display("AdvancedSearch", $("div.advanced", $newSec), {companies: companies, educations: educations});
                             });
                       });
                    }
                }
                var mainView = view.$el.bView("MainView");
                if(this.searchMode != oldMode){
                    var query = mainView.contentView.$el.find("input.search-query").val();
                    if(this.searchMode == 'simple'){
                        if (/^\s*[^\s].+[^\s]\s*$/.test(query)) {
                            view.$el.trigger("DO_SEARCH");
                        } else {
                            mainView.contentView.empty();
                        }
                    } else {
                        view.$el.trigger("DO_SEARCH");
                    }
                }
            },
			"keypress; input": function(event){
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
			}else if (view.searchMode === "keyword"){
				var values = {};
                view.$el.find(".keyword-form input[type='text']").each(function () {
                    var $input = $(this);
                    var name = $input.attr("name");
                    var val = $input.val();
                    values[name] = val;
                });
                view.$el.find(".keyword-form input[type='checkbox']:checked").each(function () {
                    var $input = $(this);
                    var name = $input.attr("name");
                    var val = $input.val();
                    values[name] = val;
                });

                return values;
			}else if (view.searchMode === "advanced"){
				return $e.bFindComponents("AdvancedSearch")[0].getSearchValues();
			}
		}
	});
	
})(jQuery);