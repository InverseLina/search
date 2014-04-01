(function($) {

	brite.registerView("Setup", {
		parent : ".admincontainer",
		emptyParent : true
	}, {
		// --------- View Interface Implement--------- //
		create : function() {
			return render("Setup");
		},
		postDisplay : function(data) {
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
		events : {
			"btap;.clearCache" : function(event) {
				var view = this;
				var $btn = $(event.currentTarget);
				$btn.html("Clearing...").prop("disabled", true);
				app.getJsonData("/cache-refresh-all", {}, {
					type : "get"
				}).done(function(data) {
					setTimeout(function(){
						$btn.html("Clear Cache").prop("disabled", false);
					}, 300);
				});
			},
			"btap;.cancel" : function(event) {
				window.location.href = contextPath + "/";
			},
			"btap;.home" : function(event) {
				window.location.href = contextPath + "/";
			},
			"click;.setupStart:not([disabled])" : function(event) {
				var view = this;
				view.$el.trigger("START");
			},
			"click;.setupReset:not([disabled])" : function(event) {
				var view = this;
				view.$el.trigger("RESET");
			},
			"START" : function(event) {
				var view = this;
				app.getJsonData("/admin-sys-start", {}, {
					type : "Post"
				}).done(function(data) {
					view.$el.trigger("STATUS_CHANGE", data);
					startTimer.call(view);
				});
			},
			"RESET" : function(event) {
				var view = this;
				app.getJsonData("/admin-sys-reset", {}, {
					type : "Post"
				}).done(function(data) {
					view.$el.trigger("STATUS_CHANGE", data);
				});
			},
			"STATUS_CHANGE" : function(event, statusData) {
				var view = this;
				var $e = view.$el;
				$e.find(".save,.button").addClass("disabled");
				var $btnStart = $e.find(".setupStart");
				var $btnReset = $e.find(".setupReset");
				var $alertCreateSchema = $e.find(".create .alert").removeClass("alert-warning alert-success alert-error alert-info");
				var $alertImportZipcode = $e.find(".import .alert").removeClass("alert-warning alert-success alert-error alert-info");
				var $alertCreatePgTrgm = $e.find(".create_pg_trgm .alert").removeClass("alert-warning alert-success alert-error alert-info");
				var $alertImportCity = $e.find(".import-city .alert").removeClass("alert-warning alert-success alert-error alert-info");
				var $alertCheckColumns = $e.find(".check-columns .alert").removeClass("alert-warning alert-success alert-error alert-info");
				if (!statusData) {
					return;
				}
				if (statusData.create_sys_schema.status === "done") {
					$alertCreateSchema.addClass("alert-success").html("Done");
				} else if (statusData.create_sys_schema.status === "incomplete") {
					$alertCreateSchema.addClass("alert-warning").html("Incomplete");
				} else {
					$alertCreateSchema.addClass("alert-info").html(statusData.create_sys_schema.status);
				}

				if (statusData.import_zipcode.status === "done") {
					$alertImportZipcode.addClass("alert-success").html("Done");
				} else if (statusData.import_zipcode.status === "incomplete") {
					$alertImportZipcode.addClass("alert-warning").html("Incomplete");
				} else {
					$alertImportZipcode.addClass("alert-info").html(statusData.import_zipcode.status);
				}

				if (statusData.create_extension.status === "done") {
					$alertCreatePgTrgm.addClass("alert-success").html("Done");
				} else if (statusData.create_extension.status === "incomplete") {
					$alertCreatePgTrgm.addClass("alert-warning").html("Incomplete");
				} else {
					$alertCreatePgTrgm.addClass("alert-info").html(statusData.create_extension.status);
				}

				if (statusData.import_city.status === "done") {
					$alertImportCity.addClass("alert-success").html("Done");
				} else if (statusData.import_city.status === "incomplete") {
					$alertImportCity.addClass("alert-warning").html("Incomplete");
				} else {
					$alertImportCity.addClass("alert-info").html(statusData.import_city.status);
				}

				if (statusData.check_missing_columns.status === "done") {
					$alertCheckColumns.addClass("alert-success").html("Done");
				} else if (statusData.check_missing_columns.status === "incomplete") {
					$alertCheckColumns.addClass("alert-warning").html("Incomplete, " + "<b>Missing column(s):</b>" + statusData.check_missing_columns.missingsColumns);
				} else {
					$alertCheckColumns.addClass("alert-info").html(statusData.check_missing_columns.status);
				}

				if (statusData.status === "notstarted") {
					$btnStart.removeClass("hide").prop("disabled", false);
					$btnReset.removeClass("hide").prop("disabled", false).html("Reset");

					stopTimer.call(view);
				} else if (statusData.status === "running") {
					$btnStart.removeClass("hide").prop("disabled", true);
					$btnReset.removeClass("hide");
					if (statusData.caceling) {
						$btnReset.html("Reseting...").prop("disabled", true);
					} else {
						$btnReset.html("Reset").prop("disabled", false);
					}
					startTimer.call(view);
				} else if (statusData.status === "incomplete") {
					$btnStart.removeClass("hide").prop("disabled", false);
					$btnReset.removeClass("hide").prop("disabled", false).html("Reset");

					stopTimer.call(view);
				} else if (statusData.status === "done") {
					$btnStart.removeClass("hide").prop("disabled", true);
					$btnReset.removeClass("hide");
					stopTimer.call(view);

					$e.find(".setting .alert").removeClass("alert-info").addClass("alert-success").html("Done");
					$e.find(".save,.button").removeClass("disabled");
					$e.trigger("DO_SHOW_ORG_TAB");
				}

				if (statusData.reseting) {
					startTimer.call(view);
				} else if (statusData.status !== "running") {
					stopTimer.call(view);
				}
			},
			"FILLDATA" : function(event, result) {
				var view = this;
				var currentField;
				$.each(result.data, function(key, value) {
					currentField = view.$el.find("[name='" + key + "']");
					if (currentField.length > 0) {
						currentField.val(value);
					}
				});
			}
		}
		// --------- /Events--------- //
	});

	function startTimer() {
		var view = this;
		if (!view.timer) {
			view.timer = true;
			getStatus.call(view);
		}

	}

	function stopTimer() {
		var view = this;
		if (view.timer) {
			view.timer = false;
		}
	}

	function getStatus() {
		var view = this;
		var $e = view.$el;
		app.getJsonData("/admin-sys-status", {}, "Get").done(function(data) {
			$e.trigger("STATUS_CHANGE", data);
			if (view.timer) {
				setTimeout(function() {
					getStatus.call(view);
				}, 1000);
			}
		});

	}

})(jQuery); 