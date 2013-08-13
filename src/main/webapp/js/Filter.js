(function ($) {
    brite.registerView("Filter", {emptyParent: false},{
    	   create:function(data,config){
    		   return render("Filter",data);
    	   }
    });
})(jQuery);