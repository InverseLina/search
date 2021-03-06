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
				selectOperation.call(view, value);
				$e.find(".operations").addClass("hide");
				checkChange.call(view);
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
				var $input = $(e.currentTarget);
				var $apply = $e.find(".btnApplyValue");
				e.preventDefault();
				changeAutoComplete.call(view, e.which);
				checkChange.call(view);
			},
			"click; .autocomplete-item":function(e){
				e.stopPropagation();
				var view = this;
				var $e = view.$el;
				var $item = $(e.currentTarget);
				var value = $item.attr("data-value");
				addItem.call(view, value);
			},
			"click; .clearStringItem":function(e){
				var view = this;
				var $e = view.$el;
				e.stopPropagation();
				var $item = $(e.currentTarget).closest(".selected-item");
				$item.remove();
				checkEmpty.call(view);
				if($e.closest(".HeaderPopup").size() == 0){
					applyValues.call(view,false);
				}
				checkChange.call(view);
			},
			"click; .btnApplyValue":function(e){
				var view = this;
				var $e = view.$el;
				var $input = $e.find(".autocomplete-input-wrapper .valueInput");
				var value = $input.val();
				addItem.call(view, value);
				applyValues.call(view,true);
			}
		},
		docEvents:{
			"click":function(e){
				var view = this;
				var $e = view.$el;
				var $target = $(e.target);
				if($e){
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
		// --------- Filter Common API--------- //
		showMode:function(mode){
			var view = this;
			var $e = view.$el;
			mode = mode || view.mode;
			view.mode = mode;
			
			if(mode == 'edit'){
				$e.find(".editContainer").removeClass("hide");
				$e.find(".viewContainer").addClass("hide");
				
				showEditValues.call(view, view.getValue());
				checkEmpty.call(view);
			}else{
				var $input = $e.find(".autocomplete-input-wrapper .valueInput");
				var value = $input.val();
				if(value != ""){
					addItem.call(view, value);
				}
				
				showViewValues.call(view, view.getValue());
				if($e.find(".viewContainer .value-containers .selected-item").size() > 0){
					$e.find(".viewContainer").removeClass("hide");
				}else{
					$e.find(".viewContainer").addClass("hide");
				}
				$e.find(".editContainer").addClass("hide");
			}
			checkChange.call(view);
		},
		
		getValue:function(){
			var view = this;
			var $e = view.$el;
			var valueObj = null;
			if($e.closest(".HeaderPopup").size() > 0){
				valueObj = app.ParamsControl.getHeaderCustomFilter(view.paramName);
			}else{
				valueObj = app.ParamsControl.getSideCustomAdvancedFilter(view.paramName);
			}
			return valueObj;
		},
		
		setValue:function(filter){
			var view = this;
			var $e = view.$el;
			
			if($e.closest(".HeaderPopup").size() > 0){
				if(filter){
					app.ParamsControl.saveHeaderCustomFilter(filter);
				}else{
					if(app.ParamsControl.getHeaderCustomFilter(view.paramName)){
						//clear
						app.ParamsControl.saveHeaderCustomFilter({field:view.paramName,conditions:null});
					}
				}
			}else{
				if(filter){
					app.ParamsControl.saveSideCustomAdvancedFilter(filter);
				}else{
					console.log(1);
					if(app.ParamsControl.getSideCustomAdvancedFilter(view.paramName)){
						//clear
						app.ParamsControl.saveSideCustomAdvancedFilter({field:view.paramName,conditions:null});
					}
				}
			}
			
			view.showMode();
		},
		
		clearFields:function(){
			var view = this;
			var $e = view.$el;
			view.setValue({field: view.paramName, conditions:null});
			view.showMode();
		}
		// --------- /Filter Common API--------- //
	});
	
	// --------- Private Methods--------- //
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
					activeNextItem.call(view, true);
					break;
				case _keys.UP:
					activeNextItem.call(view, false);
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
		if($e.closest(".HeaderPopup").size() == 0){
			applyValues.call(view,false);
		}
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
	
	function checkChange(){
		var view = this;
		var $e = view.$el;
		var oper = $e.find(".operationSelect .value").attr("data-value");
		var valueInput = $e.find("input[name='value']").val();
		var size = $e.find(".editContainer .wrap-items .selected-item").size();
		if(oper == "isnot" || valueInput != "" || size > 0){
			$e.trigger("HEADER_POPUP_TOGGLE_CUSTOM_CLEAR", true);
		}else{
			$e.trigger("HEADER_POPUP_TOGGLE_CUSTOM_CLEAR", false);
		}
	}
	
	function selectOperation(oper){
		var view = this;
		var $e = view.$el;
		var $value = $e.find(".operationSelect .value");
		var $operations = $e.find(".operations");
		var text = $operations.find(".operation-item[data-value='"+oper+"']").text();
		$value.attr("data-value", oper).find(".lbl").html(text);
	}
	
	function activeNextItem(oriention){
		var view = this;
		var $e = view.$el;
		var $container = $e.find(".autocomplete-container");
		var $activeItem = $container.find(".autocomplete-item.active");
		var $input = $e.find(".autocomplete-input-wrapper .valueInput");
		$container.find(".autocomplete-item").removeClass("active");
		var $next;
		if(oriention){
			$next = $activeItem.next();
		}else{
			$next = $activeItem.prev();
		}
		
		if ($activeItem.length == 0 || $next.length == 0) {
			$activeItem = $container.find(".autocomplete-item:first").addClass("active");
		} else {
			$activeItem = $next;
			$activeItem.addClass("active");
		}
		
		var dataValue = $activeItem.attr("data-value");
		if (dataValue && dataValue != "") {
			$input.val(dataValue);
			$input.focus();
		}
	}
	
	function applyValues(search){
		var view = this;
		var $e = view.$el;
		
		
		var valueObject = null;
		if ($e.find(".editContainer .selectedItems .selected-item").size() > 0) {
			valueObject = {
				field : view.paramName,
				conditions : {}
			};
			var isArray = [];
			var isnotArray = [];
			$e.find(".editContainer .selectedItems .selected-item").each(function() {
				var $item = $(this);
				var operation = $item.attr("data-oper");
				var value = $item.attr("data-value");
				if (operation == "is") {
					isArray.push(value);
				} else {
					isnotArray.push(value);
				}
			});

			if (isArray.length > 0) {
				valueObject.conditions["=="] = isArray;
			}

			if (isnotArray.length > 0) {
				valueObject.conditions["!="] = isnotArray;
			}
		}
		
		view.setValue(valueObject);
			
		if(search){
			$e.trigger("DO_SEARCH"); 
		}

	}
	
	function showEditValues(filter) {
		var view = this;
		var $e = view.$el;
		$e.find(".editContainer .selectedItems .selected-item").remove();
		if (filter && filter.conditions) {
			var isArray = filter.conditions["=="];
			if (isArray) {
				for (var i = 0; i < isArray.length; i++) {
					var value = isArray[i];
					var $selectedItem = $(render("CustomFilterString-edit-item", {
						value : value,
						operation : "is"
					}));
					$selectedItem.insertBefore($e.find(".editContainer .selectedItems .cb"));
				}
			}
			var isnotArray = filter.conditions["!="];
			if (isnotArray) {
				for (var i = 0; i < isnotArray.length; i++) {
					var value = isnotArray[i];
					var $selectedItem = $(render("CustomFilterString-edit-item", {
						value : value,
						operation : "isnot"
					}));
					$selectedItem.insertBefore($e.find(".editContainer .selectedItems .cb"));
				}
			}
		}else{
			$e.find(".autocomplete-input-wrapper .valueInput").val("");
			$e.find(".editContainer .selectedItems .selected-item").remove();
			selectOperation.call(view, "is");
			checkEmpty.call(view);
		}
	}

	function showViewValues(filter) {
		var view = this;
		var $e = view.$el;
		$e.find(".viewContainer .selectedItems .selected-item").remove();
		if (filter && filter.conditions) {
			var isArray = filter.conditions["=="];
			if (isArray) {
				for (var i = 0; i < isArray.length; i++) {
					var value = isArray[i];
					var $selectedItem = $(render("CustomFilterString-view-item", {
						value : value,
						operation : "is"
					}));
					$selectedItem.insertBefore($e.find(".viewContainer .selectedItems .cb"));
				}
			}
			var isnotArray = filter.conditions["!="];
			if (isnotArray) {
				for (var i = 0; i < isnotArray.length; i++) {
					var value = isnotArray[i];
					var $selectedItem = $(render("CustomFilterString-view-item", {
						value : value,
						operation : "isnot"
					}));
					$selectedItem.insertBefore($e.find(".viewContainer .selectedItems .cb"));
				}
			}
		}else{
			$e.find(".viewContainer .selectedItems .selected-item").remove();
			checkEmpty.call(view);
		}
	}

	// --------- /Private Methods--------- //
})(jQuery);
