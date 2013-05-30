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
				if(this.searchMode=="advanced"){
					var companies = [{name:'Google',num:234},{name:'Adobe',num:234},{name:'LinkedIn',num:234},{name:'Facebook',num:234}];
					var educations = [{name:'Standford',num:234},{name:'MIT',num:234}];
					brite.display("AdvancedSearch",$("div.advanced",$newSec),{companies:companies,educations:educations});
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
			if (view.searchMode === "simple"){
				// when simple mode, return nothing.
				return null;
			}else if (view.searchMode === "keyword"){
				var values = {};
				var isValid = true;
				var hasSearch = false;
				view.$el.find(".keyword-form input").each(function(){
					var $input = $(this);
					var nameValue = {};
					var name = $input.attr("name");
					var val = $input.val();
					if (val){
						if(val.length>2){
							values[name] = val;
						}else{
							alert(name+" text must not less than 3 chars");
							isValid = false;
						}
						hasSearch = true;
					}
				});
				
				if(hasSearch){//if there has type some letters in nav text field
					if(isValid){
						return values;
					}else{
						return {};
					}
				}else{
					return null;
				}
			}else if (view.searchMode === "advanced"){
				// FIXME: need to do later, when we have the UI.
				return null;
			}
		}
	});
	
})(jQuery);