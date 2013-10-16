/**
 * View: OAuthInfo
 *
 * OAuthInfo
 *
 *
 */
(function ($) {
    brite.registerView("OAuthInfo", {emptyParent: true},
        {
            create: function (data, config) {
                return render("OAuthInfo", data);
            },

            postDisplay: function (data) {

            },
            events: {
                "btap; .home": function(){
                    window.location.href = contextPath + "/";
                }
            },
            docEvents: {}
        });
})(jQuery);
