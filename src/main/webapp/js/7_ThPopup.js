var app = app || {};
(function($) {
  function ThPopup(type) {
    this.type = type;
  };

  ThPopup.prototype.create = function(data, config) {
    var dfd = $.Deferred();
    var view = this;
    var type = this.type;
    var $e = $(render(view.name));
    var $html = $(render("filterPanel",data));
    $html.find(".popover-content").html($e);
    dfd.resolve($html);
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