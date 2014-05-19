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
			data = data || {};
			
			var maxValue = 5000;
			var minValue = 0;
			
			var count = data.count || 1;
			var exact = data.exact || false;
			
			var label = count;
			if(!exact){
				label = count + "+";
			}
			
			var left = count / (maxValue - minValue) * 100;
			if(count > maxValue){
				left = 100;
			}
			
			var $label = $e.find(".EstimateBar-label");
			$label.text(label);
			$label.css("marginLeft","-"+$label.width()/2+"px");
			$label.css("left", left + "%");

		}
		// --------- /View Interface Implement--------- //
	});

	// --------- Private Methods--------- //

	// --------- /Private Methods--------- //

})(jQuery);
