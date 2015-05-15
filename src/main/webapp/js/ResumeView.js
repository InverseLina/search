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
		app.getJsonData("getResume", {
			cid : data.id,
			keyword : query
		}).done(function(result) {
			if (result) {
				view.resume = result.resume;
				var exact = result.exact;
				var hasExact = result.hasExact || false;
				var hasNotExact = result.hasNotExact || false;
				var hasExactValue = result.hasExactValue || [];
				var notExactValue = result.notExactValue || [];
				if(exact || hasExact || hasNotExact){
					if(exact){
						renderResume.call(view,query,"exact")
					}else{
						for(var j=0;j<hasExactValue.length;j++){
							renderResume.call(view,hasExactValue[j],"exact");
						}
					}
					if(hasNotExact){
						for(var i=0;i<notExactValue.length;i++){
							renderResume.call(view,notExactValue[i],"notExact");
						}
					}
				}
				
				$content = $(render("ResumeView-content", {
					resume : view.resume
				}));
				$body.html("<pre>" + view.resume + "</pre>");
			} else {
				$content = $(render("ResumeView-content", {
					resume : "not resume"
				}));
				$body.append($content);
			}

		});
	}

	// --------- /Private Methods--------- //

	function renderResume(query,label) {
		var view = this;
		var encapeChars = ["\\","*",".","?","+","$","^","[","]","(",")","{","}","|","/"];
		var keyWord = query;
		if(label == "exact"){
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
					var reg = new RegExp("\\b"+ keyWord +"\\b", "gmi");
					view.resume = view.resume.replace(reg, "<span class=\"highlight\">" + keyWord + "</span>");;
					renderKeyWordContains.call(view,keyWord);
				}
			}
		}else{
			var reg = new RegExp("("+ keyWord +")", "gmi");
			view.resume = view.resume.replace(reg, "<span class=\"highlight\">" + keyWord + "</span>");
			renderKeyWordContains.call(view,keyWord);
		}
	}


	function renderKeyWordContains(keyWord) {
		var view = this;
		var keyWordsSplitedBySpace = keyWord.split(" ");
		for(var s in keyWordsSplitedBySpace){
			var keyWordS = keyWordsSplitedBySpace[s];
			var keywordString = keyWord.replace(keyWordS,"<span class=\"highlight\">" + keyWordS + "</span>");
			var regLabel = new RegExp(keywordString, "gmi");
			view.resume = view.resume.replace(regLabel, "<span class=\"highlight\">" + keyWord + "</span>");
		}
	}
})(jQuery);
