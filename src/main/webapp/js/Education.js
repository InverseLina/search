/**
 * View: Education
 *
 * Education
 *
 *
 */
(function ($) {
    var searchDao = app.SearchDaoHandler;
    brite.registerView("Education", {emptyParent: true},
        {
            create: function (data, config) {
                return render("Education");
            },

            postDisplay: function (data) {
                var view = this;
                var educationLimit = app.preference.get("education",6);
                searchDao.getAdvancedMenu({educationLimit:educationLimit}).always(function (result) {
                    var html = render("Education-detail", result || {});
                    view.$el.append(html);
                    updateResultInfo.call(view, result);
                });
            },
            updateSearchValues:function(data){
                var view  = this;
                var update = function(){
                    if(data.educationNames){
                        $.each(data.educationNames.split(","), function(idx, item){
                            view.$el.find("li[data-name='" + item + "'] input").prop("checked", true);
                            view.$el.find("li[data-name='" + item + "']").addClass("selected")
                        })
                    }
                };
                if(view.$el.find(".education").length > 0){
                    update();
                }else{
                    setTimeout(update, 500);
                }
            },
            getSearchValues:function(){
                var view = this;
                var $e = view.$el;
                var $educationContainer = $e.find("ul.educationList");
                var companyALL = $educationContainer.find("li[data-name='ALL']").hasClass("selected");
                var educationStr = "";
                // get companies filter
                $educationContainer.find("li[data-name][data-name!='ALL']").find("input[type='checkbox']:checked").each(function(i) {
                    var value = $(this).closest("li").attr("data-name");
                    //get selected or all option is selected.
                        if (educationStr.length != 0) {
                            educationStr += ",";
                        }
                        educationStr += value;
                });
                if(!/^\s*$/.test(educationStr)){
                    return {educationNames:educationStr};
                }else{
                    return {};
                }

            },
            events: {
                "clear": function(){
                	var view = this;
                    view.$el.find("li[data-name!='ALL']").removeClass("selected").find(":checkbox").prop("checked", false);
                    var $allLi=view.$el.find("li[data-name='ALL']");
                    if(!$allLi.hasClass("selected")){
                    	$allLi.addClass("selected");
                    }
                    $allLi.find(":checkbox").prop("checked", true);
                    view.$el.find("input[type='text']").val("");

                },
                "btap; li[data-name] label" : function(event) {
                    var view = this;
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
                        setTimeout(function () {
                            view.$el.trigger("DO_SEARCH");
                        }, 200);

                    }
                },
                "btap;.btns span" : function(event) {
                    var view = this;
                    var $btn = $(event.currentTarget);
                    var $li = $btn.parent("li");
                    var flag = $btn.attr("data-show");
                    var $ul = $btn.closest("ul");
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
                                $li.before(render("Education-add", data));
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
                },
                "UPDATE_RESULT_CHANGE":function(event, result){
                    var $e = this.$el;
                    var companies = result.educations || [];
                    $e.find("li .validCount").show().html("0/");

                    for(var i = 0; i < companies.length; i++){
                        var obj = companies[i];
                        $e.find(".education li[data-name='"+obj.name+"'] .validCount").html(obj.count+"/");
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
                  qParams.type = "education";
                  
                  if (result.count > 0) {
                    searchDao.getGroupValuesForAdvanced(qParams).done(function(result){
                      var educations = result.list || [];
                      console.log(result.list);
                      $e.find("li .validCount").show().html("0/");
                      
                      for(var i = 0; i < educations.length; i++){
                        var obj = educations[i]; 
                        $e.find("li[data-name='"+obj.name+"'] .validCount").html(obj.count+"/");
                      }
                      
                    });
                  }else{
                    $e.find("li .validCount").hide();
                  }
                }
              }
            },
        });

    function updateResultInfo(result) {
        var view = this;
        var $e = view.$el;
        var $resultInfo = $e.find(".resultInfo");
        var $count = $resultInfo.find(".result-count");
        var $duration = $resultInfo.find(".result-duration");
        var $callDuration = $resultInfo.find(".result-callDuration");
        var count = 0;

        var duration = result.duration, companies = result.companies, educations = result.educations, skills = result.skills,
            callDuration = result.educationDuration, skillDuration = result.skillDuration;

        if (educations && educations.length) {
            //except the no education
            count = count + educations.length - 1;
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
})(jQuery);