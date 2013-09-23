(function($){
	
	var defaultPathInfo = {paths:["setup"]};
	
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
        if(viewName == "organization"){
          brite.display("Organization");
        }else{
          brite.display("Setup");
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