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
    },
    
    events:{
      "btap;.home":function(event){
        window.location.href="/";
        },
      "click;.save":function(event){
    	  alert("c");
    	  var view = this;
    	  var $createBtn = $(event.target);
    	  app.getJsonData("/createSysSchema",{},{type:"Post"}).done(function(){
    		  $createBtn.prop("disabled",true).html("Created");
    	  });
      }
    }
  });
  
})(jQuery);