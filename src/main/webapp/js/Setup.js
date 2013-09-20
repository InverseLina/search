(function($){
  
  brite.registerView("Setup",{parent:".admincontainer",emptyParent:true},{
    create: function(){
      return render("Setup");
    },
    postDisplay:function(data){
      var view = this;
      view.section = app.pathInfo.paths[0] || "setup";
    
    view.$navTabs = view.$el.find(".nav-tabs");
    view.$tabContent = view.$el.find(".tab-content");
    view.$navTabs.find("li.active").removeClass("active");
    view.$navTabs.find("a[href='#setup']").closest("li").addClass("active");  
    },
    
    events:{
      "btap;.home":function(event){
        window.location.href="/";
        }
    }
  });
  
})(jQuery);