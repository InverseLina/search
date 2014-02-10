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
      view.$navTabs.find("a[href='#setup']").closest("li").addClass("active");
      view.$el.find(".create,.import,.create_pg_trgm").prop("disabled",true).html("Loading...");
      view.$el.trigger("STATUS_CHANGE");

      app.getJsonData("/config/get/",{orgId:-1}).done(function(data){
    	if(view && view.$el){
    	  	view.$el.trigger("FILLDATA",{data:data});
    	}
      });
      
      brite.display("AdminSearchConfig");
    },
    // --------- /View Interface Implement--------- //
    
    // --------- Events--------- //
    events:{
      "btap;.cancel":function(event){
    		window.location.href=contextPath + "/";
       },
      "btap;.home":function(event){
        window.location.href=contextPath + "/";
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
    			  $createBtn.prop("disabled",false).html("Create System schema").removeClass("btn-success");
    		  }else{
    			  $createBtn.html("System schema Created").addClass("btn-success");
    			  view.$el.find(".import").prop("disabled",false).removeClass("btn-success");
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
    			  $createBtn.prop("disabled",false).html("Create Extensions").removeClass("btn-success");
    		  }else{
    			  view.$el.trigger("STATUS_CHANGE");
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
    			  $importBtn.prop("disabled",false).html("Import Zipcode table").removeClass("btn-success");
    		  }else{
    			  $importBtn.html("Zipcode table Imported").addClass("btn-success");
    			  $alert.html("&nbsp;").addClass("transparent");
    		  }
    	  });
      },
      "STATUS_CHANGE":function(event){
    	  var view = this;
    	  app.getJsonData("/checkSysSchema",{},{type:"Get"}).done(function(result){
    		  var createdExtensionsInfo="",extentionWarning="",missingExentions="" ;
    		  $.each(result.extensions,function(e,index){
    			  if(result.extensions[e]==1){
    				  createdExtensionsInfo +=e+" , ";
    			  }else if(result.extensions[e]==2){
    				  extentionWarning +=e+" , "
    			  }else{
    				  missingExentions+=e+" , ";
    			  }
    		  });
    		  
    		  if(!missingExentions){
    			  view.$el.find(".create_pg_trgm").prop("disabled",true).html("Extensions Created").addClass("btn-success");
    		  }else{
    			  view.$el.find(".create_pg_trgm").prop("disabled",false).html("Create Extensions").removeClass("btn-success");
    		  }
    		  if(createdExtensionsInfo||extentionWarning||missingExentions){
    			  var info = "";
    			  var $alert =  view.$el.find(".create_pg_trgm").closest(".setting").find(".alert");
    			  if(extentionWarning){
    				  info+=" <strong>Extention(s) not in pg_catalog: </strong>"+extentionWarning.substring(0, extentionWarning.length-2);
    			  }
    			  if(missingExentions){
    				  info+=" <strong>Missing Extention(s): </strong>"+missingExentions.substring(0, missingExentions.length-2);
    			  }
    			  if(!(extentionWarning||missingExentions)){//when there no warnings and no missing,show the created info
    				  $alert.removeClass("alert-danger").addClass("alert-info");
    				  if(createdExtensionsInfo){
        				  info+=" <strong>Created Extention(s): </strong>"+createdExtensionsInfo.substring(0, createdExtensionsInfo.length-2);
        			  }
    			  }else if(extentionWarning&&!missingExentions){//when only has warning,make button gray see#893
    				  view.$el.find(".create_pg_trgm").prop("disabled",true).html("Create Extensions").removeClass("btn-success");
    			  }
    			  $alert.html(info).removeClass("transparent");
    		  }
    		  var schemaInfo = "";
    		  if(!result.schema_create){
    			  schemaInfo+="schema not created";
    		  }else{
	    		  if(!result.tables.config){
	    			  schemaInfo+="config ";
	    		  }
	    		  if(!result.tables.org){
	    			  schemaInfo+="org ";
	    		  }
	    		  if(!result.tables.zipcode_us){
	    			  schemaInfo+="zipcode_us ";
	    		  }
    		  }
    			
    		  if(schemaInfo){
    			  view.$el.find(".create").prop("disabled",false).html("Create System schema").removeClass("btn-success");
    		  }else{
    			  view.$el.find(".create").prop("disabled",true).html("System schema Created").addClass("btn-success");
    		  }
    		  
    		  if(result.zipcode_import){
    			  view.$el.find(".import").prop("disabled",true).html("Zipcode table Imported").addClass("btn-success");
    		  }else{
    			  if(result.tables.zipcode_us){
    				  view.$el.find(".import").prop("disabled",false).html("Import Zipcode table").removeClass("btn-success");
    			  }else{
    				  view.$el.find(".import").prop("disabled",true).html("Import Zipcode table").removeClass("btn-success");
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
    	  $.each(result.data,function(key,value){
    	    currentField = view.$el.find("[name='"+key+"']");
    	    if(currentField.length > 0){
    	      currentField.val(value);
    	    }
    	  });
    	 },
      "btap;.save":function(event){
    	  var view = this;
    	  var values = {};
    	  var configs = {};
    	      configs["saleforce.apiKey"]=view.$el.find("[name='saleforce.apiKey']").val();
    	      configs["saleforce.apiSecret"]=view.$el.find("[name='saleforce.apiSecret']").val();
    	      configs["jss.feature.userlist"]=view.$el.find("[name='jss.feature.userlist']").val();
              var callbackUrl = $.trim(view.$el.find("[name='saleforce.callBackUrl']").val());
              if(validateURL(callbackUrl)){
    	        configs["saleforce.callBackUrl"]=view.$el.find("[name='saleforce.callBackUrl']").val();
              }
    	      values["orgId"]=-1;
    	      console.log(configs);
    	      values.configsJson = JSON.stringify(configs);
    	      app.getJsonData("/config/save", values,"Post").done(function(data){
    	          window.location.href=contextPath + "/";
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