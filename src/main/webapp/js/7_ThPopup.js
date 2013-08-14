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
  
  app.ThPopup = ThPopup;
})(jQuery); 