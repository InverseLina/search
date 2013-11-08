(function($) {
    var perfSearchDao = app.PerfDaoHandler;

    brite.registerView("PerfView", {
        parent : ".admincontainer",
        emptyParent : true
    }, {
        // --------- View Interface Implement--------- //
        create : function(data) {
            return render("PerfView");
        },
        postDisplay : function(data) {
            var view = this;
            view.section = app.pathInfo.paths[0] || "setup";

            view.$navTabs = $(".nav-tabs");
            view.$tabContent = view.$el.find(".tab-content");
            view.$navTabs.find("li.active").removeClass("active");
            if (view.$navTabs.find('li').size() > 3) {
                view.$navTabs.find('li:last').prev("li").remove();
            }
            view.$navTabs.find("a[href='#perf']").closest("li").addClass("active");
        },
        // --------- /View Interface Implement--------- //

        // --------- Events--------- //
        events : {
            "click;button.go" : function(event) {
                var view = this;
                var $button = $(event.target);
                var $allButton = view.$el.find(".all");
                $allButton.attr("disabled", "true");
                view.goAll = false;
                doSearch.call(view, $button);
            },
            "click; .all" : function(event) {
                var view = this;
                $("input[type=text]").each(function() {
                    if ($(this).val() == '') {
                        alert("Please enter all the value!");
                        return false;
                    }
                });
                view.$el.find(".perf-value").empty();
                var $buttons = view.$el.find(".go");
                var $allButton = view.$el.find(".all");
                $buttons.attr("disabled", "true");
                $allButton.attr("disabled", "true").addClass("running").html("Running...");
                view.goAll = true;
                doSearchAll.call(view, $buttons, 0);
            }
        }
        // --------- /Events--------- //

    });
    // --------- Private Methods --------- //

    function doSearch($button, $buttons) {
        var dfd = $.Deferred();
        var view = this;
        var $perfItem = $button.closest(".perf-item");
        var $perfValues = $button.closest(".perf-values");
        $button.attr("disabled", "true").addClass("running").html("Running...");
        var data = {};
        $perfValues.find("input").each(function() {
            var $input = $(this);

            if ($input.attr("name") != "q_search") {
                var datas = [];
                var opt = {};
                opt = {
                    name : $.trim($input.val())
                };
                datas.push(opt);
                data[$input.attr("name")] = datas;
            } else {
                data["q_search"] = $input.val();
            }

        });
        var searchParameter = getSearchParameter(view, data);
        var methodName = $perfItem.attr("data-perf-method");
        if (methodName == 'autocomplete') {
            searchParameter.type = $button.attr("data-type");
            searchParameter.queryString = data["q_search"];
            searchParameter.orderByCount = true;
        }
        var perfPromise = perfSearchDao[methodName](searchParameter);

        var $s, $c, $match;
        if (methodName == 'autocomplete') {
            if ($button.hasClass("perf-left")) {
                $s = $perfItem.find(".left-items").find("[data-perf = 's']");
                $match = $perfItem.find(".left-items").find("[data-perf = 'match']");
            } else {
                $s = $perfItem.find(".right-items").find("[data-perf = 's']");
                $match = $perfItem.find(".right-items").find("[data-perf = 'match']");
            }
        } else {
            $s = $button.closest("div").next("div").find("[data-perf = 's']");
            $c = $button.closest("div").next("div").find("[data-perf = 'c']");
            $match = $button.closest("div").next("div").find("[data-perf = 'match']");
            $c.empty();
        }
        $s.empty();
        $match.empty();
        perfPromise.done(function(response) {
            if (methodName == 'autocomplete') {
                $s.html(response.duration + " ms");
                $match.html(response.count);
            } else {
                $c.html(response.countDuration + " ms");
                $s.html(response.duration + " ms");
                $match.html(response.count);
            }
            if (!view.goAll) {
                $button.removeAttr("disabled").removeClass("running").html("GO");
                var $btns = view.$el.find(".go");
                var flag = true;
                $btns.each(function() {
                    if ($(this).attr("disabled") == 'disabled') {
                        flag = false;
                    }
                });
                if (flag) {
                    view.$el.find(".all").removeAttr("disabled");
                }
            }
            dfd.resolve($buttons);
        });
        return dfd.promise();
    }

    function doSearchAll($buttons, index) {
        var view = this;
        var $btn = $($buttons[index]);
        doSearch.call(view, $btn, $buttons).done(function(data) {
            index++;
            $btn.removeAttr("disabled").removeClass("running").html("GO");
            if (data.length > index) {
                doSearchAll.call(view, data, index);
            } else {
                var $allButton = view.$el.find(".all");
                $allButton.removeAttr("disabled").removeClass("running").html("GO ALL");
            };
        });
    }

    function getSearchParameter(view, searchData) {
        var result = {};
        result.searchColumns = "contact,company,skill,education,location";
        result.searchValues = JSON.stringify(searchData);
        return result;
    }

    // --------- /Private Methods --------- //
})(jQuery); 