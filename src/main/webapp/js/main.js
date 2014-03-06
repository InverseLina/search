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
        var reg, result = this;
        if (arguments.length == 1 && typeof (args) == "object") {
            for (var key in args) {
                reg = new RegExp("({" + key + "})", "g");
                result = result.replace(reg, args[key]);
            }
        } else {
            for (var i = 0; i < arguments.length; i++) {
                if (arguments[i] == undefined) {
                    return "";
                } else {
                    reg = new RegExp("({[" + i + "]})", "g");
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

(function(){
    var fields = ["firstName","lastName","email","title","objectType","status"];
    app.getContactDisplayName = function(contact){
        contact = contact||{};
        var displayName = "";
        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            displayName +=" " + (contact[field]||"");
        }
        displayName = $.trim(displayName.replace(/\s+/, " "));
        return displayName;
    }


    /**
     * A method about use ajax to get json data
     */
    var defaultOption = {dataType:"json", async: true,type:"Get"};
    var defaultError = {errorCode: "ERROR",
        errorMessage: "Unknown error."
    }

    app.getJsonData = function(url, params, options) {
        var dfd =  $.Deferred();
        params = params || {};
        if($.isPlainObject(options)){
            options = $.extend({}, defaultOption, options||{});
        }else if($.isFunction(options)){
            options = $.extend({}, defaultOption, {fail:options});
        }else{
            options = $.extend({}, defaultOption, {type:options||"Get"});
        }
		if(url != null && url.indexOf("/")  == -1 ){
        	url = "/" + url;
        }
        jQuery.ajax({
            type : options.type || "Get",
            url : contextPath+url,
            async : options.async,
            data : params,
            dataType : options.dataType
        }).success(function(data) {
                //console.log(data);
                if(data.success === true){
                    dfd.resolve(data.result);
                }else{
                    data = $.extend({}, defaultError, data||{})
                    if(options.fail && $.isFunction(options.fail)){
                        dfd.fail(options.fail);
                        dfd.reject(data);
                    }else{
                        dfd.fail(function (data) {
                            $(document).trigger("ERROR_PROCESS", data);
                        });
                        dfd.reject(data);

                    }

                }

            }).fail(function(jxhr, arg2) {
                try {
                    if (jxhr.responseText) {
                        console.log(" WARNING: json not well formatted, falling back to JS eval");
                        var data = eval("(" + jxhr.responseText + ")");
                        dfd.resolve(data);
                    } else {
                        throw " EXCEPTION: Cannot get content for " + url;
                    }
                } catch (ex) {
                    console.log(" ERROR: " + ex + " Fail parsing JSON for url: " + url + "\nContent received:\n" + jxhr.responseText);
                }
            });

        return dfd.promise();
    };

    app.in_array=function(element,array){
    	for(var e in array){
    		if(element==array[e]){
    			return true;
    		}
    	}
    	return false;
    };
    function buildPathInfo(){
        var pathInfo = {};
        var hash = window.location.hash;
        if (hash){
            hash = hash.substring(1);
            if (hash){
                var pathAndParam = hash.split("!");
                pathInfo.paths = pathAndParam[0].split("/");
                if(pathInfo.paths && pathInfo.paths.length ==3 && pathInfo.paths[1] == "list"){
                    pathInfo.labelAssigned = true;
                }
                // TODO: need to add the params
            }
        }
        app.pathInfo = pathInfo;
        return pathInfo;
    }

    app.buildPathInfo = buildPathInfo;

})();



