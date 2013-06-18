var app = app || {};
(function($) {
  app.sidesection = {};
  var searchDao = app.SearchDaoHandler;
  function BaseSideAdvanced(dataType,dataName) {
    //need to init these values
    this.dataType = dataType;
    this.dataName = dataName;
  };

  BaseSideAdvanced.prototype.create = function(data, config) {
    return render(this.name);
  }
  
  BaseSideAdvanced.prototype.postDisplay = function(data) {
    var view = this;
    var $e = view.$el;
    var dataType = this.dataType;
    
    var limit = app.preference.get(dataType, app.defaultMenuSize);
    searchDao.getAdvancedMenu({limit : limit, type : dataType}).always(function(result) {
      var html = render(view.name+"-detail", result || {});
      view.$el.append(html);
      view.refreshSelections();
      view.updateResultInfo(result);
    }); 
    
    $e.on("btap",".clear",function(){
      view.clearValues();
    });

    $e.on("btap","li[data-name] label", function(event) {
      var $li = $(event.target).closest("li");
      var $ul = $li.parent("ul");
      
      // FIXME: needs to use custom checkbox element to simplify
      //        and reuse code for all the different SideSectionContent
      if ($li.hasClass("all") && $li.hasClass("selected")) {
        setTimeout(function() {
          $li.find(":checkbox").prop("checked", true);
        }, 10);
        return;
      }

      if ($li.hasClass("all")) {
        $("li:not(.all)", $ul).removeClass("selected").find(":checkbox").prop("checked", false);
      } else {
        $("li.all", $ul).removeClass("selected").find(":checkbox").prop("checked", false);
      }

      if ($li.hasClass("selected")) {
        $li.removeClass("selected");
      } else {
        $li.addClass("selected");
      }

      view.refreshSelections();

      setTimeout(function() {
        view.$el.trigger("DO_SEARCH");
      }, 200);
    });

    $e.on("btap",".btns span", function(event) {
      var $btn = $(event.currentTarget);
      var $li = $btn.parent("li");
      var flag = $btn.attr("data-show");
      var $ul = $btn.closest("ul");
      var type = this.dataType;
      var dataName = this.dataName;
      // show more items
      if (flag == "more") {
        // get advanced menu data from server
        searchDao.getAdvancedMenu({
          type : type,
          offset : app.preference.get(type, app.defaultMenuSize),
          limit : 20
        }).pipe(function(data) {
          view.updateResultInfo(data);
          $li.before(render(view.name+"-add", data));
          $li.closest("ul").find(".toShow").show(1000, function() {
            $(this).removeClass("toShow");
          })

          //save the offset
          app.preference.store(type, (parseInt(app.preference.get(type, app.defaultMenuSize)) + data[dataName].length));
          $btn.next().show();
          if (data.length < 20) {
            $btn.hide();
          }
          view.$el.trigger("DO_SEARCH");
        });
        // show less items
      } else {
        var itemNum = parseInt(app.preference.get(type, app.defaultMenuSize));
        var hideNum = 0;
        if ((itemNum - app.defaultMenuSize) % 20 == 0) {
          hideNum = 20;
        } else {
          hideNum = (itemNum - app.defaultMenuSize) % 20;
        }
        app.preference.store(type, (itemNum - hideNum));
        var num = 0;
        var $hideLi = $("li:not('.btns'):gt(" + (itemNum - hideNum + 2) + ")", $ul);
        $hideLi.hide(1000, function() {
          $(this).remove();
          num++;
          if (num == $hideLi.length) {
            view.$el.trigger("DO_SEARCH");
          }
        });
        $btn.prev().show();
        if ((itemNum - hideNum) <= app.defaultMenuSize) {
          $btn.hide();
        }
      }

    });

    $(document).on("SEARCH_RESULT_CHANGE." + view.id, function(event,result) {
      var mainView = view.$el.bView("MainView");
      var contentSearchValues = mainView.contentView.getSearchValues();
      var navContectSearchValues = mainView.sideNav.getSearchValues();
      var searchValues = $.extend({}, contentSearchValues, navContectSearchValues);
      // we remove this dataName because we do not want it in the results (otherwise always 0/...)
      delete searchValues[view.dataType + "Names"];
      // just add the "q_"
      var qParams = {};
      $.each(searchValues, function(key, val) {
        qParams["q_" + key] = $.trim(val);
      });
      qParams.type = view.dataType;

      if (result.count > 0) {
        searchDao.getGroupValuesForAdvanced(qParams).done(function(result) {
          var items = result.list || [];

          $e.find("li .validCount").show().html("0/");

          for (var i = 0; i < items.length; i++) {
            var obj = items[i];
            $e.find("li[data-name='" + obj.name + "'] .validCount").html(obj.count + "/");
          }

        });
      } else {
        $e.find("li .validCount").hide();
      }

    });
  }


  BaseSideAdvanced.prototype.updateSearchValues = function(data) {
    var view = this;
    var dataType = this.dataType;
    var names = data[dataType+"Names"];
    var update = function() {
      if (names) {
        $.each(names.split(","), function(idx, item) {
          view.$el.find("li[data-name='" + item + "'] input").prop("checked", true);
          view.$el.find("li[data-name='" + item + "']").addClass("selected")
        })

      }
    };
    if (view.$el.find("."+dataType).length > 0) {
      update();
    } else {
      setTimeout(update, 500);
    }

  }


  BaseSideAdvanced.prototype.getSearchValues = function() {
    var view = this;
    var $e = view.$el;
    var dataType = this.dataType;
    var $itemContainer = $e.find("ul."+dataType+"List");
    var itemALL = $itemContainer.find("li[data-name='ALL']").hasClass("selected");
    var itemStr = "";
    // get companies filter
    $itemContainer.find("li[data-name][data-name!='ALL']").find("input[type='checkbox']:checked").each(function(i) {
      var value = $(this).closest("li").attr("data-name");
      //get selected or all option is selected.
      if (itemStr.length != 0) {
        itemStr += ",";
      }
      itemStr += value;
    });

    var result = {};

    if (!/^\s*$/.test(itemStr)) {
      result[dataType+"Names"] = itemStr;
    }

    return result;
  }


  BaseSideAdvanced.prototype.updateResultInfo = function(result) {
    var view = this;
    var $e = view.$el;
    var $resultInfo = $e.find(".resultInfo");
    var $count = $resultInfo.find(".result-count");
    var $duration = $resultInfo.find(".result-duration");
    var $callDuration = $resultInfo.find(".result-callDuration");
    var count = 0;
    var dataType = this.dataType;
    var dataName = this.dataName;

    var duration = result.duration, items = result[dataName], callDuration = result[dataType+"Duration"];

    if (items && items.length) {
      //except the no company
      count = count + items.length;
    }

    $count.html(count);

    if (duration != null && typeof duration != "undefined") {
      $duration.html(duration);
    }

    if (callDuration != null && typeof callDuration != "undefined") {
      $callDuration.html(callDuration);
    } else {
      $callDuration.html(0);
    }
  }

  BaseSideAdvanced.prototype.refreshSelections = function() {
    var view = this;
    
    if (view.$el.find("li:not(.all).selected").length === 0) {
      view.$el.find("li.all").addClass("selected").find(":checkbox").prop("checked", true);
    }else{
      view.$el.find("li.all").removeClass("selected").find(":checkbox").prop("checked", false);
    }
  } 

  BaseSideAdvanced.prototype.clearValues = function() {
    var view = this;
    view.$el.find("li[data-name!='ALL']").removeClass("selected").find(":checkbox").prop("checked", false);
    var $allLi = view.$el.find("li[data-name='ALL']");
    if (!$allLi.hasClass("selected")) {
      $allLi.addClass("selected");
    }
    $allLi.find(":checkbox").prop("checked", true);
    view.$el.find("input[type='text']").val(""); 

  } 

  app.sidesection.BaseSideAdvanced = BaseSideAdvanced;
})(jQuery); 