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
				app.getJsonData("getOrgSearchConfig").done(function(result){
					view.$el.find("textarea[name='searchConfig']").val(result.content);
					if(result.errorMsg){
                    	view.$el.trigger("DO_SHOW_MSG",{selector:".search-config-alert",msg:result.errorMsg,type:"error"});
                		view.$el.find(".search-content").css("background","#ffdddd");
                    }
				});
				view.$el.find("button.saveSearchConfig, button.resetSearchConfig").attr("disable", "true");
			}else if(app.pathInfo.paths[1] == "edit"){
				view.orgId = app.pathInfo.paths[2] * 1;
					getDate.call(view, app.pathInfo.paths[2] * 1).done(function (orgName) {
							view.orgName = orgName;
							var li = render("OrganizationInfo-li", {type: "Organization: " + orgName, url: "#" + app.pathInfo.paths[0] + "/" + app.pathInfo.paths[1] + "/" + app.pathInfo.paths[2]});
							view.$navTabs.find('li:last').before(li);
							view.$el.trigger("WRONGINDEXES");
							app.getJsonData("getOrgSearchConfig", {orgName: view.orgName}).done(function (result) {
									view.$el.find("textarea[name='searchConfig']").val(result.content);
									if(result.errorMsg){
				                    	view.$el.trigger("DO_SHOW_MSG",{selector:".search-config-alert",msg:result.errorMsg,type:"error"});
				                		view.$el.find(".search-content").css("background","#ffdddd");
				                    }
							});
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
			window.location.href=contextPath + "/";
			},
			"click;.cancel":function(event){
				window.location.href=contextPath + "/admin#organization";
			},
			"change;:checkbox,select":function(event){
			var view = this;
			var $saveBtn = view.$el.find(".save");
			$saveBtn.removeClass("disabled");
		},
		"click;.save":function(event){
			var view = this;
			var $btn = $(event.currentTarget);
			$btn.prop("disabled",true).html("saving...");
			var values = {};
				doValidate.call(view);
				if (view.validation) {
					var configs = {};
					configs["local_distance"] = view.$el.find("[name='local_distance']").val();
					configs["local_date"] = view.$el.find("[name='local_date']").val();
					configs["action_add_to_sourcing"] = view.$el.find("[name='action_add_to_sourcing']").prop("checked");
					configs["action_favorite"] = view.$el.find("[name='action_favorite']").prop("checked");
					configs["skill_assessment_rating"] = view.$el.find("[name='skill_assessment_rating']").prop("checked");
					configs["advanced_auto_complete"] = view.$el.find("[name='advanced_auto_complete']").prop("checked");
					values["orgId"] = view.orgId;
					configs["instance_url"] = view.$el.find("[name='instance_url']").val();
					configs["force_login_url"] = view.$el.find("[name='force_login_url']").val();
					configs["apex_resume_url"] = view.$el.find("[name='apex_resume_url']").val();
					configs["canvasapp_secret"] = view.$el.find("[name='canvasapp_secret']").val();
					configs["jss.feature.userlist"] = view.$el.find("[name='jss.feature.userlist']").val();
					var sfTimeout = $.trim(view.$el.find("[name='sf_session_timeout']").val());

					if(/^\d+$/.test(sfTimeout)){
							configs["sf_session_timeout"] = sfTimeout;
					}


					values.configsJson = JSON.stringify(configs);
					app.getJsonData("/config/save", values, "Post").done(function(data) {
							$.extend(app.orgInfo||{}, configs);
						values = {};
						values["name"] = view.$el.find("[name='name']").val();
						values["id"] = view.$el.find("[name='id']").val();
						values["schemaname"] = view.$el.find("[name='schemaname']").val();
						values["sfid"] = view.$el.find("[name='sfid']").val();

						app.getJsonData("/org/save", values, "Post").done(function(data) {
                    		view.$el.trigger("DO_SHOW_MSG",{selector:$(".alert",$btn.closest(".btns")),msg:"Values saved successfully",type:"success"});
                    		$btn.prop("disabled",false).html("Save");
                    		if(app.pathInfo.paths[1] == "add"){
                    			 window.location.hash="#organization/edit/"+data;
                    		}
						});
					});
				}

		},
		"FILLDATA":function(event,result){
			var view = this;
			var currentField;
			$.each(result.data,function(key,value){
				currentField = view.$el.find("[name='"+key+"']");
				if(currentField.length>0){
					if(currentField.attr("type") == 'checkbox'){
						currentField.prop("checked",value=='true');
					}else{
						currentField.val(value);
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
					$createExtraBtn.html("Extra Tables Created").addClass("btn-success");
					//view.$el.find(".resume").prop("disabled",false).html("Create Index Resume").removeClass("btn-success");
					//view.$el.find(".index").prop("disabled",false).html("Create Index Columns").removeClass("btn-success");
					refresh.call(view);
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
			view.indexIntervalId = window.setInterval(function(){
						$(view.el).trigger("INDEXCOLUMNSSTATUS");
					}, 3000);
			app.getJsonData("/createIndexColumns", {orgName:view.currentOrgName,contactEx:false},{type:"Post"}).done(function(data){
				window.clearInterval(view.indexIntervalId);
				if(data){
					$alert.removeClass("transparent").html("ErrorCode:"+data.errorCode+"<p>"+data.errorMsg);
					$createIndexBtn.prop("disabled",false).html("Create Index Columns").removeClass("btn-success");
				}else{
					$createIndexBtn.html("Index Columns Created").addClass("btn-success");
					$alert.addClass("hide");
					//view.$el.find(".index-status-bar").hide();
				}
				$(view.el).trigger("INDEXCOLUMNSSTATUS");
			});
		},
		"click;.contact-index":function(event){
			var view = this;
			var $createIndexBtn = $(event.target);
			if($createIndexBtn.prop("disabled")){
				return false;
			}
			var $alert = $createIndexBtn.closest("tr").find(".alert");
			$createIndexBtn.prop("disabled",true).html("Creating...");
			$alert.addClass("transparent");
			view.contactIndexIntervalId = window.setInterval(function(){
						$(view.el).trigger("CONTACTINDEXCOLUMNSSTATUS");
					}, 3000);
			app.getJsonData("/createIndexColumns", {orgName:view.currentOrgName,contactEx:true},{type:"Post"}).done(function(data){
				window.clearInterval(view.contactIndexIntervalId);
				if(data){
					$alert.removeClass("transparent").html("ErrorCode:"+data.errorCode+"<p>"+data.errorMsg);
					$createIndexBtn.prop("disabled",false).html("Create jss_contact Indexes").removeClass("btn-success");
				}else{
					$createIndexBtn.html("jss_contact Indexes Created").addClass("btn-success");
					$alert.addClass("hide");
				}
				$(view.el).trigger("CONTACTINDEXCOLUMNSSTATUS");
			});
		},
		"click;.sfid":function(event){
			var view = this;
			var $createIndexBtn = $(event.target);
			if($createIndexBtn.prop("disabled")){
				return false;
			}
			var $alert = $createIndexBtn.closest("tr").find(".alert");
			var status = $createIndexBtn.attr("data-status");
			if(!status){
				status="copy";
			}
			if(status=="copy"||status=="resume"){
				$alert.addClass("transparent");
				$createIndexBtn.html("Pause copy sfid");
				$createIndexBtn.attr("data-status","pause");
				app.getJsonData("/copySfid", {orgName:view.currentOrgName},{type:"Post"}).done(function(data){
					if(data&&data.errorCode){
						$alert.removeClass("transparent").html("ErrorCode:"+data.errorCode+"<p>"+data.errorMsg);
						$createIndexBtn.prop("disabled",false).html("Resume copy sfid").removeClass("btn-success");
						$createIndexBtn.attr("data-status","resume");
					}else{
						view.$el.trigger("SFIDSTATUS");
					}
					window.clearInterval(view.sfidIntervalId);
				});
				view.sfidIntervalId = window.setInterval(function(){
							$(view.el).trigger("SFIDSTATUS");
						}, 3000);
			}else if(status=="pause"){
				window.clearInterval(view.sfidIntervalId);
				$createIndexBtn.prop("disabled",true).html("Pausing");
				app.getJsonData("/stopCopySfid", {orgName:view.currentOrgName},{type:"Post"}).done(function(data){
					$createIndexBtn.prop("disabled",false).html("Resume copy sfid").removeClass("btn-success");
					$createIndexBtn.attr("data-status","resume");
					view.$el.trigger("SFIDSTATUS");
				});
			}
		},
		"WRONGINDEXES":function(){
			var view = this;
			app.getJsonData("/getWrongIndexes",{orgName:view.currentOrgName}).done(function(data){
				if(data.length>0){
					view.$el.find(".remove-wrong-index").prop("disabled",false);
					view.$el.find(".remove-wrong-index").closest("tr").find(".alert").removeClass("transparent").html(data);
				}else{
					view.$el.find(".remove-wrong-index").prop("disabled",true);
				}
			});
		},
		"click;.remove-wrong-index":function(event){
			var view = this;
			var $btn = $(event.target);
			$btn.html("Removing...").prop("disabled",true);
			app.getJsonData("/removeWrongIndexes",{orgName:view.currentOrgName},{type:'Post'}).done(function(data){
				view.$el.find(".remove-wrong-index").closest("tr").find(".alert").addClass("transparent").html("");
				$btn.html("Remove Wrong Indexes").prop("disabled",true);
			});
		},
		"click;.contact-tsv":function(event){
			var view = this;
			var $createIndexBtn = $(event.target);
			if($createIndexBtn.prop("disabled")){
				return false;
			}
			var $alert = $createIndexBtn.closest("tr").find(".alert");
			var status = $createIndexBtn.attr("data-status");
			if(!status){
				status="copy";
			}
			if(status=="copy"||status=="resume"){
				$alert.addClass("transparent");
				$createIndexBtn.html("Pause Create contact_tsv");
				$createIndexBtn.attr("data-status","pause");
				app.getJsonData("/createContactTsv", {orgName:view.currentOrgName},{type:"Post"}).done(function(data){
					if(data&&data.errorCode){
						$alert.removeClass("transparent").html("ErrorCode:"+data.errorCode+"<p>"+data.errorMsg);
						$createIndexBtn.prop("disabled",false).html("Resume Create contact_tsv").removeClass("btn-success");
						$createIndexBtn.attr("data-status","resume");
					}else{
						view.$el.trigger("CONTACT_TSVSTATUS");
					}
					window.clearInterval(view.contactIntervalId);
				});
				view.contactIntervalId = window.setInterval(function(){
							$(view.el).trigger("CONTACT_TSVSTATUS");
						}, 3000);
			}else if(status=="pause"){
				window.clearInterval(view.contactIntervalId);
				$createIndexBtn.prop("disabled",true).html("Pausing");
				app.getJsonData("/stopCreateContactTsv", {orgName:view.currentOrgName},{type:"Post"}).done(function(data){
					$createIndexBtn.prop("disabled",false).html("Resume Create contact_tsv").removeClass("btn-success");
					$createIndexBtn.attr("data-status","resume");
					view.$el.trigger("CONTACT_TSVSTATUS");
				});
			}
		},
		"click;.jss_grouped_skills":function(event){
			var view = this;
			$(event.target).html("Creating...").prop("disabled",true);
			app.getJsonData("/createExtraGrouped", {orgName:view.currentOrgName,tableName:'jss_grouped_skills'},{type:'Post'}).done(function(data){
				$(event.target).html("jss_grouped_skills Created").prop("disabled",true).addClass("btn-success");
				refresh.call(view);
			});
		},
		"click;.jss_grouped_educations":function(event){
			var view = this;
			$(event.target).html("Creating...").prop("disabled",true);
			app.getJsonData("/createExtraGrouped", {orgName:view.currentOrgName,tableName:'jss_grouped_educations'},{type:'Post'}).done(function(data){
				$(event.target).html("jss_grouped_educations Created").prop("disabled",true).addClass("btn-success");
				refresh.call(view);
			});
		},
		"click;.jss_grouped_employers":function(event){
			var view = this;
			$(event.target).html("Creating...").prop("disabled",true);
			app.getJsonData("/createExtraGrouped", {orgName:view.currentOrgName,tableName:'jss_grouped_employers'},{type:'Post'}).done(function(data){
				$(event.target).html("jss_grouped_employers Created").prop("disabled",true).addClass("btn-success");
			});
		},
		"click;.jss_grouped_locations":function(event){
			var view = this;
			$(event.target).html("Creating...").prop("disabled",true);
			app.getJsonData("/createExtraGrouped", {orgName:view.currentOrgName,tableName:'jss_grouped_locations'},{type:'Post'}).done(function(data){
				$(event.target).html("jss_grouped_locations Created").prop("disabled",true).addClass("btn-success");
				refresh.call(view);
			});
		},
		"click;.fix-table":function(event){
			var view = this;
			$(event.target).html("Fixing...").prop("disabled",true);
			app.getJsonData("/fixJssTableNames", {orgName:view.currentOrgName},{type:'Post'}).done(function(data){
				$(event.target).html("Jss Table Names Fixed").prop("disabled",true).addClass("btn-success");
				disableAllBtns.call(view);
				refresh.call(view);
			});
		},
		"INDEXCOLUMNSSTATUS":function(){
			var view = this;
			view.$el.find(".index-status-bar").show();
			app.getJsonData("/getIndexColumnsStatus", {orgName:view.currentOrgName}).done(function(data){
				percentage = data.created/data.all*100;
				if(data.created<data.all){
					view.$el.find(".index").prop("disabled",false);
				}else{
					view.$el.find(".index").prop("disabled",true).addClass("btn-success").html("Other Indexes Created");
				}
				view.$el.find(".index-status-bar .progress-bar-success").css("width",percentage+"%");
				view.$el.find(".index-status-bar .db-percentage").html(data.created+"/"+data.all);
				view.$el.trigger("WRONGINDEXES");
					
			});
		},
		"CONTACTINDEXCOLUMNSSTATUS":function(){
			var view = this;
			view.$el.find(".contact-index-status-bar").show();
			app.getJsonData("/getIndexColumnsStatus", {orgName:view.currentOrgName,contactEx:true}).done(function(data){
				if(data.created<3){
					view.$el.find(".contact-index").prop("disabled",false);
				}else{
					view.$el.find(".contact-index").prop("disabled",true).addClass("btn-success").html("jss_contact Indexes Created");
				}
				percentage = data.created/3*100;
				view.$el.find(".contact-index-status-bar .progress-bar-success").css("width",percentage+"%");
				view.$el.find(".contact-index-status-bar .contact-index-percentage").html(data.created+"/3");
			});
		},
		"click;.resume":function(event){ 
			var view = this;
			var $createIndexBtn = $(event.target);
			if($createIndexBtn.prop("disabled")){
				return false;
			}
			var status = $createIndexBtn.attr("data-status");
			if(!status){
				status="create";
			}
			var $alert = $createIndexBtn.closest("tr").find(".alert");
			if(status=="create"||status=="resume"){
				$alert.addClass("transparent");
				$createIndexBtn.html("Pause Index Resume").attr("data-status","pause");
				app.getJsonData("/createIndexResume", {orgName:view.currentOrgName},{type:"Post"}).done(function(data){
					if(data&&data.errorCode){
						$alert.removeClass("transparent").html("ErrorCode:"+data.errorCode+"<p>"+data.errorMsg);
						$createIndexBtn.prop("disabled",false).html("Resume Index Resume").attr("data-status","resume").removeClass("btn-success");
					}else{
						refresh.call(view);
					}
					window.clearInterval(view.intervalId);
				});
				view.intervalId = window.setInterval(function(){
					$(view.el).trigger("RESUMEINDEXSTATUS");
				}, 3000);

			}else if(status=="pause"){
				window.clearInterval(view.intervalId);
				$createIndexBtn.prop("disabled",true).html("Pausing");
				app.getJsonData("/stopCreateIndexResume", {orgName:view.currentOrgName},{type:"Post"}).done(function(data){
					$createIndexBtn.prop("disabled",false).html("Resume Index Resume").attr("data-status","resume").removeClass("btn-success");
					refresh.call(view);
				});
			}
		},
		"click;.status":function(event){
			var view = this;
			refresh.call(view);
		},
		"RESUMEINDEXSTATUS":function(event,init){
			var view = this;
			app.getJsonData("/getResumeIndexStatus",{orgName:view.currentOrgName}).done(function(data){
						var percentage = ((data.perform/( data.perform+data.remaining))*100)+"";
						if(init&&data.perform>0&&data.remaining>0){
							view.$el.find(".resume").html("Resume Index Resume").removeClass("btn-success");
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
							view.$el.find(".sfid").html("Resume copy sfid").removeClass("btn-success");
						}
						if(percentage.indexOf(".")!=-1){
							percentage = percentage.substring(0,percentage.indexOf("."));
						}
						fillProgressBarForSfid.call(view,percentage,data.perform,data.perform+data.remaining);
			});
		},
		"CONTACT_TSVSTATUS":function(event,init){
			var view = this;
			app.getJsonData("/getContactTsvStatus",{orgName:view.currentOrgName}).done(function(data){
				var percentage = ((data.perform/( data.perform+data.remaining))*100)+"";
						if(init&&data.perform>0&&data.remaining>0){
							view.$el.find(".sfid").html("Resume Create contact_tsv").removeClass("btn-success");
						}
						if(percentage.indexOf(".")!=-1){
							percentage = percentage.substring(0,percentage.indexOf("."));
						}
						fillProgressBarForContactTsv.call(view,percentage,data.perform,data.perform+data.remaining);
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
			$btn.toggleClass("pause");
			if($btn.hasClass("pause")){
				$btn.html("Pause Multiply");
				app.getJsonData("/multiplyData",{orgName:view.currentOrgName,times:view.time,tableName:view.tableName},{type:"POST"}).done(function(data){
					window.clearInterval(view.multiplyIntervalId);
					$btn.prop("disabled",false).html("Multiply Data");
					view.$el.find(".multiply-info").hide();
					view.$el.find(".db-info").html("Current Database Size: "+data.currentDbSize+"MB , Cost: "+data.cost+"s").show();
					//view.$el.trigger("MULTIPLY_STATUS_CHANGE");
					$btn.html("Multiply Data");
					$btn.removeClass("pause");
				});
				view.multiplyIntervalId = window.setInterval(function(){
							$(view.el).trigger("MULTIPLY_STATUS_CHANGE");
						}, 5000);
				view.$el.trigger("MULTIPLY_STATUS_CHANGE");
			}else{
				app.getJsonData("/stopMultiply",{},{type:"POST"}).done(function(data){
					$btn.html("Multiply Data");
				});
			}
			
		},
		"MULTIPLY_STATUS_CHANGE":function(event){
			var view = this;
			var $info = view.$el.find(".multiply-info");
			$info.show();
			view.$el.find(".db-info").hide();
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
		"click;.fix-missing-columns":function(event){
			var $btn = $(event.currentTarget);
			var view = this;
			app.getJsonData("/fixJssColumns",{orgName:view.currentOrgName},{type:"Post"}).done(function(result){
				if(!result){
					$btn.addClass("hide");
					refresh.call(view);
					view.$el.find(".jsstable-info").removeClass("alert-danger").addClass("alert-success").html("jss tables valid");
				}
			});
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
			$li.closest(".control-group").find("[name='tableName']").val($li.attr("data-table"));
		},
		"click;.org-reset":function(event){
			var view = this;
			var orgName = view.currentOrgName;
			app.getJsonData("/reset-org-setup",{org:orgName},{type:"Post"}).done(function(result){
				window.clearInterval(view.orgSetupIntervalId);
				refresh.call(view);
			});
		},
		"click;.org-pause":function(event){
			var view = this;
			var orgName = view.currentOrgName;
			app.getJsonData("/stop-org-setup",{org:orgName},{type:"Post"}).done(function(result){
				window.clearInterval(view.orgSetupIntervalId);
				refresh.call(view);
			});
		},
		"click;.org-setup":function(event){
			var view = this;
			var orgName = view.currentOrgName;
			var $btn = $(event.currentTarget);
			$btn.prop("disabled",true).html("Setup...");
			app.getJsonData("/admin-org-setup",{org:orgName},{type:"Post"}).done(function(result){
				window.clearInterval(view.orgSetupIntervalId);
				refresh.call(view);
			});
			view.orgSetupIntervalId = window.setInterval(function(){
				refresh.call(view);
			}, 3000);
		},
		"STATUS_CHANGE":function(event,init){
			var view = this;
			var orgName = view.currentOrgName;
			var addClass = "alert-success",removeClass="alert-danger";
			var $setupBtn = view.$el.find(".org-setup"),
				$pasueBtn = view.$el.find(".org-pause"),
				$resetBtn = view.$el.find(".org-reset");
			app.getJsonData("/admin-org-status",{org:orgName},{type:"Get"}).done(function(result){
				switch(result.status){
					case  "done"      :
						$setupBtn.prop("disabled",true).html("Setup");
						$pasueBtn.prop("disabled",true);
						$resetBtn.prop("disabled",false);
						break;
					case  "part"      :
						$setupBtn.prop("disabled",false).html("Resume");
						$pasueBtn.prop("disabled",true);
						$resetBtn.prop("disabled",false);
						break;
					case  "error"     :
						$setupBtn.prop("disabled",true).html("Setup");
						$pasueBtn.prop("disabled",true);
						$resetBtn.prop("disabled",true);
						break;
					case  "notstarted":
						$setupBtn.prop("disabled",false).html("Setup");
						$pasueBtn.prop("disabled",true);
						$resetBtn.prop("disabled",false);
						break;
					case  "running"   :
						$setupBtn.prop("disabled",true).html("Setup...");
						$pasueBtn.prop("disabled",false);
						$resetBtn.prop("disabled",true);
						setTimeout(function(){
							refresh.call(view);
						},3000);
						break;
				
				}
				$.each(result.setups,function(index,setup){
					switch(setup.status){
						case "done":addClass = "alert-success";removeClass="alert-danger";break;
						case "part":addClass = "alert-danger";removeClass="alert-success";
						case "error":addClass = "alert-danger";removeClass="alert-success";
						case "notstarted":addClass = "alert-danger";removeClass="alert-success";
					}
					if(setup.progress||setup.progress===0){
						fillProgressBar.call(view,setup)
					}else{
						view.$el.find("."+setup.name+"_info").addClass(addClass)
						.removeClass(removeClass).removeClass("hide").html(setup.msg);
					}
				});			
			});
		},
		"click; button.saveSearchConfig":function(event){
			event.stopPropagation();
			event.preventDefault();
			var view = this;
			var $btn = $(event.currentTarget);
			$btn.prop("disabled",true).html("saving...");
			var content = view.$el.find("textarea[name='searchConfig']").val();
			app.getJsonData("saveOrgSearchConfig", {orgName:view.orgName, content:content}, "Post").done(function(result){
				if(!result.valid){
            		view.$el.find(".search-content").css("background","#ffdddd");
            		view.$el.trigger("DO_SHOW_MSG",{selector:".search-config-alert",msg:result.errorMsg,type:"error"});
             	}else{
            		view.$el.find(".search-content").css("background","#ffffff");
            		view.$el.trigger("DO_SHOW_MSG",{selector:".search-config-alert",msg:"Values saved successfully",type:"success"});
             	}
				$btn.prop("disabled",false).html("Save");
			});
			return false;
		},
		"click; button.resetSearchConfig":function(event){
			var view = this;
			var $btn = $(event.currentTarget);
			$btn.prop("disabled",true).html("resetting...");
			app.getJsonData("resetOrgSearchConfig", {orgName:view.orgName}).done(function(result){
					view.$el.find("textarea[name='searchConfig']").val(result);
					view.$el.find(".search-content").css("background","#ffffff");
                	view.$el.trigger("DO_SHOW_MSG",{selector:".search-config-alert",msg:"search config has been reset successfully.",type:"success"});
                	$btn.prop("disabled",false).html("Reset");
			});
		},
		"click;.disable-indexes":function(event){
			var view = this;
			var disableBtn = $(event.currentTarget);
			disableBtn.prop("disabled",true).html("Disabling...");
			app.getJsonData("removeAllIndexes", {orgName:view.orgName},{type:'Post'}).done(function(result){
				disableBtn.html("Indexes Disabled");
				refresh.call(view);
			});
		},
		"click;.enable-indexes":function(event){
			var view = this;
			var $disableBtn = $(event.currentTarget);
			refresh.call(view);
			$(".alert",$disableBtn.closest("div")).show();
		},
		"click;.drop-ex":function(event){
			var view = this;
			var $disableBtn = $(event.currentTarget);
			app.getJsonData("/dropExTables", {orgName:view.orgName},{type:'Post'}).done(function(result){
				refresh.call(view);
				$(".alert",$disableBtn.closest("div")).show();
		});
	}
		}

		// --------- /Events--------- //
	});
	
	// --------- Private Methods--------- //
	function refresh(){
		var view = this;
		view.$el.trigger("STATUS_CHANGE");
	}
	
	function fillProgressBar(setup){
		var view = this;
		var name = setup.name,
			all = setup.progress.perform+setup.progress.remaining,
			perform = setup.progress.perform,
			percentage = perform/all*100+"";
		if(setup.progress === 0){
			all = 0,
			perform = 0,
			percentage = "0";
		}
		if(percentage.indexOf(".")!=-1){
			percentage = percentage.substring(0,percentage.indexOf("."));
		}
		if(percentage === 0){
			view.$el.find("."+name+"_status_bar").hide();
		}else{
			view.$el.find("."+name+"_status_bar").show();
		}
		view.$el.find("."+name+"_status_bar .progress-bar-success").css("width",percentage+"%");
		if(perform==all){
			if(setup.progress === 0){
				view.$el.find("."+name+"_status_bar .percentage").html("Not Started");
			}else{
				view.$el.find("."+name+"_status_bar .percentage").html(formateNumber(all));
			}
			view.$el.find("."+name+"_status_bar .count-info").empty();
		}else{
			view.$el.find("."+name+"_status_bar  .percentage").html(percentage+"%");
			view.$el.find("."+name+"_status_bar  .count-info").html(formateNumber(perform)+" / "+formateNumber(all)+"");
			}
	}
	function formateNumber(val){
		val=val+"";
		var newVal = "";
		var po = 0;
		for(var i=val.length-1; i>=0; i--){
			if(po%3 === 0 && po !== 0){
				newVal=","+newVal;
			}
			newVal =val.substring(i,i+1)+newVal;
			po++;
		}
		return newVal;
	}
	function getDate(id){
		var view = this;
		var dfd = $.Deferred();
		app.getJsonData("/org/get/", {id:id}).done(function(data){
			view.currentOrgName = data[0].name;
			data[0]["jss_feature_userlist"] = data[0]["jss.feature.userlist"];
				var html = render("OrganizationInfo-content",{data:data[0]});
				view.$tabContent.bEmpty();
				view.$tabContent.html(html);
				view.$el.trigger("STATUS_CHANGE",false);
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

		if(view.$el.find("[name='name']").val() === ''){
			$nameMsg.removeClass("hide");
		}else{
			$nameMsg.addClass("hide");
		}

		if(view.$el.find("[name='schemaname']").val() === ''){
			$schemanameMsg.removeClass("hide");
		}else{
			$schemanameMsg.addClass("hide");
		}

		if(view.$el.find(".alert-error:not(.hide)").length>0){
			view.validation=false;
			view.$el.find(".save").prop("disabled",false).html("Save");
		}else{
			view.validation=true;
		}
	}
	
	function disableAllBtns(){
		var view = this;
		var $e = view.$el;
		$e.find(".content-table .btn").prop("disabled", true);
	}
	
	// --------- /Private Methods--------- //
})(jQuery);