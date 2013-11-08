(function($){
	
  brite.registerView("OrganizationInfo",{parent:".admincontainer",emptyParent:true},{
    // --------- View Interface Implement--------- //
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
  		  view.$navTabs.find('li:last').before(li);
    	  var html = render("OrganizationInfo-content",{data:null});
    	  view.$tabContent.html(html);
    	  view.orgId = -1;
    	  view.$el.find(".extra,.resume,.index").prop("disabled",true);
       }else if(app.pathInfo.paths[1] == "edit"){
    	   view.orgId = app.pathInfo.paths[2] * 1;
    	   getDate.call(view,app.pathInfo.paths[2] * 1).done(function(orgName){
    	   var li = render("OrganizationInfo-li",{type:"Organization: "+orgName,url:"#"+app.pathInfo.paths[0]+"/"+app.pathInfo.paths[1]+"/"+app.pathInfo.paths[2]});
    	   view.$navTabs.find('li:last').before(li);
    	  });
       }
      
      $(document).on("btap." + view.cid, function(event){
    	 $(".time-list,.table-list",view.$el).hide();
      });
    },
    // --------- /View Interface Implement--------- //
    
    // --------- Events--------- //
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
          configs["skill_assessment_rating"] = view.$el.find("[name='skill_assessment_rating']").prop("checked");
          values["orgId"] = view.orgId;
          configs["instance_url"] = view.$el.find("[name='instance_url']").val();
          configs["apex_resume_url"] = view.$el.find("[name='apex_resume_url']").val();

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
		"click;.sfid":function(event){
			var view = this;
			var $createIndexBtn = $(event.target);
			if($createIndexBtn.prop("disabled")){
				return false;
			}
			var $alert = $createIndexBtn.closest("tr").find(".alert");
			if($createIndexBtn.html()=="copy sfid"||$createIndexBtn.html()=="Resume copy sfid"){
				$alert.addClass("transparent");
				$createIndexBtn.html("Pause copy sfid");
				app.getJsonData("/copySfid", {orgName:view.currentOrgName},{type:"Post"}).done(function(data){
					if(data&&data.errorCode){
						$alert.removeClass("transparent").html("ErrorCode:"+data.errorCode+"<p>"+data.errorMsg);
						$createIndexBtn.prop("disabled",false).html("Resume copy sfid");
					}else{
						view.$el.trigger("SFIDSTATUS");
					}
					window.clearInterval(view.intervalId);
				});
				view.intervalId = window.setInterval(function(){
			    	   $(view.el).trigger("SFIDSTATUS");
			      }, 3000);
			}else if($createIndexBtn.html()=="Pause copy sfid"){
				window.clearInterval(view.intervalId);
				$createIndexBtn.prop("disabled",true).html("Pausing");
				app.getJsonData("/stopCopySfid", {orgName:view.currentOrgName},{type:"Post"}).done(function(data){
					$createIndexBtn.prop("disabled",false).html("Resume copy sfid");
					view.$el.trigger("SFIDSTATUS");
				});
			}
		},
		"INDEXCOLUMNSSTATUS":function(){
			var view = this;
			app.getJsonData("/getIndexColumnsStatus", {orgName:view.currentOrgName}).done(function(data){
				percentage = data/11*100;
			    view.$el.find(".index-status-bar .progress-bar-success").css("width",percentage+"%");
			    view.$el.find(".index-status-bar .db-percentage").html(data+"/11");
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
		"SFIDSTATUS":function(event,init){
			var view = this;
			 app.getJsonData("/getSfidStatus",{orgName:view.currentOrgName}).done(function(data){
				 var percentage = ((data.perform/( data.perform+data.remaining))*100)+"";
	 	    	   if(init&&data.perform>0&&data.remaining>0){
	 	    		   view.$el.find(".sfid").html("Resume copy sfid");
	 	    	   }
	 	    	   if(percentage.indexOf(".")!=-1){
	 	    		   percentage = percentage.substring(0,percentage.indexOf("."));
	 	    	   }
	 	    	  fillProgressBarForSfid.call(view,percentage,data.perform,data.perform+data.remaining);
			 });
		},
		"click;.multiply":function(event){
			 var view = this;
			 if(!view.time ){
				 view.time  = 1;
			 }
			 if(!view.tableName){
				 view.tableName = "contact";
			 }
			 var $btn = $(event.target);
			 if($btn.prop("disabled")){
				 return false;
			 }
			 $btn.prop("disabled",true);
			 app.getJsonData("/multiplyData",{orgName:view.currentOrgName,times:view.time,tableName:view.tableName},{type:"POST"}).done(function(data){
				 window.clearInterval(view.multiplyIntervalId);
				 $btn.prop("disabled",false);
				 view.$el.find(".multiply-info").hide();
				 //view.$el.trigger("MULTIPLY_STATUS_CHANGE");
			 });
			 view.multiplyIntervalId = window.setInterval(function(){
		    	   $(view.el).trigger("MULTIPLY_STATUS_CHANGE");
		  		}, 3000);
			 view.$el.trigger("MULTIPLY_STATUS_CHANGE");
		},
		"MULTIPLY_STATUS_CHANGE":function(event){
			 var view = this;
			 var $info = view.$el.find(".multiply-info");
			 $info.show();
			 app.getJsonData("/getMultiplyStatus",{},{type:"GET"}).done(function(data){
				 $(".time",$info).html(data.currentTime);
				 $(".perform",$info).html(data.performCounts);
				 $(".total",$info).html(data.contactCounts);
			 });
		},
		"click;.drawdown":function(event){
			var view = this;
			var $arrow = $(event.currentTarget);
			if($arrow.next().css("display")!="none"){
				$arrow.next().hide();
			}else{
				$arrow.next().show();
			}
		},
		"click;[data-time]":function(event){
			var view = this;
			var $li = $(event.currentTarget);
			$li.closest(".time-list").hide();
			view.time = $li.attr("data-time");
			$li.closest(".control-group").find("[name='time']").val($li.attr("data-time"));
		},
		"click;[data-table]":function(event){
			var view = this;
			var $li = $(event.currentTarget);
			$li.closest(".table-list").hide();
			view.tableName = $li.attr("data-table");
			console.log($li.attr("data-table"));
			$li.closest(".control-group").find("[name='tableName']").val($li.attr("data-table"));
		},
		 "STATUS_CHANGE":function(event,init){
	    	  var view = this;
	    	  var orgName = view.currentOrgName;
	    	  app.getJsonData("/checkOrgSchema",{org:orgName},{type:"Get"}).done(function(result){
	    		  console.log(result);
	    		  var tableInfo = "";
	    		  for(var table in result.tables){
	    			 if(!result.tables[table]){
	    				 tableInfo+=table+" ";
	    			 }
	    		  }
	    		  
	    		  if(tableInfo){
	    			  view.$el.find(".extra").prop("disabled",false).html("Create Extra Tables");
	    		  }else{
	    			  view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
	    		  }
	    		  if(result.pgtrgm){
		    		  var indexInfo = "";
		    		  for(var indexName in result.indexes){
		    			 if(!result.indexes[indexName]){
		    				 indexInfo+=indexName+" ";
		    			 }
		    		  }
		    		  if(indexInfo){
		    			  view.$el.find(".index").prop("disabled",false).html("Create Index Columns");
		    		  }else{
		    			  view.$el.find(".index").prop("disabled",true).html("Index Columns Created");
		    		  }
	    		  }else{
	    			  view.$el.find(".index").prop("disabled",true).html("Create Index Columns");
	    		  }
	    		  if(result.resume=="running"){
	    			  view.$el.find(".resume").prop("disabled",false).html("Pause Index Resume");
	    			  view.intervalId = window.setInterval(function(){
				    	   $(view.el).trigger("RESUMEINDEXSTATUS");
	  		  			}, 3000);
	    			  view.$el.trigger("RESUMEINDEXSTATUS");
	    		  }else if(result.resume=="done"){
	    			  view.$el.find(".resume").prop("disabled",true).html("Index Resume created");
	    			  view.$el.trigger("RESUMEINDEXSTATUS");
	    		  }else if(result.resume=="part"){
	    			  view.$el.find(".resume").prop("disabled",false).html("Resume Index Resume");
	    			  view.$el.trigger("RESUMEINDEXSTATUS");
	    		  }else{
	    			  if(tableInfo.indexOf("contact_ex")==-1){
	    				  view.$el.find(".resume").prop("disabled",false);
	    			  }
	    		  }
	    		  
	    		  
	    		  if(result.sfid=="running"){
	    			  view.$el.find(".sfid").prop("disabled",false).html("Pause copy sfid");
	    			  view.intervalId = window.setInterval(function(){
				    	   $(view.el).trigger("SFIDSTATUS");
	  		  			}, 3000);
	    			  view.$el.trigger("SFIDSTATUS");
	    		  }else if(result.sfid=="done"){
	    			  view.$el.find(".resume").prop("disabled",true).html("sfid copied");
	    			  view.$el.trigger("SFIDSTATUS");
	    		  }else if(result.sfid=="part"){
	    			  view.$el.find(".sfid").prop("disabled",false).html("Resume copy sfid");
	    			  view.$el.trigger("SFIDSTATUS");
	    		  }else{
	    			  if(tableInfo.indexOf("contact_ex")==-1){
	    				  view.$el.find(".sfid").prop("disabled",false);
	    			  }
	    		  }
	    		  
	    		 /* switch(result){
	    		  	case 3: 	// system schema created,org schema not existed not created
	    			view.$el.find(".extra,.index,.resume").prop("disabled",true);
		  		  	break;
		  		  	
	        	  	case 7:	//org extra not created
    		  		view.$el.find(".extra").prop("disabled",false).html("Create Extra Tables");
	  	  			view.$el.find(".index").prop("disabled",true).html("Create Index Columns");
		  			view.$el.find(".resume").prop("disabled",true).html("Create Index Resume");
		  			break;
		  			
	        	  	case 15:	//org extra created,pg_trgm not existed,resume not finished
	  				view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
	  				view.$el.find(".index").prop("disabled",true).html("Create Index Columns");
	  				view.$el.find(".resume").prop("disabled",false);//.html("Resume Index Resume");
	  				view.$el.trigger("RESUMEINDEXSTATUS",true);
	  				break;
	  				
	        	  	case 47:	//system schema created,pg_trgm is not existed,org extra existed,index resume created
    		  		view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
	  	  			view.$el.find(".index").prop("disabled",true).html("Create Index Columns");
	  		  		view.$el.find(".resume").prop("disabled",true).html("Index Resume Created");
			  		view.$el.trigger("RESUMEINDEXSTATUS");
			  		break;
			  		
	        	  	case 79:	//system schema created,pg_trgm is not existed,
	        		  		//org extra existed,index resume runing
    		  		view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
	  	  			view.$el.find(".index").prop("disabled",true).html("Create Index Columns");
	  		  		view.$el.find(".resume").prop("disabled",false).html("Pause Index Resume");
		  		  	view.intervalId = window.setInterval(function(){
				    	   $(view.el).trigger("RESUMEINDEXSTATUS");
	  		  		}, 3000);
			  		view.$el.trigger("RESUMEINDEXSTATUS");
			  		break;
			  		
	        	    case 143: //pg_trgm created,index columns not created,resume is not finished
		  		    view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
		  		    view.$el.find(".index").prop("disabled",false).html("Create Index Columns");
		  		    view.$el.find(".resume").prop("disabled",false);//.html("Resume Index Resume");
		  		    view.$el.trigger("RESUMEINDEXSTATUS",true);
		  		    break;
		  		   
	        	    case 159: //resume is not finished
			  		view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
					view.$el.find(".index").prop("disabled",true).html("Index Columns Created");
					view.$el.find(".resume").prop("disabled",false);//.html("Resume Index Resume");
					view.$el.trigger("RESUMEINDEXSTATUS",true);
					break;
					
	        	    case 175: //org extra created,index column not created,resume is done
    		  		view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
					view.$el.find(".index").prop("disabled",false).html("Create Index Columns");
					view.$el.find(".resume").prop("disabled",true).html("Index Resume Created");
					view.$el.trigger("RESUMEINDEXSTATUS");
					break;
					
	        	    case 191: //All done
    		  		view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
	  	  			view.$el.find(".index").prop("disabled",true).html("Index Columns Created");
	  		  		view.$el.find(".resume").prop("disabled",true).html("Index Resume Created");
	  		  		view.$el.trigger("RESUMEINDEXSTATUS");
		    		view.$el.find(".index-info,.status").addClass("hide");
					break;
					
	        	    case 207:	//resume is running,and index columns not created
    		  		view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
	  	  			view.$el.find(".index").prop("disabled",false).html("Create Index Columns");
		  	  		view.$el.find(".resume").prop("disabled",false).html("Pause Index Resume");
	  	  			view.intervalId = window.setInterval(function(){
				    	   $(view.el).trigger("RESUMEINDEXSTATUS",false);
	  		  		}, 3000);
	  		  		view.$el.trigger("RESUMEINDEXSTATUS");
					break;
							
	        	    case 223:	//resume is running,and index columns  created
    		  		view.$el.find(".extra").prop("disabled",true).html("Extra Tables Created");
	  	  			view.$el.find(".index").prop("disabled",true).html("Index Columns Created");
		  	  		view.$el.find(".resume").prop("disabled",false).html("Pause Index Resume");
	  	  			view.intervalId = window.setInterval(function(){
				    	   $(view.el).trigger("RESUMEINDEXSTATUS",false);
	  		  		}, 3000);
	  		  		view.$el.trigger("RESUMEINDEXSTATUS");
					break;
					
	        	  }*/
	          });
	      }
    }

    // --------- /Events--------- //
   });
  
  // --------- Private Methods--------- //
  function fillProgressBar(percentage,perform,all){
	  var view = this;
	  if(percentage==0){
		  view.$el.find(".db-status-bar").hide();
	  }else{
		  view.$el.find(".db-status-bar").show();
	  }
	  view.$el.find(".db-status-bar .progress-bar-success").css("width",percentage+"%");
	  
	  if(perform==all){
		  view.$el.find(".db-status-bar .db-percentage").html(formateNumber(all));
		  view.$el.find(".db-status-bar .db-count-info").empty();
		  view.$el.find(".resume").prop("disabled",true).html("Index Resume Created");
	  }else{
		  view.$el.find(".db-status-bar .db-percentage").html(percentage+"%");
		  view.$el.find(".db-status-bar .db-count-info").html(formateNumber(perform)+" / "+formateNumber(all)+"");
  	  }
  }
  
  function fillProgressBarForSfid(percentage,perform,all){
	  var view = this;
	  if(percentage==0){
		  view.$el.find(".sfid-status-bar").hide();
	  }else{
		  view.$el.find(".sfid-status-bar").show();
	  }
	  view.$el.find(".sfid-status-bar .progress-bar-success").css("width",percentage+"%");
	  if(perform==all){
		  view.$el.find(".sfid-status-bar .sfid-percentage").html(formateNumber(all));
		  view.$el.find(".sfid-status-bar .sfid-count-info").empty();
		  view.$el.find(".sfid").prop("disabled",true).html("sfid copied");
	  }else{
		  //perform = perform/1000;
		  //all = all/1000;
		  view.$el.find(".sfid-status-bar .sfid-percentage").html(percentage+"%");
		  view.$el.find(".sfid-status-bar .sfid-count-info").html(formateNumber(perform)+" / "+formateNumber(all)+"");
  	  }
  }
  
  function formateNumber(val){
	 val=val+"";
  	 var newVal = "";
  	 for(var i=0;i<val.length;i++){
  		 if(i%3==0&&i!=0){
  			newVal+=","
  		 }
  		newVal += val.substring(i,i+1);
  	 }
  	 return newVal;
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
  // --------- /Private Methods--------- //
})(jQuery);