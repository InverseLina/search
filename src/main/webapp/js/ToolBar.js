/**
 * View: ToolBar
 *
 * ToolBar
 *
 *
 */
(function($) {
	brite.registerView("ToolBar", {
		parent : ".toolbar-ctn"
	}, {
		// --------- Implement Interface--------- //
		create : function(data, config) {
			var $e = $(render("ToolBar"));
			return $e;
		},

		postDisplay : function(data) {
			var view = this;
		},
		// --------- /Implement Interface--------- //
		events : {
			"click; .btnApply" : function(event) {
				var view = this;
				var list = [];
				$(".search-result").find("tr.applySelect").each(function(idx, tr) {
					var $tr = $(tr);
					list.push({
						id : $tr.attr("data-entity-id"),
						sfid : $tr.attr("data-sfid")
					});
				});
				view.$el.trigger("APPLY_PRESS", {
					selectedContactList : list
				});
			},
			"click; .btnAddToShotList" : function(event) {
				var view = this;
				var list = [];
				$(".search-result").find("tr.applySelect").each(function(idx, tr) {
					var $tr = $(tr);
					list.push({
						id : $tr.attr("data-entity-id"),
						sfid : $tr.attr("data-sfid")
					});
				});
				view.$el.trigger("SHORTLIST_PRESS", {
					selectedContactList : list
				});
			},
			"click; .btnClearSearch" : function(event) {
				var view = this;
				view.$el.trigger("CLEAR_SEARCH_QUERY");
			},

		},
		docEvents : {
			"DO_TOOLBAR_ACTIVE_BUTTONS" : function() {
				var view = this;
				var $e = view.$el;
				$e.find(".action-button").removeClass("disabled");
				view.$el.find(".btnAddToShotList").prop("disabled", false).removeClass("disabled");
				view.$el.find(".btnApply").prop("disabled", false).removeClass("disabled");
			},
			"DO_TOOLBAR_DEACTIVE_BUTTONS" : function() {
				var view = this;
				var $e = view.$el;
				$e.find(".action-button").addClass("disabled");
			},

		}
	});

})(jQuery);
