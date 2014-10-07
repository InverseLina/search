/**
 * View: HeaderPopup
 *
 * HeaderPopup
 *
 *
 */
(function($) {
	brite.registerView("HeaderPopup", {
		emptyParent : false
	}, {
		
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			var dfd = $.Deferred();
			var view = this;
			return render("HeaderPopup", data);
		},

		postDisplay : function(data) {
			var arrowPos, deltaX, view = this;
			var $e = view.$el;
			view.$content = $e.find(".popover-content");
			if (data.$target) {
				var $target = view.$target = data.$target;
				view.$target.addClass("popupShow");
				var targetCenter = $target.offset().left + $target.outerWidth() / 2;
				var left = targetCenter - $e.width() / 2;
				var top = $target.offset().top + $target.height() + 24;

				if (left <= 10) {
					arrowPos = $e.find(".arrow").offset();
					deltaX = left - 10;
					arrowLeft = arrowPos.left + deltaX;
					$e.find(".arrow").offset({
						left : arrowLeft,
						top : arrowPos.top
					});
					left = 10;
				}

				if (left + $e.width() + 10 >= $(window).width()) {
					arrowPos = $e.find(".arrow").offset();
					deltaX = left + $e.width() + 10 - $(window).width();
					arrowLeft = arrowPos.left + deltaX;
					$e.find(".arrow").offset({
						left : arrowLeft,
						top : arrowPos.top
					});
					left = $(window).width() - $e.width() - 10;
				}

				$e.offset({
					left : left,
					top : top
				});
				//call filterRender method
				var custom = $target.attr("data-custom");
				var filterRender;
				if(custom == "true"){
					filterRender = app.getFilterRender("custom");
					view.$content.addClass("custom");
					view.$content.append("<span class='clearCustomFilter icon-fa fa-times-circle'></span>");
				}else{
					var type = $target.attr("data-column");
					filterRender = app.getFilterRender(type);
				}
				filterRender(view.$content, $target);

			}
			view.$el.css("opacity", 1);

		},
		// --------- /View Interface Implement--------- //
		
		// --------- Events --------- //
		events:{
			"click;.clearCustomFilter" : function(){
				var view = this;
				var $e = view.$el;
				var component = $(view.$content.children()[1]).bView();
				app.ParamsControl.saveHeaderCustomFilter({field:component.paramName});
				component.clearFields();
				$e.trigger("DO_SEARCH");
			}
		},
		// --------- /Events--------- //
		
		// --------- Document Events--------- //
		docEvents : {
			"click" : function(event) {
				var view = this;
				var width = view.$el.width();
				var height = view.$el.height();
				var pos = view.$el.offset();
				var $target = $(event.target);
				if ($target.closest("."+view.name).size() == 0) {
					close.call(view);
				}
			},
			"POPUP_CLOSE" : function(event) {
				var view = this;
				close.call(view);
			}

		}
		// --------- /Document Events--------- //
	});

	function close() {
		var view = this;
		view.$el.bRemove();
		if(view.$target){
			view.$target.removeClass("popupShow");
		}
	}

})(jQuery);
