(function($){
  
  brite.registerView("Setup",{parent:".admincontainer",emptyParent:true},{
    // --------- View Interface Implement--------- //
    create: function(){
      return render("Setup");
    },
    postDisplay:function(data){
      var view = this;
      var $e = view.$el;
      view.section = app.pathInfo.paths[0] || "setup";

      view.$navTabs = $(".nav-tabs");
      view.$tabContent = view.$el.find(".tab-content");
      view.$navTabs.find("li.active").removeClass("active");
      view.$navTabs.find("a[href='#setup']").closest("li").addClass("active");
      view.$el.find(".setting .alert").html("Loading...");
      
      app.getJsonData("/admin-sys-status", {},"Get").done(function(data){
  	  	$e.trigger("STATUS_CHANGE", data);
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
      "click;.setupStart:not([disabled])":function(event){
      	var view = this;
      	view.$el.trigger("START",false);
      },
      "click;.setupResume:not([disabled])":function(event){
      	var view = this;
      	view.$el.trigger("START",false);
      },
      "click;.setupPause:not([disabled])":function(event){
      	var view = this;
      	view.$el.trigger("PAUSE");
      },
      "click;.setupRestart:not([disabled])":function(event){
      	var view = this;
      	view.$el.trigger("START",true);
      },
      "START":function(event,force){
    	  var view = this;
    	  app.getJsonData("/admin-sys-start",{force:force},{type:"Post"}).done(function(data){
    	  	view.$el.trigger("STATUS_CHANGE", data);
    	  	startTimer.call(view);
    	  });
      },
      "PAUSE":function(event){
    	  var view = this;
    	  app.getJsonData("/admin-sys-pause",{},{type:"Post"}).done(function(data){
    	  	stopTimer.call(view);
    	  });
      },
      "STATUS_CHANGE":function(event, statusData){
    	  var view = this;
    	  var $e = view.$el;
    	  var $btnStart = $e.find(".setupStart");
    	  var $btnPause = $e.find(".setupPause");
    	  var $btnRestart = $e.find(".setupRestart");
    	  var $btnResume = $e.find(".setupResume");
    	  var $alertCreateSchema = $e.find(".create .alert");
    	  var $alertImportZipcode = $e.find(".import .alert");
    	  var $alertCreatePgTrgm = $e.find(".create_pg_trgm .alert");
    	  var $alertImportCity = $e.find(".import-city .alert");
    	  var $alertCheckColumns = $e.find(".check-columns .alert");
    	  console.log(statusData);
    	  if(!statusData){
    	  	return ;
    	  }
    	  
    	  if(statusData.create_sys_schema.status == "done"){
    	  	$alertCreateSchema.removeClass("alert-info").addClass("alert-success").html("Done");
    	  }else if(statusData.create_sys_schema.status == "incomplete"){
    	  	$alertCreateSchema.removeClass("alert-info").addClass("alert-warning").html("Incomplete");
    	  }else{
    	  	$alertCreateSchema.html(statusData.create_sys_schema.status);
    	  }
    	  
    	  if(statusData.import_zipcode.status == "done"){
    	  	$alertImportZipcode.removeClass("alert-info").addClass("alert-success").html("Done");
    	  }else if(statusData.import_zipcode.status == "incomplete"){
    	  	$alertImportZipcode.removeClass("alert-info").addClass("alert-warning").html("Incomplete");
    	  }else{
    	  	$alertImportZipcode.html(statusData.import_zipcode.status);
    	  }
    	  
    	  if(statusData.create_extension.status == "done"){
    	  	$alertCreatePgTrgm.removeClass("alert-info").addClass("alert-success").html("Done");
    	  }else if(statusData.create_extension.status == "incomplete"){
    	  	$alertCreatePgTrgm.removeClass("alert-info").addClass("alert-warning").html("Incomplete");
    	  }else{
    	  	$alertCreatePgTrgm.html(statusData.create_extension.status);
    	  }
    	  
    	  if(statusData.import_city.status == "done"){
    	  	$alertImportCity.removeClass("alert-info").addClass("alert-success").html("Done");
    	  }else if(statusData.import_city.status == "incomplete"){
    	  	$alertImportCity.removeClass("alert-info").addClass("alert-warning").html("Incomplete");
    	  }else{
    	  	$alertImportCity.html(statusData.import_city.status);
    	  }
    	  
    	  if(statusData.check_missing_columns.status == "done"){
    	  	$alertCheckColumns.removeClass("alert-info").addClass("alert-success").html("Done");
    	  }else if(statusData.check_missing_columns.status == "incomplete"){
    	  	$alertCheckColumns.removeClass("alert-info").addClass("alert-warning").html("Incomplete, "+"<b>Missing column(s):</b>"+statusData.check_missing_columns.missingsColumns);
    	  }else{
    	  	$alertCheckColumns.html(statusData.check_missing_columns.status);
    	  }
    	  
    	  if(statusData.status == "notstarted"){
    	  	$btnStart.removeClass("hide").prop("disabled",false);
    	  	// $btnStart.removeClass("hide").prop("disabled",false);
    	  	// $btnResume.addClass("hide");
    	  	// $btnPause.removeClass("hide").prop("disabled",true);
    	  	// $btnRestart.removeClass("hide").prop("disabled",true);
    	  	
    	  	stopTimer.call(view);
    	  }else if(statusData.status == "running"){
    	  	$btnStart.removeClass("hide").prop("disabled",true).html("Running...");
    	  	// $btnStart.addClass("hide");
    	  	// $btnResume.removeClass("hide").prop("disabled",true);
    	  	// $btnPause.removeClass("hide").prop("disabled",false);
    	  	// $btnRestart.removeClass("hide").prop("disabled",false);
    	  	
    	  	startTimer.call(view);
    	  }else if(statusData.status == "incomplete"){
    	  	$btnStart.removeClass("hide").prop("disabled",false);
    	  	// $btnStart.addClass("hide");
    	  	// $btnResume.removeClass("hide").prop("disabled",false);
    	  	// $btnPause.removeClass("hide").prop("disabled",true);
    	  	// $btnRestart.removeClass("hide").prop("disabled",false);
    	  	
    	  	stopTimer.call(view);
    	  }else if(statusData.status == "done"){
    	  	$btnStart.hide();
    	  	$btnResume.hide();
    	  	$btnPause.hide();
    	  	$btnRestart.hide();
    	  	stopTimer.call(view);
    	  	
    	  	$e.find(".setting .alert").removeClass("alert-info").addClass("alert-success").html("Done");
    	  }
    	  
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
    	  var $btn = $(event.currentTarget);
		  $btn.prop("disabled",true).html("saving...");
	      configs["saleforce.apiKey"]=view.$el.find("[name='saleforce.apiKey']").val();
	      configs["saleforce.apiSecret"]=view.$el.find("[name='saleforce.apiSecret']").val();
	      configs["jss.feature.userlist"]=view.$el.find("[name='jss.feature.userlist']").val();
          //var callbackUrl = $.trim(view.$el.find("[name='saleforce.callBackUrl']").val());
//              if(validateURL(callbackUrl)){
//    	        configs["saleforce.callBackUrl"]=view.$el.find("[name='saleforce.callBackUrl']").val();
//              }
	      values["orgId"]=-1;
	      values.configsJson = JSON.stringify(configs);
	      app.getJsonData("/config/save", values,"Post").done(function(data){
	          //window.location.href = window.location.href;
	    	  view.$el.trigger("DO_SHOW_MSG",{selector:".config-alert",msg:"Values saved successfully",type:"success"});
	    	  $btn.prop("disabled",false).html("Save");
	      });
      }
    }
    // --------- /Events--------- //
  });
  
  	function startTimer(){
  		var view = this;
  		var $e = view.$el;
  		if(!view.timer){
  			view.timer = setInterval(function(){
  				app.getJsonData("/admin-sys-status", {},"Get").done(function(data){
  					$e.trigger("STATUS_CHANGE", data);
			    });
  			}, 500);
  		}
  	}
  	
  	function stopTimer(){
  		var view = this;
  		if(view.timer){
  			clearInterval(view.timer);
  			view.timer = null;
  		}
  	}

    function validateURL(textval) {
        var urlregex = new RegExp(
            "^(http|https)\://([a-zA-Z0-9\.\-]+(\:[a-zA-Z0-9\.&amp;%\$\-]+)*@)*((25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9])\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[0-9])|localhost|([a-zA-Z0-9\-]+\.)*[a-zA-Z0-9\-]+\.(com|edu|gov|int|mil|net|org|biz|arpa|info|name|pro|aero|coop|museum|[a-zA-Z]{2}))(\:[0-9]+)*(/($|[a-zA-Z0-9\.\,\?\'\\\+&amp;%\$#\=~_\-]+))*$");
        return urlregex.test(textval);
    }
    
})(jQuery);