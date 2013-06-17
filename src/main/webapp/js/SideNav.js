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
			"keypress; input[type='text']": function(event){
				if (event.which === 13){
					this.$el.trigger("DO_SEARCH");
				}
			}
		},

        parentEvents : {
            MainView : {
                "SEARCH_RESULT_CHANGE" : function(event, result) {
                    var view = this;
                    var $e = view.$el;
                    var mainView = view.$el.bView("MainView");
                    var contentSearchValues = mainView.contentView.getSearchValues();
                    var navContectSearchValues = mainView.sideNav.getSearchValues();
                    var searchValues = $.extend({},contentSearchValues ,navContectSearchValues);
                    // just add the "q_"
                    var qParams = {};
                    $.each(searchValues, function (key, val) {
                        qParams["q_" + key] = $.trim(val);
                    });
                    if (result.count > 0) {
                        searchDao.getGroupValuesForAdvanced(qParams).done(function(result){
                            $.each(view.$el.bFindComponents("SideSection"), function(idx, sideSection){
                                if(sideSection.subComponent && sideSection.subComponent.$el){
                                    sideSection.subComponent.$el.trigger("UPDATE_RESULT_CHANGE", result);
                                }
                            });
                        });
                    }
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