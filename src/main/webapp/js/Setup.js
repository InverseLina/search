(function($){
  
  brite.registerView("Setup",{parent:".admincontainer",emptyParent:true},{
    create: function(){
      return render("Setup");
    },
    postDisplay:function(data){
      var view = this;
      view.section = app.pathInfo.paths[0] || "setup";
    
      view.$navTabs = $(".nav-tabs");
      view.$tabContent = view.$el.find(".tab-content");
      view.$navTabs.find("li.active").removeClass("active");
      if(view.$navTabs.find('li').size() > 2){
		view.$navTabs.find('li:last').remove();
	  } 
      view.$navTabs.find("a[href='#setup']").closest("li").addClass("active"); 
      view.$el.find(".create,.import").prop("disabled",true).html("Loading...");
      view.$el.trigger("STATUS_CHANGE");
      
      app.getJsonData("/config/get/",{orgId:-1}).done(function(data){
    	  if(view && view.$el){
    	   view.$el.trigger("FILLDATA",{data:data});
    	  }
    	});
    },
    
    events:{
       "btap;.cancel":function(event){
    		window.location.href="/";
       },
      "btap;.home":function(event){
        window.location.href="/";
        },
      "click;.create":function(event){
    	  var view = this;
    	  var $createBtn = $(event.target);
    	  if($createBtn.prop("disabled")){
    		  return false;
    	  }
    	  $createBtn.prop("disabled",true).html("Creating...");
    	  app.getJsonData("/createSysSchema",{},{type:"Post"}).done(function(){
    		  $createBtn.html("Created");
    		  view.$el.find(".import").prop("disabled",false);
    		  $(".organization-tab").removeClass("hide");
    	  });
      },
      "click;.import":function(event){
    	  var view = this;
    	  var $importBtn = $(event.target);
    	  if($importBtn.prop("disabled")){
    		  return false;
    	  }
    	  $importBtn.prop("disabled",true).html("Importing...");
    	  app.getJsonData("/updateZipCode",{},{type:"Post"}).done(function(){
    		  $importBtn.html("Imported");
    	  });
      },
      "STATUS_CHANGE":function(event){
    	  var view = this;
    	  app.getJsonData("/checkSetupStatus",{type:"SYSTEM"},{type:"Get"}).done(function(result){
        	  switch(result){
        	  case 0:	view.$el.find(".create").prop("disabled",false).html("Create");
        	  			view.$el.find(".import").prop("disabled",false).html("Importe");
        	  			break;
        	  case 1:	view.$el.find(".create").prop("disabled",true).html("Created");
        	  			view.$el.find(".import").prop("disabled",false).html("Import");
    					break;
        	  case 2:	view.$el.find(".create").prop("disabled",true).html("Created");
    		  			view.$el.find(".import").prop("disabled",true).html("Imported");
    					break;
        	  }
          });
      },
      "FILLDATA":function(event,result){
    	  var view = this;
    	  var currentField;
    	  $.each(result.data,function(index,e){
    	    currentField = view.$el.find("[name='"+e.name+"']");
    	    if(currentField.length>0){
    	      currentField.val(e.value);
    	    }
    	  });
    	 },
      "btap;.save":function(event){
    	  var view = this;
    	  var values = {};
    	  values["config_canvasapp_key"]=view.$el.find("[name='config_canvasapp_key']").val();
    	      values["config_apiKey"]=view.$el.find("[name='config_apiKey']").val();
    	      values["config_apiSecret"]=view.$el.find("[name='config_apiSecret']").val();
    	      values["config_callBackUrl"]=view.$el.find("[name='config_callBackUrl']").val();
    	      values["needAdmin"]="false";
    	      values["orgId"]=-1;
    	      app.getJsonData("/config/save", values,"Post").done(function(data){
    	          window.location.href="/";  
    	  });  
    	}
    }
  });
  
})(jQuery);