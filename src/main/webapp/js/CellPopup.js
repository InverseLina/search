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
					data.names.push(value)
				})

			}
			//                console.log(data);

			var html = render("CellPopup-inner", data);
			view.$el.html(html);

			//                console.log(data.pos);
			//                console.log(view.$el.height());
			//                console.log($(window).height());
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
				var $li = $(event.currentTarget).closest("li");
				name = $li.text();
				if ($li.hasClass("filter")) {

					//                        app.ParamsControl.remove({type: view.type, name: name})
					$li.removeClass("filter");
					$li.trigger("REMOVE_FILTER", {
						type : view.type,
						name : name
					});
				} else {
					$li.addClass("filter");
					$li.trigger("ADD_FILTER", {
						type : view.type,
						name : name
					});
				}

			}

		}
		// --------- /Events--------- //
	});
})(jQuery);
