(function($){
	
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
				this.searchMode = $newSec.attr("data-search-mode");
                console.log(this.searchMode);

				var $oldSec = view.$el.find(".sec.sel");

				$oldSec.removeClass("sel");
				
				$newSec.addClass("sel");		
				if(this.searchMode=="advanced"){

                    if ($e.find(".AdvancedSearch").size() == 0) {
                        $.ajax({
                            url: "getTopCompaniesAndEducations",
                            type: "GET",
                            dataType: "json"
                        }).always(function (data) {
                                var result = data.result;
                                var companies = result.companies;
                                var educations = result.educations;
                                brite.display("AdvancedSearch", $("div.advanced", $newSec), {companies: companies, educations: educations});
                            });
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
                view.$el.find(".keyword-form input").each(function () {
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