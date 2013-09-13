(function($){
	
	brite.registerView("Admin",{parent:".container",emptyParent:true},{
		create: function(){
			return render("Admin");
		},
		postDisplay:function(data){
			var view = this;
            app.getJsonData("/config/get/").done(function(data){
				view.$el.trigger("FILLDATA",{data:data});
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
                    app.getJsonData("/config/save", values,"Post").done(function(data){
				        window.location.href="/";
					});
				}
			},
			"btap;.cancel":function(event){
				window.location.href="/";
			},

			"FILLDATA":function(event,result){
				var view = this;
				var currentField;
				$.each(result.data,function(index,e){
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
		},
        docEvents: {
            ON_ERROR:function(event, extra){
                view = this;
                if(extra.errorCode === "NO_ORG"){
                    view.$el.find(".alert").show();
                }
            }
        }

	});
})(jQuery);