/**
 * View: LocationFilterView
 *
 *
 *
 *
 */
(function($) {
	var component = {
		create : function(data, config) {
			this.type = "location";
			var html = render("LocationFilterView", {});
			return html;
		},
		afterPostDisplay : function() {
			var view = this;
			//set max length

			app.getJsonData("/config/getByName/local_distance").done(function(result) {
				app.localDistance = "m";
				var label = "Radius(miles)";
				if (result && result === "k") {
					label = "Radius(km)";
					app.localDistance = "k";
				}
				view.$el.find(".labelText").html(label);
			});

		}

	};
	brite.registerView("LocationFilterView", {
		emptyParent : true
	}, app.mixin(app.FilterViewMixIn(), component));
})(jQuery);
