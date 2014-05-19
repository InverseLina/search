/**
 * View: CellPopup
 *  param: data {columnName, contactName, names, type}
 *
 *
 *
 *
 */
(function($) {
	brite.registerView("CellPopup", {
		emptyParent : false,
		parent : "body"
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			return render("CellPopup");
		},

		postDisplay : function(data) {
			var oldNames, value, view = this;
			this.type = data.type;

			if (data.names && data.names.length > 0) {
				oldNames = data.names;
				data.names = [];
				$.each(oldNames, function(idx, name) {
					value = {
						name : name
					};
					if (app.ParamsControl.get(data.type, name)) {
						value.isFilter = true;
					}
					if(data.ids  && data.ids.length > 0){
						value.id = data.ids[idx];
					}
					data.names.push(value);
				});

			}
			var html = render("CellPopup-inner", data);
			view.$el.html(html);

			if (data.pos.y + view.$el.outerHeight() > $(window).height()) {

				data.pos.y = $(window).height() - view.$el.outerHeight();
			}
			var pos = {
				left : data.pos.x - 104,
				top : data.pos.y
			};
			view.$el.css(pos);
			$(document).on("click." + view.cid, function(event) {
				var width = view.$el.outerWidth();
				var height = view.$el.outerHeight();
				var pos = view.$el.offset();
				if (event.pageX > pos.left && event.pageX < pos.left + width && event.pageY > pos.top && event.pageY < pos.top + height) {
				} else {
					view.$el.bRemove();
				}
			});
		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"click; li" : function(event) {
				var name, view = this;
				var $e = view.$el;
				var $li = $(event.currentTarget).closest("li");
				name = $li.text();
				if ($li.hasClass("filter")) {
					$li.removeClass("filter");
					$li.trigger("REMOVE_FILTER", {
						type : view.type,
						name : name
					});
				} else {
					$li.addClass("filter");
					var item = {
						type : view.type,
						name : name
					};
					if($e.find(".operator-btn-group").size() > 0){
						var operator = $e.find(".operator-btn-group .btn.active").attr("data-value");
						item.operator = operator;
					}
					if($li.attr("data-id") != ""){
						var groupedid = $li.attr("data-id");
						item.groupedid = groupedid;
					}
					$li.trigger("ADD_FILTER", item);
				}

			},
			"click;.operator-btn-group .btn-group .btn" : function(event) {
				var view = this;
				var $e = view.$el;
				var $btn = $(event.currentTarget);
				$btn.closest(".btn-group").find(".btn").removeClass("active");
				$btn.addClass("active");
			}

		}
		// --------- /Events--------- //
	});
})(jQuery);
