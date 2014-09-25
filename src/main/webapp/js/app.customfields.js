var app = app || {};

(function($) {
	var cache;

	app.getCustomFields = function(update) {
		var result;
		if (update) {
			cache = getFields();
		}
		if (!cache){
			cache = getFields();
		}
		result = cache;
		return $.extend([], result);
	};


	function getFields(){
		var fields = [];
		app.getJsonData("/getCustomFields", {}, {async : false}).done(function(result) {
			fields = result;
		});
		return fields;
	}

})(jQuery);
