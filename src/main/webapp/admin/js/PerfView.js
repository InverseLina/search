(function() {

	brite.registerView("PerfView", {
		parent : ".admincontainer",
		emptyParent : true
	}, {

		create : function() {
			return render("PerfView");
		},

		postDisplay : function() {
			var view = this;
			view.$tbodyMethods = view.$el.find(".tbody-methods");
			view.$tbodyRequests = view.$el.find(".tbody-requests");
			view.$javaInfo = view.$el.find(".java-info");
			view.$poolInfo = view.$el.find(".pool-info");
			view.$roPoolInfo = view.$el.find(".roPool-info");
			view.$refresh = view.$el.find(".do-perf-refresh");

			refresh.call(view);
		},

		events : {
			"click; .do-perf-clear" : function(event) {
				var view = this;
				var $button = $(event.target);
				var html = $button.html();
				$button.html("...");

				$.post("/perf-clear").done(function(response) {
					refresh.call(view);
					$button.html(html);
				});
			},

			"click; .do-perf-refresh" : function(event) {
				var view = this;
				refresh.call(view);
			}
		}

	});

	// --------- Private Methods --------- //
	function refresh() {
		var view = this;
		var html = view.$refresh.html();
		view.$refresh.html("..........");
		app.getJsonData("perf-get-all").done(function(response) {
			view.$tbodyMethods.html(render("PerfView-tbody", {
				perfs : response.methodPerfSortValues
			}));
			view.$tbodyRequests.html(render("PerfView-tbody", {
				perfs : response.requestPerfSortValues
			}));
			view.$javaInfo.html(render("PerfView-javaInfo", response.appPerf.javaInfo));
			view.$poolInfo.html(render("PerfView-poolInfo", response.appPerf.poolInfo));
			if(response.appPerf.roPoolInfo){
				view.$roPoolInfo.html(render("PerfView-roPoolInfo", response.appPerf.roPoolInfo));
			}
			
			view.$refresh.html(html);

		});
	}

	// --------- /Private Methods --------- //

})();
