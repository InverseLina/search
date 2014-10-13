var app = app || {};

(function($) {
	var cache;
	
	var getConfig = function() {
		var org = app.cookie("org");
		if (org) {
			var filters = null;
			app.getJsonData("searchuiconfig", {
				org : org
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
		} else {
			return {};
		}
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
