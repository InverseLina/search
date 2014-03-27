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
            // --------- View Interface Implement--------- //
            create: function (data, config) {
                return render("LoginModal");
            },

            postDisplay: function (data) {
            },
            // --------- /View Interface Implement--------- //


            // --------- Events--------- //
            events: {
                "click;.cancel,.close": function(){
                    var view = this;
                    view.$el.bRemove();
                },
                "validate":function(event){
                	var view = this;
                	$.ajax({
                		url:contextPath + "/admin-login",
                		type:"Post",
                		data:{
                			password:view.$el.find(":password").val()
                		}
                	}).done(function(data){
                		if(data.success){
                			window.location.href=contextPath + "/admin/";
                		}else{
                			view.$el.find(".alert").show();
                		}
                	});
                },
                "click;.login":function(event){
                	this.$el.trigger("validate");
                },
                "keyup;:input":function(event){
                	if(event.which==13){
                		this.$el.trigger("validate");
                	}
                }
            },
            // --------- /Events--------- //
            docEvents: {}
        });
})(jQuery);
