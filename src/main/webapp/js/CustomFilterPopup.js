/**
 * View: CustomFilterPopup
 *
 *
 *
 */
(function($) {
	
	//FIXME hardcode for now
	var _fieldLabels = {
		"desiredstartdate":{label:"Desired Start Date"},
		"lastactivitydate":{label:"Last Activity Date"},
		"certifications":{label:"Certifications"},
		"desiredsalary":{label:"Desired Salary"},
		"iswillingtorelocate":{label:"Is Willing to Relocate"},
		"candidatesource":{label:"Candidate Source"}
	};
	
	brite.registerView("CustomFilterPopup", {
		parent : ".SearchDataGrid .search-result"
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			return render("CustomFilterPopup");
		},

		postDisplay : function(data) {
			var view = this;
			var $e = view.$el;
			
			
			app.getJsonData("/getCustomFields").done(function(result) {
				console.log(result);
				for(var i = 0; i < result.length; i++){
					var field = result[i];
					var $filterItem = $(render("CustomFilterPopup-filter", {
						label : _fieldLabels[field.name].label,
						name : field.name
					}));
					$filtersContainer.append($filterItem);
					var viewName;
					
					if (field.type == 'Number') {
						viewName = "CustomFilterNumber";
					} else if (field.type == 'String') {
						viewName = "CustomFilterString";
					} else if (field.type == 'Date') {
						viewName = "CustomFilterDate";
					} else if (field.type == 'Boolean') {
						viewName = "CustomFilterBoolean";
					}
	
					if (viewName) {
						brite.display(viewName, $filterItem.find(".filter-item-container"), {
							name : field.name
						});
					}
				}
			});
			
			var $filtersContainer = $e.find(".filters-content").empty();
			for (var key in _fieldLabels) {
				
			}

		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"click":function(e){
				e.stopPropagation();				
			},
			"click;.CustomFilterPopup-slider":function(){
				var view = this;
				var $e = view.$el;
				$e.toggleClass("show");
			},
			"click;.filter-item-header":function(e){
				var view = this;
				var $e = view.$el;
				var $target = $(e.currentTarget);
				var $icon = $target.find(".icon-fa");
				var $item = $target.closest(".filter-item");
				var component = $($item.find(".filter-item-container").children()[0]).bView();
				if($item.hasClass("extend")){
					$item.removeClass("extend");
					$icon.removeClass("fa-chevron-down");
					$icon.addClass("fa-chevron-right");
					if(component){
						component.showMode('view');
					}
				}else{
					$item.addClass("extend");
					$icon.addClass("fa-chevron-down");
					$icon.removeClass("fa-chevron-right");
					if(component){
						component.showMode('edit');
					}
				}
			}
		},
		docEvents:{
			"click":function(){
				var view = this;
				var $e = view.$el;
				$e.removeClass("show");
			}
		},
		
		// --------- /Events--------- //
		getValues:function(){
			var view = this;
			var $e = view.$el;
			var values = [];
			$e.find(".filter-item").each(function(){
				var $item = $(this);
				var itemComponent = $($item.find(".filter-item-container").children()[0]).bView();
				if(itemComponent){
					var value = itemComponent.getValue();
					if(value){
						values.push(value);
					}
				}
			});
			return values;
		}
	});
})(jQuery);
