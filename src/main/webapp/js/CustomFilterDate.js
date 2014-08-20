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
				var $group = $li.closest(".operation");
				$group.find(".operation-item").removeClass("active");
				$li.addClass("active");
				
				var oper = $li.attr("data-oper");
				if(oper == "between"){
					$e.find("input[name='value1']").closest(".date-input-wrapper").removeClass("hide");
				}else if(oper == "after"){
					$e.find("input[name='value1']").closest(".date-input-wrapper").addClass("hide");
				}else if(oper == "before"){
					$e.find("input[name='value1']").closest(".date-input-wrapper").addClass("hide");
				}
				$e.find(".alert").addClass("hide");
			},
			"click; .btnApply" : function(e){
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
				}
			},
			"click; .date-input-wrapper .icon-fa" : function(e){
				var view = this;
				var $e = this.$el;
				var $icon = $(e.currentTarget);
				var $inputWrapper = $icon.closest(".date-input-wrapper");
				var $input = $inputWrapper.find("input");
				brite.display("DatePicker",$inputWrapper,{target:$input});
			}
		},
		showMode:function(mode){
			var view = this;
			var $e = view.$el;
			var oper = $e.find(".viewContainer .operValue").attr("data-oper");
			if(mode == 'edit'){
				$e.find(".editContainer").removeClass("hide");
				$e.find(".viewContainer").addClass("hide");
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
			console.log(valueObject);
			return valueObject;
		}
		// --------- /Events--------- //
	});
	
	function validateInput($input){
		var view = this;
		var $e = view.$el;
		var value = $input.val();
		if(value == ""){
			$input.next().removeClass("hide");
			return false;
		}
		if(!/^\d\d\/\d\d\/\d\d\d\d$/.test(value)){
			$input.next().removeClass("hide");
			return false;
		}
		var dateValue = Date.parse(value);
		if(isNaN(dateValue) || dateValue <= 0){
			$input.next().removeClass("hide");
			return false;
		}
		$e.find(".alert").addClass("hide");
		return true;
	}
})(jQuery);
