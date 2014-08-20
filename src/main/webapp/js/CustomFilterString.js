/**
 * View: CustomFilterString
 *
 *
 *
 */
(function($) {
	brite.registerView("CustomFilterString", {
		parent : ".filter-item-container"
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			var view = this;
			data = data || {};
			view.paramName = data.name;
			return render("CustomFilterString");
		},

		postDisplay : function(data) {
			var view = this;
			var $e = view.$el;
			
		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"click; .operationSelect .value":function(e){
				var view = this;
				var $e = view.$el;
				var $value = $(e.currentTarget);
				var value = $value.attr("data-value");
				var $operations = $e.find(".operationSelect .operations");
				$operations.removeClass("hide");
				$operations.find(".operation-item").removeClass("hide");
				$operations.find(".operation-item[data-value="+value+"]").addClass("hide");
			},
			"click; .operation-item":function(e){
				var view = this;
				var $e = view.$el;
				var $item = $(e.currentTarget);
				var value = $item.attr("data-value");
				var $value = $e.find(".operationSelect .value")
				$value.html(value).attr("data-value", value);
				$e.find(".operations").addClass("hide");
			},
			"keyup; .valueInput":function(e){
				var view = this;
				var $e = view.$el;
				var $item = $(e.currentTarget);
				var value = $item.val();
				if(e.which == 13){
					if(value != ""){
						var $value = $e.find(".operationSelect .value");
						var operation = $value.attr("data-value");
						$e.find(".autocomplete-container").addClass("hide");
						var $selectedItem = $(render("CustomFilterString-edit-item",{value:value, operation:operation}));
						$selectedItem.insertBefore($e.find(".editContainer .selectedItems .cb"));
					}
				}else{
					$e.find(".autocomplete-container").removeClass("hide");
				}
			},
			"click; .autocomplete-item":function(e){
				var view = this;
				var $e = view.$el;
				var $item = $(e.currentTarget);
				var $input = $e.find(".valueInput");
				$input.val($item.attr("data-value"));
				$e.find(".autocomplete-container").addClass("hide");
			},
			"click; .clear":function(e){
				var view = this;
				var $e = view.$el;
				var $item = $(e.currentTarget).closest(".selected-item");
				$item.remove();
			},
			"click; .btnApply":function(e){
				var view = this;
				var $e = view.$el;
				$e.find(".value-containers .selected-item").remove();
				$e.find(".editContainer .selectedItems .selected-item").each(function(){
					var $item = $(this);
					var operation = $item.attr("data-oper");
					var value = $item.attr("data-value");
					var $viewSelectedItem = $(render("CustomFilterString-view-item",{value:value, operation:operation}));
					$viewSelectedItem.insertBefore($e.find(".value-containers .cb"));
				});
			}
		},
		docEvents:{
			"click":function(e){
				var view = this;
				var $e = view.$el;
				var $target = $(e.target);
				if($target.closest(".operationSelect").size() == 0){
					$e.find(".operations").addClass("hide");
				}
				if($target.closest(".input-containers").size() == 0){
					$e.find(".autocomplete-container").addClass("hide");
				}
			}
		},
		// --------- /Events--------- //
		showMode:function(mode){
			var view = this;
			var $e = view.$el;
			if(mode == 'edit'){
				$e.find(".editContainer").removeClass("hide");
				$e.find(".viewContainer").addClass("hide");
				
				$e.find(".editContainer .selectedItems .selected-item").remove();
				$e.find(".value-containers .selected-item").each(function(){
					var $item = $(this);
					var operation = $item.attr("data-oper");
					var value = $item.attr("data-value");
					var $selectedItem = $(render("CustomFilterString-edit-item",{value:value, operation:operation}));
					$selectedItem.insertBefore($e.find(".editContainer .selectedItems .cb"));
				});
			}else{
				if($e.find(".viewContainer .value-containers .selected-item").size() > 0){
					$e.find(".viewContainer").removeClass("hide");
				}else{
					$e.find(".viewContainer").addClass("hide");
				}
				$e.find(".editContainer").addClass("hide");
			}
		},
		getValue:function(){
			var view = this;
			var $e = view.$el;
			var valueObject = null;
			if($e.find(".viewContainer .value-containers .selected-item").size() > 0){
				valueObject = {
					field:view.paramName,
					conditions:{}
				};
				var isArray = [];
				var isnotArray = [];
				$e.find(".viewContainer .value-containers .selected-item").each(function(){
					var $item = $(this);
					var operation = $item.attr("data-oper");
					var value = $item.attr("data-value");
					if(operation == "is"){
						isArray.push(value);
					}else{
						isnotArray.push(value);
					}
				});
				valueObject.conditions = {
					"==" : isArray,
					"!=" : isnotArray
				}
			}
			return valueObject;
		}
	});
})(jQuery);
