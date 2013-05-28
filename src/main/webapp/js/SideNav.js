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
				var $newSec = $(event.target).parent(".sec");
				this.searchMode = $newSec.attr("data-search-mode");

				var $oldSec = view.$el.find(".sec.sel");

				$oldSec.removeClass("sel");
				
				$newSec.addClass("sel");				
			},
			
			"keypress; input": function(event){
				if (event.which === 13){
					this.$el.trigger("DO_SEARCH");
				}
			}
		}, 
		
		getSearchValues: function(){
			var view = this;
			if (view.searchMode === "simple"){
				// when simple mode, return nothing.
				return null;
			}else if (view.searchMode === "keyword"){
				var values = {};
				view.$el.find(".keyword-form input").each(function(){
					var $input = $(this);
					var nameValue = {};
					var name = $input.attr("name");
					var val = $input.val();
					if (val){
						values[name] = val;
					}
   			});
   			return values;
			}else if (view.searchMode === "advanced"){
				// FIXME: need to do later, when we have the UI.
				return null;
			}
		}
	});
	
})(jQuery);