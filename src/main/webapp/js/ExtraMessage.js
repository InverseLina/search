/**
 * View: ExtraMessage
 *
 */
(function ($) {
    brite.registerView("ExtraMessage", {emptyParent: false, parent: "body"},
        {
            create: function (data, config) {
                var data = data || {};
                var $e = $(render("ExtraMessage", {
                    title : data.title,
                    message : data.message
                }));
                
                return $e;
            },

            postDisplay: function (data) {
            },
            events: {
                "btap; .btn-primary, .close": function(){
                    var view = this;
                    view.$el.bRemove();
                },
            },
            docEvents: {}
        });
})(jQuery);
