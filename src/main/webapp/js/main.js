(function(w){  
  w.render = function(templateName,data){
    var tmpl = Handlebars.templates[templateName];
    if (tmpl){
      return $.trim(tmpl(data));
    }else{
      // obviously, handle this case as you think most appropriate.
      return "<small>Error: could not find template: " + templateName + "</small>";
    }
  };
    w.hasTemplate = function(templateName){
        var tmpl = Handlebars.templates[templateName];
        if (tmpl){
            return true;
        }else{
            return false;
        }
    }

})(window);
 
var app = app||{};
app.defaultMenuSize = 5;



