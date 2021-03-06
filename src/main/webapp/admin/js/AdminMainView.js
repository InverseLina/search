(function($) {

	var defaultPathInfo = {
		paths : ["setup"]
	};

	brite.registerView("AdminMainView", {
		parent : ".container",
		emptyParent : true
	}, {

		// --------- View Interface Implement--------- //
		create : function(data) {
			return render("AdminMainView");
		},

		postDisplay : function(data) {
			var view = this;
			$(".home").removeClass("hide");
			$(".config").addClass("hide");
			$(".clear-all").addClass("hide");
			view.$el.find(".organization-tab").addClass("hide");
			app.getJsonData("/admin-sys-status", {}, {
				type : "Get"
			}).done(function(result) {
				if (result.status === "done") {
					showOrgTab.call(view);
				}
			});

			this.$el.trigger("PATH_INFO_CHANGE", buildPathInfo());
		},
		// --------- /View Interface Implement--------- //

		// --------- Windows Event--------- //
		winEvents : {
			hashchange : function(event) {
				this.$el.trigger("PATH_INFO_CHANGE", buildPathInfo());
			}
		},
		// --------- /Windows Event--------- //

		// --------- Events--------- //
		events : {
			"PATH_INFO_CHANGE" : function(event, pathInfo) {
				changeView.call(this, pathInfo);
			},
			"DO_SHOW_MSG" : function(event, opts) {
				var $place = $(opts.selector);
				if (opts.type === "success") {
					$place.removeClass("alert-danger").removeClass("alert-warning").addClass("alert-success").removeClass("hide").removeClass("out").addClass("in").html(opts.msg);
					setTimeout(function() {
						$place.addClass("out").removeClass("in");
					}, 2000);
				} else if (opts.type === "warn"){
					$place.removeClass("alert-success").removeClass("alert-danger").addClass("alert-warning").removeClass("hide").removeClass("out").addClass("in").html(opts.msg);
					setTimeout(function() {
						$place.addClass("out").removeClass("in");
					}, 3000);
				}else{
					$place.removeClass("alert-success").removeClass("alert-warning").addClass("alert-danger").removeClass("out").addClass("in").removeClass("hide").html(opts.msg);
				}
			}
		},
		docEvents : {
			"DO_SHOW_ORG_TAB" : function() {
				showOrgTab.call(this);
			}
		}
		// --------- /Events--------- //

	});
	// --------- Private Methods --------- //
	function changeView(pathInfo) {
		var view = this;
		view.$el.find(".nav-tabs li.OrganizationInfo").remove();
		pathInfo = pathInfo || defaultPathInfo;
		var viewName = pathInfo.paths[0];
		if (viewName === "org") {
			brite.display("Organization");
		} else if (viewName === "perf") {
			brite.display("PerfView");
		} else if (viewName === "trigger-test") {
			brite.display("TriggerTestView");
		} else if (viewName === "search-config") {
			brite.display("AdminSearchConfig");
		} else if (viewName === "sync-sf") {
			brite.display("SyncView");
		} else if (viewName === "warmup") {
			brite.display("WarmupView");
		} else {
			brite.display("Setup");
		}
		// change the nav selection
		if (pathInfo.paths.length === 1) {
			view.$el.find(".nav-tabs li.active").removeClass("active");
			view.$el.find(".nav-tabs li." + viewName).addClass("active");
		}
	}

	function showOrgTab() {
		var view = this;
		view.$el.find(".organization-tab").removeClass("hide");
	}

	// --------- /Private Methods --------- //

	// --------- Utilities--------- //
	function buildPathInfo() {
		var pathInfo = $.extend({}, defaultPathInfo);
		var hash = window.location.hash;
		if (hash) {
			hash = hash.substring(1);
			if (hash) {
				var pathAndParam = hash.split("!");
				pathInfo.paths = pathAndParam[0].split("/");
				// TODO: need to add the params
			}
		}
		app.pathInfo = pathInfo;
		return pathInfo;
	}

	// --------- /Utilities--------- //

})(jQuery); 