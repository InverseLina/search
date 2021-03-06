/**
 * View: SelectColumns
 *
 * SelectColumns
 *
 *
 */
(function($) {
	brite.registerView("SelectColumns", {
		emptyParent : false,
		parent : ".search-result"
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			var colOrders = app.columns.listAllWithOrder();
			return render("SelectColumns", {
				columns : colOrders
			});
		},

		postDisplay : function(data) {
			var view = this;
			var columns = app.columns.get();
			if (app.buildPathInfo().labelAssigned) {
				view.$el.addClass("favFilter");
			}
			$.each(columns, function(idx, item) {
				view.$el.find("input[value='" + item + "']").attr("checked", true);
			});

			$(document).on("btap." + view.cid, function(event) {
				var width = view.$el.width();
				var height = view.$el.height();
				var pos = view.$el.offset();
				if (event.pageX > pos.left && event.pageY < pos.left + width && event.pageY > pos.top && event.pageY < pos.top + height) {
					//do nothing
					//view.$el.off("mouseleave");
				} else {
					view.$el.bRemove();
					$(document).off("btap." + view.cid);
				}
			});
			
			view.$el.on("mouseleave", function() {
				if(!view._dragging){
					view.$el.bRemove();
				}
			});

		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"click; input[type='checkbox']" : function(e) {
				var view = this;
				var $checkbox = $(e.target);
				var columns = [];
				view.$el.find("input[type='checkbox']:checked").each(function() {
					columns.push($(this).val());
				});
				
				if($checkbox.val() == 'contact' || $checkbox.val() == 'location'){
					var checked = $checkbox.prop("checked");
					if(!checked){
						view.$el.trigger("DO_CLEAR_ORDER");
					}
				}
				
				if (columns.length > 0) {
					view.$el.trigger("DO_SET_COLUMNS", {
						columns : columns
					});
				} else {
					alert("Please a column at least!");
					$checkbox.prop("checked", true);
				}
			},
			"bdragstart; li" : function(event) {
				event.stopPropagation();
				event.preventDefault();
				var $li = $(event.currentTarget);
				var view = this;
				var $e = view.$el;
				view._dragging = true;
				var $clone = $li.clone();
				$clone.attr("id", "columnsClone");
				var pos = $li.position();
				$clone.css({
					"display" : "block",
					"position" : "absolute",
					opacity : 0.5,
					left : pos.left,
					top : pos.top,
					"cursor" : "pointer",
					"width" : $li.outerWidth()
				});

				var index = $li.index();
				$e.find(".columns li:nth-child(" + (index + 1) + ")").each(function() {
					$(this).addClass("holderSpace");
				});

				view.$el.append($clone);
			},
			"bdragmove; li" : function(e) {
				var view = this;
				var $e = view.$el;
				e.stopPropagation();
				e.preventDefault();
				var $clone = view.$el.find("#columnsClone");
				var ppos = $clone.position();
				var pos = {
					top : ppos.top + e.bextra.deltaY,
					left : 10
				};
				$clone.css(pos);

				var $holder = $e.find(".holderSpace");
				$e.find(".columns li").each(function(idx, li) {
					var $li = $(li);
					var tpos = $li.offset();
					if (e.bextra.pageY > tpos.top && e.bextra.pageY < tpos.top + $li.outerHeight()) {
						if (e.bextra.pageY < tpos.top + $li.outerHeight() / 2) {
							$holder.insertAfter($li);
						} else {
							$holder.insertBefore($li);
						}
					}
				});
			},
			"bdragend; li" : function(e) {
				e.stopPropagation();
				e.preventDefault();
				var view = this;
				var $e = view.$el;

				var columns = [];
				$e.find(".columns li input:checked").each(function(idx, th) {
					var $checkbox = $(this);
					columns.push($checkbox.val());
				});
				view.$el.find("#columnsClone").remove();
				$e.find(".columns li").removeClass("holderSpace");
				
				
				app.columns.save(columns).done(function(){
					view.$el.trigger("DO_SET_COLUMNS", {
						columns : columns
					});
				});
				
				view._dragging = false;
			}

		}
		// --------- /Events--------- //
	});
})(jQuery);
