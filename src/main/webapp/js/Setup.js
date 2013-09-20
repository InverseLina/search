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
      "btap;.cancel":function(event){
        window.location.href="/admin";
      },
      
      "btap;.home":function(event){
        window.location.href="/";
        }
    }
  });
  
  
  function refreshEntityTable(){
    var view = this;
    app.getJsonData("/org/list").done(function(data){
        var html = render("Organization-list",{list:data});
        view.$tabContent.bEmpty();
        view.$tabContent.html(html);
    });
  }
  
  function doDelete(id){
    var view = this;
      app.getJsonData("/org/del/",{id:id},"Post").done(function(data){
      refreshEntityTable.call(view);
    });
  }
  
})(jQuery);