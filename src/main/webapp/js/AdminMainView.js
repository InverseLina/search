(function($){
	
	var defaultPathInfo = {paths:["organization"]};
	
	brite.registerView("AdminMainView",{parent:".container",emptyParent:true},{
		create: function(data){
			return render("AdminMainView");
	 }, 
	 
	 postDisplay: function(data){
		 $(".home").removeClass("hide");
		 $(".config").addClass("hide");
		 this.$el.trigger("PATH_INFO_CHANGE",buildPathInfo());
	 },
	 winEvents: {
	    hashchange: function(event){
	     this.$el.trigger("PATH_INFO_CHANGE",buildPathInfo());
	    }
	 },
	 events: {
		"PATH_INFO_CHANGE": function(event,pathInfo){
	      changeView.call(this,pathInfo);
	    }
	 }
	});
    // --------- Private Methods --------- //
    function changeView(pathInfo){
      pathInfo = pathInfo || defaultPathInfo;
      var viewName = pathInfo.paths[0];
        if(viewName == "administration"){
          brite.display("AdminHome");
        }else{
          brite.display("Organization");
        }
        // change the nav selection
    }
    
    // --------- /Private Methods --------- //  
    
    
    // --------- Utilities --------- //
    function buildPathInfo(){
      var pathInfo = $.extend({},defaultPathInfo);
      var hash = window.location.hash;
      if (hash){
        hash = hash.substring(1);
        if (hash){
          var pathAndParam = hash.split("!");
          pathInfo.paths = pathAndParam[0].split("/");
          // TODO: need to add the params
        }
      }
      app.pathInfo = pathInfo;
      return pathInfo;    
    }
	
})(jQuery);