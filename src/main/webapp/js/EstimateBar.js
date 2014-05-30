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
			if(count < 10){
				count = count;
	       	}else if(count < 100){
	       		count = parseInt((count/10))*10;
	       	}else if(count < 1000){
	       		count = parseInt((count/100))*100;
	       	}else if(count < 10000){
	       		count = parseInt((count/1000))*1000;
	       	}else{
	       		count = parseInt((count/10000))*10000;
	       	}
			label = count + "+";
		}

		var left = count / (maxValue - minValue) * 100;
		if (count > maxValue) {
			left = 100;
		}

		var $label = $e.find(".EstimateBar-label");
		if(parseInt(count) == -1){
			$label.text("");
			$label.append("<i class=\"glyphicon glyphicon-warning-sign\"  title=\"Cannot get count at this time\"></i>");
		}else{
			$label.text(label);
			$label.css("marginLeft", "-" + $label.width() / 2 + "px"); 
			$label.css("left", left + "%");
		}
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
