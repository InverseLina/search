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

                $.ajax({type: "GET", url: "getResume", data: {cid: data.id}, dataType: "json"}).success(function (result) {
                    if (result.success && result.result.length > 0) {
                        dfd.resolve(render("ResumeView", {name: result.result[0]["Name"],
                            resume: result.result[0]["ts2__Text_Resume__c"]}));
                    } else {
                        dfd.resolve(render("ResumeView", {resume: "not resume"}));
                    }
                }).fail(function (result) {
                        console.log(result);
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
