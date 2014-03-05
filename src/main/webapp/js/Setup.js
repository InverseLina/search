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
      view.$el.find(".create,.import,.create_pg_trgm,.import-city,.compute-city").prop("disabled",true).html("Loading...");
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
        		  refresh.call(view);
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
    			  refresh.call(view);
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
    			  refresh.call(view);
    		  }
    	  });
      },
      "click;.compute-city":function(event){
    	  var view = this;
    	  var $computeBtn = $(event.target);
    	  if($computeBtn.prop("disabled")){
    		  return false;
    	  }
    	  var $alert = $computeBtn.closest(".setting").find(".alert");
    	  $computeBtn.prop("disabled",true).html("computing...");
    	  view.$el.find(".import-city").prop("disabled",true);
    	  app.getJsonData("/computeCity",{},{type:"Post"}).done(function(data){
    		  if(data){
    			  $alert.removeClass("transparent").html("ErrorCode:"+data.errorCode+"<p>"+data.errorMsg);
    			  $computeBtn.prop("disabled",false).html("Compute City long/lat").removeClass("btn-success");
    			  view.$el.find(".import-city").prop("disabled",false).removeClass("btn-success").html("Import City");
    		  }else{
    			  $computeBtn.html("City long/lat computed").addClass("btn-success");
    			  view.$el.find(".import-city").prop("disabled",true).addClass("btn-success").html("City Imported");
    			  $alert.html("&nbsp;").addClass("transparent");
    		  }
    		  refresh.call(view);
    	  });
      },
      "click;.import-city":function(event){
    	  var view = this;
    	  var $importBtn = $(event.target);
    	  if($importBtn.prop("disabled")){
    		  return false;
    	  }
    	  var $alert = $importBtn.closest(".setting").find(".alert");
    	  $importBtn.prop("disabled",true).html("importing...");
    	  view.$el.find(".compute-city").prop("disabled",true);
    	  app.getJsonData("/importCity",{},{type:"Post"}).done(function(data){
    		  if(data){
    			  $alert.removeClass("transparent").html("ErrorCode:"+data.errorCode+"<p>"+data.errorMsg);
    			  $importBtn.prop("disabled",false).html("Import City").removeClass("btn-success");
    			  view.$el.find(".compute-city").prop("disabled",false).removeClass("btn-success").html("Compute City long/lat");
    		  }else{
    			  $importBtn.html("City Imported").addClass("btn-success");
    			  view.$el.find(".compute-city").prop("disabled",true).addClass("btn-success").html("City long/lat Computed");
    			  $alert.html("&nbsp;").addClass("transparent");
    		  }
    		  refresh.call(view);
    	  });
      },
      "click;.fix-missing-columns":function(event){
			var $btn = $(event.currentTarget);
			var view = this;
			app.getJsonData("/fixJssColumns",{orgName:view.currentOrgName,sys:true},{type:"Post"}).done(function(result){
				if(!result){
					$btn.addClass("hide");
					view.$el.find(".jsstable-info").removeClass("alert-danger").addClass("alert-success").html("jss_sys tables valid");
					refresh.call(view);
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
    		  if(view.$el){
    			  if(result.jssTable==""){
    				  view.$el.find(".columns-info").removeClass("alert-danger").addClass("alert-success").html("jss_sys Tables Valid");
    			  }else{
    				  view.$el.find(".columns-info").removeClass("alert-success")
    				  .addClass("alert-danger").html("<b>Missing column(s):</b>"+result.jssTable);
    			  }
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

                  if(result.city){
                      view.$el.find(".compute-city").prop("disabled",true).addClass("btn-success").html("City long/lat Computed");
                      view.$el.find(".import-city").prop("disabled",true).addClass("btn-success").html("City Imported");
                  }else{
                      view.$el.find(".compute-city").prop("disabled",false).removeClass("btn-success").html("Compute City long/lat");
                      view.$el.find(".import-city").prop("disabled",false).removeClass("btn-success").html("Import City");
                  }

                  var schemaInfo = "";
                  if(!result.schema_create){
                      schemaInfo+="schema not created";
                      view.$el.find(".schema-info").removeClass("alert-success").addClass("alert-danger").html("jss_sys Schema Not Exists");
                  }else{
                	  view.$el.find(".schema-info").removeClass("alert-danger").addClass("alert-success").html("jss_sys Schema Exists");
                      if(!result.tables.config){
                          schemaInfo+="config ";
                      }
                      if(!result.tables.org){
                          schemaInfo+="org ";
                      }
                      if(!result.tables.zipcode_us){
                          schemaInfo+="zipcode_us ";
                      }
                      if(!result.tables.city){
                          schemaInfo+="city ";
                      }
                      if(schemaInfo){
                          schemaInfo="Missing table(s): "+schemaInfo;
                      }
                  }

                  if(schemaInfo){
                      view.$el.find(".create").prop("disabled",false).html("Create System schema").removeClass("btn-success");
                      view.$el.find(".create").closest(".setting").find(".alert").removeClass("transparent").html(schemaInfo);
                  }else{
                      view.$el.find(".create").prop("disabled",true).html("System schema Created").addClass("btn-success");
                      view.$el.find(".create").closest(".setting").find(".alert").addClass("transparent").html("&nbsp;");
                      view.$el.trigger("DO_SHOW_ORG_TAB");
                      if(result.jssTable!=""){
                    	  view.$el.find(".fix-missing-columns").removeClass("hide");
                      }
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
              //var callbackUrl = $.trim(view.$el.find("[name='saleforce.callBackUrl']").val());
//              if(validateURL(callbackUrl)){
//    	        configs["saleforce.callBackUrl"]=view.$el.find("[name='saleforce.callBackUrl']").val();
//              }
    	      values["orgId"]=-1;
    	      values.configsJson = JSON.stringify(configs);
    	      app.getJsonData("/config/save", values,"Post").done(function(data){
    	          //window.location.href = window.location.href;
    	    	  view.$el.trigger("DO_SHOW_MSG",{selector:".config-alert",msg:"Values saved successfully",type:"success"});
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
    
    function refresh(){
    	var view = this;
		view.$el.trigger("STATUS_CHANGE");
    }
})(jQuery);