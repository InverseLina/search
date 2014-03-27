/**
 * View: MessagePanel
 * Description: show message in content view center.
 */
(function($) {
	brite.registerView("MessagePanel", {
		emptyParent : true,
		parent : ".search-result"
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			return render("MessagePanel", {
				message : data.message
			});
		},

		postDisplay : function(data) {

		}

		// --------- /View Interface Implement--------- //
	});
})(jQuery);
