(function($){
	
	var defaultPathInfo = {paths:["setup"]};
	
	brite.registerView("AdminMainView",{parent:".container",emptyParent:true},{

    // --------- View Interface Implement--------- //
	create: function(data){
			return render("AdminMainView");
	 },
	 

	 postDisplay: function(data){
		 var view = this;
		 $(".home").removeClass("hide");
		 $(".config").addClass("hide");
		 view.$el.find(".organization-tab").addClass("hide");
		 app.getJsonData("/checkSysSchema",{},{type:"Get"}).done(function(result){
			 if(result.schema_create){
				 view.$el.find(".organization-tab").removeClass("hide");
			 }
	      });

		 this.$el.trigger("PATH_INFO_CHANGE",buildPathInfo());
	 },
    // --------- /View Interface Implement--------- //

    // --------- Windows Event--------- //
	 winEvents: {
	    hashchange: function(event){
	     this.$el.trigger("PATH_INFO_CHANGE",buildPathInfo());
	    }
	 },
    // --------- /Windows Event--------- //

    // --------- Events--------- //
	 events: {
		"PATH_INFO_CHANGE": function(event,pathInfo){
	      changeView.call(this,pathInfo);
	    }
	 }
    // --------- /Events--------- //

	});
    // --------- Private Methods --------- //
    function changeView(pathInfo){
      var view = this;
       view.$el.find(".nav-tabs li.OrganizationInfo").remove()
      pathInfo = pathInfo || defaultPathInfo;
      var viewName = pathInfo.paths[0];
        console.log(viewName)
        if(viewName == "organization"){
          brite.display("Organization");
        }else if(viewName == "perf"){
        	brite.display("PerfView");
        }else if(viewName == "trigger-test"){
        	brite.display("TriggerTestView");
        }else if(viewName == "search-config"){
            brite.display("AdminSearchConfig");
        }else{
          brite.display("Setup");
        }
        // change the nav selection
        console.log(pathInfo)
        if(pathInfo.paths.length == 1) {
            view.$el.find(".nav-tabs li.active").removeClass("active");
            console.log(view.$el.find(".nav-tabs li." + viewName))
            view.$el.find(".nav-tabs li." + viewName).addClass("active");
        }
    }
    
    // --------- /Private Methods --------- //  
    
    
    // --------- Utilities--------- //
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
    // --------- /Utilities--------- //
	
})(jQuery);