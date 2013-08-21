var app = app || {};
(function($) {
  var searchDao = app.SearchDaoHandler;
  function ThPopup(type) {
    this.type = type;
  };

  ThPopup.prototype.create = function(data, config) {
    var dfd = $.Deferred();
    var view = this;
    var type = this.type;
    if(type=="company"||type=="education"||type=="skill"||type=="location"){
	    searchDao.getAutoCompleteData({limit : 5, type : type}).always(function(result) {
	        var $e = $(render(view.name,result));
	        var $html = $(render("filterPanel",data));
	        $html.find(".popover-content").html($e);
	        dfd.resolve($html);
	    }); 
    }else{
    	var $e = $(render(view.name));
        var $html = $(render("filterPanel",data));
        $html.find(".popover-content").html($e);
        dfd.resolve($html);
    }
    return dfd.promise();
  }

    ThPopup.prototype.getValue = function () {
        var datasName = this.type.substring(0,1).toLocaleLowerCase() + this.type.substring(1) + "s";
        if(datasName=="companys"){
        	dataName="companies";
        }
        alert(dataName);
        var data = {};
        var values = data[datasName] = [], value;
        var view = this;
        view.$el.find(".item").each(function(){
            value = $(this).data("value");
            values.push(value);
        });
        return data;
    }

    ThPopup.prototype.postDisplay=function(data){
	  var html, displayName, view = this;
      $(document).on("btap."+view.cid, function(event){
          var width = view.$el.find(".popover").width();
          var height = view.$el.find(".popover").height();
          var pos = view.$el.find(".popover").offset();
          if(event.pageX > pos.left && event.pageX < pos.left + width
              && event.pageY > pos.top && event.pageY < pos.top + height){
          }else{
              view.$el.bRemove();
              $(document).off("btap."+view.cid);
          }
      });
      data = (data||{}).data||[];
        $.each(data, function(idx, val){
           if(view.type == "Contact"){
               displayName = (val.firstName||"")  + " " + (val.lastName||"")
           }else{
               displayName = val;
           }
           var html = render("filterPanel-selectedItem-add", {name:displayName});
            view.$el.find("span.add").before(html);

        });

     if(view.afterPostDisplay){
         view.afterPostDisplay();
     }
     if(view.$el.find(".sliderBarContainer").length > 0){
         brite.display("Slider", ".sliderBarContainer");
     }
  };

    ThPopup.prototype.events = {
        "btap; span.add": function (event) {
            var view = this;
            view.$el.find(".save").parent().removeClass("hide");
            var $span =$(event.target);
            var autoComplete = $span.attr("data-auto-complete");
            if(autoComplete){
            	$span.addClass("hide").next().removeClass("hide");
            }
//            view.$el.find("div.content").show();
//            if(view.$el.find(".sliderBarContainer").length > 0){
//                brite.display("Slider", ".sliderBarContainer");
//            }
        },
        "click;.glyphicon-remove":function(event){
        	var view = this;
        	var $icon = $(event.target);
        	$icon.parent().addClass("hide").prev().removeClass("hide");
        },
        "keyup;.autoComplete":function(event){
        	 var view = this;
        	 var $input = $(event.currentTarget);
        	 var type = $input.attr("data-type");
        	 var resultType = (type=="company")?"companies":(type+"s");
        	 searchDao.getAutoCompleteData({limit : 5, type : type,keyword:$input.val()}).always(function(result) {
        		 if(type=="company"){
        			 type = "employer";
        		 }
       	        $input.closest(".Filter"+type.substring(0, 1).toUpperCase()+type.substring(1)).find(".autoCompleteList").html(render("filterPanel-autoComplete-list",{results:result[resultType],type:type}));
       	    }); 
        },
        "btap; div.content div[class$='Row'][class!='contactRow']": function (event) {
            var view = this;
            var data = $.trim($(event.currentTarget).find(".contentText").text());
            var len = view.$el.find(".selectedItems .item[data-name='" + data + "']").length;
            if (len == 0) {
                view.$el.find(".selectedItems span.add").before(render("filterPanel-selectedItem-add", {name: data}))
                var $ele = $(view.$el.find(".selectedItems .item[data-name='" + data + "']")[0]);
                $ele.data("value", data);
                view.$el.trigger("ADD_FILTER", {type:view.type, name: data, value: data})

            }

        },
        "btap; span.clear":function(event){
            var view = this;
            var dataName = $(event.currentTarget).closest("span[data-name]").attr("data-name");
            setTimeout(function(){
                view.$el.find("span[data-name='" + dataName + "']").remove();
                view.$el.trigger("REMOVE_FILTER", {name: dataName, type: view.type});
            }, 200);
        }

    };


  
  app.ThPopup = ThPopup;
})(jQuery); 