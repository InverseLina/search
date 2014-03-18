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
      
      getStatus.call(view);

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
      	view.$el.trigger("START");
      },
      "click;.setupResume:not([disabled])":function(event){
      	var view = this;
      	view.$el.trigger("START");
      },
      "click;.setupPause:not([disabled])":function(event){
      	var view = this;
      	view.$el.trigger("PAUSE");
      },
      "click;.setupReset:not([disabled])":function(event){
      	var view = this;
      	view.$el.trigger("RESET");
      },
      "START":function(event){
    	  var view = this;
    	  app.getJsonData("/admin-sys-start",{},{type:"Post"}).done(function(data){
    	  	view.$el.trigger("STATUS_CHANGE", data);
    	  	startTimer.call(view);
    	  });
      },
      "PAUSE":function(event){
    	  var view = this;
    	  app.getJsonData("/admin-sys-pause",{},{type:"Post"}).done(function(data){
    	  	view.$el.trigger("STATUS_CHANGE", data);
    	  	stopTimer.call(view);
    	  });
      },
      "RESET":function(event){
    	  var view = this;
    	  app.getJsonData("/admin-sys-reset",{},{type:"Post"}).done(function(data){
    	  	view.$el.trigger("STATUS_CHANGE", data);
    	  });
      },
      "STATUS_CHANGE":function(event, statusData){
    	  var view = this;
    	  var $e = view.$el;
    	  $e.find(".save,.button").addClass("disabled");
    	  var $btnStart = $e.find(".setupStart");
    	  var $btnPause = $e.find(".setupPause");
    	  var $btnReset = $e.find(".setupReset");
    	  var $btnResume = $e.find(".setupResume");
    	  var $alertCreateSchema = $e.find(".create .alert").removeClass("alert-warning alert-success alert-error alert-info");
    	  var $alertImportZipcode = $e.find(".import .alert").removeClass("alert-warning alert-success alert-error alert-info");;
    	  var $alertCreatePgTrgm = $e.find(".create_pg_trgm .alert").removeClass("alert-warning alert-success alert-error alert-info");;
    	  var $alertImportCity = $e.find(".import-city .alert").removeClass("alert-warning alert-success alert-error alert-info");;
    	  var $alertCheckColumns = $e.find(".check-columns .alert").removeClass("alert-warning alert-success alert-error alert-info");;
    	  if(!statusData){
    	  	return ;
    	  }
    	  console.log(statusData);
    	  if(statusData.create_sys_schema.status == "done"){
    	  	$alertCreateSchema.addClass("alert-success").html("Done");
    	  }else if(statusData.create_sys_schema.status == "incomplete"){
    	  	$alertCreateSchema.addClass("alert-warning").html("Incomplete");
    	  }else{
    	  	$alertCreateSchema.addClass("alert-info").html(statusData.create_sys_schema.status);
    	  }
    	  
    	  if(statusData.import_zipcode.status == "done"){
    	  	$alertImportZipcode.addClass("alert-success").html("Done");
    	  }else if(statusData.import_zipcode.status == "incomplete"){
    	  	$alertImportZipcode.addClass("alert-warning").html("Incomplete");
    	  }else{
    	  	$alertImportZipcode.addClass("alert-info").html(statusData.import_zipcode.status);
    	  }
    	  
    	  if(statusData.create_extension.status == "done"){
    	  	$alertCreatePgTrgm.addClass("alert-success").html("Done");
    	  }else if(statusData.create_extension.status == "incomplete"){
    	  	$alertCreatePgTrgm.addClass("alert-warning").html("Incomplete");
    	  }else{
    	  	$alertCreatePgTrgm.addClass("alert-info").html(statusData.create_extension.status);
    	  }
    	  
    	  if(statusData.import_city.status == "done"){
    	  	$alertImportCity.addClass("alert-success").html("Done");
    	  }else if(statusData.import_city.status == "incomplete"){
    	  	$alertImportCity.addClass("alert-warning").html("Incomplete");
    	  }else{
    	  	$alertImportCity.addClass("alert-info").html(statusData.import_city.status);
    	  }
    	  
    	  if(statusData.check_missing_columns.status == "done"){
    	  	$alertCheckColumns.addClass("alert-success").html("Done");
    	  }else if(statusData.check_missing_columns.status == "incomplete"){
    	  	$alertCheckColumns.addClass("alert-warning").html("Incomplete, "+"<b>Missing column(s):</b>"+statusData.check_missing_columns.missingsColumns);
    	  }else{
    	  	$alertCheckColumns.addClass("alert-info").html(statusData.check_missing_columns.status);
    	  }
    	  
    	  if(statusData.status == "notstarted"){
    	  	$btnStart.removeClass("hide").prop("disabled",false);
    	  	$btnReset.removeClass("hide").prop("disabled",false).html("Reset");
    	  	// $btnStart.removeClass("hide").prop("disabled",false);
    	  	// $btnResume.addClass("hide");
    	  	// $btnPause.removeClass("hide").prop("disabled",true);
    	  	// $btnReset.removeClass("hide").prop("disabled",true);
    	  	
    	  	stopTimer.call(view);
    	  }else if(statusData.status == "running"){
    	  	$btnStart.removeClass("hide").prop("disabled",true);
    	  	$btnReset.removeClass("hide");
    	  	if(statusData.caceling){
	    	  	$btnReset.html("Reseting...").prop("disabled",true);
    	  	}else{
	    	  	$btnReset.html("Reset").prop("disabled",false);
    	  	}
    	  	// $btnStart.addClass("hide");
    	  	// $btnResume.removeClass("hide").prop("disabled",true);
    	  	// $btnPause.removeClass("hide").prop("disabled",false);
    	  	// $btnReset.removeClass("hide").prop("disabled",false);
    	  	startTimer.call(view);
    	  }else if(statusData.status == "incomplete"){
    	  	$btnStart.removeClass("hide").prop("disabled",false);
    	  	$btnReset.removeClass("hide").prop("disabled",false).html("Reset");
    	  	// $btnStart.addClass("hide");
    	  	// $btnResume.removeClass("hide").prop("disabled",false);
    	  	// $btnPause.removeClass("hide").prop("disabled",true);
    	  	// $btnReset.removeClass("hide").prop("disabled",false);
    	  	
    	  	stopTimer.call(view);
    	  }else if(statusData.status == "done"){
    	  	$btnStart.addClass("hide");
    	  	$btnResume.addClass("hide");
    	  	$btnPause.addClass("hide");
    	  	$btnReset.addClass("hide");
    	  	stopTimer.call(view);
    	  	
    	  	$e.find(".setting .alert").removeClass("alert-info").addClass("alert-success").html("Done");
			$e.find(".save,.button").removeClass("disabled");
    	  	$e.trigger("DO_SHOW_ORG_TAB");
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
  		if(!view.timer){
	  		view.timer = true;
			getStatus.call(view);
  		}

  	}
  	
  	function stopTimer(){
  		var view = this;
  		if(view.timer){
	  		view.timer = false;
  		}
  	}
  	
  	function getStatus(){
  		var view = this;
  		var $e = view.$el;
		app.getJsonData("/admin-sys-status", {}, "Get").done(function(data) {
			$e.trigger("STATUS_CHANGE", data);
			if (view.timer) {
				setTimeout(function(){
					getStatus.call(view);
				},1000);
			}
		}); 

  	}

    function validateURL(textval) {
        var urlregex = new RegExp(
            "^(http|https)\://([a-zA-Z0-9\.\-]+(\:[a-zA-Z0-9\.&amp;%\$\-]+)*@)*((25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9])\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[0-9])|localhost|([a-zA-Z0-9\-]+\.)*[a-zA-Z0-9\-]+\.(com|edu|gov|int|mil|net|org|biz|arpa|info|name|pro|aero|coop|museum|[a-zA-Z]{2}))(\:[0-9]+)*(/($|[a-zA-Z0-9\.\,\?\'\\\+&amp;%\$#\=~_\-]+))*$");
        return urlregex.test(textval);
    }
    
})(jQuery);