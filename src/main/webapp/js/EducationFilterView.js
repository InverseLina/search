/**
 * View: EducationFilterView
 *
 *
 *
 *
 */
(function ($) {
    var component = {
        create: function (data, config) {
            this.type = "education";
            var html = render("FilterEducation", {});
            return html;
        },

    };
    brite.registerView("EducationFilterView", {emptyParent: true}, app.mixin(app.FilterViewMixIn(), component));
})(jQuery);
