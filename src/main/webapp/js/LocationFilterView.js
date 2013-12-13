/**
 * View: LocationFilterView
 *
 *
 *
 *
 */
(function ($) {
    var component = {
        create: function (data, config) {
            this.type = "location";
            var html = render("FilterLocation", {});
            return html;
        },
        afterPostDisplay: function () {
            var view = this;
            //set max length

            app.getJsonData("/config/getByName/local_distance").done(function (result) {
                var label = "Radius(miles)"
                if (result && result.value == "k") {
                    label = "Radius(km)";
                }
                view.$el.find(".labelText").html(label);
            })
        }

    };
    brite.registerView("LocationFilterView", {emptyParent: true}, app.mixin(app.FilterViewMixIn(), component));
})(jQuery);
