var app = app || {};
(function($){
	//-------- Search Dao handler ---------//
	
	function SearchDaoHandler(){
	}
	
	SearchDaoHandler.prototype.search = function(qParams,mode,pageIdx,pageSize){
		var data = $.extend({},qParams);
		data.searchMode = mode;
		data.pageIdx = pageIdx;
		data.pageSize = pageSize;
		
		return $.ajax({
			type : "GET",
			url : "search",
			data : data,
			dataType : "json"
		}).pipe(function(val) {
			return val.result;
		});
	}
	
	SearchDaoHandler.prototype.getAdvancedMenu = function(){

		return $.ajax({
			type : "GET",
			url : "getTopCompaniesAndEducations",
			dataType : "json"
		}).pipe(function(val) {
			return val.result;
		});
	}
	
	//-------- /Search Dao handler ---------//
	
	
	app.SearchDaoHandler = new SearchDaoHandler();
})(jQuery);