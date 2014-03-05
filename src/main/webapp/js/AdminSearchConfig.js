/**
 * View: AdminSearchConfig
 *
 *
 *
 *
 */
(function ($) {
    brite.registerView("AdminSearchConfig",{parent:".search-config", emptyParent: true} ,
        {
            create: function (data, config) {
                return render("AdminSearchConfig");
            },

            postDisplay: function (data) {
                var view = this;
                app.getJsonData("getSearchConfig").done(function(result){
                    view.$el.find("textarea").val(result.content);
                    if(result.errorMsg){
                    	view.$el.find(".alert").removeClass("hide")
                		.removeClass("alert-success").addClass("alert-danger").html(result.errorMsg);
                		view.$el.find(".search-content").css("background","#ffdddd");
                    }
                });
            },
            events: {
                "btap; button.reset": function(){
                    var view = this;
                    app.getJsonData("resetSearchConfig").done(function(result){
                        view.$el.find("textarea").val(result);
                        view.$el.find(".search-content").css("background","#ffffff");
                		view.$el.find(".alert").addClass("alert-success").removeClass("hide")
                		.removeClass("alert-danger").html("search config has been reset successfully.");
                    });
                },
                "submit; form":function(event){
                    var view = this;
                    event.preventDefault();
                    event.stopPropagation();
                    var content = view.$el.find("form textarea").val();
                    app.getJsonData("saveSearchConfig",{content:content} ,"Post").done(function(result){
                    	if(!result.valid){
                    		view.$el.find(".alert").removeClass("hide")
                    		.removeClass("alert-success").addClass("alert-danger").html(result.errorMsg);
                    		view.$el.find(".search-content").css("background","#ffdddd");
                     		//alert("The search config xml has something incorrect.");
                     	}else{
                    		view.$el.find(".search-content").css("background","#ffffff");
                    		view.$el.find(".alert").addClass("alert-success").removeClass("hide")
                    		.removeClass("alert-danger").html("search config has been saved successfully.");
                     	}
                    })
                }
            },
            docEvents: {}
        });
})(jQuery);
