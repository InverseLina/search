var app = app || {};
(function($){
    function getCookie(key, defaultVal){
        var cookie = document.cookie;
        var startIndex = cookie.indexOf(key+"=");
        if(startIndex==-1){
            return defaultVal?defaultVal:null;
        }else{
            var endIndex = cookie.indexOf(";",startIndex)
            if(endIndex==-1){
                return cookie.substring(startIndex+key.length+1);
            }else{
                return cookie.substring(startIndex+key.length+1,endIndex);
            }
        }
    }

    var displayColumns = [{name:"id", display:"ID"},{name:"name", display:"Name"},{name:"createdate",display:"Create Date"}
        ,{name:"title",display:"Title"}, {name:"company",display:"Company"}
        ,{name:"skill",display:"Skill"},{name:"education",display:"Education"}];
    var defaultColumns = "id,name,createdate,title";
    var defaultSectionOpen = {
        ContactInfo: 'close',
        Company: 'open',
        Education: 'close',
        Skill: 'close'
    };

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
                columns = getCookie("columns", defaultColumns);
                return columns.split(",");
            }else{
                if(arguments[0] && $.type(arguments[0]) == "array" ){
                    columns = arguments[0];
                    if(columns.length > 0) {
                        document.cookie = "columns=" + columns.join(",") + "; expires=0";
                    }
                }
            }

        },
        defaultSectionOpen: defaultSectionOpen,
        displayColumns: displayColumns
    }
})(jQuery);