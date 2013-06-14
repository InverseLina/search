(function($){
	var searchDao = app.SearchDaoHandler;
	brite.registerView("SideNav",{parent:"#sidenav-ctn"},{
		create: function(){
			return render("SideNav");
		},
		
		postDisplay: function(){
		  // save serarch mode to the view
          var view = this;
          brite.display("SideSection", view.$el, {title: "Contact Info", component: "ContactInfo"});
          brite.display("SideSection", view.$el, {title: "Company", component: "Company"});
          brite.display("SideSection", view.$el, {title: "Education", component: "Education"});
          brite.display("SideSection", view.$el, {title: "Skill", component: "Skill"});
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
                           if(!isEmptyQuery(this.getSearchValues())){
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
			var result = {};
            $.each($e.bFindComponents("SideSection"), function(idx,item){
                $.extend(result, item.getSearchValues());
            });
            console.log(result);
            return result;

        }
	});

    function isEmptyQuery(params){
        params = params||{};
        var val;
        var empty = true;
        for(key in params) {
           val = params[key]||'';
            if(!/^\s*$/.test(val)){
                empty = false;
                break;
            }
        }
        return empty;
    }
	
})(jQuery);