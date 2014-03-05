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
                    	view.$el.trigger("DO_SHOW_MSG",{selector:".search-config-alert",msg:result.errorMsg,type:"error"});
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
                		view.$el.trigger("DO_SHOW_MSG",{selector:".search-config-alert",msg:"search config has been reset successfully.",type:"success"});
                    });
                },
                "submit; form":function(event){
                    var view = this;
                    event.preventDefault();
                    event.stopPropagation();
                    var content = view.$el.find("form textarea").val();
                    app.getJsonData("saveSearchConfig",{content:content} ,"Post").done(function(result){
                    	if(!result.valid){
                    		view.$el.find(".search-content").css("background","#ffdddd");
                    		view.$el.trigger("DO_SHOW_MSG",{selector:".search-config-alert",msg:result.errorMsg,type:"error"});
                     	}else{
                    		view.$el.find(".search-content").css("background","#ffffff");
                    		view.$el.trigger("DO_SHOW_MSG",{selector:".search-config-alert",msg:"Values saved successfully",type:"success"});
                     	}
                    })
                }
            },
            docEvents: {}
        });
})(jQuery);
