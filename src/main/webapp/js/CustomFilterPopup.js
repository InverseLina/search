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
			
			showFields.call(view);
		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"click;.CustomFilterPopup-slider":function(e){
				e.stopPropagation();
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
				callFilterViewMethod.call(view, $item, "clearFields");
				$e.trigger("DO_SEARCH");
			}
		},
		// --------- /Events--------- //
		// --------- Document Events--------- //
		docEvents:{
			"click":function(e){
				var view = this;
				var $e = view.$el;
				var $target = $(e.target);
				if($target.closest("."+view.name).size() == 0){
					$e.removeClass("show");
				}
			},
			"CLEAR_ALL_CUSTOM_FIELDS":function(){
				var view = this;
				var $e = view.$el;
				$e.find(".filter-item").each(function(){
					var $item = $(this);
					callFilterViewMethod.call(view, $item, "clearFields");
				});
			},
			"MODE_CHANGE":function(e, extra){
				var view = this;
				var $e = view.$el;
				var $item = $(extra.filterItem)
				var $icon = $item.find(".filter-item-header .left-side .icon-fa");
				if($item.hasClass("extend")){
					$item.removeClass("extend");
					$icon.removeClass("fa-chevron-down");
					$icon.addClass("fa-chevron-right");
					callFilterViewMethod.call(view, $item, "showMode", "view");
				}else{
					$item.addClass("extend");
					$icon.addClass("fa-chevron-down");
					$icon.removeClass("fa-chevron-right");
					callFilterViewMethod.call(view, $item, "showMode", "edit");
				}
			},
			"REFRESH_POPUP_CUSTOM_FIELDS":function(){
				var view = this;
				showFields.call(view);
			}
		},
		
		// --------- /Document Events--------- //
		// --------- Public APIs--------- //
		getValues:function(){
			var view = this;
			var $e = view.$el;
			var values = [];
			$e.find(".filter-item").each(function(){
				var $item = $(this);
				var value = callFilterViewMethod.call(view, $item, "getValue");
				if (value) {
					values.push(value);
				}

			});
			return values;
		}
		// --------- /Public APIs--------- //
	});
	
	// --------- Private methods--------- //
	function showFields(){
		var view = this;
		var $e = view.$el;
		var fields = app.columns.getCustomColumnsNotSelected();
		var $filtersContainer = $e.find(".filters-content").empty();
		
		if(fields && fields.length > 0){
			$e.removeClass("hide");
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
		}else{
			$e.addClass("hide");
		}
		
	}
	
	function callFilterViewMethod($currentItem, methodName){
		var view = this;
		var $e = view.$el;
		var args = [];
		
		if(arguments.length > 2){
			args = Array.prototype.slice.call(arguments, 2);
		}
		
		var component = $($currentItem.find(".filter-item-container").children()[0]).bView();
		if (component && component[methodName] && $.isFunction(component[methodName])) {
			return component[methodName].apply(component, args);
		}
		return null;
	}
	// --------- /Private methods--------- //
})(jQuery);
