/**
 * View: ExtraMessage
 *
 */
(function($) {
	brite.registerView("ExtraMessage", {
		emptyParent : false,
		parent : "body"
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			data = data || {};
			var $e = $(render("ExtraMessage", {
				title : data.title,
				message : data.message
			}));

			return $e;
		},

		postDisplay : function(data) {
		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"btap; .btn-primary, .close" : function() {
				var view = this;
				view.$el.bRemove();
			},

		},
		// --------- /Events--------- //
		docEvents : {}
	});
})(jQuery);
