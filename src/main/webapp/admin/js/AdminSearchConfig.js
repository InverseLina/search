/**
 * View: AdminSearchConfig
 *
 *
 *
 *
 */
(function($) {
	brite.registerView("AdminSearchConfig", {
		parent : ".search-config",
		emptyParent : true
	}, {
		create : function(data, config) {
			return render("AdminSearchConfig");
		},

		postDisplay : function(data) {
			var view = this;
			app.getJsonData("getSearchConfig").done(function(result) {
				view.$el.find("textarea").val(result.content);
				if (result.errorMsg) {
					view.$el.trigger("DO_SHOW_MSG", {
						selector : ".search-config-alert",
						msg : result.errorMsg,
						type : "error"
					});
					view.$el.find(".search-content").css("background", "#ffdddd");
				}
			});
		},
		events : {
			"btap; button.reset" : function(event) {
				var view = this;
				var $btn = $(event.currentTarget);
				$btn.prop("disabled", true).html("resetting...");
				app.getJsonData("resetSearchConfig").done(function(result) {
					view.$el.find("textarea").val(result);
					view.$el.find(".search-content").css("background", "#ffffff");
					view.$el.trigger("DO_SHOW_MSG", {
						selector : ".search-config-alert",
						msg : "search config has been reset successfully.",
						type : "success"
					});
					$btn.prop("disabled", false).html("Reset");
				});
			},
			"submit; form" : function(event) {
				var view = this;
				event.preventDefault();
				event.stopPropagation();
				var content = view.$el.find("form textarea").val();
				var $btn = $("form :submit");
				$btn.prop("disabled", true).html("saving...");
				app.getJsonData("saveSearchConfig", {
					content : content
				}, "Post").done(function(result) {
					if (!result.valid) {
						view.$el.find(".search-content").css("background", "#ffdddd");
						view.$el.trigger("DO_SHOW_MSG", {
							selector : ".search-config-alert",
							msg : result.errorMsg,
							type : "error"
						});
					} else {
						view.$el.find(".search-content").css("background", "#ffffff");
						view.$el.trigger("DO_SHOW_MSG", {
							selector : ".search-config-alert",
							msg : "Values saved successfully",
							type : "success"
						});
					}
					$btn.prop("disabled", false).html("Save");
				})
			}
		},
		docEvents : {}
	});
})(jQuery);
