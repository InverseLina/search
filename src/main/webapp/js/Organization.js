(function($){
	
  brite.registerView("Organization",{parent:".admincontainer",emptyParent:true},{
    create: function(){
      return render("Organization");
    },
    postDisplay:function(data){
      var view = this;
      
      view.section = app.pathInfo.paths[0] || "organization";
		
	  view.$navTabs = view.$el.find(".nav-tabs");
	  view.$tabContent = view.$el.find(".tab-content");
	  view.$navTabs.find("li.active").removeClass("active");
      
	  if(app.pathInfo.paths[1] == "add" || app.pathInfo.paths[1] == "edit"){
		   brite.display("OrganizationInfo");
		  }else{
		   view.$navTabs.find("a[href='#organization']").closest("li").addClass("active");  
		   refreshEntityTable.call(view);
		  }
    },
    
    events:{
      "btap;.cancel":function(event){
    	  window.location.href="/admin#organization";
      },
      
      "btap;.home":function(event){
    	  window.location.href="/";
        },
      
      "btap;.add":function(event){
        var view = this;
        var html = render("Organization-content",{data:null});
        view.$tabContent.html(html);
        window.location.href="admin#organization/add";
      },
      "click; .del": function(event){
        var view = this;
        var entityInfo = $(event.target);
        doDelete.call(view,entityInfo.attr("data-entity"));
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