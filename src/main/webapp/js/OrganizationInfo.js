(function($){
	
  brite.registerView("OrganizationInfo",{parent:".admincontainer",emptyParent:true},{
    create: function(){
      return render("OrganizationInfo");
    },
    postDisplay:function(data){
      var view = this;
      
      view.section = app.pathInfo.paths[0] || "organization";
		
      view.$navTabs = $(".nav-tabs");
	  view.$tabContent = view.$el.find(".tab-content");
	  view.$navTabs.find("li.active").removeClass("active");
      
      if(app.pathInfo.paths[1] == "add"){
    	  var li = render("OrganizationInfo-li",{type:"OrganizationInfo",url:"#organization/add"});
    	  view.$navTabs.append(li);	
    	  var html = render("OrganizationInfo-content",{data:null});
    	  view.$tabContent.html(html);
    	  view.orgId = -1;
       }else if(app.pathInfo.paths[1] == "edit"){
    	   view.orgId = app.pathInfo.paths[2] * 1;
    	  getDate.call(view,app.pathInfo.paths[2] * 1).done(function(orgName){
    		  var li = render("OrganizationInfo-li",{type:"Organization:"+orgName,url:"#"+app.pathInfo.paths[0]+"/"+app.pathInfo.paths[1]+"/"+app.pathInfo.paths[2]});
         	  view.$navTabs.append(li);	
    	  }); 
       }
    },
    
    events:{
      "btap;.home":function(event){
    	window.location.href="/";
      },
      "btap;.cancel":function(event){
        window.location.href="/admin#organization";
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
    			//values["config_canvasapp_key"]=view.$el.find("[name='config_canvasapp_key']").val();
		        //values["config_apiKey"]=view.$el.find("[name='config_apiKey']").val();
		        //values["config_apiSecret"]=view.$el.find("[name='config_apiSecret']").val();
		        //values["config_callBackUrl"]=view.$el.find("[name='config_callBackUrl']").val();
		        values["needAdmin"]="true";
		        values["orgId"]= view.orgId;
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
		},
		"click;.extra":function(event){
			var view = this;
			var $createExtraBtn = $(event.target);
			if($createExtraBtn.prop("disabled")){
				return false;
			}
			$createExtraBtn.prop("disabled",true).html("Creating...");
			var $alert = $createExtraBtn.closest("tr").find(".alert");
			$alert.addClass("transparent");
			app.getJsonData("/createExtraTables", {orgName:view.currentOrgName},{type:"Post"}).done(function(data){
				if(data){
					$alert.removeClass("transparent").html("ErrorCode:"+data.errorCode+"<p>"+data.errorMsg); 
					$createExtraBtn.prop("disabled",false).html("Create");
				}else{
					$createExtraBtn.html("Created");
					view.$el.find(".resume").prop("disabled",false).html("Run");
					$alert.addClass("hide");
				}
			});
		},
		"click;.index":function(event){
			var view = this;
			var $createIndexBtn = $(event.target);
			if($createIndexBtn.prop("disabled")){
				return false;
			}
			var $alert = $createIndexBtn.closest("tr").find(".alert");
			$createIndexBtn.prop("disabled",true).html("Creating...");
			$alert.addClass("transparent");
			app.getJsonData("/createIndexColumns", {orgName:view.currentOrgName},{type:"Post"}).done(function(data){
				if(data){
					$alert.removeClass("transparent").html("ErrorCode:"+data.errorCode+"<p>"+data.errorMsg); 
					$createIndexBtn.prop("disabled",false).html("Create");
				}else{
					$createIndexBtn.html("Created");
					$alert.addClass("hide");
				}
			});
		},
		"click;.resume":function(event){
			var view = this;
			var $createIndexBtn = $(event.target);
			if($createIndexBtn.prop("disabled")){
				return false;
			}
			var $alert = $createIndexBtn.closest("tr").find(".alert");
			if($createIndexBtn.html()=="Run"){
				$alert.addClass("transparent");
				$createIndexBtn.html("Stop");
				app.getJsonData("/createIndexResume", {orgName:view.currentOrgName},{type:"Post"}).done(function(data){
					if(data){
						$alert.removeClass("transparent").html("ErrorCode:"+data.errorCode+"<p>"+data.errorMsg); 
						$createIndexBtn.prop("disabled",false).html("Run");
					}else{
						view.$el.trigger("STATUS_CHANGE");
						$alert.addClass("hide");
					}
					window.clearInterval(view.intervalId);
				});
				view.intervalId = window.setInterval(function(){
			    	   $(view.el).trigger("RESUMEINDEXSTATUS");
			      }, 3000);
			}else if($createIndexBtn.html()=="Stop"){
				window.clearInterval(view.intervalId);
				$createIndexBtn.prop("disabled",true).html("Stopping");
				app.getJsonData("/stopCreateIndexResume", {orgName:view.currentOrgName},{type:"Post"}).done(function(data){
					$createIndexBtn.prop("disabled",false).html("Run");
					view.$el.trigger("STATUS_CHANGE");
				});
			}
		},
		"click;.status":function(event){
			var view = this;
			view.$el.trigger("STATUS_CHANGE");
		},
		"RESUMEINDEXSTATUS":function(event){
			var view = this;
			 app.getJsonData("/getResumeIndexStatus",{orgName:view.currentOrgName}).done(function(data){
				   view.$el.find(".index-info,.status").removeClass("hide");
	 	    	   view.$el.find(".index-info").html("Perform:"+data.perform+",Remaining:"+data.remaining);
	 	      });
		},
		 "STATUS_CHANGE":function(event){
	    	  var view = this;
	    	  var orgName = view.currentOrgName;
	    	  app.getJsonData("/checkSetupStatus",{type:"ORG",orgName:orgName},{type:"Get"}).done(function(result){
	    		  switch(result){
	    		  case 1:
	    		  case 2: 	view.$el.find(".resume").prop("disabled",true);
	    		  			break;
	        	  case 3:	view.$el.find(".extra").prop("disabled",true).html("Created");
	        	  			view.$el.trigger("RESUMEINDEXSTATUS");
	        	  			break;
	        	  case 4:	view.$el.find(".extra,.index").prop("disabled",true).html("Created");
	        	  			view.$el.find(".index-info,.status").removeClass("hide");
	        	  			view.$el.trigger("RESUMEINDEXSTATUS");
	    					break;
	        	  case 5:	view.$el.find(".extra").prop("disabled",true).html("Created");
	        		  		view.$el.find(".resume").prop("disabled",true).html("Created");
		    	    		view.$el.find(".index-info,.status").addClass("hide");
	    					break;
	        	  case 6:	view.$el.find(".extra").prop("disabled",true).html("Created");
	        		  		view.$el.find(".resume").prop("disabled",false).html("Stop");
	        		  		view.intervalId = window.setInterval(function(){
	     			    	   $(view.el).trigger("RESUMEINDEXSTATUS");
	        		  		}, 3000);
	        		  		view.$el.trigger("RESUMEINDEXSTATUS");
							break;
	        	  case 20:	view.$el.find(".extra,.resume,.index").prop("disabled",true).html("Created");
				    		view.$el.find(".index-info,.status").addClass("hide");
							break;
	        	  case 24:	view.$el.find(".extra,.index").prop("disabled",true).html("Created");
	        	  			view.$el.find(".resume").prop("disabled",false).html("Stop");
	        	  			view.$el.trigger("RESUMEINDEXSTATUS");
							break;
	        	  }
	          });
	      }
    }
  });
  
  function getDate(id){
    var view = this;
    var dfd = $.Deferred();
    app.getJsonData("/org/get/", {id:id}).done(function(data){
    	view.currentOrgName = data[0].name;
        var html = render("OrganizationInfo-content",{data:data[0]});
        view.$tabContent.bEmpty();
        view.$tabContent.html(html);
        view.$el.trigger("STATUS_CHANGE");
        dfd.resolve(data[0].name);
        app.getJsonData("/config/get/",{orgId:view.orgId}).done(function(data){
           if(view && view.$el){
    		    view.$el.trigger("FILLDATA",{data:data});
           }
    	});
    });
    return dfd.promise();
  }
  
  function doValidate(){
	    var view = this;
		var $nameMsg = view.$el.find(".alert-error.name");
		var $schemanameMsg = view.$el.find(".alert-error.schemaname");
		
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
		
		if(view.$el.find(".alert-error:not(.hide)").length>0){
			view.validation=false;
		}else{
			view.validation=true;
		}
	  }
})(jQuery);