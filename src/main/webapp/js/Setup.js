(function($){
  
  brite.registerView("Setup",{parent:".admincontainer",emptyParent:true},{
    // --------- View Interface Implement--------- //
    create: function(){
      return render("Setup");
    },
    postDisplay:function(data){
      var view = this;
      view.section = app.pathInfo.paths[0] || "setup";

      view.$navTabs = $(".nav-tabs");
      view.$tabContent = view.$el.find(".tab-content");
      view.$navTabs.find("li.active").removeClass("active");
      if(view.$navTabs.find('li').size() > 3){
			view.$navTabs.find('li:last').prev('li').remove();
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
    // --------- /View Interface Implement--------- //
    
    // --------- Events--------- //
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
    	  app.getJsonData("/checkSysSchema",{},{type:"Get"}).done(function(result){
    		  if(result.pgtrgm){
    			  view.$el.find(".create_pg_trgm").prop("disabled",true).html("pg_trgm Created");
    		  }else{
    			  view.$el.find(".create_pg_trgm").prop("disabled",false).html("Create pg_trgm");
    		  }
    		  var schemaInfo = "";
    		  if(!result.schema_create){
    			  schemaInfo+="schema not created"
    		  }else{
	    		  if(!result.tables.config){
	    			  schemaInfo+="config "
	    		  }
	    		  if(!result.tables.org){
	    			  schemaInfo+="org "
	    		  }
	    		  if(!result.tables.zipcode_us){
	    			  schemaInfo+="zipcode_us "
	    		  }
    		  }
    			
    		  if(schemaInfo){
    			  view.$el.find(".create").prop("disabled",false).html("Create System schema");
    		  }else{
    			  console.log(schemaInfo);
    			  view.$el.find(".create").prop("disabled",true).html("System schema Created");
    		  }
    		  
    		  if(result.zipcode_import){
    			  view.$el.find(".import").prop("disabled",true).html("Zipcode table Imported");
    		  }else{
    			  if(result.tables.zipcode_us){
    				  view.$el.find(".import").prop("disabled",false).html("Import Zipcode table");
    			  }else{
    				  view.$el.find(".import").prop("disabled",true).html("Import Zipcode table");
    			  }
    		  }
        	
    		  if(result.errorMsg){
    			  view.$el.find(".create").closest(".setting").find(".alert").removeClass("transparent").html("Fail to load status,Please try to refresh page.")
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
              var callbackUrl = $.trim(view.$el.find("[name='config_callBackUrl']").val());
              if(validateURL(callbackUrl)){
    	        configs["config_callBackUrl"]=view.$el.find("[name='config_callBackUrl']").val();
              }
    	      values["orgId"]=-1;
    	      values.configsJson = JSON.stringify(configs);
    	      app.getJsonData("/config/save", values,"Post").done(function(data){
    	          window.location.href="/";
    	  });
    	}
    }
    // --------- /Events--------- //
  });

    function validateURL(textval) {
        var urlregex = new RegExp(
            "^(http|https)\://([a-zA-Z0-9\.\-]+(\:[a-zA-Z0-9\.&amp;%\$\-]+)*@)*((25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9])\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[0-9])|localhost|([a-zA-Z0-9\-]+\.)*[a-zA-Z0-9\-]+\.(com|edu|gov|int|mil|net|org|biz|arpa|info|name|pro|aero|coop|museum|[a-zA-Z]{2}))(\:[0-9]+)*(/($|[a-zA-Z0-9\.\,\?\'\\\+&amp;%\$#\=~_\-]+))*$");
        return urlregex.test(textval);
    }
})(jQuery);