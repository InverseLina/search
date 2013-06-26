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
    var dfd = $.Deferred();
    var view = this;
    var dataType = this.dataType;
    var limit = app.preference.get(dataType, app.defaultMenuSize);
    searchDao.getAdvancedMenu({limit : limit, type : dataType}).always(function(result) {
      var $e = $(render(view.name));
      var html = render(view.name+"-detail", result || {});
      $e.append(html);
      view.updateResultInfo(result,$e);
      view.refreshSelections($e);
      dfd.resolve($e);
    }); 
    return dfd.promise();
  }
  
  BaseSideAdvanced.prototype.postDisplay = function(data) {
    var view = this;
    var $e = view.$el;
    var dataType = this.dataType
    setTimeout(function(){
        view.updateScore($e);
    }, 300);  
  }


    BaseSideAdvanced.prototype.events  = {
        "clear": function(){
            var view = this;
            view.clearValues();
        },
        "btap; li[data-name] label":function(event) {
            var view = this;
            var $li = $(event.target).closest("li");
            var $ul = $li.parent("ul");

            // FIXME: needs to use custom checkbox element to simplify
            //        and reuse code for all the different SideSectionContent

            if ($li.hasClass("all") ) {
                view.$el.find("li:not(.all)", $ul).removeClass("selected").find(":checkbox").prop("checked", false);
                $ul.find("input[type='text']").val("");
                setTimeout(function() {
                    $li.find(":checkbox").prop("checked", true);
                    $li.removeClass("selected").addClass("selected");
                    view.$el.trigger("DO_SEARCH");
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
        },
        "change; li[data-name] input[type='checkbox']": function(event) {
            var view = this;
            var values = view.getSearchValues();
            app.preference.store(view.name + ".values", JSON.stringify(values));
        },
        "btap; .btns span": function(event) {
            var view = this;
            var $btn = $(event.currentTarget);
            var $li = $btn.parent("li");
            var flag = $btn.attr("data-show");
            var $ul = $btn.closest("ul");
            var type = view.dataType;
            var dataName = view.dataName;
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
                var $hideLi = $("li[data-name][data-name!='ALL']:not('.btns'):gt(" + (itemNum - hideNum - 1) + ")", $ul);

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

        },
        "keyup;input[type='text']":function(event){
            var $input = $(event.target);
            var view = this;
            if($input.val().length>0){
                view.$el.find("li[data-name='ALL']").removeClass("selected").find(":checkbox").prop("checked", false);
            }else{
                if (view.$el.find("li:not(.all).selected").length === 0) {
                    view.$el.find("li.all").addClass("selected").find(":checkbox").prop("checked", true);
                }
            }
        }
    };

  BaseSideAdvanced.prototype.parentEvents  = {
          MainView: {
              "SEARCH_RESULT_CHANGE": function(event, result) {
                  var view = this;
                  view.updateScore(view.$el, result);
              },
              "NO_SEARCH":function(event){
                  var view = this;
                  view.updateScore(view.$el);
              }
          }
      };




   BaseSideAdvanced.prototype.updateSearchValues = function(data) {
    var view = this;
    var dataType = this.dataType;
    var names = data[dataType+"Names"];
    if (names) {
      $.each(names.split(","), function(idx, item) {
        view.$el.find("li[data-name='" + item + "'] input").prop("checked", true);
        view.$el.find("li[data-name='" + item + "']").addClass("selected")
      })
    }
   var curName = "cur" + dataType.substr(0,1).toUpperCase() + dataType.substr(1);
   if (data[curName]) {
       view.$el.find("input[name='" + curName + "']").prop("checked", true);
   }
   var searchName = "search" + dataType.substr(0,1).toUpperCase() + dataType.substr(1);
   if (data[searchName]) {
       view.$el.find('input[type=text]').val(data[searchName]);
       view.$el.find("li.all").removeClass("selected").find(":checkbox").prop("checked", false);
   }

    view.refreshSelections();

  };


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

      var searchName = "search" + dataType.substr(0,1).toUpperCase() + dataType.substr(1);
      var searchValue = view.$el.find('input[type=text]').val();
      if (!/^\s*$/.test(searchValue)) {
          result[searchName] = searchValue;
      }
      var curName = "cur" + dataType.substr(0,1).toUpperCase() + dataType.substr(1);
      var curValue = view.$el.find("input[name='" + curName + "']").prop("checked");
      if (curValue) {
          result[curName] = curValue;
      }

    return result;
  };


  BaseSideAdvanced.prototype.updateResultInfo = function(result,$e) {
    var view = this;
    $e = $e || view.$el;
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
  };

  BaseSideAdvanced.prototype.refreshSelections = function($e) {
    var view = this;
    $e = $e||view.$el;
    var searchValue = $e.find('input[type=text]').val();

    if ($e.find("li:not(.all).selected").length === 0 && /^\s*$/.test(searchValue) ) {
      $e.find("li.all").addClass("selected").find(":checkbox").prop("checked", true);
    }else{
      $e.find("li.all").removeClass("selected").find(":checkbox").prop("checked", false);
    }
  };

  BaseSideAdvanced.prototype.clearValues = function() {
    var view = this;
    view.$el.find("li[data-name!='ALL']").removeClass("selected").find(":checkbox").prop("checked", false);
    var $allLi = view.$el.find("li[data-name='ALL']");
    if (!$allLi.hasClass("selected")) {
      $allLi.addClass("selected");
    }
    $allLi.find(":checkbox").prop("checked", true);
    view.$el.find("input[type='text']").val(""); 

  };
  BaseSideAdvanced.prototype.updateScore = function($e, result) {
      var view = this;
      var $e = $e || view.$el;
      var mainView = $e.bView("MainView");
      if(!result){
          //is not call from serch change, get result form result info;
          result = {count:0};
          var $searchInfo = mainView.contentView.$el.find(".search-info")
          if($searchInfo.length> 0 ) {
              var searchInfo = $searchInfo.text();
              var regex = /^Result.*:\s*(\d+)\s*\|/;
              var match = regex.exec(searchInfo);
              if (match != null) {
                  result.count = match[1];
              }else{
                  //not result
                  result.count = -1;
              }

          }else{
              result.count = -1;
          }
      }

      var contentSearchValues = mainView.contentView.getSearchValues();
      if(contentSearchValues.sort){
          delete contentSearchValues.sort;
      }
      var navContectSearchValues = mainView.sideNav.getSearchValues();
      var searchValues = $.extend({}, contentSearchValues, navContectSearchValues);
      // remove this section names as we do not want to have all 0/..
      delete searchValues[this.dataType + "Names"];
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
      } else if(result.count == 0){
          $e.find("li .validCount").show().html("0/");
      } else{
          $e.find("li .validCount").hide();
      }
  };

  app.sidesection.BaseSideAdvanced = BaseSideAdvanced;
})(jQuery); 