/**
 * View: Slider
 *
 *
 *
 *
 */
(function($) {
	brite.registerView("EstimateBar", {
		loadTmpl : false,
		emptyParent : true
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			data = data || {};
			return render("EstimateBar");
		},

		postDisplay : function(data) {
			var view = this;
			var $e = this.$el;
			var label = data.label||"";
			
			var $label = $e.find(".EstimateBar-label");
			$label.text(label);
			$label.css("marginLeft","-"+$label.width()/2+"px");
			$label.css("left","10px");

		}
		// --------- /View Interface Implement--------- //
	});

	// --------- Private Methods--------- //

	// --------- /Private Methods--------- //

})(jQuery);
