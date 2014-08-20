/**
 * View: CustomFilterPopup
 *
 *
 *
 */
(function($) {
	
	//FIXME hardcode for now
	var _fieldLabels = {
		"desiredstartdate":{label:"Desired Start Date", type:"date"},
		"lastactivitydate":{label:"Last Activity Date", type:"date"},
		"certifications":{label:"Certifications", type:"string"},
		"desiredsalary":{label:"Desired Salary", type:"number"},
		"iswillingtorelocate":{label:"Is Willing to Relocate", type:"boolean"},
		"candidatesource":{label:"Candidate Source", type:"string"}
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
			
			
			var $filtersContainer = $e.find(".filters-content").empty();
			for (var key in _fieldLabels) {
				var $filterItem = $(render("CustomFilterPopup-filter", {
					label : _fieldLabels[key].label,
					name : key
				}));
				$filtersContainer.append($filterItem);
				var viewName;
				
				if (_fieldLabels[key].type == 'number') {
					viewName = "CustomFilterNumber";
				} else if (_fieldLabels[key].type == 'string') {
					viewName = "CustomFilterString";
				} else if (_fieldLabels[key].type == 'date') {
					viewName = "CustomFilterDate";
				} else if (_fieldLabels[key].type == 'boolean') {
					viewName = "CustomFilterBoolean";
				}

				if (viewName) {
					brite.display(viewName, $filterItem.find(".filter-item-container"), {
						name : key
					});
				}
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
