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
			
			refresh.call(view,{count:0, exact:true});
		},
		docEvents:{
			"REFRESH_ESTIMATE_COUNT":function(e, extra){
				var view = this;
				refresh.call(view,extra);
			},
			"DO_ESTIMATE_BAR_WAITING":function(view){
				var view = this;
				doWaiting.call(view);
			}
		}
		// --------- /View Interface Implement--------- //
	});

	// --------- Private Methods--------- //
	function refresh(data){
		var view = this;
		var $e = view.$el;
		data = data || {};
		var count = data.count || 0;
		var exact = data.exact || false;
		if(view.timing){
			clearInterval(view.timing);
		}

		var maxValue = 5000;
		var minValue = 0;
		
		var label = count;
		if (!exact) {
			label = count + "+";
		}

		var left = count / (maxValue - minValue) * 100;
		if (count > maxValue) {
			left = 100;
		}

		var $label = $e.find(".EstimateBar-label");
		$label.text(label);
		$label.css("marginLeft", "-" + $label.width() / 2 + "px"); 
		$label.css("left", left + "%");
	}
	
	
	function doWaiting(){
		var view = this;
		var $e = view.$el;
		var $label = $e.find(".EstimateBar-label");
		var c = 0;
		if(view.timing){
			clearInterval(view.timing);
		}
		$label.text(".");
		$label.css("marginLeft", "-" + $label.width() / 2 + "px"); 
		$label.css("left", "0%");
		view.timing = setInterval(function(){
			c = c % 3;
			var label = "";
			for(var i = 0; i <= c; i++){
				label +=".";
			}
			$label.text(label);	
			c++;
		}, 500);
		
	}
	// --------- /Private Methods--------- //

})(jQuery);
