(function($){
	
	var searchDao = app.SearchDaoHandler;
	brite.registerView("Admin",{parent:".container",emptyParent:true},{
		create: function(){
			return render("Admin");
		},
		postDisplay:function(data){
			var view = this;
			$.ajax({
				url:"/config/get/",
				type:"Get",
				dataType:'json'
			}).done(function(data){
				if(data.success){
					view.$el.trigger("FILLDATA",data);
				}
			});
		},
		events:{
			"change;:checkbox,select":function(event){
				var view = this;
				var $saveBtn = view.$el.find(".save");
				$saveBtn.removeClass("disabled");
			},
			"btap;.save":function(event){
				var view = this;
				var $btn = $(event.target);
				var values = {};
				values["local_distance"]=view.$el.find("[name='local_distance']").val();
				values["local_date"]=view.$el.find("[name='local_date']").val();
				values["action_add_to_sourcing"]=view.$el.find("[name='action_add_to_sourcing']").prop("checked");
				values["action_favorite"]=view.$el.find("[name='action_favorite']").prop("checked");
				if(!$btn.hasClass("disabled")){
					$.ajax({
						url:"/config/save",
						type:"Post",
						dataType:'json',
						data:values
					}).done(function(data){
						if(data.success){
							window.location.href="/";
						}
					});
				}
			},
			"btap;.cancel":function(event){
				window.location.href="/";
			},
			"FILLDATA":function(event,data){
				var view = this;
				var currentField;
				$.each(data.result,function(index,e){
					currentField = view.$el.find("[name='"+e.name+"']");
					if(currentField.length>0){
						if(currentField.get(0).tagName=='INPUT'){
							currentField.prop("checked",e.value=='true');
						}else{
							currentField.val(e.value);
						}
					}
				});
			}
		}
	});
})(jQuery);