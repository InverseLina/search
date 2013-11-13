(function($){
	
  brite.registerView("Organization",{parent:".admincontainer",emptyParent:true},{

    // --------- View Interface Implement--------- //
    create: function(){
      return render("Organization");
    },
    postDisplay:function(data){
      var view = this;

      view.section = app.pathInfo.paths[0] || "organization";

      view.$navTabs = $(".nav-tabs");
	  view.$tabContent = view.$el.find(".tab-content");
	  view.$navTabs.find("li.active").removeClass("active");
	  if(view.$navTabs.find('li').size() > 3){
			view.$navTabs.find('li:last').prev("li").remove();
		  }

	  if(app.pathInfo.paths[1] == "add" || app.pathInfo.paths[1] == "edit"){
		   brite.display("OrganizationInfo");
		  }else{
		   view.$navTabs.find("a[href='#organization']").closest("li").addClass("active");
		   refreshEntityTable.call(view);
		  }
    },
    // --------- /View Interface Implement--------- //
    
   // --------- Events--------- //
    events:{
      "btap;.home":function(event){
    	  window.location.href=contextPath + "/";
        },

      "btap;.add":function(event){
        var view = this;
        var html = render("Organization-content",{data:null});
        view.$tabContent.html(html);
        window.location.href=contextPath + "/admin#organization/add";
      },
      "click; .del": function(event){
        var view = this;
        var entityInfo = $(event.target);
        doDelete.call(view,entityInfo.attr("data-entity"));
      }
    }
   // --------- /Events--------- //
  });
  
  
// --------- Private Methods--------- //
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
// --------- /Private Methods--------- //
  
})(jQuery);