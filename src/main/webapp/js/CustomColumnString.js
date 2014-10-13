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

	
	brite.registerView("CustomColumnString", {
		parent : ".filter-item-container"
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			var view = this;
			data = data || {};
			view.paramName = data.name;
			view.color = data.color;
			return render("CustomColumnString");
		},

		postDisplay : function(data) {
			var view = this;
			var $e = view.$el;
			var $input = $e.find(".autocomplete-input-wrapper .valueInput");
			$input.focus();
			changeAutoComplete.call(view);
			applyValues.call(view);
			showColor.call(view);
		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"click; .operatorBtnGroups .btn:not(.active)":function(e){
				var view = this;
				var $e = view.$el;
				var $btn = $(e.currentTarget);
				var $operatorBtnGroups = $btn.closest(".operatorBtnGroups");
				$operatorBtnGroups.find(".btn").toggleClass("active");
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
				e.preventDefault();
				changeAutoComplete.call(view, e.which);
			},
			"click; .autocomplete-item":function(e){
				e.stopPropagation();
				var view = this;
				var $e = view.$el;
				var $item = $(e.currentTarget);
				$item.closest(".autoCompleteList").find(".autocomplete-item").removeClass("active").css("backgroundColor","");
				$item.addClass("active").css("backgroundColor",view.color);
				var value = $item.attr("data-value");
				var oper = $e.find(".operatorBtnGroups .btn.active").attr("data-value");
				addItem.call(view, value, oper);
				applyValues.call(view);
			},
			"click; .clearStringItem":function(e){
				var view = this;
				var $e = view.$el;
				e.stopPropagation();
				var $item = $(e.currentTarget).closest(".selected-item");
				removeItem.call(view, $item.attr("data-value"), $item.attr("data-oper"));
				applyValues.call(view);
			},
			"mouseover; .autocomplete-item" : function(event) {
				var view = this;
				view.$el.find(".autocomplete-item").removeClass("active").css("backgroundColor","");
				$(event.currentTarget).addClass("active").css("backgroundColor",view.color);
			},
		},
		// --------- /Events--------- //
	});
	
	// --------- Private Methods--------- //
	function changeAutoComplete(keyCode){
		var view = this;
		var $e = view.$el;
		var $container = $e.find(".autoCompleteList");
		var $activeItem = $container.find(".autocomplete-item.active");
		var $input = $e.find(".autocomplete-input-wrapper .valueInput");
		var oper = $e.find(".operatorBtnGroups .btn").attr("data-value");
		switch (keyCode) {
				case _keys.ENTER:
				case _keys.TAB:
					var value = $input.val();
					if ($activeItem.length > 0) {
						value = $activeItem.attr("data-value");
					}
			
					addItem.call(view, value, oper);
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
							$e.find(".autoCompleteList").empty();
							for (var i = 0; i < result.data.length; i++) {
								var $item = $(render("CustomColumnString-autocomplete-item", result.data[i]));
								$container.append($item);
							}
							$container.find(".autocomplete-item:first").addClass("active").css("backgroundColor",view.color); 
						}
					});
			}
	}
	
	function addItem(value, oper) {
		var view = this;
		var $e = view.$el;
		var $input = $e.find(".autocomplete-input-wrapper .valueInput");

		if (value != "") {
			$input.val("");
			$input.focus();
			var filter = app.ParamsControl.getHeaderCustomColumnFilter(view.paramName);
			var isArray = filter["=="] || [];
			var isNotArray = filter["!="] || [];
			if(oper == "is"){
				var exist = false;
				for(var i = 0; i < isArray.length; i++){
					if(isArray[i] == value){
						exist = true;
						break;
					}
				}
				
				if(!exist){
					isArray.push(value);
				}
			}else{
				var exist = false;
				for(var i = 0; i < isNotArray.length; i++){
					if(isNotArray[i] == value){
						exist = true;
						break;
					}
				}
				
				if(!exist){
					console.log();
					isNotArray.push(value);
				}
			}
			var data = {};
			if(isArray.length > 0){
				data["=="] = isArray;
			}
			if(isNotArray.length > 0){
				data["!="] = isNotArray;
			}
			app.ParamsControl.saveHeaderCustomColumnFilter(view.paramName, data);
			$e.trigger("DO_SEARCH"); 
		}
	}
	
	function removeItem(value, oper) {
		var view = this;
		var $e = view.$el;
		var $input = $e.find(".autocomplete-input-wrapper .valueInput");

		if (value != "") {
			$input.val("");
			$input.focus();
			
			var filter = app.ParamsControl.getHeaderCustomColumnFilter(view.paramName);
			var isArray = filter["=="] || [];
			var isNotArray = filter["!="] || [];
			if(oper == "is"){
				var exist = false;
				var index = -1;
				for(var i = 0; i < isArray.length; i++){
					if(isArray[i] == value){
						index = i;
						break;
					}
				}
				
				if(index >= 0){
					isArray.splice(index, 1);
				}
			}else{
				var index = -1;
				for(var i = 0; i < isNotArray.length; i++){
					if(isNotArray[i] == value){
						index = i;
						break;
					}
				}
				
				if(index >= 0){
					isNotArray.splice(index, 1);
				}
			}
			var data = {};
			if(isArray.length > 0){
				data["=="] = isArray;
			}
			if(isNotArray.length > 0){
				data["!="] = isNotArray;
			}
			app.ParamsControl.saveHeaderCustomColumnFilter(view.paramName, data);
			$e.trigger("DO_SEARCH"); 
		}
	}

	function activeNextItem(oriention){
		var view = this;
		var $e = view.$el;
		var $container = $e.find(".autoCompleteList");
		var $activeItem = $container.find(".autocomplete-item.active");
		var $input = $e.find(".autocomplete-input-wrapper .valueInput");
		$container.find(".autocomplete-item").removeClass("active").css("backgroundColor","");
		var $next;
		if(oriention){
			$next = $activeItem.next();
		}else{
			$next = $activeItem.prev();
		}
		
		if ($activeItem.length == 0 || $next.length == 0) {
			$activeItem = $container.find(".autocomplete-item:first").addClass("active").css("backgroundColor",view.color);
		} else {
			$activeItem = $next;
			$activeItem.addClass("active").css("backgroundColor",view.color);
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
		var filter = app.ParamsControl.getHeaderCustomColumnFilter(view.paramName);
		
		$e.find(".selectedItems .selected-item").remove();
		var filter = app.ParamsControl.getHeaderCustomColumnFilter(view.paramName);
		var isArray = filter["=="] || [];
		var isNotArray = filter["!="] || [];
		
		for (var i = 0; i < isArray.length; i++) {
			var obj = {
				operation:"is",
				value: isArray[i]
			}
			
			var $html = $(render("CustomColumnString-selected-item", obj));
			$html.insertBefore($e.find(".selectedItems .separateLine"));
		}
		
		for (var i = 0; i < isNotArray.length; i++) {
			var obj = {
				operation:"isnot",
				value: isNotArray[i]
			}
			
			var $html = $(render("CustomColumnString-selected-item", obj));
			$html.insertBefore($e.find(".selectedItems .separateLine"));
		}
		
		view.$el.find(".selectedItems .selected-item").css("backgroundColor", view.color);
	}
	
	function showColor(){
		var view = this;
		view.$el.find(".toggleBtnsContainer .btn").css("backgroundColor", view.color);
	}
	
	// --------- /Private Methods--------- //
})(jQuery);
