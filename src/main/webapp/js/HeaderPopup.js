/**
 * View: HeaderPopup
 *
 * HeaderPopup
 *
 *
 */
(function ($) {
	brite.registerView("HeaderPopup", {
		emptyParent : false
	}, {
		create : function(data, config) {
			var dfd = $.Deferred();
		    var view = this;
			return render("HeaderPopup", data);
		},

		postDisplay : function(data) {
			var view = this;
			var $e = view.$el;
			view.$content = $e.find(".popover-content");
			view.$el.css("opacity",0);
			if (data.$target) {
				var $target = data.$target;
				var targetCenter = $target.offset().left + $target.outerWidth() / 2;
				var left = targetCenter - $e.width() / 2;
				var top = $target.offset().top + $target.height() + 10;
				
				if(left <= 10){
					var arrowPos = $e.find(".arrow").offset();
					var deltaX = left - 10;
					var arrowLeft = arrowPos.left + deltaX;
					$e.find(".arrow").offset({left:arrowLeft, top: arrowPos.top});
					left = 10;
				}
				
				if(left + $e.width() + 10 >= $(window).width()){
					var arrowPos = $e.find(".arrow").offset();
					var deltaX = left + $e.width() + 10 - $(window).width();
					var arrowLeft = arrowPos.left + deltaX;
					$e.find(".arrow").offset({left:arrowLeft, top: arrowPos.top});
					left = $(window).width() - $e.width() - 10;
				}
				
				$e.offset({
					left : left,
					top : top
				});
                //call render method
                var type = $target.attr("data-column");
                /*
                var data = app.ParamsControl.getFilterParams()[type]||[];

                if(type=="company") {
                    type= "employer";
                }
                if(type=="contact") {
                    data = app.ParamsControl.getFilterParams()["Contact"]||[];
                }*/
                var render = app.getFilterRender(type);
                render(view.$content, $target);
				
			}
			view.$el.css("opacity",1);

		},
		docEvents:{
			"btap":function(event){
				var view = this;
				var width = view.$el.width();
				var height = view.$el.height();
				var pos = view.$el.offset();
				if (event.pageX > pos.left && event.pageX < pos.left + width && event.pageY > pos.top && event.pageY < pos.top + height) {
				} else {
					close.call(view);
				}
			},
			"POPUP_CLOSE":function(event){
				var view = this;
				close.call(view);
			}
		},
	}); 
	
	function close(){
		var view = this;
		view.$el.bRemove();
	}


})(jQuery);
