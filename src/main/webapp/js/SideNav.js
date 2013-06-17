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

		getSearchValues: function(){
			var view = this;
			var $e = view.$el;
			var result = {};
			$.each($e.bFindComponents("SideSection"), function(idx,item){
			    $.extend(result, item.getSearchValues());
			});
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