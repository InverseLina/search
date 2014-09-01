/**
 * View: CustomFilterPopup
 *
 *
 *
 */
(function($) {
	
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
			data = data || {};
			var fields = data.fields;
			showFields.call(view, fields);
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
			"click;.left-side":function(e){
				var view = this;
				var $e = view.$el;
				var $target = $(e.currentTarget);
				var $item = $target.closest(".filter-item");
				$e.trigger("MODE_CHANGE",{filterItem:$item});
			},
			"click;.clearAll":function(e){
				var view = this;
				var $e = view.$el;
				var $target = $(e.currentTarget);
				var $item = $target.closest(".filter-item");
				var component = $($item.find(".filter-item-container").children()[0]).bView();
				if(component && component.clearFields && $.isFunction(component.clearFields)){
					component.clearFields();
					$e.trigger("DO_SEARCH");
				}
			}
		},
		docEvents:{
			"click":function(){
				var view = this;
				var $e = view.$el;
				$e.removeClass("show");
			},
			"CLEAR_ALL_CUSTOM_FIELDS":function(){
				var view = this;
				var $e = view.$el;
				$e.find(".filter-item").each(function(){
					var $item = $(this);
					var component = $($item.find(".filter-item-container").children()[0]).bView();
					if(component && component.clearFields && $.isFunction(component.clearFields)){
						component.clearFields();
					}
				});
			},
			"MODE_CHANGE":function(e, extra){
				var view = this;
				var $e = view.$el;
				var $item = $(extra.filterItem)
				var $icon = $item.find(".filter-item-header .left-side .icon-fa");
				var component = $($item.find(".filter-item-container").children()[0]).bView();
				if($item.hasClass("extend")){
					$item.removeClass("extend");
					$icon.removeClass("fa-chevron-down");
					$icon.addClass("fa-chevron-right");
					if(component && component.showMode && $.isFunction(component.showMode)){
						component.showMode('view');
					}
				}else{
					$item.addClass("extend");
					$icon.addClass("fa-chevron-down");
					$icon.removeClass("fa-chevron-right");
					if(component && component.showMode && $.isFunction(component.showMode)){
						component.showMode('edit');
					}
				}
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
	
	function showFields(fields){
		var view = this;
		var $e = view.$el;
		var $filtersContainer = $e.find(".filters-content").empty();
		for (var i = 0; i < fields.length; i++) {
			var field = fields[i];
			var label = field.name;

			if (field.label) {
				label = field.label;
			}

			var $filterItem = $(render("CustomFilterPopup-filter", {
				label : label,
				name : field.name
			}));
			$filtersContainer.append($filterItem);
			var viewName;

			if (field.type.toLowerCase() == 'number') {
				viewName = "CustomFilterNumber";
			} else if (field.type.toLowerCase() == 'string') {
				viewName = "CustomFilterString";
			} else if (field.type.toLowerCase() == 'date') {
				viewName = "CustomFilterDate";
			} else if (field.type.toLowerCase() == 'boolean') {
				viewName = "CustomFilterBoolean";
			}

			if (viewName) {
				brite.display(viewName, $filterItem.find(".filter-item-container"), {
					name : field.name
				});
			}
		}
	}
	
})(jQuery);
