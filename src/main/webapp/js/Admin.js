(function($){
	
	var searchDao = app.SearchDaoHandler;
	brite.registerView("Admin",{parent:".container",emptyParent:true},{
		create: function(){
			return render("Admin");
		},
		events:{
			"change;:checkbox,select":function(event){
				var view = this;
				var $saveBtn = view.$el.find(".save");
				$saveBtn.removeClass("disabled");
			}
		}
	});
})(jQuery);