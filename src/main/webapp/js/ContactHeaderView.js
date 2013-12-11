/**
 * View: ContactHeaderView
 *
 * ContactHeaderView
 *
 *
 */
(function ($) {
	brite.registerView("ContactHeaderView", {
		emptyParent : true
	}, {
		create : function(data, config) {
			return render("ContactHeaderView", data);
		},

		postDisplay : function(data) {

		},
		events : {
		},
		docEvents : {}
	}); 

})(jQuery);
