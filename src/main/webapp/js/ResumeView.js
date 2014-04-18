/**
 * View: ResumeView
 *
 *
 *
 *
 */
(function($) {

	brite.registerView("ResumeView", {
		emptyParent : false,
		parent : "body"
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			$("#resumeModal").bRemove();
			data = data || {};

			return render("ResumeView", {
				name : data.name
			});
		},

		postDisplay : function(data) {
			showView.call(this, data);

		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"click; .btn-primary, .close" : function() {
				var view = this;
				view.$el.bRemove();
			}

			// --------- /Events--------- //

		},
		docEvents : {}
	});

	// --------- Private Methods--------- //
	function showView(data) {
		var $content, view = this;
		var $e = view.$el;
		var $body = $e.find(".modal-body");
		app.getJsonData("getResume", {
			cid : data.id
		}).done(function(result) {
			if (result.length > 0) {
				var resume = result[0]["ts2__text_resume__c"];
				var keyWord = app.ParamsControl.getQuery();
				var keyWordsSplited = keyWord;
				var pattern = /^\s*\".+\"\s*$/g;
				var exact = false;
				if (pattern.test(keyWord)) {
					var reg = new RegExp("\"", "g");
					keyWordsSplited = keyWordsSplited.replace(reg, "");
					exact = true;
				}
				pattern = /[^A-z0-9 ]/g;
				if (pattern.test(keyWord)) {
					keyWordsSplited = keyWordsSplited.replace(pattern, "");
				}
				if(exact){
					keyWordsSplited = [keyWordsSplited];
				}else{
					keyWordsSplited = keyWordsSplited.split(/\s+/);
				}
				for (var k in keyWordsSplited) {
					if (keyWordsSplited[k] != "AND" && keyWordsSplited[k] !== "OR" && keyWordsSplited[k] !== "NOT"){
						var reg = new RegExp("(" + keyWordsSplited[k] + ")", "gi");
						resume = resume.replace(reg, "<span class=\"highlight\">$1</span>");
					}
				}
				$content = $(render("ResumeView-content", {
					resume : resume
				}));
				$body.html("<pre>" + resume + "</pre>");
			} else {
				$content = $(render("ResumeView-content", {
					resume : "not resume"
				}));
				$body.append($content);
			}

		});
	}

	// --------- /Private Methods--------- //

})(jQuery);
