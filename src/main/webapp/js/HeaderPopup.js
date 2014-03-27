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
		create : function(data, config) {
			var dfd = $.Deferred();
			var view = this;
			return render("HeaderPopup", data);
		},

		postDisplay : function(data) {
			var arrowPos, deltaX, deltaX, view = this;
			var $e = view.$el;
			view.$content = $e.find(".popover-content");
			if (data.$target) {
				var $target = data.$target;
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
				var type = $target.attr("data-column");
				var filterRender = app.getFilterRender(type);
				filterRender(view.$content, $target);

			}
			view.$el.css("opacity", 1);

		},
		docEvents : {
			"click" : function(event) {
				var view = this;
				var width = view.$el.width();
				var height = view.$el.height();
				var pos = view.$el.offset();
				if (event.pageX > pos.left && event.pageX < pos.left + width && event.pageY > pos.top && event.pageY < pos.top + height) {
				} else {
					close.call(view);
				}
			},
			"POPUP_CLOSE" : function(event) {
				var view = this;
				close.call(view);
			}

		},
	});

	function close() {
		var view = this;
		view.$el.bRemove();
	}

})(jQuery);
