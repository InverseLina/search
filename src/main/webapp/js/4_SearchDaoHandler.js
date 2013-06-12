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
	SearchDaoHandler.prototype.search = function(qParams,mode,pageIdx,pageSize, searchColumns){
		var data = $.extend({},qParams);
		data.searchMode = mode;
		data.pageIdx = pageIdx;
		data.pageSize = pageSize;
        data.searchColumns = searchColumns;
		
		return $.ajax({
			type : "GET",
			url : "search",
			data : data,
			dataType : "json"
		}).pipe(function(val) {
			return val.result;
		});
	}
	
	/**
	 * do search get group values
	 * @Param qPrams, Object, which contains keyword, name and so on
	 */ 
	SearchDaoHandler.prototype.getGroupValuesForAdvanced = function(qParams,limitData){
		var data = $.extend({},qParams,limitData);
		
		return $.ajax({
			type : "GET",
			url : "getGroupValuesForAdvanced",
			data : data,
			dataType : "json"
		}).pipe(function(val) {
			return val.result;
		});
	}
	
	/**
   * get menu data
   * @Param opts, Object, which contains type, offset,limit
   */ 
	SearchDaoHandler.prototype.getAdvancedMenu = function(opts){
		opts = opts||{};
		return $.ajax({
			type : "GET",
			url : "getTopCompaniesAndEducations",
			data:opts,
			dataType : "json"
		}).pipe(function(val) {
			return val.result;
		});
	}
	
	//-------- /Search Dao handler ---------//
	
	
	app.SearchDaoHandler = new SearchDaoHandler();
})(jQuery);