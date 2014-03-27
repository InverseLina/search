var app = app || {};
(function($) {

	app.WarmupDaoHandler = {};

	app.WarmupDaoHandler.search = function(data) {
		return app.getJsonData(contextPath + "/warmup/search", data);
	}

	app.WarmupDaoHandler.autocomplete = function(data) {
		return app.getJsonData(contextPath + "/warmup/autocomplete", data);
	}

	app.WarmupDaoHandler.checkStatus = function() {
		return app.getJsonData(contextPath + "/warmup/checkStatus");
	}
})(jQuery);
