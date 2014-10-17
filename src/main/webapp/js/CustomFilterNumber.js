/**
 * View: CustomFilterNumber
 *
 *
 *
 */
(function($) {
	brite.registerView("CustomFilterNumber", {
		parent : ".filter-item-container"
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			data = data || {};
			var view = this;
			view.paramName = data.name;
			return render("CustomFilterNumber");
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
			},
			"click; .btnApplyValue" : function(e){
				var view = this;
				var $e = this.$el;
				applyValue.call(view, false, true);
			},
			"keyup; .valueInput" : function(e){
				var view = this;
				var $e = this.$el;
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

			var oper = $e.find(".viewContainer .operValue").attr("data-oper");
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
		},
		
		getValue:function(){
			var view = this;
			var $e = view.$el;
			var valueObject;
			if ($e.closest(".HeaderPopup").size() > 0) {
				valueObject = app.ParamsControl.getHeaderCustomFilter(view.paramName);
			} else {
				valueObject = app.ParamsControl.getHeaderCustomAdvancedFilter(view.paramName);
			}

			return valueObject;
		},
		
		setValue : function(filter){
			var view = this;
			var $e = view.$el;
					
			if(filter){
				if($e.closest(".HeaderPopup").size() > 0){
					app.ParamsControl.saveHeaderCustomFilter(filter);
				}else{
					app.ParamsControl.saveHeaderCustomAdvancedFilter(filter);
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
	function validateInput($input, showMessage){
		var view = this;
		var $e = view.$el;
		var value = $input.val();
		if(value == ""){
			if(showMessage){
				$input.next().removeClass("hide");
			}
			return false;
		}
		if(isNaN(value * 1)){
			if(showMessage){
				$input.next().removeClass("hide");
			}
			return false;
		}
		$e.find(".alert").addClass("hide");
		return true;
	}
	
	function validateNumberRange($input, $input1, showMessage){
		var view = this;
		var $e = view.$el;
		var value = $input.val();
		var value1 = $input1.val();
		var $alert = $e.find(".rangeError");
		var validated = validateInput.call(view, $input, showMessage) && validateInput.call(view, $input1, showMessage);
		if(validated){
			value = value * 1;
			value1 = value1 * 1;
			if(value > value1){
				if(showMessage){
					$alert.removeClass("hide");
				}
				return false;
			}
		}else{
			return false;
		}
		$alert.addClass("hide");
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
			$e.find("input").removeClass("hide");
		} else if (oper == "lt") {
			$e.find("input[name='value1']").removeClass("hide");
			$e.find("input[name='value']").addClass("hide");
		} else if (oper == "gt") {
			$e.find("input[name='value1']").addClass("hide");
			$e.find("input[name='value']").removeClass("hide");
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
				oper = "gt";
			} else {
				oper = "lt";
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
			validated = validateNumberRange.call(view, $input, $input1, showMessage);
		} else if (oper == "lt") {
			validated = validateInput.call(view, $input1, showMessage);
		} else if (oper == "gt") {
			validated = validateInput.call(view, $input, showMessage);
		}

		if (validated) {
			var valueObject = {field:view.paramName, conditions:{}};
			var resultValue = $input.val() * 1;
			var resultValue1 = $input1.val() * 1;
			if (oper == "between") {
				valueObject.conditions[">="] = resultValue;
				valueObject.conditions["<="] = resultValue1;
			} else if (oper == "gt") {
				valueObject.conditions[">="] = resultValue;
			} else if (oper == "lt") {
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
				showByOper.call(view, "gt");
				$input1.val("");
			} else {
				showByOper.call(view, "lt");
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
				$e.find(".viewContainer .resultValue").attr("data-value", greaterValue).text(formatNumber(greaterValue));
			}

			var lessValue = filter.conditions["<="];
			if (lessValue) {
				$e.find(".viewContainer .resultValue1").attr("data-value", lessValue).text(formatNumber(lessValue));
			}
			
			if (greaterValue && lessValue) {
				$e.find(".viewContainer .operValue").attr("data-oper", "between").text("Between");
				$e.find(".viewContainer .resultValue, .viewContainer .resultValue1").removeClass("hide");
			} else if (greaterValue) {
				$e.find(".viewContainer .operValue").attr("data-oper", "gt").text("Greater than");
				$e.find(".viewContainer .resultValue1").addClass("hide");
				$e.find(".viewContainer .resultValue").removeClass("hide");
			} else {
				$e.find(".viewContainer .operValue").attr("data-oper", "lt").text("Less than");
				$e.find(".viewContainer .resultValue").addClass("hide");
				$e.find(".viewContainer .resultValue1").removeClass("hide");
			}
		}else{
			$e.find(".viewContainer .operValue").attr("data-oper", "").text("");
			$e.find(".viewContainer .resultValue").addClass("hide").attr("data-value", "").text("");
			$e.find(".viewContainer .resultValue1").addClass("hide").attr("data-value", "").text("");
		}
		
	}
	
	
	function formatNumber(val){
		var restult = val * 1 + "";
		if(!isNaN(restult)){
			var nStr = restult + "";
			var x = nStr.split('.');
			var x1 = x[0];
			var x2 = x.length > 1 ? '.' + x[1] : "";
			var rgx = /(\d+)(\d{3})/;
			while(rgx.test(x1)){
				x1 = x1.replace(rgx, '$1' + ',' + '$2');
			}
			return x1 + x2;
		}
		return result;		
	}
	// --------- /Private Methods--------- //
})(jQuery);
