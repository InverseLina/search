/**
 * View: CustomFilterString
 *
 *
 *
 */
(function($) {
	
	var _keys = {
		LEFT : 37,
		RIGHT : 39,
		UP : 38,
		DOWN : 40,
		TAB : 9,
		ESC : 27,
		ENTER : 13
	}; 

	
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
			
			checkEmpty.call(view);
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
				$value.attr("data-value", value).find(".lbl").html($item.text());
				$e.find(".operations").addClass("hide");
			},
			"keydown; .valueInput":function(e) {
				var view = this;
				var $e = view.$el;
				if (e.which == _keys.TAB) {
					e.preventDefault();
				}
			},
			"keyup; .valueInput":function(e){
				var view = this;
				var $e = view.$el;
				e.preventDefault();
				changeAutoComplete.call(view, e.which);
			},
			"click; .autocomplete-item":function(e){
				var view = this;
				var $e = view.$el;
				var $item = $(e.currentTarget);
				var value = $item.attr("data-value");
				addItem.call(view, value);
			},
			"click; .clear":function(e){
				var view = this;
				var $e = view.$el;
				var $item = $(e.currentTarget).closest(".selected-item");
				$item.remove();
				checkEmpty.call(view);
			},
			"click; .btnApplyValue":function(e){
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
				var $filterItem = $e.closest(".filter-item");
				$e.trigger("MODE_CHANGE",{filterItem:$filterItem});
			}
		},
		parentEvents:{
			CustomFilterPopup:{
				"click":function(e){
					var view = this;
					var $e = view.$el;
					var $target = $(e.target);
					if($target.closest(".operationSelect").size() == 0){
						$e.find(".operations").addClass("hide");
					}
					if($target.closest(".input-containers").size() == 0){
						$e.find(".autocomplete-container").addClass("hide").empty();
					}
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
				
				if(isArray.length > 0){
					valueObject.conditions["=="] = isArray;
				}
				
				if(isnotArray.length > 0){
					valueObject.conditions["!="] = isnotArray;
				}
			}
			return valueObject;
		}
	});
	
	
	function changeAutoComplete(keyCode){
		var view = this;
		var $e = view.$el;
		var $container = $e.find(".autocomplete-container");
		var $activeItem = $container.find(".autocomplete-item.active");
		var $input = $e.find(".autocomplete-input-wrapper .valueInput");
		switch (keyCode) {
				case _keys.ENTER:
				case _keys.TAB:
					var value = $input.val();
					if ($activeItem.length > 0) {
						value = $activeItem.attr("data-value");
					}
			
					addItem.call(view, value);
					break;
				case _keys.ESC:
					$container.empty().addClass("hide");
					break;
				case _keys.DOWN:
					$container.find(".autocomplete-item").removeClass("active");
					if($activeItem.length == 0 || $activeItem.next().size() == 0){
						$activeItem = $container.find(".autocomplete-item:first").addClass("active");
					}else{
						$activeItem = $activeItem.next();
						$activeItem.addClass("active");
					}
					var dataValue = $activeItem.attr("data-value");
					if(dataValue && dataValue != ""){
						$input.val(dataValue);
						$input.focus();
					}
					break;
				case _keys.UP:
					$container.find(".autocomplete-item").removeClass("active");
					if($activeItem.length == 0 || $activeItem.prev().size() == 0){
						$container.find(".autocomplete-item:first").addClass("active");
					}else{
						$activeItem = $activeItem.prev();
						$activeItem.addClass("active");
					}
					var dataValue = $activeItem.attr("data-value");
					if(dataValue && dataValue != ""){
						$input.val(dataValue);
						$input.focus();
					}
					break;
				default:
					
					var searchText = $input.val();
					var fieldName = view.paramName;
					app.getJsonData("/getCustomFieldAutoCompleteData", {searchText:searchText, fieldName:fieldName}).done(function(result) {
						if(result.searchText == $input.val()){
							$e.find(".autocomplete-container").empty();
							if(result.data.length > 0){
								$e.find(".autocomplete-container").removeClass("hide");
								for(var i = 0; i < result.data.length; i++){
									var $item = $(render("CustomFilterString-autocomplete-item",result.data[i]));
									$container.append($item);
								}
								$container.find(".autocomplete-item:first").addClass("active");
							}else{
								$e.find(".autocomplete-container").addClass("hide");
							}
						}
					});
			}
	}
	
	function addItem(value) {
		var view = this;
		var $e = view.$el;
		var $input = $e.find(".autocomplete-input-wrapper .valueInput");
		var $value = $e.find(".operationSelect .value");
		var operation = $value.attr("data-value");

		if (value != "") {
			$e.find(".autocomplete-container").addClass("hide").empty();
			$input.val("");
			$input.focus();

			var isExist = false;
			$e.find(".editContainer .selectedItems .selected-item").each(function() {
				var $item = $(this);
				if ($item.attr("data-oper") == operation && value == $item.attr("data-value")) {
					isExist = true;
				}
			});

			if (!isExist) {
				var $selectedItem = $(render("CustomFilterString-edit-item", {
					value : value,
					operation : operation
				}));
				$selectedItem.insertBefore($e.find(".editContainer .selectedItems .cb"));
			}
		}
		checkEmpty.call(view);
	}

	
	function checkEmpty(){
		var view = this;
		var $e = view.$el;
		var $selectedCon = $e.find(".editContainer .wrap-items");
		var size = $selectedCon.find(".selected-item").size();
		if(size > 0){
			$selectedCon.removeClass("empty");
		}else{
			$selectedCon.addClass("empty");
		}
	}
	
})(jQuery);
