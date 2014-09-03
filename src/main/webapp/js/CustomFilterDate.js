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
				showEditValue.call(view,oper);
				showByOper.call(view,oper);
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
					});
				});
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
					showByOper.call(view, oper);
				}else{
					showByOper.call(view,"between");
				}
				
			}else{
				applyValue.call(view, true);
				oper = $e.find(".viewContainer .operValue").attr("data-oper");
				if(oper != ""){
					$e.find(".viewContainer").removeClass("hide");
					if(oper == "between"){
						$e.find(".viewContainer .resultValue1, .viewContainer .resultValue").removeClass("hide");
					}else if(oper == "after"){
						$e.find(".viewContainer .resultValue").removeClass("hide");
						$e.find(".viewContainer .resultValue1").addClass("hide");
					}else if(oper == "before"){
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
			var resultValue = $e.find(".viewContainer .resultValue").attr("data-value");
			var resultValue1 = $e.find(".viewContainer .resultValue1").attr("data-value");
			if (oper == "between") {
				valueObject.conditions[">="] = resultValue;
				valueObject.conditions["<="] = resultValue1;
			} else if (oper == "after") {
				valueObject.conditions[">="] = resultValue;
			} else if (oper == "before") {
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
		} else if (oper == "after") {
			$input.val(resultValue);
		} else if (oper == "before") {
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
			validated = validateInput.call(view, $input, showMessage) && validateInput.call(view, $input1, showMessage);
			operLabel = "Between";
		} else if (oper == "after") {
			validated = validateInput.call(view, $input, showMessage);
			operLabel = "After";
		} else if (oper == "before") {
			validated = validateInput.call(view, $input1, showMessage);
			operLabel = "Before";
		}

		if (validated) {
			$e.find(".viewContainer .operValue").text(operLabel).attr("data-oper", oper);
			$e.find(".viewContainer .resultValue").text($input.val()).attr("data-value", $input.val());
			$e.find(".viewContainer .resultValue1").text($input1.val()).attr("data-value", $input1.val());
			if (search) {
				$e.trigger("DO_SEARCH");
			}

		}
		
	}
	// --------- /Private Methods--------- //
})(jQuery);
