/**
 * View: ContactFilterView
 *
 * ContactFilterView
 *
 *
 */
(function($) {
	var cookiePrefix = "contact_filter_";
	brite.registerView("ContactFilterView", {
		emptyParent : true
	}, {
		
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			this.type = "contact";
			return render("ContactFilterView", data);
		},

		postDisplay : function(data) {
			var view = this;
			var $e = view.$el;
			view.$el.find("input:first").focus();
			var items = app.ParamsControl.get("contact");
			items = items || [];
			$.each(items, function(idx, val) {
				var item = {
					name : val.name
				};
				val = val.value;
				if (val.minYears || val.minRadius) {
					item.min = val.minYears || val.minRadius;
				}
				var html = render("ContactFilterView-selectedItem-add", item);
				view.$el.find("span.add").before(html);

			});
			showSelectedItem.call(view);
			$(":text").placeholder();
			$('.btn').button();

			var objectType = app.preference.get(cookiePrefix + "objectType");
			var status = app.preference.get(cookiePrefix + "status");
			if (objectType) {
				$e.find("input[name='objectType']").closest(".btn").removeClass("active");
				$e.find("input[name='objectType'][value='" + objectType + "']").closest(".btn").addClass("active");
			}

			if (status) {
				$e.find("input[name='status']").closest(".btn").removeClass("active");
				$e.find("input[name='status'][value='" + status + "']").closest(".btn").addClass("active");
			}

		},
		
		// --------- /View Interface Implement--------- //
		
		// --------- Events--------- //
		events : {
			"click;.cancel" : function(event) {
				var view = this;
				var $span = $(event.target);
				view.$el.find(".save").parent().addClass("hide");
				view.$el.find(".selectedItems .add").removeClass("hide");
				view.$el.find(":text").val("");
			},
			"click;.selectedItems .item" : function(event) {
				var view = this;
				var $span = $(event.target);
				var dataName = $span.closest(".item").attr("data-name");
				var contact = app.ParamsControl.get("contact", dataName);
				var value = contact.value.value;
				view.$el.find(":input[name='FirstName']").val(value.firstName || "");
				view.$el.find(":input[name='LastName']").val(value.lastName || "");
				view.$el.find(":input[name='Email']").val(value.email || "");
				view.$el.find(":input[name='Title']").val(value.title || "");

			},
			"click; .content .contactRow .clear" : function(event) {
				var view = this;
				var $input = $(event.currentTarget).closest("div").find("input");
				$input.val("").focus().change();
				event.preventDefault();
				event.stopPropagation();
			},
			"keydown focus change; input[type='text']" : function(event) {
				$input = $(event.currentTarget);
				var val, $input, view = this;
				if (event.keyCode === 13) {
					addItem.call(view);
					setTimeout(function() {
						$input.focus();
					}, 200);
					event.preventDefault();
				} else if (event.keyCode === 27) {
					view.close();
				} else {

					val = $input.val();
					if (/^\s*$/.test(val)) {
						$input.closest("div").removeClass("active");
					} else {
						$input.closest("div").addClass("active");
					}
				}
			},
			"click;.contact-btn-group .btn-group .btn" : function(event) {
				var view = this;
				var $btn = $(event.currentTarget);
				var $radio = $btn.find("input[type='radio']");
				var name = $radio.attr("name");
				var value = $radio.val();
				app.preference.store(cookiePrefix + name, value);
				view.$el.trigger("DO_SEARCH");
			},
			"click; .selectedItems span.clear" : function(event) {
				event.preventDefault();
				event.stopPropagation();
				var view = this;
				var dataName = $(event.currentTarget).closest("span[data-name]").attr("data-name");
				setTimeout(function() {
					view.$el.find(".selectedItems span[data-name='" + dataName + "']").remove();
					if (view.$el.find(".selectedItems span[data-name]").length === 0) {
						showSPline.call(view, false);
						if (view.type === "contact") {
							view.$el.find(".selectedItems").hide();
						}
					}
					view.$el.trigger("REMOVE_FILTER", {
						name : dataName,
						type : view.type
					});
				}, 200);
				view.$el.find("input:first").focus();

			},
			"click; .btnSearchForPerson" : function(e) {
				var view = this;
				var $btn = $(e.currentTarget);
				var $i = $btn.find("i");
				var $personFields = view.$el.find(".personFields");
				if ($btn.hasClass("active")) {
					$btn.removeClass("active");
					$i.addClass("glyphicon-chevron-down");
					$i.removeClass("glyphicon-chevron-up");
					$personFields.hide();
				} else {
					$btn.addClass("active");
					$i.addClass("glyphicon-chevron-up");
					$i.removeClass("glyphicon-chevron-down");
					$personFields.show();
				}
			}

		}
		// --------- /Events--------- //
	});

	// --------- Private Methods--------- //
	function showSPline(status) {
		var view = this;
		if (status) {
			view.$el.find(".separateLine").show();
		} else {
			view.$el.find(".separateLine").hide();
		}
	}

	function addItem() {
		var $item, view = this, ele;
		var data = {};
		$item = view.$el.find(":input[name='FirstName']");
		if (!/^\s*$/g.test($item.val())) {
			data.firstName = $item.val();
		}
		$item = view.$el.find(":input[name='LastName']");
		if (!/^\s*$/g.test($item.val())) {
			data.lastName = $item.val();
		}

		$item = view.$el.find(":input[name='Email']");
		if (!/^\s*$/g.test($item.val())) {
			data.email = $item.val();
		}

		$item = view.$el.find(":input[name='Title']");
		if (!/^\s*$/g.test($item.val())) {
			data.title = $item.val();
		}

		var displayName = app.getContactDisplayName(data);
		if (/^\s*$/g.test(displayName)) {
			return;
		}

		var $eles = view.$el.find(".selectedItems .item[data-name='" + displayName + "']");
		var len = $eles.length;
		if (len === 0) {
			view.$el.find(".selectedItems span.add").before(render("ContactFilterView-selectedItem-add", {
				name : displayName
			}));
			$eles = view.$el.find(".selectedItems .item[data-name='" + displayName + "']");
			view.$el.trigger("ADD_FILTER", {
				type : "contact",
				name : displayName,
				value : data
			});
		}

		ele = $($eles[0]);
		ele.data("value", data);
		//        view.$el.find(".save").parent().addClass("hide");
		view.$el.find(".selectedItems .add").removeClass("hide");
		view.$el.find(":text").val("").removeClass("placeholder").trigger("blur.placeholder");
		view.$el.find("input:first").focus();
		showSelectedItem.call(view);
	}

	function showSelectedItem() {
		var view = this;
		var data = app.ParamsControl.get("contact");
		if (data && data.length > 0) {
			view.$el.find(".selectedItems").show();
		}
	}

	// --------- /Private Methods--------- //
})(jQuery);