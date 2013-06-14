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
                return render("Skill");
            },

            postDisplay: function (data) {
                var view = this;
                var skillLimit = app.preference.get("skill",6);
                searchDao.getAdvancedMenu({skillLimit:skillLimit}).always(function (result) {
                    var html = render("Skill-detail", result || {});
                    view.$el.append(html);
                    updateResultInfo.call(view, result);
                });
            },
            updateSearchValues:function(data){
                var view  = this;
                var update = function(){
                    if(data.skillNames){
                        $.each(data.skillNames.split(","), function(item){
                            view.$el.find("li[data-name='" + item + "'] input").prop("checked", true);
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
                var $educationContainer = $e.find("ul.skill");
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
                                $li.before(render("Skill-add", data));
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
                        console.log({itemNum:itemNum, hideNum:hideNum})
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
            docEvents: {}
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
