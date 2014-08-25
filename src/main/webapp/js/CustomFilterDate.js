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
				showByOper.call(view,oper);
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
					operLabel = "between";
				}else if(oper == "after" || oper == "before"){
					validated = validateInput.call(view, $input);
					operLabel = oper == "after" ? "After" : "Before";
				}
				
				if(validated){
					$e.find(".viewContainer .operValue").text(operLabel).attr("data-oper", oper);
					$e.find(".viewContainer .resultValue").text($input.val()).attr("data-value", $input.val());
					$e.find(".viewContainer .resultValue1").text($input1.val()).attr("data-value", $input1.val());
					var $filterItem = $e.closest(".filter-item");
					$e.trigger("MODE_CHANGE",{filterItem:$filterItem});
				}
			},
			"click; .date-input-wrapper .fa-calendar" : function(e){
				var view = this;
				var $e = this.$el;
				var $icon = $(e.currentTarget);
				var $inputWrapper = $icon.closest(".date-input-wrapper");
				var $input = $inputWrapper.find("input");
				var value = $input.data("date");
				brite.display("DatePicker",$inputWrapper,{target:$input, value:value});
			}
		},
		showMode:function(mode){
			var view = this;
			var $e = view.$el;
			var oper = $e.find(".viewContainer .operValue").attr("data-oper");
			if(mode == 'edit'){
				$e.find(".editContainer").removeClass("hide");
				$e.find(".viewContainer").addClass("hide");
				
				//reset value
				var oper = $e.find(".viewContainer .operValue").attr("data-oper");
				var $input = $e.find("input[name='value']");
				var $input1 = $e.find("input[name='value1']");
				if(oper && oper != null){
					$e.find(".operation-item[data-oper='"+oper+"']").attr("data-oper");
					var resultValue = $e.find(".viewContainer .resultValue").attr("data-value");
					var resultValue1 = $e.find(".viewContainer .resultValue1").attr("data-value");
					if (oper == "between") {
						$input.val(resultValue);
						$input1.val(resultValue1);
					} else if (oper == "after") {
						$input.val(resultValue);
					} else if (oper == "before") {
						$input.val(resultValue);
					}
					showByOper.call(view,oper);
				}else{
					$input.val("");
					$input1.val("");
					showByOper.call(view,"between");
				}
				
			}else{
				if(oper != ""){
					$e.find(".viewContainer").removeClass("hide");
					if(oper == "between"){
						$e.find(".viewContainer .resultValue1").removeClass("hide");
					}else if(oper == "after"){
						$e.find(".viewContainer .resultValue1").addClass("hide");
					}else if(oper == "before"){
						$e.find(".viewContainer .resultValue1").addClass("hide");
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
			var resultValue = $e.find(".viewContainer .resultValue").attr("data-value");
			var resultValue1 = $e.find(".viewContainer .resultValue1").attr("data-value");
			if (oper == "between") {
				valueObject.conditions[">="] = resultValue;
				valueObject.conditions["<="] = resultValue1;
			} else if (oper == "after") {
				valueObject.conditions[">="] = resultValue;
			} else if (oper == "before") {
				valueObject.conditions["<="] = resultValue;
			}else{
				return null;
			}
			return valueObject;
		}
		// --------- /Events--------- //
	});
	
	function validateInput($input){
		var view = this;
		var $e = view.$el;
		var value = $input.val();
		var $alert = $input.closest(".date-input-wrapper").next();
		if(value == ""){
			$alert.removeClass("hide");
			return false;
		}
		if(!/^\d\d\/\d\d\/\d\d\d\d$/.test(value)){
			$alert.removeClass("hide");
			return false;
		}
		var dateValue = Date.parse(value);
		if(isNaN(dateValue) || dateValue <= 0){
			$alert.removeClass("hide");
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
			$e.find("input[name='value1']").closest(".date-input-wrapper").removeClass("hide");
		} else if (oper == "after" || oper == "before") {
			$e.find("input[name='value1']").closest(".date-input-wrapper").addClass("hide");
		}
		$e.find(".alert").addClass("hide"); 
	}
})(jQuery);
