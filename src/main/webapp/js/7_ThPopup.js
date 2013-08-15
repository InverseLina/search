var app = app || {};
(function($) {
  var searchDao = app.SearchDaoHandler;
  function ThPopup(type) {
    this.type = type;
  };

  ThPopup.prototype.create = function(data, config) {
    var dfd = $.Deferred();
    var view = this;
    var type = this.type;
    if(type=="company"||type=="education"||type=="skill"||type=="location"){
	    searchDao.getAdvancedMenu({limit : 5, type : type}).always(function(result) {
	        var $e = $(render(view.name,result));
	        console.log(result);
	        var $html = $(render("filterPanel",data));
	        $html.find(".popover-content").html($e);
	        dfd.resolve($html);
	    }); 
    }else{
    	var $e = $(render(view.name));
        var $html = $(render("filterPanel",data));
        $html.find(".popover-content").html($e);
        dfd.resolve($html);
    }
    return dfd.promise();
  }
  
  ThPopup.prototype.postDisplay=function(data){
	  var view = this;
      $(document).on("btap."+view.cid, function(event){
          var width = view.$el.find(".popover").width();
          var height = view.$el.find(".popover").height();
          var pos = view.$el.find(".popover").offset();
          if(event.pageX > pos.left && event.pageX < pos.left + width
              && event.pageY > pos.top && event.pageY < pos.top + height){
          }else{
              view.$el.bRemove();
              $(document).off("btap."+view.cid);
          }
      });
  }
  
  app.ThPopup = ThPopup;
})(jQuery); 