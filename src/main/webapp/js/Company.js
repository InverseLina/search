/**
 * View: Company
 *
 * Company
 *
 *
 */
(function ($) {
    var searchDao = app.SearchDaoHandler;
    brite.registerView("Company", {emptyParent: true},
        {
            create: function (data, config) {
                return render("Company");
            },

            postDisplay: function (data) {
                var view = this;
                var companyLimit = app.preference.get("company",app.defaultMenuSize);
                searchDao.getAdvancedMenu({limit:companyLimit,type:"company"}).always(function (result) {
                    var html = render("Company-detail", result || {});
                    view.$el.append(html);
                    updateResultInfo.call(view, result);
                });
            },
            
            updateSearchValues:function(data){
                var view  = this;
                var update = function(){
                    if(data.companyNames){
                        $.each(data.companyNames.split(","), function(idx,item){
                            view.$el.find("li[data-name='" + item + "'] input").prop("checked", true);
                            view.$el.find("li[data-name='" + item + "']").addClass("selected")
                        })
                    }
                    if(data.curCompany){
                        view.$el.find("input[name='curCompany']").prop("checked", true);
                    }
                    if(data.searchCompany){
                        view.$el.find('input[type=text]').val(data.searchCompany);
                    }
                };
                if(view.$el.find(".company").length > 0){
                    update();
                }else{
                    setTimeout(update, 500);
                }
            },
            
            getSearchValues:function(){
                var view = this;
                var $e = view.$el;
                var $companyContainer = $e.find("ul.companyList");
                var companyALL = $companyContainer.find("li[data-name='ALL']").hasClass("selected");
                var companyStr = "";
                // get companies filter
                $companyContainer.find("li[data-name][data-name!='ALL']").find("input[type='checkbox']:checked").each(function(i) {
                    var value = $(this).closest("li").attr("data-name");
                    //get selected or all option is selected.
                        if (companyStr.length != 0) {
                            companyStr += ",";
                        }
                        companyStr += value;
                });

                var result = {};


                var searchCompany = view.$el.find('input[type=text]').val();
                if(!/^\s*$/.test(searchCompany)){
                    result.searchCompany = searchCompany;
                }
                var curCompany = view.$el.find("input[name='curCompany']").prop("checked");
                if(curCompany){
                    result.curCompany = curCompany;
                }

                if(!/^\s*$/.test(companyStr)){
                    result.companyNames = companyStr;
                }


                return result;
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
                
                "change; input[name='curCompany']" : function(event) {
                    var view = this;
                    view.$el.trigger("DO_SEARCH");
                },
                
                "btap; li[data-name] label" : function(event) {
                    var view = this;
                    //left click
                    if (event.which == 1) {
                        var $li = $(event.target).closest("li");
                        var $ul = $li.parent("ul");
                        if ($li.hasClass("all")) {
                            $("li:gt(1)", $ul).removeClass("selected").find(":checkbox").prop("checked", false);
                        } else {
                            $("li:eq(1)", $ul).removeClass("selected").find(":checkbox").prop("checked", false);
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
                    var type = "company";
                    var dataName = "companies";
                    // show more items
                    if (flag == "more") {
                        // get advanced menu data from server
                        searchDao.getAdvancedMenu({
                            type : type,
                            offset : app.preference.get(type, app.defaultMenuSize),
                            limit : 20
                        }).pipe(function(data) {
                                updateResultInfo.call(view, data);
                                $li.before(render("Company-add", data));
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
                        var $hideLi = $("li:not('.btns'):gt(" + (itemNum - hideNum+2) + ")", $ul);
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
                    var companies = result.companies || [];
                    $e.find("li .validCount").show().html("0/");

                    for(var i = 0; i < companies.length; i++){
                        var obj = companies[i];
                        $e.find(".company li[data-name='"+obj.name+"'] .validCount").html(obj.count+"/");
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
                  qParams.type = "company";
                  
                  if (result.count > 0) {
                    searchDao.getGroupValuesForAdvanced(qParams).done(function(result){
                      var companies = result.list || [];
                      
                      $e.find("li .validCount").show().html("0/");
                      
                      for(var i = 0; i < companies.length; i++){
                        var obj = companies[i]; 
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

        var duration = result.duration, companies = result.companies, callDuration = result.companyDuration;

        if (companies && companies.length) {
            //except the no company
            count = count + companies.length - 1;
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
