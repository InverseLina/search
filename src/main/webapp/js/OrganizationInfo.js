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
    	  view.$el.find(".extra,.resume,.index").prop("disabled",true);
       }else if(app.pathInfo.paths[1] == "edit"){
    	   view.orgId = app.pathInfo.paths[2] * 1;
    	  getDate.call(view,app.pathInfo.paths[2] * 1).done(function(orgName){
    		  var li = render("OrganizationInfo-li",{type:"Organization: "+orgName,url:"#"+app.pathInfo.paths[0]+"/"+app.pathInfo.paths[1]+"/"+app.pathInfo.paths[2]});
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
        if (view.validation) {
          var configs = {};
          configs["local_distance"] = view.$el.find("[name='local_distance']").val();
          configs["local_date"] = view.$el.find("[name='local_date']").val();
          configs["action_add_to_sourcing"] = view.$el.find("[name='action_add_to_sourcing']").prop("checked");
          configs["action_favorite"] = view.$el.find("[name='action_favorite']").prop("checked");
          //values["config_canvasapp_secret"]=view.$el.find("[name='config_canvasapp_secret']").val();
          //values["config_apiKey"]=view.$el.find("[name='config_apiKey']").val();
          //values["config_apiSecret"]=view.$el.find("[name='config_apiSecret']").val();
          //values["config_callBackUrl"]=view.$el.find("[name='config_callBackUrl']").val();
          //values["needAdmin"]="true";
          values["orgId"] = view.orgId;
          configs["instance_url"] = view.$el.find("[name='instance_url']").val();

          values.configsJson = JSON.stringify(configs);
          app.getJsonData("/config/save", values, "Post").done(function(data) {
            values = {};
            values["name"] = view.$el.find("[name='name']").val();
            values["id"] = view.$el.find("[name='id']").val();
            values["schemaname"] = view.$el.find("[name='schemaname']").val();
            values["sfid"] = view.$el.find("[name='sfid']").val();

            app.getJsonData("/org/save", values, "Post").done(function(data) {
              window.location.href = "/admin#organization";
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
					$createExtraBtn.prop("disabled",false).html("Create Extra Tables");
				}else{
					$createExtraBtn.html("Extra Tables Created");
					view.$el.find(".resume").prop("disabled",false).html("Create Index Resume");
					view.$el.find(".index").prop("disabled",false).html("Create Index Columns");
					view.$el.find(".index-info,.status").removeClass("hide");
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
			view.$el.find(".index-status-bar").show();
			view.indexIntervalId = window.setInterval(function(){
		    	   $(view.el).trigger("INDEXCOLUMNSSTATUS");
		      }, 3000);
			app.getJsonData("/createIndexColumns", {orgName:view.currentOrgName},{type:"Post"}).done(function(data){
				window.clearInterval(view.indexIntervalId);
				if(data){
					$alert.removeClass("transparent").html("ErrorCode:"+data.errorCode+"<p>"+data.errorMsg); 
					$createIndexBtn.prop("disabled",false).html("Create Index Columns");
				}else{
					$createIndexBtn.html("Index Columns Created");
					$alert.addClass("hide");
					view.$el.find(".index-status-bar").hide();
				}
			});
		},
		"INDEXCOLUMNSSTATUS":function(){
			var view = this;
			app.getJsonData("/getIndexColumnsStatus", {orgName:view.currentOrgName}).done(function(data){
				percentage = data/5*100;
			    view.$el.find(".index-status-bar .progress-bar-success").css("width",percentage+"%");
			    view.$el.find(".index-status-bar .db-percentage").html(data+"/5");
			});
		},
		"click;.resume":function(event){
			var view = this;
			var $createIndexBtn = $(event.target);
			if($createIndexBtn.prop("disabled")){
				return false;
			}
			var $alert = $createIndexBtn.closest("tr").find(".alert");
			if($createIndexBtn.html()=="Create Index Resume"||$createIndexBtn.html()=="Resume Index Resume"){
				$alert.addClass("transparent");
				$createIndexBtn.html("Pause Index Resume");
				app.getJsonData("/createIndexResume", {orgName:view.currentOrgName},{type:"Post"}).done(function(data){
					if(data&&data.errorCode){
						$alert.removeClass("transparent").html("ErrorCode:"+data.errorCode+"<p>"+data.errorMsg); 
						$createIndexBtn.prop("disabled",false).html("Resume Index Resume");
					}else{
						view.$el.trigger("STATUS_CHANGE");
					}
					window.clearInterval(view.intervalId);
				});
				view.intervalId = window.setInterval(function(){
			    	   $(view.el).trigger("RESUMEINDEXSTATUS");
			      }, 3000);
			}else if($createIndexBtn.html()=="Pause Index Resume"){
				window.clearInterval(view.intervalId);
				$createIndexBtn.prop("disabled",true).html("Pausing");
				app.getJsonData("/stopCreateIndexResume", {orgName:view.currentOrgName},{type:"Post"}).done(function(data){
					$createIndexBtn.prop("disabled",false).html("Resume Index Resume");
					view.$el.trigger("STATUS_CHANGE");
				});
			}
		},
		"click;.status":function(event){
			var view = this;
			view.$el.trigger("STATUS_CHANGE");
		},
		"RESUMEINDEXSTATUS":function(event,init){
			var view = this;
			 app.getJsonData("/getResumeIndexStatus",{orgName:view.currentOrgName}).done(function(data){
				   //view.$el.find(".index-info,.status").removeClass("hide");
	 	    	   //view.$el.find(".index-info").html("Perform:"+data.perform+",Remaining:"+data.remaining);
	 	    	   var percentage = ((data.perform/( data.perform+data.remaining))*100)+"";
	 	    	   if(init&&data.perform>0&&data.remaining>0){
	 	    		   view.$el.find(".resume").html("Resume Index Resume");
	 	    	   }
	 	    	   if(percentage.indexOf(".")!=-1){
	 	    		   percentage = percentage.substring(0,percentage.indexOf("."));
	 	    	   }
	 	    	   fillProgressBar.call(view,percentage,data.perform,data.perform+data.remaining); 
			 });
		},
		 "STATUS_CHANGE":function(event,init){
	    	  var view = this;
	    	  var orgName = view.currentOrgName;
	    	  app.getJsonData("/checkSetupStatus",{type:"ORG",orgName:orgName},{type:"Get"}).done(function(result){
	    		  switch(result){
	    		  case 1:
	    		  case 2: 	view.$el.find(".extra").prop("disabled",false);
	    			  		view.$el.find(".index,.resume").prop("disabled",true);
	    		  			break;
	        	  case 3:	view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
	        	  			view.$el.find(".index").prop("disabled",false).html("Create Index Columns");
	        	  			view.$el.find(".resume").prop("disabled",false);//.html("Create Index Resume");
	        	  			view.$el.trigger("RESUMEINDEXSTATUS",init);
	        	  			break;
	        	  case 4:  	view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
				        	view.$el.find(".index").prop("disabled",true).html("Index Columns Created");
				        	view.$el.find(".resume").prop("disabled",false);//.html("Create Index Resume");
	        	  			view.$el.find(".index-info,.status").removeClass("hide");
	        	  			view.$el.trigger("RESUMEINDEXSTATUS",init);
	    					break;
	        	  case 5:	view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
	        		  		view.$el.find(".resume").prop("disabled",true).html("Index Resume Created");
	        		  		view.$el.find(".index").prop("disabled",false).html("Create Index Columns");
		    	    		view.$el.find(".index-info,.status").addClass("hide");
		    	    		view.$el.trigger("RESUMEINDEXSTATUS",init);
	    					break;
	        	  case 6:	view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
	        		  		view.$el.find(".resume").prop("disabled",false).html("Pause Index Resume");
	        		  		view.intervalId = window.setInterval(function(){
	     			    	   $(view.el).trigger("RESUMEINDEXSTATUS",init);
	        		  		}, 3000);
	        		  		view.$el.trigger("RESUMEINDEXSTATUS");
							break;
	        	  case 7:	view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
			  	  			view.$el.find(".index").prop("disabled",false).html("Create Index Columns");
				  			view.$el.find(".resume").prop("disabled",false);//.html("Create Index Resume");
				  			view.$el.trigger("RESUMEINDEXSTATUS",init);
				  			break;
	        	  case 20:	view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
	        	  			view.$el.find(".index").prop("disabled",true).html("Index Columns Created");
			  		  		view.$el.find(".resume").prop("disabled",true).html("Index Resume Created");
			  		  		view.$el.trigger("RESUMEINDEXSTATUS");
				    		view.$el.find(".index-info,.status").addClass("hide");
							break;
	        	  case 24:	view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
  	  						view.$el.find(".index").prop("disabled",true).html("Index Columns Created");
	        	  			view.$el.find(".resume").prop("disabled",false).html("Pause Index Resume");
	        	  			view.intervalId = window.setInterval(function(){
	     			    	   $(view.el).trigger("RESUMEINDEXSTATUS",init);
	        		  		}, 3000);
	        		  		view.$el.trigger("RESUMEINDEXSTATUS");
							break;
	        	  case 30:	view.$el.find(".extra,.resume,.index").prop("disabled",true); 
							break;
	        	  case 31:	view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
		  	  				view.$el.find(".index").prop("disabled",true).html("Create Index Columns");
			  		  		view.$el.find(".resume").prop("disabled",false);//.html("Create Index Resume");
							break;
	        	  case 35:	view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
	        	  			view.$el.find(".index").prop("disabled",true).html("Create Index Columns");
			  		  		view.$el.find(".resume").prop("disabled",true).html("Index Resume Created");
			  		  		view.$el.trigger("RESUMEINDEXSTATUS",init);
							break;
	        	  case 42:	view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
			  	  			view.$el.find(".index").prop("disabled",true).html("Create Index Columns");
			  		  		view.$el.find(".resume").prop("disabled",false).html("Pause Index Resume");
					  		view.intervalId = window.setInterval(function(){
						    	   $(view.el).trigger("RESUMEINDEXSTATUS",init);
					  		}, 3000);
					  		view.$el.trigger("RESUMEINDEXSTATUS");
							break;
	        	  }
	          });
	      }
    }
  });
  
  function fillProgressBar(percentage,perform,all){
	  var view = this;
	  if(percentage==0){
		  view.$el.find(".db-status-bar").hide();
	  }else{
		  view.$el.find(".db-status-bar").show();
	  }
	  view.$el.find(".db-status-bar .progress-bar-success").css("width",percentage+"%");
	  if(perform==all){
		  view.$el.find(".db-status-bar .db-percentage").html(all);
		  view.$el.find(".db-status-bar .db-count-info").empty();
	  }else{
		  perform = perform/1000;
		  all = all/1000;
		  view.$el.find(".db-status-bar .db-percentage").html(percentage+"%");
		  view.$el.find(".db-status-bar .db-count-info").html(perform+"k/"+all+"k");
  	  }
  }
  function getDate(id){
    var view = this;
    var dfd = $.Deferred();
    app.getJsonData("/org/get/", {id:id}).done(function(data){
    	view.currentOrgName = data[0].name;
        var html = render("OrganizationInfo-content",{data:data[0]});
        view.$tabContent.bEmpty();
        view.$tabContent.html(html);
        view.$el.trigger("STATUS_CHANGE",true);
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