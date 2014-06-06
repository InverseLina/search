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
			
			if(brite.ua.hasCanvas()){
				drawCurveBar.call(view);
			}
			
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
	
	function drawCurveBar(){
		var view = this;
		var $e = this.$el;
		$e.find(".elementsCon").css("opacity",0);
		$e.append("<canvas class='EstimateBar-canvas' width=0 height=0 ></canvas>");
		var $canvas = $e.find(".EstimateBar-canvas");
		var gtx = brite.gtx($canvas);
		gtx.fitParent();
		
		var width = $canvas.width();
		var height = $canvas.height();
		var firstWidth = 26;
		var minWidth = 10;
		var minHeight = 5;
		var minDeltaWidth = 3;
		var mintGapWidth = 2;
		var count = 7;
		gtx.beginPath();
		gtx.moveTo(0, height);
		gtx.lineTo(width, height);
		
		var maxHeight = getDeltaTarget.call(view, minHeight, 1, 1, count);
		gtx.lineTo(width, height - maxHeight);
		gtx.bezierCurveTo(width - width/5, height - maxHeight*2/3, width - width * 3 / 5, height - maxHeight / 3, 0, height);
		// gtx.lineTo(1, minHeight);
		gtx.lineTo(0, height);
		gtx.closePath();
		
		var gradient = gtx.createLinearGradient(0, height, width, height);
		gradient.addColorStop(0.00, "#6d999c");
		gradient.addColorStop(1.00, "#c2eef6");
		gtx.fillStyle(gradient);
		gtx.fill();
		
		gtx.fillStyle("#ffffff");
		var start = minWidth;
		var totalX = firstWidth;
		var clearHeight = getDeltaTarget.call(view, minHeight, 1, 1, 6);
		for(var i = 0; i < count; i++){
			var width = getDeltaTarget.call(view,minWidth,minDeltaWidth,1, i);
			var gapWidth = getDeltaTarget.call(view,mintGapWidth,0.5,0.1,i);
			gtx.fillRect(totalX, height - clearHeight, gapWidth, clearHeight);
			totalX += width + gapWidth;
		}
	}
	
	function getDeltaTarget(start,delta,acceleration, pos){
		var end = start;
		for(var i = 0; i < pos; i++){
			end = end + delta + i * acceleration;
		}
		return end;
	}
	// --------- /Private Methods--------- //

})(jQuery);
