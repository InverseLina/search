/**
 * View: SkillFilterView
 *
 *
 *
 *
 */
(function($) {
	var component = {
		create : function(data, config) {
			this.type = "skill";
			var html = render("SkillFilterView", {});
			return html;
		},

	};
	brite.registerView("SkillFilterView", {
		emptyParent : true
	}, app.mixin(app.FilterViewMixIn(), component));
})(jQuery);
