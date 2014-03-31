/**
 * View: PassCodeModal
 *
 *
 *
 *
 */
(function($) {
	brite.registerView("PassCodeModal", {
		emptyParent : false,
		parent : "body"
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			$(".container").hide();
			$(".admin").hide();
			return render("PassCodeModal");
		},

		postDisplay : function(data) {
			var view = this;
			view.$el.find("input[name='passcode']").focus();
		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"btap;.cancel,.close" : function() {
			},
			"validate" : function(event) {
				var view = this;
				var passcode = view.$el.find("input[name='passcode']").val();
				var fail = function() {
					view.$el.find(".alert").show();
				};


				app.getJsonData(contextPath + "/passcode", {
					passcode : passcode
				}, {
					fail : fail,
					type : "Post"
				}).done(function(res) {
					window.location.href = contextPath + "/";
				});
			},
			"btap;.login" : function(event) {
				this.$el.trigger("validate");
			},
			"keyup;:input" : function(event) {
				if (event.which === 13) {
					this.$el.trigger("validate");
				}
			}

		},
		// --------- /Events--------- //
		docEvents : {}
	});
})(jQuery);
