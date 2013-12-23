var app = app || {};
(function($){
	//-------- Search Dao handler ---------//
	
	function SearchDaoHandler(){
	}
	
	/**
	 * do search contacts with params
	 * @Param qPrams, Object, which contains keyword, name and so on
	 * @Param mode, String, the search mode, like 'simple', 'keyword', 'advanced'
	 * @Param pageIdx, Int, the page index
	 * @Param pageSize, Int, the page size
	 */ 
	SearchDaoHandler.prototype.search = function(qParams){
//		var data = $.extend({},qParams);
//		data.searchMode = mode;
//		data.pageIdx = pageIdx;
//		data.pageSize = pageSize;
//        data.searchColumns = searchColumns;
		
		return app.getJsonData("search", qParams).pipe(function(result) {
			return result;
		});
	}
	
	/**
	 * do search get group values
	 * @Param qPrams, Object, which contains keyword, name and so on
	 */ 
	SearchDaoHandler.prototype.getAutoCompleteData = function(qParams,limitData){
		var data = $.extend({},qParams,limitData);

        return app.getJsonData("getAutoCompleteData", data).pipe(function(val) {
			return val;
		});
	}
	
	//-------- /Search Dao handler ---------//
	
	
	app.SearchDaoHandler = new SearchDaoHandler();
})(jQuery);