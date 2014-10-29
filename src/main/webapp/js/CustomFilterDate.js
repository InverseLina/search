/**
 * View: CustomFilterDate
 *
 *
 *
 */
(function($) {
	brite.registerView("CustomFilterDate", {
		parent : ".filter-item-container"
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			data = data || {};
			var view = this;
			view.paramName = data.name;
			return render("CustomFilterDate");
		},

		postDisplay : function(data) {
		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"click; .operation-item" : function(e){
				var view = this;
				var $e = this.$el;
				var $li = $(e.currentTarget);
				var oper = $li.attr("data-oper");
				showEditValues.call(view, view.getValue());
				showByOper.call(view, oper);
				checkChange.call(view);
			},
			"click; .btnApplyValue" : function(e){
				var view = this;
				var $e = this.$el;
				applyValue.call(view, false, true);
			},
			"click; .date-input-wrapper .fa-calendar" : function(e){
				var view = this;
				var $e = this.$el;
				var $icon = $(e.currentTarget);
				var $inputWrapper = $icon.closest(".date-input-wrapper");
				var $input = $inputWrapper.find("input");
				var value = $input.val();
				brite.display("DatePicker",$inputWrapper,{target:$input, value:value}).done(function(datePickerView){
					datePickerView.onSelect(function(dateValue){
						$input.val(dateValue);
						applyValue.call(view, true);
						checkChange.call(view);
					});
				});
			},
			"keyup; .valueInput" : function(e){
				var view = this;
				var $e = this.$el;
				checkChange.call(view);
				if(e.which == 13){
					applyValue.call(view);
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
				
				//reset value
				showEditValues.call(view, view.getValue());
				
			}else{
				applyValue.call(view, true);
				var valueObj = view.getValue();
				showViewValues.call(view, valueObj);
				oper = getOper.call(view, valueObj);
				if (oper != "") {
					$e.find(".viewContainer").removeClass("hide");
				} else {
					$e.find(".viewContainer").addClass("hide");
				}
				$e.find(".editContainer").addClass("hide"); 
			}
			checkChange.call(view);
		},
		
		getValue:function(){
			var view = this;
			var $e = view.$el;
			var valueObject;
			if ($e.closest(".HeaderPopup").size() > 0) {
				valueObject = app.ParamsControl.getHeaderCustomFilter(view.paramName);
			} else {
				valueObject = app.ParamsControl.getSideCustomAdvancedFilter(view.paramName);
			}

			return valueObject;
		},
		
		setValue:function(filter){
			var view = this;
			var $e = view.$el;
			if(filter){
				if($e.closest(".HeaderPopup").size() > 0){
					app.ParamsControl.saveHeaderCustomFilter(filter);
				}else{
					app.ParamsControl.saveSideCustomAdvancedFilter(filter);
				}
			}
		},
		
		clearFields:function(){
			var view = this;
			var $e = view.$el;
			$e.find(".viewContainer .operValue").attr("data-oper","");
			$e.find(".viewContainer .resultValue").attr("data-value", "").html();
			$e.find(".viewContainer .resultValue1").attr("data-value", "").html();
			$e.find("input[name='value']").val("");
			$e.find("input[name='value1']").val("");
			view.setValue({field: view.paramName, conditions:null});
			view.showMode();
		}
		// --------- /Filter Common API--------- //
	});
	
	// --------- Private Methods--------- //
	function checkChange(){
		var view = this;
		var $e = view.$el;
		var oper = $e.find(".editContainer .operation-item.active").attr("data-oper");
		var value = $e.find("input[name='value']").val();
		var value1 = $e.find("input[name='value1']").val();
		if(oper != "between" || value != "" || value1 != ""){
			$e.trigger("HEADER_POPUP_TOGGLE_CUSTOM_CLEAR", true);
		}else{
			$e.trigger("HEADER_POPUP_TOGGLE_CUSTOM_CLEAR", false);
		}
	}
	
	function validateInput($input, showMessage){
		var view = this;
		var $e = view.$el;
		var value = $input.val();
		var $alert = $input.closest(".date-input-wrapper").next();
		if(value == ""){
			if(showMessage){
				$alert.removeClass("hide");
			}
			return false;
		}
		
		if(!/^\d\d\/\d\d\/\d\d\d\d$/.test(value)){
			if(showMessage){
				$alert.removeClass("hide");
			}
			return false;
		}
		var dateValue = Date.parse(value);
		if(isNaN(dateValue) || dateValue <= 0){
			if(showMessage){
				$alert.removeClass("hide");
			}
			return false;
		}
		$e.find(".alert").addClass("hide");
		return true;
	}
	
	function validateDateRange($input, $input1, showMessage){
		var view = this;
		var $e = view.$el;
		var value = $input.val();
		var value1 = $input1.val();
		var $alert = $e.find(".rangeError"); 
		var validated = validateInput.call(view, $input, showMessage) && validateInput.call(view, $input1, showMessage);
		if(validated){
			var dateValue = Date.parse(value);
			var dateValue1 = Date.parse(value1);
			if(dateValue > dateValue1){
				if(showMessage){
					$alert.removeClass("hide");	
				}
				return false;
			}
		}else{
			return false;
		}
		$alert.addClass(".hide");
		return true;
	}
	
	function showByOper(oper){
		var view = this;
		var $e = view.$el;
		var $li = $e.find(".operation-item[data-oper='"+oper+"']");
		var $group = $li.closest(".operation");
		$group.find(".operation-item").removeClass("active");
		$li.addClass("active");

		if (oper == "between") {
			$e.find("input[name='value']").closest(".date-input-wrapper").removeClass("hide");
			$e.find("input[name='value1']").closest(".date-input-wrapper").removeClass("hide");
		} else if (oper == "after") {
			$e.find("input[name='value']").closest(".date-input-wrapper").removeClass("hide");
			$e.find("input[name='value1']").closest(".date-input-wrapper").addClass("hide");
		} else if (oper == "before") {
			$e.find("input[name='value']").closest(".date-input-wrapper").addClass("hide");
			$e.find("input[name='value1']").closest(".date-input-wrapper").removeClass("hide");
		}
		$e.find(".alert").addClass("hide"); 
	}
	
	function getOper(filter){
		var view = this;
		var $e = view.$el;
		var oper = "";
		if (filter && filter.conditions) {
			var greaterValue = filter.conditions[">="];
			var lessValue = filter.conditions["<="];
			if (greaterValue && lessValue) {
				oper = "between";
			} else if (greaterValue) {
				oper = "after";
			} else {
				oper = "before";
			}
		}
		return oper;
	}
	
	function applyValue(force, search){
		var view = this;
		var $e = this.$el;
		var oper = $e.find(".operation-item.active").attr("data-oper");
		var $input = $e.find("input[name='value']");
		var $input1 = $e.find("input[name='value1']");
		var validated = false;
		var showMessage = typeof force == "undefined" || !force ? true : false;
		if (oper == "between") {
			validated = validateDateRange.call(view, $input, $input1, showMessage);
		} else if (oper == "before") {
			validated = validateInput.call(view, $input1, showMessage);
		} else if (oper == "after") {
			validated = validateInput.call(view, $input, showMessage);
		}

		if (validated) {
			var valueObject = {field:view.paramName, conditions:{}};
			var resultValue = $input.val();
			var resultValue1 = $input1.val();
			if (oper == "between") {
				valueObject.conditions[">="] = resultValue;
				valueObject.conditions["<="] = resultValue1;
			} else if (oper == "after") {
				valueObject.conditions[">="] = resultValue;
			} else if (oper == "before") {
				valueObject.conditions["<="] = resultValue1;
			}else{
				valueObject = null;
			}
			view.setValue(valueObject);
			
			if (search) {
				$e.trigger("DO_SEARCH");
			}

		}
		
	}
	
	function showEditValues(filter){
		var view = this;
		var $e = view.$el;
		var $oper = $e.find(".operation");
		var $input = $e.find("input[name='value']");
		var $input1 = $e.find("input[name='value1']");
		

		if (filter && filter.conditions) {
			var greaterValue = filter.conditions[">="];
			if (greaterValue) {
				$input.val(greaterValue);
			}

			var lessValue = filter.conditions["<="];
			if (lessValue) {
				$input1.val(lessValue);
			}
			
			if (greaterValue && lessValue) {
				showByOper.call(view, "between");
			} else if (greaterValue) {
				showByOper.call(view, "after");
				$input1.val("");
			} else {
				showByOper.call(view, "before");
				$input.val("");
			}
		}else{
			showByOper.call(view, "between");
			$input.val("");
			$input1.val("");
		}
	}
	
	function showViewValues(filter){
		var view = this;
		var $e = view.$el;
		if (filter && filter.conditions) {
			var greaterValue = filter.conditions[">="];
			if (greaterValue) {
				$e.find(".viewContainer .resultValue").attr("data-value", greaterValue).text(greaterValue);
			}

			var lessValue = filter.conditions["<="];
			if (lessValue) {
				$e.find(".viewContainer .resultValue1").attr("data-value", lessValue).text(lessValue);
			}
			
			if (greaterValue && lessValue) {
				$e.find(".viewContainer .operValue").attr("data-oper", "between").text("Between");
				$e.find(".viewContainer .resultValue, .viewContainer .resultValue1").removeClass("hide");
			} else if (greaterValue) {
				$e.find(".viewContainer .operValue").attr("data-oper", "after").text("After");
				$e.find(".viewContainer .resultValue1").addClass("hide");
				$e.find(".viewContainer .resultValue").removeClass("hide");
			} else {
				$e.find(".viewContainer .operValue").attr("data-oper", "before").text("Before");
				$e.find(".viewContainer .resultValue").addClass("hide");
				$e.find(".viewContainer .resultValue1").removeClass("hide");
			}
		}else{
			$e.find(".viewContainer .operValue").attr("data-oper", "").text("");
			$e.find(".viewContainer .resultValue").addClass("hide").attr("data-value", "").text("");
			$e.find(".viewContainer .resultValue1").addClass("hide").attr("data-value", "").text("");
		}
		
	}
	
	// --------- /Private Methods--------- //
})(jQuery);
