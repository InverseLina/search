/**
 * View: AutoComplete
 *
 * AutoComplete component
 *
 *
 */
(function ($) {
    var borderKey = { UP: 38, DOWN: 40, TAB: 9, ESC: 27, ENTER: 13 };
    var suggest = {};
    var defaults = {
        url: null,
        params: null,
        delay: 100,
        cache: false,
        formId: '#search_form',
        focus: null,
        callback: true
    };
    brite.registerView("AutoComplete", {emptyParent: false},
        {
            create: function (data, config) {
                //return render("AutoComplete");
            },

            postDisplay: function (opts) {

                var view = this;
                //this.data = data || [];
                var options = $.extend({}, defaults, borderKey, opts || {});

                view.delay = options.delay || 300;
                view.cache = options.cache||false;
                view.focus = options.focus;
                view.$rebox = $('<ul/>').attr("id", "suggest").addClass("dropdown-menu");
                view.htmlLi = null;
                view.litop = null;
                view.lileft = null;
                view.liwth = null;
                view.tip = false;
                view.val = null;
                view.rlen = null;
                view.index = -1;
                view.choseKey = null;
                view.backval = null;
                view.hidden = false;
                view.locksuggest = false;
                view.$input = view.$el.find("input");
                view.list = options.list;
                view.$el.append(view.$rebox);
                view.$rebox.css("display:none");
                console.log(view);

            },
            events: {
                "keyup; input": function (event) {
                    var view = this;
                    setTimeout(function(){
                        getKey.call(view);
                    }, view.delay);
                },
                "blur; input": function () {
                    var view = this;
                    setTimeout(function(){
                        hideResult.call(view);
                    },300);

                },
                "mouseover; li":function(event){
                    var view = this;
                    var $li =  $(event.currentTarget);
                    view.$el.find("li").removeClass("active");
                    $li.addClass("active");
                },
                "click; li":function(event){
                    var view = this;
                    var val = $(event.currentTarget).html();
                    view.$input.val(val);

                },
                "keydown; input":function(e){
                    var view = this;
                    if (e.keyCode == borderKey.UP) {
                        if (view.$rebox.css('display') == 'none') {
                            reSet.call(view);
                            return false;
                        }
                        view.index--;
                        if (view.index < 0) {
                            view.index = Math.abs(view.rlen) - 1;
                        }
                        console.log(view.index);

                        changeSelect.call(view,false);
                        e.preventDefault();
                        return false;
                    } else if (e.keyCode == borderKey.DOWN) {
                        console.log("down");
                        if (view.$rebox.css('display') == 'none') {
                            console.log("XXXXX");
                            reSet.call(view);
                            return false;
                        }
                        view.index++;
                        if (view.index >= view.rlen) {
                            view.index = 0;
                        }
                        changeSelect.call(view, false);
                        e.preventDefault();
                        return false;
                    } else if (e.keyCode == borderKey.TAB) {
                        hideResult.call(view);
                    } else if (e.keyCode == borderKey.ESC) {
                        hideResult.call(view);
                        return false;
                    } else if (e.keyCode == borderKey.ENTER) {
                        //
                    }
                }
            },
            docEvents: {}
        });
    //get search key
    function getKey() {
        var view = this;
        var val = $.trim(view.$input.val());

            getResult.call(view, val,  function (htmltemp, htmllen) {
                view.index = -1;
                view.rlen = htmllen;
                appendSuggest.call(view, htmltemp);
            });


        // 关键词为空
        if (!val && !view.hidden) {
            hideResult.call(view);
        }
    }

    // 获取提示数据

    function getResult(inputWord,   fn) {
        var view = this;
            var htmltemp = '', htmllen = 0;

            $.each(view.list, function (i, word) {
                if (startWith(word, inputWord)) {
                    htmltemp += '<li>' + word + '</li>';
                    htmllen++;
                }
            });
            fn.call(document, htmltemp, htmllen)

    }

    // 插入提示数据

    function appendSuggest(result) {
        var view = this;
        var $input = view.$input;
        view.locksuggest = view.hidden = false;
        if (!!result) {
            console.log(view);
            if (!view.tip) {
                view.litop = $input.offset().top + $input.outerHeight() - 1;
                view.lileft = $input.offset().left;
                view.liwth = $input.outerWidth() - 2;
                view.$rebox.css({
                    'position': 'absolute',
                    'top': view.litop,
                    'left': view.lileft,
                    'width': view.liwth,
                    'display':"block"
                }).html(result).show();
                view.tip = true;
            } else {
                view.$rebox.html(result).show();
            }
            view.$rebox.show();
        } else {
            // 如果检索结果为空，清空提示层
            view.$rebox.hide();
        }
    }

    function changeSelect(v) {
        var view = this;
        v = v == false ? false : true;
        console.log(view.index);
        var obj = view.$rebox.find('li').eq(view.index);
        view.$rebox.find('li.active').removeClass('active');
        obj.addClass("active");
        if (v) {
            view.choseKey = view.backval = obj.html();
            view.$input.val(view.choseKey);
        }
    }

    function reSet() {
        var view = this;
        if (!!view.$input.val()) {
            view.index = -1;
            view.$rebox.css('display', 'block');
            view.$rebox.find('li.active').removeClass('active');
            view.rlen = view.$rebox.find('li').size();
        }
    }

    function hideResult() {
        var view = this;
/*        if (!view.locksuggest) {
            console.log("hide");
            view.choseKey = view.backval = null;*/
            view.hidden = true;
            view.$rebox.hide();
       // }
    }

    function searchSubmit() {
/*        self.val(choseKey);
        hideResult();
        clearInterval(searchtimer);
        formobj.submit();*/
    }
     startWith=function(key, str){
        var reg=new RegExp("^"+str);
        return reg.test(key);
    }

})(jQuery);
