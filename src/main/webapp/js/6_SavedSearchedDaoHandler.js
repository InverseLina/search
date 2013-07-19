var app = app || {};
(function($){
	//-------- Search Dao handler ---------//
	
	function SavedSearchesDaoHandler(){
	}

    SavedSearchesDaoHandler.prototype.list = function(limitData){
		return $.ajax({
			type : "GET",
			url : "getSavedSearches",
			dataType : "json"
		}).pipe(function(val) {
			return val.result;
		});
	}


    SavedSearchesDaoHandler.prototype.update = function(id, content){
		return $.ajax({
			type : "POST",
			url : "updateSavedSearches",
			data : {id:id, content: content},
			dataType : "json"
		}).pipe(function(val) {
			return val.result;
		});
	}

    SavedSearchesDaoHandler.prototype.save = function(name, content){
		return $.ajax({
			type : "POST",
			url : "saveSavedSearches",
			data : {name:name, content: content},
			dataType : "json"
		}).pipe(function(val) {
			return val.result;
		});
	}


    SavedSearchesDaoHandler.prototype.delete = function(id){
		return $.ajax({
			type : "POST",
			url : "deleteSavedSearches",
			data:{id:id},
			dataType : "json"
		}).pipe(function(val) {
			return val.result;
		});
	}
	
	//-------- /Search Dao handler ---------//
	
	
	app.SavedSearchesDaoHandler = new SavedSearchesDaoHandler();
})(jQuery);