/**
 * View: MessagePanel
 *
 *
 *
 *
 */
(function ($) {
    brite.registerView("MessagePanel", {emptyParent: true, parent: ".search-result"},
        {
            create: function (data, config) {
                return render("MessagePanel", {message:data.message});
            },

            postDisplay: function (data) {

            },
            events: {},
            docEvents: {}
        });
})(jQuery);
