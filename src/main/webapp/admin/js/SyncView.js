/**
 * View: TriggerTestView
 *
 *
 *
 *
 */
(function($) {
	brite.registerView("SyncView", {
		parent : ".admincontainer",
		emptyParent : true
	}, {
		create : function(data, config) {
			return render("SyncView");
		},

		postDisplay : function(data) {
			var view = this;
			app.getJsonData("/syncsf/downloadStatus").done(function(result) {
				updateStatus.call(view, result);
			});
		},
		events : {
			"btap; button.download" : function(event) {
				var view = this;
				app.getJsonData("/syncsf/downloadStatus").done(function(result) {
					if (result === true) {
						app.getJsonData("/syncsf/stopDownload");
						setTimeout(function() {
							checkStatus.call(view);
						}, 300);
					} else {
						app.getJsonData("/syncsf/startDownload");
						setTimeout(function() {
							checkStatus.call(view);
						}, 300);
					}
				});

			}
		},
		docEvents : {}
	});
	function updateStatus(status) {
		var view = this;
		var btn = view.$el.find("button.download");
		if (status === true) {
			btn.html("Stop");
		} else {
			btn.html("Start");
		}
	}

	function checkStatus() {
		var view = this;
		var getStatus = function() {
			app.getJsonData("/syncsf/downloadStatus").done(function(result) {
				if (result === false) {
					updateStatus.call(view, result);
				} else {
					if (view) {
						setTimeout(getStatus, 10 * 1000);
						updateStatus.call(view, true);
					}

				}
			});
		};
		getStatus();

	}

})(jQuery);
