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
		events:{
			"click;.skill-btn-group .btn-group .btn" : function(event) {
				var view = this;
				var $e = view.$el;
				var $btn = $(event.currentTarget);
				$btn.closest(".btn-group").find(".btn").removeClass("active");
				$btn.addClass("active");
			}
		}

	};
	brite.registerView("SkillFilterView", {
		emptyParent : true
	}, app.mixin(app.FilterViewMixIn(), component));
})(jQuery);
