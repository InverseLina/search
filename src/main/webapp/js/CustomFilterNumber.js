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
				var $btn = $(e.currentTarget);
				var oper = $e.find(".operation-item.active").attr("data-oper");
				var $input = $e.find("input[name='value']");
				var $input1 = $e.find("input[name='value1']");
				var validated = false;
				var operLabel;
				if(oper == "between"){
					validated = validateInput.call(view, $input) && validateInput.call(view, $input1);
					operLabel = "Between";
				}else if(oper == "lt"){
					validated = validateInput.call(view, $input1);
					operLabel = "Less than";
				}else if(oper == "gt"){
					validated = validateInput.call(view, $input);
					operLabel = "Greater than";
				}
				
				if(validated){
					$e.find(".viewContainer .operValue").text(operLabel).attr("data-oper", oper);
					$e.find(".viewContainer .resultValue").text($input.val()).attr("data-value", $input.val());
					$e.find(".viewContainer .resultValue1").text($input1.val()).attr("data-value", $input1.val());
					$e.trigger("DO_SEARCH");
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
	function validateInput($input){
		var view = this;
		var $e = view.$el;
		var value = $input.val();
		if(value == ""){
			$input.next().removeClass("hide");
			return false;
		}
		if(isNaN(value * 1)){
			$input.next().removeClass("hide");
			return false;
		}
		$e.find(".alert").addClass("hide");
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
	// --------- /Private Methods--------- //
})(jQuery);
