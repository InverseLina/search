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
      view.$el.find(".create,.import,.create_pg_trgm").prop("disabled",true).html("Loading...");
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
    	  var $alert = $createBtn.closest(".setting").find(".alert");
    	  $alert.addClass("transparent");
    	  $createBtn.prop("disabled",true).html("Creating...");
    	  app.getJsonData("/createSysSchema",{},{type:"Post"}).done(function(data){
    		  if(data){
    			  $alert.removeClass("transparent").html("ErrorCode:"+data.errorCode+"<p>"+data.errorMsg); 
    			  $createBtn.prop("disabled",false).html("Create System schema");
    		  }else{
    			  $createBtn.html("System schema Created");
    			  view.$el.find(".import").prop("disabled",false);
    			  $alert.html("&nbsp;").addClass("transparent");
        		  $(".organization-tab").removeClass("hide");
    		  }
    	  });
      },
      "click;.create_pg_trgm":function(event){
    	  var view = this;
    	  var $createBtn = $(event.target);
    	  if($createBtn.prop("disabled")){
    		  return false;
    	  }
    	  var $alert = $createBtn.closest(".setting").find(".alert");
    	  $alert.addClass("transparent");
    	  $createBtn.prop("disabled",true).html("Creating...");
    	  app.getJsonData("/createPgTrgm",{},{type:"Post"}).done(function(data){
    		  if(data){
    			  $alert.removeClass("transparent").html("ErrorCode:"+data.errorCode+"<p>"+data.errorMsg); 
    			  $createBtn.prop("disabled",false).html("Create pg_trgm");
    		  }else{
    			  $createBtn.html("pg_trgm Created");
    			  $alert.html("&nbsp;").addClass("transparent");
    		  }
    	  });
      },
      "click;.import":function(event){
    	  var view = this;
    	  var $importBtn = $(event.target);
    	  if($importBtn.prop("disabled")){
    		  return false;
    	  }
    	  var $alert = $importBtn.closest(".setting").find(".alert");
    	  $importBtn.prop("disabled",true).html("importing...");
    	  app.getJsonData("/updateZipCode",{},{type:"Post"}).done(function(data){
    		  if(data){
    			  $alert.removeClass("transparent").html("ErrorCode:"+data.errorCode+"<p>"+data.errorMsg); 
    			  $importBtn.prop("disabled",false).html("Import Zipcode table");
    		  }else{
    			  $importBtn.html("Zipcode table Imported");
    			  $alert.html("&nbsp;").addClass("transparent");
    		  }
    	  });
      },
      "STATUS_CHANGE":function(event){
    	  var view = this;
    	  app.getJsonData("/checkSetupStatus",{type:"SYSTEM"},{type:"Get"}).done(function(result){
    		  console.log(result);
        	  switch(result){
        	  case 0:	view.$el.find(".create").prop("disabled",false).html("Create System schema");
        	  			view.$el.find(".import").prop("disabled",true).html("Import Zipcode table");
        	  			view.$el.find(".create_pg_trgm").prop("disabled",false).html("Create pg_trgm");
        	  			break;
        	  case 1:	view.$el.find(".create").prop("disabled",true).html("System schema Created");
        	  			view.$el.find(".import").prop("disabled",false).html("Import Zipcode table");
        	  			view.$el.find(".create_pg_trgm").prop("disabled",false).html("Create pg_trgm")
    					break;
        	  case 2:	view.$el.find(".create").prop("disabled",true).html("System schema Created");
    		  			view.$el.find(".import").prop("disabled",true).html("Zipcode table Imported");
    		  			view.$el.find(".create_pg_trgm").prop("disabled",false).html("Create pg_trgm")
    					break;
        	  case 7:	view.$el.find(".create").prop("disabled",false).html("Create System schema");
			  			view.$el.find(".import").prop("disabled",true).html("Import Zipcode table");
			  			view.$el.find(".create_pg_trgm").prop("disabled",true).html("pg_trgm Created");
						break;
        	  case 8:	view.$el.find(".create").prop("disabled",true).html("System schema Created");
        	  			view.$el.find(".import").prop("disabled",false).html("Import Zipcode table");
			  			view.$el.find(".create_pg_trgm").prop("disabled",true).html("pg_trgm Created");
						break;
        	  case 9:	view.$el.find(".create").prop("disabled",true).html("System schema Created");
			  			view.$el.find(".import").prop("disabled",true).html("Zipcode table Imported");
			  			view.$el.find(".create_pg_trgm").prop("disabled",true).html("pg_trgm Created");
			  			break;
    		  default: view.$el.find(".create").closest(".setting").find(".alert").removeClass("transparent").html("Fail to load status,Please try to refresh page.")
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
    	  var configs = {};
    	  configs["config_canvasapp_secret"]=view.$el.find("[name='config_canvasapp_secret']").val();
    	      configs["config_apiKey"]=view.$el.find("[name='config_apiKey']").val();
    	      configs["config_apiSecret"]=view.$el.find("[name='config_apiSecret']").val();
    	      configs["config_callBackUrl"]=view.$el.find("[name='config_callBackUrl']").val();
    	      values["orgId"]=-1;
    	      values.configJson = JSON.stringify(configs);
    	      app.getJsonData("/config/save", values,"Post").done(function(data){
    	          window.location.href="/";  
    	  });  
    	}
    }
  });
  
})(jQuery);