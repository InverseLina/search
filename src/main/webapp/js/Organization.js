(function($){
  
  brite.registerView("Organization",{parent:".container",emptyParent:true},{
    create: function(){
      return render("Organization");
    },
    postDisplay:function(data){
      var view = this;
      view.$tabContent = view.$el.find(".tab-content");
      refreshEntityTable.call(view);
    },
    events:{
      "btap;.save":function(event){
        var view = this;
        var values = {};
        values["name"]=view.$el.find("[name='name']").val();
        values["oldname"]=view.$el.find("[name='oldname']").val();
        values["schemaname"]=view.$el.find("[name='schemaname']").val();
        values["sfid"]=view.$el.find("[name='sfid']").val();
        doValidate.call(view);
        if(view.validation){
        	 $.ajax({
                 url:"/sys/save",
                 type:"Post",
                 dataType:'json',
                 data:values
               }).done(function(data){
                 if(data.success){
                   refreshEntityTable.call(view);
                 }
               });
        }
      },
      "btap;.cancel":function(event){
        var view = this;
        refreshEntityTable.call(view);
      },
      
      "btap;.home":function(event){
    	  window.location.href="/";
        },
      
      "btap;.add":function(event){
        var view = this;
        var html = render("Organization-content",{data:null});
        view.$tabContent.html(html);
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
    $.ajax({
      url:"/sys/list",
      type:"Get",
      dataType:'json'
    }).done(function(data){
      if(data.success){
        var html = render("Organization-list",{list:data.result});
        view.$tabContent.bEmpty();
        view.$tabContent.html(html);
      }
    });
  }
  
  function doDelete(name){
    var view = this;
    $.ajax({
      url:"/sys/del/",
      type:"Get",
      data:{name:name},
      dataType:'json'
    }).done(function(data){
      if(data.success){
        refreshEntityTable.call(view);
      }
    });
  }
  
  function getDate(name){
    var view = this;
    $.ajax({
      url:"/sys/get/",
      type:"Get",
      data:{name:name},
      dataType:'json',
    }).done(function(data){
      if(data.success){
        var html = render("Organization-content",{data:data.result[0]});
        view.$tabContent.bEmpty();
        view.$tabContent.html(html);
      }
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