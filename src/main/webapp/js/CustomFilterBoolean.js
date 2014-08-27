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
			"click;.btn-group" : function(e){
				var view = this;
				var $e = view.$el;
				var $btnGroup = $(e.currentTarget);
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
				$e.trigger("DO_SEARCH");
			}
		},
		// --------- /Events--------- //
		showMode:function(mode){
			var view = this;
			var $e = view.$el;
			mode = mode || view.mode;
			view.mode = mode;
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
			var $label = $e.find(".viewContainer .valueLabel");
			var val = $label.attr("data-value"); 
			if(!val){
				return null;
			}
			var value = val * 1 ? true : false;
			var valueObject = {field:view.paramName, conditions:{"==":value}};
			return valueObject;
		},
		clearFields:function(){
			var view = this;
			var $e = view.$el;
			var $label = $e.find(".viewContainer .valueLabel");
			$label.attr("data-value","").html("");
			var $btnGroup = $e.find(".editContainer .btn-group");
			$btnGroup.find(".btn").removeClass("active");
			$btnGroup.find(".btn[data-mode='0']").addClass("active");
			view.showMode();
		}
	});
})(jQuery);
