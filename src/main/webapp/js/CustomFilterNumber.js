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
				showEditValue.call(view, oper);
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
				var oper = $e.find(".viewContainer .operValue").attr("data-oper");
				if(oper && oper != null){
					showEditValue.call(view, oper);
					showByOper.call(view,oper);
				}else{
					showByOper.call(view,"between");
				}
				
			}else{
				applyValue.call(view, true);
				oper = $e.find(".viewContainer .operValue").attr("data-oper");
				if(oper != ""){
					$e.find(".viewContainer").removeClass("hide");
					if(oper == "between"){
						$e.find(".viewContainer .resultValue, .viewContainer .resultValue1").removeClass("hide");
					}else if(oper == "gt"){
						$e.find(".viewContainer .resultValue1").addClass("hide");
						$e.find(".viewContainer .resultValue").removeClass("hide");
					}else if(oper == "lt"){
						$e.find(".viewContainer .resultValue").addClass("hide");
						$e.find(".viewContainer .resultValue1").removeClass("hide");
					}
				}else{
					$e.find(".viewContainer").addClass("hide");
				}
				$e.find(".editContainer").addClass("hide");
			}
		},
		
		getValue:function(){
			var view = this;
			var $e = view.$el;
			var valueObject = {field:view.paramName, conditions:{}};
			var oper = $e.find(".viewContainer .operValue").attr("data-oper");
			var resultValue = $e.find(".viewContainer .resultValue").attr("data-value") * 1;
			var resultValue1 = $e.find(".viewContainer .resultValue1").attr("data-value") * 1;
			if (oper == "between") {
				valueObject.conditions[">="] = resultValue;
				valueObject.conditions["<="] = resultValue1;
			} else if (oper == "gt") {
				valueObject.conditions[">="] = resultValue;
			} else if (oper == "lt") {
				valueObject.conditions["<="] = resultValue1;
			}else{
				return null;
			}
			
			return valueObject;
		},
		
		clearFields:function(){
			var view = this;
			var $e = view.$el;
			$e.find(".viewContainer .operValue").attr("data-oper","");
			$e.find(".viewContainer .resultValue").attr("data-value", "").html();
			$e.find(".viewContainer .resultValue1").attr("data-value", "").html();
			$e.find("input[name='value']").val("");
			$e.find("input[name='value1']").val("");
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
	
	function showEditValue(oper){
		var view = this;
		var $e = view.$el;
		$e.find(".operation-item[data-oper='" + oper + "']").attr("data-oper");
		var resultValue = $e.find(".viewContainer .resultValue").attr("data-value");
		var resultValue1 = $e.find(".viewContainer .resultValue1").attr("data-value");
		var $input = $e.find("input[name='value']");
		var $input1 = $e.find("input[name='value1']");
		$input.val("");
		$input1.val("");
		if (oper == "between") {
			$input.val(resultValue);
			$input1.val(resultValue1);
		} else if (oper == "gt") {
			$input.val(resultValue);
		} else if (oper == "lt") {
			$input1.val(resultValue1);
		}
	}
	
	
	function applyValue(force, search){
		var view = this;
		var $e = this.$el;
		var oper = $e.find(".operation-item.active").attr("data-oper");
		var $input = $e.find("input[name='value']");
		var $input1 = $e.find("input[name='value1']");
		var validated = false;
		var operLabel;
		var showMessage = typeof force == "undefined" || !force ? true : false;
		if (oper == "between") {
			validated = validateNumberRange.call(view, $input, $input1, showMessage);
			operLabel = "Between";
		} else if (oper == "lt") {
			validated = validateInput.call(view, $input1, showMessage);
			operLabel = "Less than";
		} else if (oper == "gt") {
			validated = validateInput.call(view, $input, showMessage);
			operLabel = "Greater than";
		}

		if (validated) {
			$e.find(".viewContainer .operValue").text(operLabel).attr("data-oper", oper);
			$e.find(".viewContainer .resultValue").text(formatNumber($input.val())).attr("data-value", $input.val());
			$e.find(".viewContainer .resultValue1").text(formatNumber($input1.val())).attr("data-value", $input1.val());
			if (search) {
				$e.trigger("DO_SEARCH");
			}

		}

	}
	
	
	function formatNumber(val){
		var restult = val;
		if(!isNaN(val)){
			var nStr = val + "";
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
