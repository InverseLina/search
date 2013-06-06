var app = app || {};
(function($){
   app.preference={
      // save key with value
      store:function(key,value) {
        document.cookie = key + "=" + value;
      },
      // get value by key
      get:function(key,defaultVal) {
        var cookie = document.cookie;
        var startIndex = cookie.indexOf(key + "=");
        if (startIndex == -1) {
          return defaultVal ? defaultVal : null;
        } else {
          var endIndex = cookie.indexOf(";", startIndex)
          if (endIndex == -1) {
            return cookie.substring(startIndex + key.length + 1);
          } else {
            return cookie.substring(startIndex + key.length + 1, endIndex);
          }
        }
      }

   }
})(jQuery);