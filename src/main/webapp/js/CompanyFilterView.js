/**
 * View: CompanyFilterView
 *
 *
 *
 *
 */
(function ($) {
    var component = {
        create: function (data, config) {
            this.type = "company";
            var html = render("FilterEmployer", {});
            return html;
        },

    };
    brite.registerView("CompanyFilterView", {emptyParent: true}, app.mixin(app.FilterViewMixIn(), component));
})(jQuery);
