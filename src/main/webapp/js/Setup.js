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
      view.$el.trigger("STATUS_CHANGE");
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
    		  $(".organization-tab").removeClass("hide");
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
      },
      "STATUS_CHANGE":function(event){
    	  var view = this;
    	  app.getJsonData("/checkSetupStatus",{type:"SYSTEM"},{type:"Get"}).done(function(result){
        	  switch(result){
        	  case 0:	view.$el.find(".create").prop("disabled",false).html("Create");
        	  			view.$el.find(".import").prop("disabled",false).html("Importe");
        	  			break;
        	  case 1:	view.$el.find(".create").prop("disabled",true).html("Created");
        	  			view.$el.find(".import").prop("disabled",false).html("Import");
    					break;
        	  case 2:	view.$el.find(".create").prop("disabled",true).html("Created");
    		  			view.$el.find(".import").prop("disabled",true).html("Imported");
    					break;
        	  }
          });
      }
    }
  });
  
})(jQuery);