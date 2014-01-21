var app = app || {};
(function($){
    var filterOrders;
    function getCookie(key, defaultVal){
        var val = app.cookie(key);
        return val?val:defaultVal;
    }

/*    var displayColumns = [{name:"contact",display:"Contact"},{name:"company",display:"Employer"}
        ,{name:"skill",display:"Skill"},{name:"education",display:"Education"},{name:"location",display:"Location"},
        {name:"resume",display:"Resume"}];*/
//    var defaultColumns = "contact,company,skill,education";
    var defaultColumns = function(){
        var filters = app.getSearchUiConfig();
        var displays = $.grep(filters, function(item, idx){
            return item.show;
        })
        return $.map(displays, function(item){
            return item.type;
        }).join(",")
    }

    app.getFilterOrders = function (update) {
        if (update) {
            filterOrders = update;
            app.getJsonData("perf/get-user-pref", {}, {async: false}).done(function(result){
                if(result){
                    filterOrders = JSON.parse(result['val_text']);
                }else{
                    var filters = app.getSearchUiConfig();
                    var displays = $.grep(filters, function(item, idx){
                        return item.show;
                    });
                    filterOrders = $.map(displays, function(item){
                        return item.type;
                    });
                }

            });
        }
        if (filterOrders) {
            return filterOrders;
        } else {
            app.getJsonData("perf/get-user-pref", {}, {async: false}).done(function(result){
                if(result){
                    filterOrders = JSON.parse(result['val_text']);
                }else{
                    var filters = app.getSearchUiConfig();
                    var displays = $.grep(filters, function(item, idx){
                        return item.show;
                    });
                    filterOrders = $.map(displays, function(item){
                        return item.type;
                    });
                }

            });
            return filterOrders;
        }
    }
    app.getSearchFilter = function(name){
        return cache[name];
    }

    app.preference={
        store:function(key,value){
            document.cookie=key+"="+value + "; expires=0";
        },
        get:function(key,defaultVal){
            return getCookie(key, defaultVal);
        },
        columns:function(){
            var columns;
            if(arguments.length == 0){
                columns = getCookie("columns", defaultColumns());
                return columns.split(",");
            }else{
                if(arguments[0] && $.type(arguments[0]) == "array" ){
                    columns = arguments[0];
//                    console.log(columns);
                    if(columns.length > 0) {
//                        document.cookie = "columns=" + columns.join(",") + ";expires=0;path=/;domain=localhost";
                        app.cookie("columns", columns.join(","));
                    }
                }
            }

        },
        displayColumns: function(){
            return app.getSearchUiConfig();
        }
    }

    app.cookie = function(name, value, options) {
        if (typeof value != 'undefined') {
            options = options || {};
            if (value === null) {
                value = '';
                options = $.extend({}, options);
                options.expires = -1;
            }
            var expires = '';
            if (options.expires && (typeof options.expires == 'number' || options.expires.toUTCString)) {
                var date;
                if (typeof options.expires == 'number') {
                    date = new Date();
                    date.setTime(date.getTime() + (options.expires * 24 * 60 * 60 * 1000));
                } else {
                    date = options.expires;
                }
                expires = '; expires=' + date.toUTCString();
            }
            var path = options.path ? '; path=' + (options.path) : '';
            var domain = options.domain ? '; domain=' + (options.domain) : '';
            var secure = options.secure ? '; secure' : '';
            document.cookie = [name, '=', encodeURIComponent(value), expires, path, domain, secure].join('');
        } else {
            var cookieValue = null;
            if (document.cookie && document.cookie != '') {
                var cookies = document.cookie.split(';');
                for (var i = 0; i < cookies.length; i++) {
                    var cookie = jQuery.trim(cookies[i]);
                    if (cookie.substring(0, name.length + 1) == (name + '=')) {
                        cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                        break;
                    }
                }
            }
            return cookieValue;
        }
    };
})(jQuery);