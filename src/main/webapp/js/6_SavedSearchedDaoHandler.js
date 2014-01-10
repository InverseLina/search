var app = app || {};
(function($){
	//-------- Search Dao handler ---------//
	
	function SavedSearchesDaoHandler(){
	}

    SavedSearchesDaoHandler.prototype.list = function(limitData){
        return app.getJsonData("listSavedSearches");
	}


    SavedSearchesDaoHandler.prototype.save = function(name, content){
        return app.getJsonData("saveSavedSearches",{name:name, content: content},"Post" );
	}

    SavedSearchesDaoHandler.prototype["del"] = function(id){
        return app.getJsonData("deleteSavedSearches",{id:id},"Post");
	}

    SavedSearchesDaoHandler.prototype["count"] = function(name){
        return app.getJsonData("countSavedSearches",{name:name} );
	}
    SavedSearchesDaoHandler.prototype["get"] = function(id){
        return app.getJsonData("getOneSavedSearches",{id:id} );
	}
	
	//-------- /Search Dao handler ---------//
	
	
	app.SavedSearchesDaoHandler = new SavedSearchesDaoHandler();
})(jQuery);