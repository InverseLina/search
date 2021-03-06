(function($) {

	brite.registerView("OrganizationInfo", {
		parent : ".admincontainer",
		emptyParent : true
	}, {
		// --------- View Interface Implement--------- //
		create : function() {
			return render("OrganizationInfo");
		},

		postDisplay : function(data) {
			var view = this;

			view.section = app.pathInfo.paths[0] || "org";

			view.$navTabs = $(".nav-tabs");
			view.$tabContent = view.$el.find(".tab-content");
			view.$navTabs.find("li.active").removeClass("active");

			if (app.pathInfo.paths[1] === "add") {
				var li = render("OrganizationInfo-li", {
					type : "OrganizationInfo",
					url : "#org/add"
				});
				view.$navTabs.find('li:last').before(li);
				var html = render("OrganizationInfo-content", {
					data : null
				});
				view.$tabContent.html(html);
				view.orgId = -1;
				view.$el.find(".extra,.resume,.index").prop("disabled", true);
				app.getJsonData("getOrgSearchConfig").done(function(result) {
					view.$el.find("textarea[name='searchConfig']").val(result.content);
					if (result.errorMsg) {
						view.$el.trigger("DO_SHOW_MSG", {
							selector : ".search-config-alert",
							msg : result.errorMsg,
							type : "error"
						});
						view.$el.find(".search-content").css("background", "#ffdddd");
					}
				});
				view.$el.find("button.saveSearchConfig, button.resetSearchConfig").attr("disable", "true");
			} else {
				view.orgId = app.pathInfo.paths[1] * 1;
				getData.call(view, view.orgId).done(function(orgName) {
					view.orgName = orgName;
					var li = render("OrganizationInfo-li", {
						type : orgName,
						url : "#" + app.pathInfo.paths[0] + "/" + app.pathInfo.paths[1]
					});
					view.$navTabs.find('li:last').before(li);
					app.getJsonData("getOrgSearchConfig", {
						orgName : view.orgName
					}).done(function(result) {
						view.$el.find("textarea[name='searchConfig']").val(result.content);
						if (result.errorMsg) {
							view.$el.trigger("DO_SHOW_MSG", {
								selector : ".search-config-alert",
								msg : result.errorMsg,
								type : "error"
							});
							view.$el.find(".search-content").css("background", "#ffdddd");
						}
					});
				}).fail(function() {
					//show emtpty message
					var html = render("OrganizationInfo-empty-item");
					view.$el.find(".tab-content").html(html);
				});
			}

			$(document).on("btap." + view.cid, function(event) {
				$(".time-list,.table-list", view.$el).hide();
			});
		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"btap;.clearCache" : function(event) {
				var view = this;
				var $btn = $(event.currentTarget);
				$btn.html("Clearing...").prop("disabled", true);
				app.getJsonData("/cache-refresh", {
					orgName:view.orgName
				}, {
					type : "get"
				}).done(function(data) {
					setTimeout(function(){
						$btn.html("Clear Cache").prop("disabled", false);
					}, 300);
				});
			},
			"btap;.home" : function(event) {
				window.location.href = contextPath + "/";
			},
			"click;.cancel":function(event){
				window.location.href=contextPath + "/admin/#org";
			},
			"change;:checkbox,select" : function(event) {
				var view = this;
				var $saveBtn = view.$el.find(".save");
				$saveBtn.removeClass("disabled");
			},
			"click;.save" : function(event) {
				var view = this;
				var $btn = $(event.currentTarget);
				$btn.prop("disabled", true).html("saving...");
				var values = {};
				doValidate.call(view);
				if (view.validation) {
					var configs = {};
					configs["local_distance"] = view.$el.find("[name='local_distance']").val();
					configs["local_date"] = view.$el.find("[name='local_date']").val();
					configs["contact_resume_behave"] = view.$el.find("[name='contact_resume_behave']").val();
					configs["action_add_to_sourcing"] = view.$el.find("[name='action_add_to_sourcing']").prop("checked");
					configs["action_favorite"] = view.$el.find("[name='action_favorite']").prop("checked");
					configs["skill_assessment_rating"] = view.$el.find("[name='skill_assessment_rating']").prop("checked");
					configs["advanced_auto_complete"] = view.$el.find("[name='advanced_auto_complete']").prop("checked");
					values["orgId"] = view.orgId;
					configs["instance_url"] = view.$el.find("[name='instance_url']").val();
					configs["force_login_url"] = view.$el.find("[name='force_login_url']").val();
					configs["apex_resume_url"] = view.$el.find("[name='apex_resume_url']").val();
					configs["canvasapp_secret"] = view.$el.find("[name='canvasapp_secret']").val();
					configs["sessionExpireDuration"] = view.$el.find("[name='sessionExpireDuration']").val();

					values.configsJson = JSON.stringify(configs);
					app.getJsonData("/config/save", values, "Post").done(function(data) {
						if(data && data.msg){
							view.$el.trigger("DO_SHOW_MSG", {
								selector : $(".alert", $btn.closest(".btns")),
								msg : data.msg,
								type : "error"
							});
							$btn.prop("disabled", false).html("Save");
							return;
						}
						$.extend(app.orgInfo || {}, configs);
						values = {};
						values["name"] = view.$el.find("[name='name']").val();
						values["id"] = view.$el.find("[name='id']").val();
						values["schemaname"] = view.$el.find("[name='schemaname']").val();
						values["sfid"] = view.$el.find("[name='sfid']").val();

						app.getJsonData("/org/save", values, "Post").done(function(data) {
							view.$el.trigger("DO_SHOW_MSG", {
								selector : $(".alert", $btn.closest(".btns")),
								msg : "Values saved successfully",
								type : "success"
							});
							$btn.prop("disabled", false).html("Save");
							if (app.pathInfo.paths[1] === "add") {
								window.location.hash = "#org/" + data;
							}
						});
					});
				}

			},
			"FILLDATA" : function(event, result) {
				var view = this;
				var currentField;
				$.each(result.data, function(key, value) {
					currentField = view.$el.find("[name='" + key + "']");
					if (currentField.length > 0) {
						if (currentField.attr("type") === 'checkbox') {
							currentField.prop("checked", value === 'true');
						} else {
							currentField.val(value);
						}
					}
				});
			},
			"click;.multiply" : function(event) {
				var view = this;
				if (!view.time) {
					view.time = 1;
				}
				if (!view.tableName) {
					view.tableName = "contact";
				}
				var $btn = $(event.target);
				if ($btn.prop("disabled")) {
					return false;
				}
				$btn.toggleClass("pause");
				if ($btn.hasClass("pause")) {
					$btn.html("Pause Multiply");
					app.getJsonData("/multiplyData", {
						orgName : view.currentOrgName,
						times : view.time,
						tableName : view.tableName
					}, {
						type : "POST"
					}).done(function(data) {
						window.clearInterval(view.multiplyIntervalId);
						$btn.prop("disabled", false).html("Multiply Data");
						view.$el.find(".multiply-info").hide();
						view.$el.find(".db-info").html("Current Database Size: " + data.currentDbSize + "MB , Cost: " + data.cost + "s").show();
						//view.$el.trigger("MULTIPLY_STATUS_CHANGE");
						$btn.html("Multiply Data");
						$btn.removeClass("pause");
					});
					view.multiplyIntervalId = window.setInterval(function() {
						$(view.el).trigger("MULTIPLY_STATUS_CHANGE");
					}, 5000);
					view.$el.trigger("MULTIPLY_STATUS_CHANGE");
				} else {
					app.getJsonData("/stopMultiply", {}, {
						type : "POST"
					}).done(function(data) {
						$btn.html("Multiply Data");
					});
				}

			},
			"MULTIPLY_STATUS_CHANGE" : function(event) {
				var view = this;
				var $info = view.$el.find(".multiply-info");
				$info.show();
				view.$el.find(".db-info").hide();
				app.getJsonData("/getMultiplyStatus", {}, {
					type : "GET"
				}).done(function(data) {
					$(".time", $info).html(data.currentTime);
					$(".perform", $info).html(data.performCounts);
					$(".total", $info).html(data.contactCounts);
				});
			},
			"click;.drawdown" : function(event) {
				var view = this;
				var $arrow = $(event.currentTarget);
				if ($arrow.next().css("display") !== "none") {
					$arrow.next().hide();
				} else {
					$arrow.next().show();
				}
			},
			"click;[data-time]" : function(event) {
				var view = this;
				var $li = $(event.currentTarget);
				$li.closest(".time-list").hide();
				view.time = $li.attr("data-time");
				$li.closest(".control-group").find("[name='time']").val($li.attr("data-time"));
			},
			"click;[data-table]" : function(event) {
				var view = this;
				var $li = $(event.currentTarget);
				$li.closest(".table-list").hide();
				view.tableName = $li.attr("data-table");
				$li.closest(".control-group").find("[name='tableName']").val($li.attr("data-table"));
			},
			"click;.org-reset" : function(event) {
				var view = this;
				var orgName = view.currentOrgName;
				app.getJsonData("/reset-org-setup", {
					org : orgName
				}, {
					type : "Post"
				}).done(function(result) {
					window.clearInterval(view.orgSetupIntervalId);
					refresh.call(view);
				});
			},
			"click;.org-pause" : function(event) {
				var view = this;
				var orgName = view.currentOrgName;
				app.getJsonData("/stop-org-setup", {
					org : orgName
				}, {
					type : "Post"
				}).done(function(result) {
					window.clearInterval(view.orgSetupIntervalId);
					refresh.call(view);
				});
			},
			"click;.org-setup" : function(event) {
				var view = this;
				var orgName = view.currentOrgName;
				var $btn = $(event.currentTarget);
				var $e = view.$el;
				var $resetBtn = view.$el.find(".org-reset");
				$btn.prop("disabled", true).html("Setup...");
				$e.find(".opts-btns .error").addClass("hide");
				$resetBtn.prop("disabled", true);
				app.getJsonData("/admin-org-setup", {
					org : orgName
				}, {
					type : "Post"
				}).done(function(result) {
					window.clearInterval(view.orgSetupIntervalId);
					refresh.call(view);
				});
				view.orgSetupIntervalId = window.setInterval(function() {
					refresh.call(view);
				}, 3000);
			},
			"click;.org-recreate" : function(event) {
				var view = this;
				var orgName = view.currentOrgName;
				app.getJsonData("/admin-org-recreate-cityscore", {
					org : orgName
				}, {
					type : "Post"
				}).done(function(result) {
					window.clearInterval(view.orgSetupIntervalId);
					refresh.call(view);
				});
			},			
			
			"STATUS_CHANGE" : function(event, times) {
				var view = this;
				times = times || 0;
				var orgName = view.currentOrgName;
				var addClass = "alert-success", removeClass = "alert-danger";
				var $setupBtn = view.$el.find(".org-setup"), $pasueBtn = view.$el.find(".org-pause"), $resetBtn = view.$el.find(".org-reset");
				var $recreateBtn = view.$el.find(".org-recreate");
				times++;
				app.getJsonData("/admin-org-status", {
					org : orgName
				}, {
					type : "Get"
				}).done(function(result) {
					if (result.errorCode) {
						result.status = "error";
					}
					switch(result.status) {
						case  "done"      :
							$setupBtn.prop("disabled", true).html("Setup");
							$pasueBtn.prop("disabled", true);
							$resetBtn.prop("disabled", false);
							$recreateBtn.prop("disabled",false);
							view.$el.find(".save,.saveSearchConfig,.disable-indexes,.multiply,.rebuild-resume,.enable-indexes,.drop-ex").removeClass("disabled");
							break;
						case  "part"      :
							$setupBtn.prop("disabled", false).html("Resume");
							$pasueBtn.prop("disabled", true);
							$resetBtn.prop("disabled", false);
							$recreateBtn.prop("disabled",true);
							view.$el.find(".save,.saveSearchConfig,.disable-indexes,.multiply,.rebuild-resume,.enable-indexes,.drop-ex").removeClass("disabled");
							break;
						case  "error"     :
							$setupBtn.prop("disabled", true).html("Setup");
							$pasueBtn.prop("disabled", true);
							$resetBtn.prop("disabled", false);
							$recreateBtn.prop("disabled",true);
							view.$el.find(".save,.saveSearchConfig,.disable-indexes,.multiply,.rebuild-resume,.enable-indexes,.drop-ex").removeClass("disabled");
							break;
						case  "notstarted":
							$setupBtn.prop("disabled", false).html("Setup");
							$pasueBtn.prop("disabled", true);
							$resetBtn.prop("disabled", false);
							$recreateBtn.prop("disabled",true);
							view.$el.find(".save,.saveSearchConfig,.disable-indexes,.multiply,.rebuild-resume,.enable-indexes,.drop-ex").removeClass("disabled");
							break;
						case  "running"   :
							$setupBtn.prop("disabled", true).html("Setup...");
							$pasueBtn.prop("disabled", false);
							$resetBtn.prop("disabled", true);
							$recreateBtn.prop("disabled",true);
							view.$el.find(".save,.saveSearchConfig,.disable-indexes,.multiply,.rebuild-resume,.enable-indexes,.drop-ex").addClass("disabled");
							setTimeout(function() {
								refresh.call(view);
							}, 3000);
							break;

					}
					$.each(result.setups, function(index, setup) {
						switch(setup.status) {
							case "done":
								addClass = "alert-success";
								removeClass = "alert-danger";
								break;
							case "part":
								addClass = "alert-danger";
								removeClass = "alert-success";
								break;
							case "error":
								addClass = "alert-danger";
								removeClass = "alert-success";
								break;
							case "notstarted":
								addClass = "alert-danger";
								removeClass = "alert-success";
						}
						if(setup.name && setup.name.indexOf("current_")==0){//now only the indexes
							var $info=view.$el.find("." + setup.name.replace("current_","") + "es_info");
							if(setup.status=="done"){
								$info.addClass("hide");
							}else{
								$info.addClass("alert-success").removeClass("alert-danger").removeClass("hide");
								$info.find("span").fadeOut(100,function(){
									$(this).html("Creating "+setup.value).fadeIn();
								});
							}
						}
						if(setup.name && setup.name.indexOf("current_custom_")==0){//now only the custom indexes
							var $info=view.$el.find("." + setup.name.replace("current_","") + "es_info");
							if(setup.status=="done"){
								$info.addClass("hide");
							}else{
								$info.addClass("alert-success").removeClass("alert-danger").removeClass("hide");
								$info.find("span").fadeOut(100,function(){
									$(this).html("Creating "+setup.value).fadeIn();
								});
							}
						}
						if (setup.progress || setup.progress === 0) {
							if(setup.name === "customFieldIndexes"){
								if(+setup.progress.perform+setup.progress.remaining===0){
									view.$el.find(".customFieldIndexes_status_bar").closest("tr").addClass("hide");
								}else{
									view.$el.find(".customFieldIndexes_status_bar").closest("tr").removeClass("hide");
								}
							}
							fillProgressBar.call(view, setup);
						} else {
							view.$el.find("." + setup.name + "_info").addClass(addClass).removeClass(removeClass).removeClass("hide").html(setup.msg);
						}
					});
					if(result.RemoveIndex){
						view.$el.find(".CustomField_Indexes_Need_Remove").closest("tr").removeClass("hide");
						view.$el.find(".CustomField_Indexes_Need_Remove").html(result.RemoveIndex);
					}else{
						view.$el.find(".CustomField_Indexes_Need_Remove").closest("tr").addClass("hide");
					}
				}).fail(function(){
					if(times < 3){
						view.$el.trigger("STATUS_CHANGE",times);
					}else{
						showStatusError.call(view);
					}
				});
			},
			"click; button.saveSearchConfig" : function(event) {
				event.stopPropagation();
				event.preventDefault();
				var view = this;
				var $btn = $(event.currentTarget);
				$btn.prop("disabled", true).html("saving...");
				var content = view.$el.find("textarea[name='searchConfig']").val();
				app.getJsonData("saveOrgSearchConfig", {
					orgId : view.orgId,
					content : content
				}, "Post").done(function(result) {
					if (!result.valid) {
						view.$el.find(".search-content").css("background", "#ffdddd");
						view.$el.trigger("DO_SHOW_MSG", {
							selector : ".search-config-alert",
							msg : result.errorMsg,
							type : "error"
						});
					} else {
						if(result.warnMsg){
							view.$el.find(".search-content").css("background", "#ffffff");
							view.$el.trigger("DO_SHOW_MSG", {
								selector : ".search-config-alert",
								msg : result.warnMsg,
								type : "warn"
							});
						}else{
							view.$el.find(".search-content").css("background", "#ffffff");
							view.$el.trigger("DO_SHOW_MSG", {
								selector : ".search-config-alert",
								msg : "Values saved successfully",
								type : "success"
							});
						}
					}
					refresh.call(view);
					$btn.prop("disabled", false).html("Save");
				});
				return false;
			},
			"click; button.resetSearchConfig" : function(event) {
				var view = this;
				var $btn = $(event.currentTarget);
				$btn.prop("disabled", true).html("resetting...");
				app.getJsonData("resetOrgSearchConfig", {
					orgName : view.orgName
				}).done(function(result) {
					view.$el.find("textarea[name='searchConfig']").val(result);
					view.$el.find(".search-content").css("background", "#ffffff");
					view.$el.trigger("DO_SHOW_MSG", {
						selector : ".search-config-alert",
						msg : "search config has been reset successfully.",
						type : "success"
					});
					$btn.prop("disabled", false).html("Reset");
					refresh.call(view);
				});
			},
			"click;.disable-indexes" : function(event) {
				var view = this;
				var disableBtn = $(event.currentTarget);
				disableBtn.prop("disabled", true).html("Disabling...");
				app.getJsonData("removeAllIndexes", {
					orgName : view.orgName
				}, {
					type : 'Post'
				}).done(function(result) {
					disableBtn.html("Indexes Disabled");
					refresh.call(view);
				});
			},
			"click;.enable-indexes" : function(event) {
				var view = this;
				var $disableBtn = $(event.currentTarget);
				refresh.call(view);
				$(".alert", $disableBtn.closest("div")).show();
			},
			"click;.drop-ex" : function(event) {
				var view = this;
				var $disableBtn = $(event.currentTarget);
				app.getJsonData("/dropExTables", {
					orgName : view.orgName
				}, {
					type : 'Post'
				}).done(function(result) {
					refresh.call(view);
					$(".alert", $disableBtn.closest("div")).show();
				});
			},
			"click;.rebuild-resume":function(event){
				var view = this;
				var $rebuildBtn = $(event.currentTarget);
				app.getJsonData("/rebuildResume",{
					orgName : view.orgName
				}, {
					type : 'Post'
				}).done(function(result) {
					refresh.call(view);
					$(".alert", $rebuildBtn.closest("div")).removeClass("hide");
				});
			}
		}

		// --------- /Events--------- //
	});

	// --------- Private Methods--------- //
	function refresh() {
		var view = this;
		view.$el.trigger("STATUS_CHANGE");
	}
	
	function showStatusError(){
		var view = this;
		var $e = view.$el;
		var $setupBtn = $e.find(".opts-btns .org-setup");
		$setupBtn.prop("disabled", false).html("Resume");
		$e.find(".opts-btns .error").removeClass("hide");
	}

	function fillProgressBar(setup) {
		var view = this;
		var name = setup.name, all = setup.progress.perform + setup.progress.remaining, perform = setup.progress.perform, percentage = perform / all * 100 + "";
		if (setup.progress === 0) {
			all = 0;
			perform = 0;
			percentage = "0";
		}
		if (percentage.indexOf(".") !== -1) {
			percentage = percentage.substring(0, percentage.indexOf("."));
		}
		if (percentage === 0) {
			view.$el.find("." + name + "_status_bar").hide();
		} else {
			view.$el.find("." + name + "_status_bar").show();
		}
		view.$el.find("." + name + "_status_bar .progress-bar-success").css("width", percentage + "%");
		if (perform === all) {
			if (setup.progress === 0) {
				view.$el.find("." + name + "_status_bar .percentage").html("Not Started");
			} else {
				view.$el.find("." + name + "_status_bar .percentage").html(formateNumber(all));
			}
			view.$el.find("." + name + "_status_bar .count-info").empty();
		} else {
			view.$el.find("." + name + "_status_bar  .percentage").html(percentage + "%");
			view.$el.find("." + name + "_status_bar  .count-info").html(formateNumber(perform) + " / " + formateNumber(all) + "");

		}
	}

	function formateNumber(val) {
		val = val + "";
		var newVal = "";
		var po = 0;
		for (var i = val.length - 1; i >= 0; i--) {
			if (po % 3 === 0 && po !== 0) {
				newVal = "," + newVal;
			}
			newVal = val.substring(i, i + 1) + newVal;
			po++;
		}
		return newVal;
	}

	function getData(id) {
		var view = this;
		var dfd = $.Deferred();
		app.getJsonData("/org/get/", {
			id : id
		}).done(function(data) {
			if (!data || data.length === 0) {
				// show empty pages
				dfd.reject();
			} else {
				view.currentOrgName = data[0].name;
				var html = render("OrganizationInfo-content", {
					data : data[0]
				});
				view.$tabContent.bEmpty();
				view.$tabContent.html(html);
				view.$el.trigger("STATUS_CHANGE");
				dfd.resolve(data[0].name);
				app.getJsonData("/config/get/", {
					orgId : view.orgId
				}).done(function(data) {
					if (view && view.$el) {
						view.$el.trigger("FILLDATA", {
							data : data
						});
					}
				});
			}

		});
		return dfd.promise();
	}

	function doValidate() {
		var view = this;
		var $nameMsg = view.$el.find(".alert-error.name");
		var $schemanameMsg = view.$el.find(".alert-error.schemaname");
		var $sessionTimeout = view.$el.find(".alert-error.sessionTimeout");

		if (view.$el.find("[name='name']").val() === '') {
			$nameMsg.removeClass("hide");
		} else {
			$nameMsg.addClass("hide");
		}

		if (view.$el.find("[name='schemaname']").val() === '') {
			$schemanameMsg.removeClass("hide");
		} else {
			$schemanameMsg.addClass("hide");
		}

		var timeout = view.$el.find("[name='sessionExpireDuration']").val();
		if (timeout != "" && (isNaN(timeout * 1) || timeout * 1 <= 0)) {
			$sessionTimeout.removeClass("hide");
		} else {
			$sessionTimeout.addClass("hide");
		}

		if (view.$el.find(".alert-error:not(.hide)").length > 0) {
			view.validation = false;
			view.$el.find(".save").prop("disabled", false).html("Save");
		} else {
			view.validation = true;
		}
	}

	// --------- /Private Methods--------- //
})(jQuery); 
