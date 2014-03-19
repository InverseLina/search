var app = app || {};
(function($) {

	app.WarmupDaoHandler = {};

	app.WarmupDaoHandler.search = function(data) {
		return app.getJsonData(contextPath + "/perf/search", data);
	}

	app.WarmupDaoHandler.autocomplete = function(data) {
		return app.getJsonData(contextPath + "/perf/autocomplete", data);
	}

	app.WarmupDaoHandler.checkStatus = function() {
		return app.getJsonData(contextPath + "/perf/checkStatus");
	}

})(jQuery);