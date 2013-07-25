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
        "btap; li[data-name]":function(event) {
            event.stopPropagation();
            var view = this;
            var $li = $(event.target).closest("li");
            var $ul = $li.parent("ul");

            // FIXME: needs to use custom checkbox element to simplify
            //        and reuse code for all the different SideSectionContent

            if ($li.hasClass("all") ) {
                view.$el.find("li:not(.all)", $ul).removeClass("selected").find(":checkbox").prop("checked", false);
                //$ul.find("input[type='text']").val("");
                $ul.find("li .filter").hide();
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
                $(".filter", $li).hide();
            } else {
                $li.addClass("selected");
                //show filter
                showFilter.call(view, $li);
            }
            view.refreshSelections();

            setTimeout(function() {
                view.$el.trigger("DO_SEARCH");
            }, 200);
        },
        "btap; li[data-name] .filter":function(event) {
          event.stopPropagation();
        },
        "btap; .btns span": function(event) {
            var view = this;
            var $btn = $(event.currentTarget);
            var $li = $btn.parent("li");
            var flag = $btn.attr("data-show");
            var $ul = $btn.closest("ul");
            var type = view.dataType;
            var dataName = view.dataName;
            var $input=$ul.find(":text");
            view.itemNum=view.itemNum?view.itemNum:5;
            // show more items
            if (flag == "more") {
                // get advanced menu data from server
                searchDao.getAdvancedMenu({
                    type : type,
                    offset : view.itemNum,
                    limit : 20,
                    match:$input.val()
                }).pipe(function(data) {
                        view.updateResultInfo(data);
                        $li.before(render(view.name+"-add", data));
                        if($input.val()){
	                        $li.closest("ul").find(".toShow").addClass("selected").each(function(index,li){
	    	                	  showFilter.call(view,$(li));
	    	                	  $(":checkbox",li).prop("checked",true);
	    	                });
                        }
                        $li.closest("ul").find(".toShow").show(1000, function() {
                            $(this).removeClass("toShow");
                        });
                        //save the offset
                        //app.preference.store(type, (parseInt(app.preference.get(type, app.defaultMenuSize)) + data[dataName].length));
                        view.itemNum = (view.itemNum?view.itemNum:5) + data[dataName].length;
                        $btn.next().show();
                        if (data[dataName].length < 20) {
                            $btn.hide();
                        }
                        view.$el.trigger("DO_SEARCH");
                    });
                // show less items
            } else {
                var itemNum =view.itemNum;
                var hideNum = 0;
                if ((itemNum - app.defaultMenuSize) % 20 == 0) {
                    hideNum = 20;
                } else {
                    hideNum = (itemNum - app.defaultMenuSize) % 20;
                }
                view.itemNum = view.itemNum-hideNum;
               // app.preference.store(type, (itemNum - hideNum));
                var num = 0;
                var $hideLi = $("li[data-name][data-name!='ALL']:not('.btns'):gt(" + (itemNum - hideNum - 1) + ")", $ul);

                $hideLi.hide(1000, function() {
                    $(this).remove();
                    num++;
                    if (num == $hideLi.length) {
                        view.$el.trigger("DO_SEARCH");
                    }
                    view.refreshSelections();
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
            var $li = $input.parent("li");
            var flag = $input.attr("data-show");
            var $ul = $input.closest("ul");
            var type = view.dataType;
            var dataName = view.dataName;
            if(event.which===13){
            	if($input.parent().hasClass("control-group")){
	        	   searchDao.getAdvancedMenu({
	                   type : type,
	                   match:$input.val()
	               }).pipe(function(data) {
	                  $ul.find("li[data-name][data-name!='ALL']:not('.btns')").remove();
	                  $ul.find("li.btns").before(render(view.name+"-add", data));
	                  $ul.find(".toShow").show();
	                  //save the offset
	                  app.preference.store(type, (parseInt(app.preference.get(type, app.defaultMenuSize))));
	                  if($input.val()){
		                  $ul.find(".toShow").addClass("selected").find(":checkbox").prop("checked", true);
		                  $ul.find(".toShow").each(function(index,li){
		                	  showFilter.call(view,$(li));
		                	  $(li).removeClass("toShow");
		                  });
		                  
		                  $ul.find("li[data-name='ALL']").removeClass("selected").find(":checkbox").prop("checked",false);
	                  }else{
	                	  $ul.find("li[data-name='ALL']").addClass("selected").find(":checkbox").prop("checked",true);
	                  }
	                  view.$el.trigger("DO_SEARCH");
	               });
            	}else{
            		view.$el.trigger("DO_SEARCH");
            	}
            }
        },
        "restoreSearchList": function(event, data){
            console.log(data);
            var restoreValue = data.value;
            var view = this;
            var limit, offset;
            if(!view.itemNum){
                view.itemNum = app.defaultMenuSize;
            }
            console.log(view)
            if(data.itemNum > view.itemNum){
                offset = view.itemNum;
                limit = data.itemNum - offset;
                //view.itemNum = data.itemNum;
            }
            $btn = view.$el.find("span[data-show='more']");
            var $li = $btn.parent("li");
            var $ul = $btn.closest("ul");
            var type = view.dataType;
            var dataName = view.dataName;
            var $input=$ul.find(":text");
            var match="";
            if(data.value["search" + view.name]){
                match = data.value["search" + view.name];
                $input.val(match);
            }
            console.log(match);
            console.log(data.value["search" + view.name]);
            // show more items

                // get advanced menu data from server
                searchDao.getAdvancedMenu({
                    type : type,
                    offset : offset,
                    limit : limit,
                    match:$input.val()
                }).pipe(function(data) {
                        view.updateResultInfo(data);
                        if(match && match.length > 0){
                            console.log($("li[data-name][data-name!='ALL']:not('.btns')", $ul))
                            $("li[data-name][data-name!='ALL']:not('.btns')", $ul).remove();
                        }
                        $li.before(render(view.name+"-add", data));
                        if($input.val()){
                            $li.closest("ul").find(".toShow").addClass("selected").each(function(index,li){
                                showFilter.call(view,$(li));
                                $(":checkbox",li).prop("checked",true);
                            });
                        }
                        $li.closest("ul").find(".toShow").show(1000, function() {
                            $(this).removeClass("toShow");
                        });
                        //save the offset
                        //app.preference.store(type, (parseInt(app.preference.get(type, app.defaultMenuSize)) + data[dataName].length));
                        view.itemNum = (view.itemNum?view.itemNum:5) + data[dataName].length;
                        $btn.next().show();
                        if (data[dataName].length < 20) {
                            $btn.hide();
                        }

                        view.$el.bView("SideSection").updateSearchValues(restoreValue);

                    });
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

    BaseSideAdvanced.prototype.validate =function(values){
        var vals = values.split("|");
        var v1=false, errors = [];
        if(vals.length < 2) {
            return errors;
        }
        if(vals[1].length > 0 ){
            if(!/\d+/g.test(vals[1])){
                errors.push(vals[0] + " min value require be number");
            }else{
                v1 = true;
            }
        }
        if(vals[2].length > 0 ){
            if(!/\d+/g.test(vals[2])){
                errors.push(vals[0] +  " max value require be number");
            }
            if(v1){
                if(parseInt(vals[2]) - parseInt(vals[1])<=0){
                    errors.push(vals[0] + " max value must big than min value");
                }
            }
        }

        return errors;
    };

   BaseSideAdvanced.prototype.updateSearchValues = function(data, itemNum) {
    var view = this;
       if(!itemNum){
           itemNum = 0;
       }
    if(itemNum > view.$el.bView("SideSection").getItemNum()){
       view.$el.trigger("restoreSearchList", {value:data, itemNum:itemNum});
       return;
    }
    var dataType = this.dataType;
    var names = data[dataType+"Names"];
    if (names) {
      $.each(names.split(","), function(idx, item) {
          var vals = item.split("|");
          item = vals[0];
          vals = vals.slice(1);
          var $li = view.$el.find("li[data-name='" + item + "']");
          $("input[type='checkbox']", $li).prop("checked", true);
          showFilter.call(view, $li);
          $(".filter input[type='text']", $li).each(function(idx, ele){
             $(ele).val(vals[idx]);
          });
         $li.addClass("selected")
      })
    }
   var curName = "cur" + dataType.substr(0,1).toUpperCase() + dataType.substr(1);
   if (data[curName]) {
       view.$el.find("input[name='" + curName + "']").prop("checked", true);
   }
   var searchName = "search" + dataType.substr(0,1).toUpperCase() + dataType.substr(1);
   if (data[searchName]) {
       view.$el.find('input[name][type=text]').val(data[searchName]);
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
    var itemErrors = [];
    // get companies filter
    $itemContainer.find("li[data-name][data-name!='ALL']").find("input[type='checkbox']:checked").each(function(i) {
      var $li = $(this).closest("li");
      var value = $li.attr("data-name");
      var errVal = [];
      $li.find(".filter input").each(function(idx, ele){
          var fval = $(ele).val();
          if(!/^\s*$/g.test(fval)){
              value = value + "|" + fval;
          }else{
              value = value + "|";
          }
      })
      if(view.validate && $.isFunction(view.validate)){
          errVal = view.validate(value);
      }
      //get selected or all option is selected.
        if (errVal.length == 0) {
            if (itemStr.length != 0) {
                itemStr += ",";
            }
            itemStr += value;
        } else {
            itemErrors.push(errVal.join(","));
        }
    });

    var result = {};

    if (!/^\s*$/.test(itemStr)) {
      result[dataType+"Names"] = itemStr;
    }
    if (itemErrors.length > 0) {
      result[dataType+"Errors"] = itemErrors;
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

      view.values=result;
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
    var $e = view.$el;
    $e.find("li[data-name!='ALL']").removeClass("selected").find(":checkbox").prop("checked", false);
    var $allLi = $e.find("li[data-name='ALL']");
    if (!$allLi.hasClass("selected")) {
      $allLi.addClass("selected");
    }
    $allLi.find(":checkbox").prop("checked", true);
    var match = $e.find("input[type='text']").val();
    $e.find("input[type='text']").val("");
    $(".filter", $e).hide();
/*    if(match.length>0){
        var limit = app.preference.get(view.dataType, app.defaultMenuSize);
        console.log($("li[data-name][data-name!='ALL']:not('.btns')", view.$el))
        $("li[data-name][data-name!='ALL']:not('.btns')", view.$el).remove();
        searchDao.getAdvancedMenu({limit : limit, type : view.dataType}).always(function(result) {
           var html = render(view.name+"-add", result || {});
            $e.append(html);
            view.updateResultInfo(result,$e);
            view.refreshSelections($e);
        });
    }*/

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
     // delete searchValues[this.dataType + "Names"];
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

  function showFilter($li){
      if($(".filter", $li).length==0){
          var templName = this.name + "-filter";
          if(hasTemplate(templName)){
              $li.append(render(templName));
          }
      }else{
          $(".filter", $li).show();
      }
  }

  app.sidesection.BaseSideAdvanced = BaseSideAdvanced;
})(jQuery); 