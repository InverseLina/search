(function($){
	
  brite.registerView("OrganizationInfo",{parent:".admincontainer",emptyParent:true},{
    create: function(){
      return render("OrganizationInfo");
    },
    postDisplay:function(data){
      var view = this;
      
      view.section = app.pathInfo.paths[0] || "organization";
		
	  view.$navTabs = view.$el.find(".nav-tabs");
	  view.$tabContent = view.$el.find(".tab-content");
	  view.$navTabs.find("li.active").removeClass("active");
      
      if(app.pathInfo.paths[1] == "add"){
    	  var li = render("Organization-li",{type:"OrganizationInfo",url:"#organization/add"});
    	  view.$navTabs.append(li);	
    	  var html = render("Organization-content",{data:null});
    	  view.$tabContent.html(html);
       }else if(app.pathInfo.paths[1] == "edit"){
    	  var li = render("Organization-li",{type:"OrganizationInfo",url:"#"+app.pathInfo.paths[0]+"/"+app.pathInfo.paths[1]+"/"+app.pathInfo.paths[2]});
     	  view.$navTabs.append(li);	
    	  getDate.call(view,app.pathInfo.paths[2] * 1); 
       }
    },
    
    events:{
      "btap;.home":function(event){
    	window.location.href="/";
      },
      "btap;.cancel":function(event){
        window.location.href="/admin#organization";
      },
      "btap;.add":function(event){
        var view = this;
        var html = render("Organization-content",{data:null});
        view.$tabContent.html(html);
        window.location.href="admin#organization/add";
      },
      "change;:checkbox,select":function(event){
			var view = this;
			var $saveBtn = view.$el.find(".save");
			$saveBtn.removeClass("disabled");
		},
		"btap;.save":function(event){
			var view = this;
			var values = {};
	        doValidate.call(view);
	        if(view.validation){
	        	values["local_distance"]=view.$el.find("[name='local_distance']").val();
    			values["local_date"]=view.$el.find("[name='local_date']").val();
    			values["action_add_to_sourcing"]=view.$el.find("[name='action_add_to_sourcing']").prop("checked");
    			values["action_favorite"]=view.$el.find("[name='action_favorite']").prop("checked");
    			values["config_canvasapp_key"]=view.$el.find("[name='config_canvasapp_key']").val();
		        values["config_apiKey"]=view.$el.find("[name='config_apiKey']").val();
		        values["config_apiSecret"]=view.$el.find("[name='config_apiSecret']").val();
		        values["config_callBackUrl"]=view.$el.find("[name='config_callBackUrl']").val();
	        	app.getJsonData("/config/save", values,"Post").done(function(data){
	        		values = {};
	        		values["name"]=view.$el.find("[name='name']").val();
			        values["id"]=view.$el.find("[name='id']").val();
			        values["schemaname"]=view.$el.find("[name='schemaname']").val();
			        values["sfid"]=view.$el.find("[name='sfid']").val();
			        
	        		app.getJsonData("/org/save", values,"Post").done(function(data){
	        			window.location.href="/admin#organization";  
		          });
  				});
	        }
		},
		"FILLDATA":function(event,result){
			var view = this;
			var currentField;
			$.each(result.data,function(index,e){
				currentField = view.$el.find("[name='"+e.name+"']");
				if(currentField.length>0){
					if(currentField.attr("type") == 'checkbox'){
						currentField.prop("checked",e.value=='true');
					}else{
						currentField.val(e.value);
					}
				}
			});
		}
    }
  });
  
  
  function getDate(id){
    var view = this;
    app.getJsonData("/org/get/", {id:id}).done(function(data){
        var html = render("Organization-content",{data:data[0]});
        view.$tabContent.bEmpty();
        view.$tabContent.html(html);
        app.getJsonData("/config/get/").done(function(data){
           if(view && view.$el){
    		    view.$el.trigger("FILLDATA",{data:data});
           }
    	});
    });
  }
  
  function doValidate(){
	    var view = this;
		var $nameMsg = view.$el.find(".alert-error.name");
		var $schemanameMsg = view.$el.find(".alert-error.schemaname");
		var $callBackUrlMsg = view.$el.find(".alert-error.callBackUrl");
		
		if(view.$el.find("[name='name']").val() == ''){
			$nameMsg.removeClass("hide");
		}else{
			$nameMsg.addClass("hide");
		}
		
		if(view.$el.find("[name='schemaname']").val() == ''){
			$schemanameMsg.removeClass("hide");
		}else{
			$schemanameMsg.addClass("hide");
		}
		
		var str = view.$el.find("[name='config_callBackUrl']").val();
		str = str.match(/http:\/\/.+/) == null ? str.match(/https:\/\/.+/) : "true"; 
		if (str == null){ 
		   $callBackUrlMsg.removeClass("hide");
		}else{ 
		   $callBackUrlMsg.addClass("hide");
		}   
		
		if(view.$el.find(".alert-error:not(.hide)").length>0){
			view.validation=false;
		}else{
			view.validation=true;
		}
	  }
})(jQuery);