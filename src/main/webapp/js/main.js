(function(w){  
  w.render = function(templateName,data){
    var tmpl = Handlebars.templates[templateName];
    if (tmpl){
      return $.trim(tmpl(data));
    }else{
      // obviously, handle this case as you think most appropriate.
      return "<small>Error: could not find template: " + templateName + "</small>";
    }
  }
})(window);
 
var app = app||{};
app.defaultMenuSize = 5;


// --------- SideSectionContentMixins --------- //
var app = app || {};

(function($){
	app.sideSectionContentMixins = {
			
			refreshSelections: function(){
				 var view = this;
				 if (view.$el.find("li.selected").length === 0){
				 	 view.$el.find("li.all").addClass("selected").find(":checkbox").prop("checked",true);	
				 }
			}		
	}
})(jQuery);
// --------- /SideSectionContentMixins --------- //

