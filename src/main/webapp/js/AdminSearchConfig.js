/**
 * View: AdminSearchConfig
 *
 *
 *
 *
 */
(function ($) {
    brite.registerView("AdminSearchConfig",{parent:".admincontainer", emptyParent: true} ,
        {
            create: function (data, config) {
                return render("AdminSearchConfig");
            },

            postDisplay: function (data) {
                var view = this;
                app.getJsonData("getSearchConfig").done(function(result){
                    view.$el.find("textarea").val(result);
                });
            },
            events: {
                "btap; button.reset": function(){
                    var view = this;
                    app.getJsonData("resetSearchConfig").done(function(result){
                        view.$el.find("textarea").val(result);
                    });
                },
                "submit; form":function(event){
                    var view = this;
                    event.preventDefault();
                    event.stopPropagation();
                    var content = view.$el.find("form textarea").val();
                    app.getJsonData("saveSearchConfig",{content:content} ,"Post").done(function(result){
                    	if(!result.valid){
                     		alert("The search config xml has something incorrect.");
                     	}
                    })
                }
            },
            docEvents: {}
        });
})(jQuery);
