/**
 * View: CustomFilterBoolean
 *
 *
 *
 */
(function($) {
	brite.registerView("CustomFilterBoolean", {
		parent : ".filter-item-container"
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			var view = this;
			data = data || {};
			view.paramName = data.name;
			return render("CustomFilterBoolean");
		},

		postDisplay : function(data) {
		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"click;.btn-group .btn:not(.active)" : function(e){
				var view = this;
				var $e = view.$el;
				var $btnGroup = $(e.currentTarget).closest(".btn-group");
				$btnGroup.find(".btn").toggleClass("active");
				var value = $btnGroup.find(".btn.active").attr("data-mode") * 1;
				var $label = $e.find(".viewContainer .valueLabel");
				if(value){
					$label.addClass("yes");
					$label.text("Yes");
					$label.attr("data-value",1);
				}else{
					$label.removeClass("yes");
					$label.text("No");
					$label.attr("data-value",0);
				}
				
				var val = value * 1 ? true : false;
				var valueObject = {field:view.paramName, conditions:{"==":val}};
				
				view.setValue(valueObject);
				
				$e.trigger("DO_SEARCH");
			}
		},
		// --------- /Events--------- //
		// --------- Filter Common API--------- //
		showMode:function(mode){
			var view = this;
			var $e = view.$el;
			mode = mode || view.mode;
			view.mode = mode;
			
			var filter = view.getValue();
			var $label = $e.find(".viewContainer .valueLabel");
			var $btnGroup = $e.find(".editContainer .btn-group");
			if (filter && filter.conditions) {
				var value = filter.conditions["=="];
				if (value) {
					$label.addClass("yes");
					$label.text("Yes");
					$label.attr("data-value", 1);
					$btnGroup.find(".btn[data-mode='1']").addClass("active");
					$btnGroup.find(".btn[data-mode='0']").removeClass("active");
				} else {
					$label.removeClass("yes");
					$label.text("No");
					$label.attr("data-value", 0);
					$btnGroup.find(".btn[data-mode='0']").addClass("active");
					$btnGroup.find(".btn[data-mode='1']").removeClass("active");
				}
			}else{
				$label.attr("data-value", "").html("");
				$btnGroup.find(".btn").removeClass("active");
				$btnGroup.find(".btn[data-mode='0']").addClass("active"); 
			}
			
			if(mode == 'edit'){
				$e.find(".editContainer").removeClass("hide");
				$e.find(".viewContainer").addClass("hide");
			}else{
				var $label = $e.find(".viewContainer .valueLabel");
				var val = $label.attr("data-value"); 
				if(val){
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
			
			if ($e.closest(".HeaderPopup").size() > 0) {
				app.ParamsControl.saveHeaderCustomFilter(filter);
			} else {
				app.ParamsControl.saveSideCustomAdvancedFilter(filter);
			}

		},
		
		clearFields:function(){
			var view = this;
			var $e = view.$el;
			view.setValue({field: view.paramName, conditions:null});
			view.showMode();
		}
		// --------- /Filter Common API--------- //
	});
})(jQuery);
