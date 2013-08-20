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


/**
 * string format function
 */
String.prototype.format = function(args) {
    if (arguments.length > 0) {
        var result = this;
        if (arguments.length == 1 && typeof (args) == "object") {
            for (var key in args) {
                var reg = new RegExp("({" + key + "})", "g");
                result = result.replace(reg, args[key]);
            }
        } else {
            for (var i = 0; i < arguments.length; i++) {
                if (arguments[i] == undefined) {
                    return "";
                } else {
                    var reg = new RegExp("({[" + i + "]})", "g");
                    result = result.replace(reg, arguments[i]);
                }
            }
        }
        return result;
    } else {
        return templat;
    }
};
 
var app = app||{};
app.defaultMenuSize = 5;



