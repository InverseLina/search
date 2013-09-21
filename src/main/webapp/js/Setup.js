(function($){
  
  brite.registerView("Setup",{parent:".admincontainer",emptyParent:true},{
    create: function(){
      return render("Setup");
    },
    postDisplay:function(data){
      var view = this;
      view.section = app.pathInfo.paths[0] || "setup";
    
      view.$navTabs = $(".nav-tabs");
      view.$tabContent = view.$el.find(".tab-content");
      view.$navTabs.find("li.active").removeClass("active");
      if(view.$navTabs.find('li').size() > 2){
		view.$navTabs.find('li:last').remove();
	  } 
      view.$navTabs.find("a[href='#setup']").closest("li").addClass("active"); 
      view.$el.find(".create,.import").prop("disabled",true).html("Loading...");
      app.getJsonData("/checkSetupStatus",{types:"SYS_CREATE_SCHEMA"},{type:"Get"}).done(function(data){
    	 if(app.in_array("SYS_CREATE_SCHEMA",data)){
    	    view.$el.find(".create").prop("disabled",true).html("Created");
    	    app.getJsonData("/checkSetupStatus",{types:"SYS_IMPORT_ZIPCODE_DATA"},{type:"Get"}).done(function(d){
    	    	if(app.in_array("SYS_IMPORT_ZIPCODE_DATA",d)){
    	     	    view.$el.find(".import").prop("disabled",true).html("Imported");
    	     	 }
    	    });
    	 }else{
    		 view.$el.find(".create").prop("disabled",false).html("Create");
    		 view.$el.find(".import").prop("disabled",true).html("Import");
    	 }
    	 
	  });
    },
    
    events:{
      "btap;.home":function(event){
        window.location.href="/";
        },
      "click;.create":function(event){
    	  var view = this;
    	  var $createBtn = $(event.target);
    	  if($createBtn.prop("disabled")){
    		  return false;
    	  }
    	  $createBtn.prop("disabled",true).html("Creating...");
    	  app.getJsonData("/createSysSchema",{},{type:"Post"}).done(function(){
    		  $createBtn.html("Created");
    		  view.$el.find(".import").prop("disabled",false);
    	  });
      },
      "click;.import":function(event){
    	  var view = this;
    	  var $importBtn = $(event.target);
    	  if($importBtn.prop("disabled")){
    		  return false;
    	  }
    	  $importBtn.prop("disabled",true).html("Importing...");
    	  app.getJsonData("/updateZipCode",{},{type:"Post"}).done(function(){
    		  $importBtn.html("Imported");
    	  });
      }
    }
  });
  
})(jQuery);