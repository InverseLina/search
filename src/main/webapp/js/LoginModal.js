/**
 * View: LoginModal
 *
 *
 *
 *
 */
(function ($) {
    brite.registerView("LoginModal", {emptyParent: false, parent: "body"},
        {
            create: function (data, config) {
                return render("LoginModal");
            },

            postDisplay: function (data) {
            },
            events: {
                "btap;.cancel,.close": function(){
                    var view = this;
                    view.$el.bRemove();
                },
                "validate":function(event){
                	var view = this;
                	$.ajax({
                		url:"/admin/validate",
                		type:"Post",
                		data:{
                			password:view.$el.find(":password").val()
                		}
                	}).done(function(data){
                		if(data.success){
                			window.location.href="/admin";
                		}else{
                			view.$el.find(".alert").show();
                		}
                	});
                },
                "btap;.login":function(event){
                	this.$el.trigger("validate");
                },
                "keyup;:input":function(event){
                	if(event.which==13){
                		this.$el.trigger("validate");
                	}
                }
            },
            docEvents: {}
        });
})(jQuery);
