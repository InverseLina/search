(function($) {

  var searchDao = app.SearchDaoHandler;

  brite.registerView("AdvancedSearch", {
    parent : ".advanced",
    emptyParent : true
  }, {
    create : function(data) {
      return render("AdvancedSearch", data);
    },

    postDisplay : function(data) {
      var view = this;
      data = data || {};
      updateResultInfo.call(view, data);

      view.$el.find(".advancedItems li.all :checkbox").prop("checked", true).closest("li").addClass("selected");
      view.$el.trigger("DO_SEARCH");
    },

    events : {
      "btap;.advancedItems label" : function(event) {
        //left click
        if (event.which == 1) {
          var $li = $(event.target).closest("li");
          var $ul = $li.parent("ul");
          if ($li.hasClass("all")) {
            $("li:gt(0)", $ul).removeClass("selected").find(":checkbox").prop("checked", false);
          } else {
            if ($li.prev().hasClass("all")) {
              $("li:eq(0)", $ul).removeClass("selected").find(":checkbox").prop("checked", false);
              $("li:gt(1)", $ul).removeClass("selected").find(":checkbox").prop("checked", false);
            } else {
              $("li:lt(2)", $ul).removeClass("selected").find(":checkbox").prop("checked", false);
            }
          }

          if ($li.hasClass("selected")) {
            $li.removeClass("selected");
          } else {
            $li.addClass("selected");
          }

          this.$el.trigger("DO_SEARCH");
        }
      },

      "btap;.btns span" : function(event) {
        var view = this;
        var $btn = $(event.currentTarget);
        var $li = $btn.parent("li");
        var flag = $btn.attr("data-show");
        var $ul = $btn.closest("ul.advancedItems");
        var type = $ul.hasClass("company") ? "company" : ($ul.hasClass("skill") ? "skill" : "education");
        var dataName = (type == "company") ? "companies" : ((type == "skill") ? "skills" : "educations");
        // show more items
        if (flag == "more") {
          // get advanced menu data from server
          searchDao.getAdvancedMenu({
            type : type,
            offset : app.preference.get(type, 6),
            limit : 20
          }).pipe(function(data) {
            updateResultInfo.call(view, data);
            $li.before(render("AdvancedSearch-" + type, data));
            $li.closest("ul").find(".toShow").show(1000, function() {
              $(this).removeClass("toShow");
            })

            //save the offset
            app.preference.store(type, (parseInt(app.preference.get(type, 6)) + data[dataName].length));
            $btn.next().show();
            if (data.length < 20) {
              $btn.hide();
            }
            view.$el.trigger("DO_SEARCH");
          });
          // show less items
        } else {
          var itemNum = parseInt(app.preference.get(type, 6));
          var hideNum = 0;
          if ((itemNum - 6) % 20 == 0) {
            hideNum = 20;
          } else {
            hideNum = (itemNum - 6) % 20;
          }
          app.preference.store(type, (itemNum - hideNum));
          var num = 0;
          var $hideLi = $("li:not('.btns'):gt(" + (itemNum - hideNum) + ")", $ul);
          $hideLi.hide(1000, function() {
            $(this).remove();
            num++;
            if (num == $hideLi.length) {
              view.$el.trigger("DO_SEARCH");
            }
          });
          $btn.prev().show();
          if ((itemNum - hideNum) <= 6) {
            $btn.hide();
          }
        }
      }

    },

    parentEvents : {
      MainView : {
        "SEARCH_RESULT_CHANGE" : function(event, result) {
          var view = this;
          var $e = view.$el;
          var mainView = view.$el.bView("MainView");
          var contentSearchValues = mainView.contentView.getSearchValues();
          var navContectSearchValues = mainView.sideNav.getSearchValues();
          var searchValues = $.extend({},contentSearchValues ,navContectSearchValues);
          // just add the "q_"
          var qParams = {};
          $.each(searchValues, function (key, val) {
            qParams["q_" + key] = $.trim(val);
          });
          if (result.count > 0) {
            searchDao.getGroupValuesForAdvanced(qParams).done(function(result){
              var companies = result.companies || [];
              var educations = result.educations || [];
              var skills = result.skills || [];
              
              $e.find(".advancedItems li .validCount").show().html("0/");
              
              for(var i = 0; i < companies.length; i++){
                var obj = companies[i]; 
                $e.find(".advancedItems.company li[data-name='"+obj.name+"'] .validCount").html(obj.count+"/");
              }
              
              for(var i = 0; i < educations.length; i++){
                var obj = educations[i]; 
                $e.find(".advancedItems.education li[data-name='"+obj.name+"'] .validCount").html(obj.count+"/");
              }
              
              for(var i = 0; i < skills.length; i++){
                var obj = skills[i]; 
                $e.find(".advancedItems.skill li[data-name='"+obj.name+"'] .validCount").html(obj.count+"/");
              }
            });
          }else{
            $e.find(".advancedItems li .validCount").hide();
          }
        }
      }
    },

    getSearchValues : function() {
      var view = this;
      var $e = view.$el;
      var $companyContainer = $e.find("ul.company");
      var $educationContainer = $e.find("ul.education");
      var $skillContainer = $e.find("ul.skill");
      var companyALL = $companyContainer.find("li[data-name='ALL']").hasClass("selected");
      var companyStr = "";
      // get companies filter
      $companyContainer.find("li[data-name!='ALL']").each(function(i) {
        var $li = $(this);
        var value = $(this).attr("data-name");
        //get selected or all option is selected.
        if ($li.hasClass("selected") || companyALL) {
          if (companyStr.length != 0) {
            companyStr += ",";
          }
          companyStr += value;
        }
      });

      if (companyALL) {
        companyStr = "Any Company";
      }

      var educationALL = $educationContainer.find("li[data-name='ALL']").hasClass("selected");
      var educationStr = "";
      // get educations filter
      $educationContainer.find("li[data-name!='ALL']").each(function(i) {
        var $li = $(this);
        var value = $(this).attr("data-name");
        //get selected or all option is selected.
        if ($li.hasClass("selected") || educationALL) {
          if (educationStr.length != 0) {
            educationStr += ",";
          }
          educationStr += value;
        }
      });
      if (educationALL) {
        educationStr = "Any Education";
      }

      var skillALL = $skillContainer.find("li[data-name='ALL']").hasClass("selected");
      var skillStr = "";
      // get educations filter
      $skillContainer.find("li[data-name!='ALL']").each(function(i) {
        var $li = $(this);
        var value = $(this).attr("data-name");
        //get selected or all option is selected.
        if ($li.hasClass("selected") || skillALL) {
          if (skillStr.length != 0) {
            skillStr += ",";
          }
          skillStr += value;
        }
      });
      if (skillALL) {
        skillStr = "Any Skill";
      }
      return {
        companyNames : companyStr,
        educationNames : educationStr,
        skillNames : skillStr
      };
    }

  });

  function updateResultInfo(result) {
    var view = this;
    var $e = view.$el;
    var $advancedSearchResultInfo = $e.find(".AdvancedSearch-resultInfo");
    var $count = $advancedSearchResultInfo.find(".result-count");
    var $duration = $advancedSearchResultInfo.find(".result-duration");
    var $companyDuration = $advancedSearchResultInfo.find(".result-companyDuration");
    var $educationDuration = $advancedSearchResultInfo.find(".result-educationDuration");
    var $skillDuration = $advancedSearchResultInfo.find(".result-skillDuration");
    var count = 0;

    var duration = result.duration, companies = result.companies, educations = result.educations, skills = result.skills, companyDuration = result.companyDuration, educationDuration = result.educationDuration, skillDuration = result.skillDuration;

    if (companies && companies.length) {
      //except the no company
      count = count + companies.length - 1;
    }

    if (educations && educations.length) {
      //except the no education
      count = count + educations.length - 1;
    }

    if (skills && skills.length) {
      //except the no skill
      count = count + skills.length - 1;
    }

    $count.html(count);

    if (duration != null && typeof duration != "undefined") {
      $duration.html(duration);
    }

    if (companyDuration != null && typeof companyDuration != "undefined") {
      $companyDuration.html(companyDuration);
    } else {
      $companyDuration.html(0);
    }

    if (educationDuration != null && typeof educationDuration != "undefined") {
      $educationDuration.html(educationDuration);
    } else {
      $educationDuration.html(0);
    }

    if (skillDuration != null && typeof skillDuration != "undefined") {
      $skillDuration.html(skillDuration);
    } else {
      $skillDuration.html(0);
    }

  }

})(jQuery); 