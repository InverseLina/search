(function($) {
    var perfSearchDao = app.PerfDaoHandler;

    brite.registerView("PerfView", {
        parent : ".admincontainer",
        emptyParent : true
    }, {
        // --------- View Interface Implement--------- //
        create : function(data) {
            var dfd = $.Deferred()
            app.getJsonData("test/getOrgs").done(function(result){
                var html = render("PerfView", {orgs: result});
                dfd.resolve(html);
            })
            return dfd.promise();
        },
        postDisplay : function(data) {
            var view = this;
            view.section = app.pathInfo.paths[0] || "setup";

            view.$navTabs = $(".nav-tabs");
            view.$tabContent = view.$el.find(".tab-content");
            view.$navTabs.find("li.active").removeClass("active");

            view.$navTabs.find("a[href='#perf']").closest("li").addClass("active");
/*            perfSearchDao.checkStatus().done(function(result){
                if(!result){
                   view.$el.find('button').attr("disabled", true);
                }
            });*/
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
                goAllStart.call(view);
            }
        }
        // --------- /Events--------- //

    });
    // --------- Private Methods --------- //

    function goAllStart(){
        var view = this;
        var repeat = view.$el.find("input.repeat").val() * 1, delay = view.$el.find("input.delay").val() ;
        var items = new Array(repeat);

        brite.whenEach(items,function(value,index){
            // we create another deferred, because we need to wait the delay
            var dfd = $.Deferred();
            goAll.call(view).done(function(){
                setTimeout(function(){dfd.resolve();}, delay);
            });
            return dfd.promise();
        });
    }

    function goAll(){
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
        return doSearchAll.call(view, $buttons, 0);
    }

    function doSearch($button, $buttons) {
        var dfd = $.Deferred();
        var view = this;
        var $perfItem = $button.closest(".perf-item");
        var $perfValues = $button.closest(".perf-values");
        var org = view.$el.find("select[name='org']").val();
        $button.attr("disabled", "true").addClass("running").html("Running...");
        var obj, datas, data = {};
        $perfValues.find("span[data-group]").each(function(idx, group) {
            var $group = $(group);
            var groupName = $group.attr('data-group');
            if( groupName == 'q_search'){
                data["q_search"] = $.trim($group.find('input').val());
            }else{
                datas = [];
                obj = {};
                $group.find("input").each(function() {
                    var $input = $(this);
                    obj[$input.attr('name')] = $.trim($input.val());
                });
                datas.push(obj);
                data[groupName] = datas;
            }
        });

        var searchParameter = getSearchParameter(view, data);
        searchParameter.org = org;
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
        perfPromise.then(function(response) {
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
        }, function(response){
            $button.attr("disabled", true).removeClass("running").html("GO");
            view.$el.find(".all").removeAttr("disabled");
            dfd.reject($buttons);
        });
        return dfd.promise();
    }

    function doSearchAll($buttons, index) {
        var view = this;
        var dfdDone = $.Deferred();
        var $allButton = view.$el.find(".all");

        brite.whenEach($buttons,function(btn,index){
            // we create another deferred, because we need to wait the delay
            var $btn = $(btn)
            var dfd = $.Deferred();
            doSearch.call(view, $btn, $buttons).done(function(){
                $btn.removeAttr("disabled").removeClass("running").html("GO");
                dfd.resolve();
                if(index == $buttons.length -1){
                    $allButton.removeAttr("disabled").removeClass("running").html("GO ALL");
                    dfdDone.resolve();
                }

            });
            return dfd.promise();
        });

        return dfdDone.promise();

    }

    function getSearchParameter(view, searchData) {
        var result = {};
        result.searchColumns = "contact,company,skill,education,location";
        result.searchValues = JSON.stringify(searchData);
        return result;
    }

    // --------- /Private Methods --------- //
})(jQuery); 