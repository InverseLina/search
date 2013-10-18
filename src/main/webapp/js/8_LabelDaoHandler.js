var app = app || {};
(function($){
	//-------- Search Dao handler ---------//
    function LabelDaoHandler(){
    }


    LabelDaoHandler.prototype.save = function(name){
        return app.getJsonData("addLabel",{name:name},"Post" );
    }


    LabelDaoHandler.prototype.delete = function(id){
        return app.getJsonData("deleteLabel",{id:id},"Post");
    }


    LabelDaoHandler.prototype.update = function(id, name){
        return app.getJsonData("updateLabel",{id:id, name:name},"Post" );
    }

    LabelDaoHandler.prototype.get = function(id){
        return app.getJsonData("getLabel",{id:id} );
    }

    LabelDaoHandler.prototype.list = function(){
        return app.getJsonData("getLabels");
	}

    LabelDaoHandler.prototype.assign = function(contactId, labelId){
        return app.getJsonData("assignLabelToContact",{contactId:contactId, labelId:labelId},"Post" );
    }
    LabelDaoHandler.prototype.unAssign = function(contactId, labelId){
        return app.getJsonData("unAssignLabelFromContact",{contactId:contactId, labelId:labelId},"Post" );
    }


	//-------- /Search Dao handler ---------//
	
	
	app.LabelDaoHandler = new LabelDaoHandler();
})(jQuery);