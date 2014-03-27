(function($) {

	brite.registerView("Organization", {
		parent : ".admincontainer",
		emptyParent : true
	}, {

		// --------- View Interface Implement--------- //
		create : function() {
			return render("Organization");
		},
		postDisplay : function(data) {
			var view = this;

			view.section = app.pathInfo.paths[0] || "org";

			view.$navTabs = $(".nav-tabs");
			view.$tabContent = view.$el.find(".tab-content");
			view.$navTabs.find("li.active").removeClass("active");

			if (app.pathInfo.paths[1] === "add" || !isNaN(app.pathInfo.paths[1] * 1)) {
				brite.display("OrganizationInfo");
			} else {
				view.$navTabs.find("a[href='#org']").closest("li").addClass("active");
				refreshEntityTable.call(view);
			}
		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"click;.home" : function(event) {
				window.location.href = contextPath + "/";
			},

			"click;.add" : function(event) {
				var view = this;
				var html = render("Organization-content", {
					data : null
				});
				view.$tabContent.html(html);
				window.location.href = contextPath + "/admin/#org/add";
			},
			"click; .del" : function(event) {
				var view = this;
				var entityInfo = $(event.target);
				doDelete.call(view, entityInfo.attr("data-entity"));
			}
		}
		// --------- /Events--------- //
	});

	// --------- Private Methods--------- //
	function refreshEntityTable() {
		var view = this;
		app.getJsonData("/org/list").done(function(data) {
			var contextUrl, baseUrl, adminIndex = window.location.pathname.indexOf("/admin");
			var loc = window.location;
			if (adminIndex === 0) {
				baseUrl = "/";
			} else {
				baseUrl = window.location.pathname.substr(0, adminIndex + 1);
			}

			contextUrl = loc.protocol + "//" + loc.host + baseUrl;
			var html = render("Organization-list", {
				list : data,
				ctxUrl : contextUrl
			});
			view.$tabContent.bEmpty();
			view.$tabContent.html(html);
		});
	}

	function doDelete(id) {
		var view = this;
		app.getJsonData("/org/del/", {
			id : id
		}, "Post").done(function(data) {
			refreshEntityTable.call(view);
		});
	}

	// --------- /Private Methods--------- //

})(jQuery); 
