var app = app || {};
(function($){
	//-------- Search Dao handler ---------//
	
	function PerfDaoHandler(){
	}
	
	/**
	 * do search contacts with params
	 * @Param qPrams, Object, which contains keyword, name and so on
	 * @Param mode, String, the search mode, like 'simple', 'keyword', 'advanced'
	 * @Param pageIdx, Int, the page index
	 * @Param pageSize, Int, the page size
	 */ 
	PerfDaoHandler.prototype.perfSearch = function(qParams){
//		var data = $.extend({},qParams);
//		data.searchMode = mode;
//		data.pageIdx = pageIdx;
//		data.pageSize = pageSize;
//        data.searchColumns = searchColumns;
		
		return app.getJsonData("perfSearch", qParams).pipe(function(result) {
			return result;
		});
	}
	
	/**
	 * do search get group values
	 * @Param qPrams, Object, which contains keyword, name and so on
	 */ 
	PerfDaoHandler.prototype.getPerfGroupValuesForAdvanced = function(qParams,limitData){
		var data = $.extend({},qParams,limitData);

        return app.getJsonData("getPerfGroupValuesForAdvanced", data).pipe(function(val) {
			return val;
		});
	}
	
	/**
   * get menu data
   * @Param opts, Object, which contains type, offset,limit
   */ 
	PerfDaoHandler.prototype.getPerfAutoCompleteData = function(opts){
		opts = opts||{};
        return app.getJsonData("getPerfAutoCompleteData", opts).pipe(function(val) {
			return val;
		});
	}
	
	//-------- /Search Dao handler ---------//
	
	
	app.PerfDaoHandler = new PerfDaoHandler();
})(jQuery);