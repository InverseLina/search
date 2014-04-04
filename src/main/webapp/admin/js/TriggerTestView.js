/**
 * View: TriggerTestView
 *
 *
 *
 *
 */
(function($) {
	brite.registerView("TriggerTestView", {
		parent : ".admincontainer",
		emptyParent : true
	}, {
		create : function(data, config) {
			var dfd = $.Deferred();
			app.getJsonData("/test/getOrgs").done(function(result) {
				var html = render("TriggerTestView", {
					orgs : result
				});
				dfd.resolve(html);
			});
			return dfd.promise();
		},

		postDisplay : function(data) {

		},
		events : {
			"submit; form" : function(event) {
				event.preventDefault();
				event.stopPropagation();
				var view = this;
				var res = {};
				$("form :input", view.$el).each(function(i, obj) {
					res[obj.name] = $(obj).val();
				});
				app.getJsonData("/test/saveContact", res, "Post").done(function() {
					$("form :input", view.$el).val("");
				});

				return false;
			}
		},
		docEvents : {}
	});
})(jQuery);
