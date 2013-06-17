/**
 * View: Skill
 *
 * Skill
 *
 *
 */
(function ($) {
    var searchDao = app.SearchDaoHandler;
    brite.registerView("Skill", {emptyParent: true},
        {
            create: function (data, config) {
            		this.dataType = "skill";
            		this.dataName = "skills";
                return render("Skill");
            },

            postDisplay: function (data) {
                var view = this;
                var skillLimit = app.preference.get("skill",app.defaultMenuSize);
                searchDao.getAdvancedMenu({limit:skillLimit,type:"skill"}).always(function (result) {
                    var html = render("Skill-detail", result || {});
                    view.$el.append(html);
                    app.sideSectionContentMixins.refreshSelections.call(view);
                    updateResultInfo.call(view, result);
                });
            },
            
            updateSearchValues:function(data){
                var view  = this;
                var update = function(){
                    if(data.skillNames){
                        $.each(data.skillNames.split(","), function(idx, item){
                            view.$el.find("li[data-name='" + item + "'] input").prop("checked", true);
                            view.$el.find("li[data-name='" + item + "']").addClass("selected")
                        })
                    }
                };
                if(view.$el.find(".skill").length > 0){
                    update();
                }else{
                    setTimeout(update, 500);
                }
            },
            
            getSearchValues:function(){
                var view = this;
                var $e = view.$el;
                var $educationContainer = $e.find("ul.skillList");
                var companyALL = $educationContainer.find("li[data-name='ALL']").hasClass("selected");
                var skillStr = "";
                // get companies filter
                $educationContainer.find("li[data-name][data-name!='ALL']").find("input[type='checkbox']:checked").each(function(i) {
                    var value = $(this).closest("li").attr("data-name");
                    //get selected or all option is selected.
                        if (skillStr.length != 0) {
                            skillStr += ",";
                        }
                        skillStr += value;
                });
                if(!/^\s*$/.test(skillStr)){
                    return {skillNames:skillStr};
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
                        
                        // FIXME: needs to use custom checkbox element to simplify
                        //        and reuse code for all the different SideSectionContent
                        if ($li.hasClass("all") && $li.hasClass("selected")){
                        	setTimeout(function(){
                        		$li.find(":checkbox").prop("checked",true);
                        	},10);
                        	return;
                        }
                                                
                        if ($li.hasClass("all")) {
                            $("li:gt(0)", $ul).removeClass("selected").find(":checkbox").prop("checked", false);
                        } else {
                            $("li:eq(0)", $ul).removeClass("selected").find(":checkbox").prop("checked", false);
                        }

                        if ($li.hasClass("selected")) {
                            $li.removeClass("selected");
                        } else {
                            $li.addClass("selected");
                        }
                        
                        app.sideSectionContentMixins.refreshSelections.call(view);
                        
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
                    var type = "skill"; 
                    var dataName = "skills";
                    // show more items
                    if (flag == "more") {
                        // get advanced menu data from server
                        searchDao.getAdvancedMenu({
                            type : type,
                            offset : app.preference.get(type, app.defaultMenuSize),
                            limit : 20
                        }).pipe(function(data) {
                                updateResultInfo.call(view, data);
                                $li.before(render("Skill-add", data));
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
                        var $hideLi = $("li:not('.btns'):gt(" + (itemNum - hideNum) + ")", $ul);
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
                
                "UPDATE_RESULT_CHANGE":function(event, result){
                    var $e = this.$el;
                    var companies = result.skills || [];
                    $e.find("li .validCount").show().html("0/");

                    for(var i = 0; i < companies.length; i++){
                        var obj = companies[i];
                        $e.find(".skill li[data-name='"+obj.name+"'] .validCount").html(obj.count+"/");
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
                  // we remove this dataName because we do not want it in the results (otherwise always 0/...)
                  delete searchValues[view.dataType + "Names"];                  
                  // just add the "q_"
                  var qParams = {};
                  $.each(searchValues, function (key, val) {
                    qParams["q_" + key] = $.trim(val);
                  });
                  qParams.type = "skill";
                  
                  if (result.count > 0) {
                    searchDao.getGroupValuesForAdvanced(qParams).done(function(result){
                      var skills = result.list || [];
                      
                      $e.find("li .validCount").show().html("0/");
                      
                      for(var i = 0; i < skills.length; i++){
                        var obj = skills[i]; 
                        $e.find("li[data-name='"+obj.name+"'] .validCount").html(obj.count+"/");
                      }
                      
                    });
                  }else{
                    $e.find("li .validCount").hide();
                  }
                }
              }
            }
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
            callDuration = result.skillDuration, skillDuration = result.skillDuration;

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
