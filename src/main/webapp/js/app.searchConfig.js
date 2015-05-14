var app = app || {};

(function($) {
	var cache;
	
	var getConfig = function() {
		var filters = null;
		app.getJsonData("searchuiconfig", {
		}, {
			async : false
		}).done(function(result) {
			filters = result;
		});
		var temp = [];
		$.each(filters || {}, function(idx, item) {
			item.label = item.title;
			temp.push(item);
			temp[item.name] = item;
		});
		return temp;
	};

	app.getSearchUiConfig = function(update) {
		var result;
		if (update) {
			cache = getConfig();
		}
		if (!cache){
			cache = getConfig();
		}
		result = cache;
		return $.extend([], result);
	};
	
})(jQuery);
