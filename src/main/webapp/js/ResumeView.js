/**
 * View: ResumeView
 *
 *
 *
 *
 */
(function ($) {
    brite.registerView("ResumeView", {emptyParent: false, parent: "body"},
        {
            create: function (data, config) {
                $("#resumeModal").bRemove();
                var dfd = $.Deferred();

                app.getJsonData("getResume", {cid: data.id}).done(function (result) {
                    console.log(result)
                    if (result.length > 0) {
                        dfd.resolve(render("ResumeView", {name: result[0]["name"],
                            resume: result[0]["ts2__text_resume__c"]}));
                    } else {
                        dfd.resolve(render("ResumeView", {resume: "not resume"}));
                    }
                });
                return dfd.promise();
            },

            postDisplay: function (data) {
//                var view = this;
//                view.$el.modal('show')
//                $("#resumeModal").modal();
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
