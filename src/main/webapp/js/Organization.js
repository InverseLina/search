(function($){
  
  brite.registerView("Organization",{parent:".container",emptyParent:true},{
    create: function(){
      return render("Organization");
    },
    postDisplay:function(data){
      var view = this;
      view.$tabContent = view.$el.find(".tab-content");
      if(app.pathInfo.paths[1] == "add"){
    	  var html = render("Organization-content",{data:null});
    	   view.$tabContent.html(html);
       }else if(app.pathInfo.paths[1] == "edit"){
    	   getDate.call(view,app.pathInfo.paths[2] * 1); 
       }else{
    	   if(window.location.href.indexOf("admin") > -1){
    		   window.location.href="/#org";
    		}
    	  refreshEntityTable.call(view);
       }
    },
    
    events:{
      "btap;.save":function(event){
        var view = this;
        var values = {};
        values["name"]=view.$el.find("[name='name']").val();
        values["id"]=view.$el.find("[name='id']").val();
        values["schemaname"]=view.$el.find("[name='schemaname']").val();
        values["sfid"]=view.$el.find("[name='sfid']").val();
        doValidate.call(view);
        if(view.validation){
            app.getJsonData("/org/save", values,"Post").done(function(data){
            	window.location.href="/#org";
          });
        }
      },
      "btap;.cancel":function(event){
        window.location.href="/#org";
      },
      
      "btap;.home":function(event){
    	  window.location.href="/";
        },
      
      "btap;.add":function(event){
        var view = this;
        var html = render("Organization-content",{data:null});
        view.$tabContent.html(html);
        window.location.href="/#org/add";
      },
      "click; .del": function(event){
        var view = this;
        var entityInfo = $(event.target);
        doDelete.call(view,entityInfo.attr("data-entity"));
      },
      "click;.edit":function(event){
        var view = this;
        var entityInfo = $(event.target);
        getDate.call(view,entityInfo.attr("data-entity"));
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
  
  function getDate(id){
    var view = this;
    app.getJsonData("/org/get/", {id:id}).done(function(data){
        var html = render("Organization-content",{data:data[0]});
        view.$tabContent.bEmpty();
        view.$tabContent.html(html);
    });
  }
  
  function doValidate(){
	    var view = this;
		var $nameMsg = view.$el.find(".alert-error.name");
		var $schemanameMsg = view.$el.find(".alert-error.schemaname");
		
		if(view.$el.find("[name='name']").val() == ''){
			$nameMsg.removeClass("hide");
		}else{
			$nameMsg.addClass("hide");
		}
		
		if(view.$el.find("[name='schemaname']").val() == ''){
			$schemanameMsg.removeClass("hide");
		}else{
			$schemanameMsg.addClass("hide");
		}
		
		if(view.$el.find(".alert-error:not(.hide)").length>0){
			view.validation=false;
		}else{
			view.validation=true;
		}
	  }
  
 
})(jQuery);