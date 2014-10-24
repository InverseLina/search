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
			var view = this;
			var $e = view.$el;
			var $resumeViewContainer = $e.closest(".resumeViewContainer");
			showView.call(this, data);

			if(app.orgInfo.contact_resume_behave === "hover"){
				$resumeViewContainer.find(".ResumeView").removeClass("modal");
				var $resumeIcon = data.targetIcon;
				$resumeIcon.off("mouseout");
				$resumeIcon.on("mouseout",function(event){
					var x=event.clientX;
	                var y=event.clientY;
	                var resumeViewContainerOffset = $resumeViewContainer.offset();
	                var divx1 = resumeViewContainerOffset.left;
	                var divy1 = resumeViewContainerOffset.top;
	                var divx2 = resumeViewContainerOffset.left + $resumeViewContainer.width();
	                var divy2 = resumeViewContainerOffset.top + $resumeViewContainer.height();
	                if( x < divx1 || x > divx2 || y < divy1 || y > divy2){
	                	$e.bRemove();
	                }
				});

				$resumeViewContainer.off("mouseleave");
				$resumeViewContainer.on("mouseleave", function(){
					$e.bRemove();
				});

			}else{
				$e.closest('body').find(".ResumeView").addClass("modal");
			}

		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"click; .btn-primary, .close" : function() {
				var view = this;
				view.$el.bRemove();
			}
		}
		// --------- /Events--------- //
	});

	// --------- Private Methods--------- //
	function showView(data) {
		var $content, view = this;
		var $e = view.$el;
		var $body = $e.find(".modal-body");
		var query = app.ParamsControl.getQuery();
		var encapeChars = ["\\","*",".","?","+","$","^","[","]","(",")","{","}","|","/"];
		app.getJsonData("getResume", {
			cid : data.id,
			keyword : query
		}).done(function(result) {
			if (result) {
				var resume = result.resume;
				var exact = result.exact;
				if(exact){
					var keyWord = query;
					var keyWordsSplited = keyWord.substring(1,keyWord.length-1);
					keyWordsSplited = keyWordsSplited.split(/["]/);
					for (var k in keyWordsSplited) {
						var Operator = new RegExp("\s*(AND|OR|NOT)\s*");
						if (keyWordsSplited[k] && !(/^ +$/.test(keyWordsSplited[k])) && !Operator.test(keyWordsSplited[k])){
							var keyWord = keyWordsSplited[k].replace(/(^\s*)|(\s*$)/g, "");
							for(var i = 0; i < encapeChars.length; i++){
								var encapeChar = encapeChars[i];
								keyWord = keyWord.replace(new RegExp("\\"+encapeChar,"g"), "\\"+encapeChar);
							}
							var reg = new RegExp("("+ keyWord +")", "gmi");
							resume = resume.replace(reg, "<span class=\"highlight\">$1</span>");
						}
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
