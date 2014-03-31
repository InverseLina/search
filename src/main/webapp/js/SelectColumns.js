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
			var i, columns = app.getSearchUiConfig(true);
			var orders = app.getFilterOrders();
			var colOrders = [];
			var ids = [];
			$.each(orders, function(idx, name) {
				$.each(columns, function(idx, item) {
					if (item.name === name) {
						colOrders.push(item);
						ids.push(idx);
					}
				});

			});

			for ( i = 0; i < columns.length; i++) {
				if ($.inArray(i, ids) < 0) {
					colOrders.push(columns[i]);
				}
			}
			return render("SelectColumns", {
				columns : colOrders
			});
		},

		postDisplay : function(data) {
			var view = this;
			var columns = app.preference.columns();
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
				view.$el.bRemove();
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
				$e.find(".columns li").each(function(idx, th) {
					var $li = $(th);
					columns.push($li.find("input").val());
				});
				view.$el.find("#columnsClone").remove();

				$e.find(".columns li").removeClass("holderSpace");

				app.getJsonData("perf/save-user-pref", {
					value : JSON.stringify(columns)
				}, "Post").done(function() {

					app.getFilterOrders(columns);
					view.$el.trigger("DO_SEARCH");
				});
			}

		}
		// --------- /Events--------- //
	});
})(jQuery);
