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
	    searchDao.getAdvancedMenu({limit : 5, type : type}).always(function(result) {
	        var $e = $(render(view.name,result));
	        console.log(result);
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
        console.log("get value");
        var datasName = this.type.substring(0,1).toLocaleLowerCase() + this.type.substring(1) + "s";
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
	  var view = this;
      $(document).on("btap."+view.cid, function(event){
          var width = view.$el.find(".popover").width();
          var height = view.$el.find(".popover").height();
          var pos = view.$el.find(".popover").offset();
          if(event.pageX > pos.left && event.pageX < pos.left + width
              && event.pageY > pos.top && event.pageY < pos.top + height){
          }else{
             view.$el.hide();
          }
      });
     if(view.afterPostDisplay){
         view.afterPostDisplay();
     }
  };

    ThPopup.prototype.events = {
        "btap; span.add": function () {
            var view = this;
            view.$el.find("div.content").show();
            if(view.$el.find(".sliderBarContainer").length > 0){
                brite.display("Slider", ".sliderBarContainer");
            }

        },
        "btap; div.content>div[class$='Row'][class!='contactRow']": function (event) {
            var view = this;
            var data = $.trim($(event.currentTarget).find(".contentText").text());
            var len = view.$el.find(".selectedItems .item[data-name='" + data + "']").length;
            if (len == 0) {
                view.$el.find(".selectedItems span.add").before(render("filterPanel-selectedItem-add", {name: data}))
                var $ele = $(view.$el.find(".selectedItems .item[data-name='" + data + "']")[0]);
                $ele.data("value", data);
                view.$el.trigger("ADD_FILTER", {type:view.type, name: data, value: data})
            }

        }

    };


  
  app.ThPopup = ThPopup;
})(jQuery); 