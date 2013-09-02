(function($){
  
  var searchDao = app.SearchDaoHandler;
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
        var $btn = $(event.target);
        var values = {};
        values["name"]=view.$el.find("[name='name']").val();
        values["shemaname"]=view.$el.find("[name='shemaname']").val();
        values["sfid"]=view.$el.find("[name='sfid']").val();
        values["isEdit"]=view.$el.find("[name='isEdit']").val();
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
      },
      "btap;.cancel":function(event){
        var view = this;
        refreshEntityTable.call(view);
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
      console.log(data);
      if(data.success){
        var html = render("Organization-content",{data:data.result[0]});
        view.$tabContent.bEmpty();
        view.$tabContent.html(html);
        view.$tabContent.find("input[name='isEdit']").val("true");
      }
    });
  }
})(jQuery);