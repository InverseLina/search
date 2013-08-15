/**
 * View: AutoComplete
 *
 * AutoComplete component
 *
 *
 */
(function ($) {
    var borderKey = { UP: 38, DOWN: 40, TAB: 9, ESC: 27, ENTER: 13 };

    brite.registerView("AutoComplete", {emptyParent: false},
        {
            create: function (data, config) {
                //return render("AutoComplete");
            },

            postDisplay: function (opts) {

                var view = this;
                //this.data = data || [];
                var options = $.extend({}, borderKey, opts || {});

                view.delay = options.delay || 300;
                view.$rebox = $('<ul/>').addClass("dropdown-menu");
                view.$input = view.$el.find("input");
                view.list = options.list;
                view.$el.append(view.$rebox);
                view.$rebox.css("display:none");

            },
            events: {
                "focus; input": function(){
                    var view = this;
                    showClear.call(view);
                    setTimeout(function () {
                        getKey.call(view);
                    }, view.delay);
                },
                "blur; input": function () {
                    var view = this;
                    showClear.call(view);
                    setTimeout(function () {
                        hideResult.call(view);
                    }, 200);

                },
                "mouseover; li": function (event) {
                    var view = this;
                    var $li = $(event.currentTarget);
                    view.$el.find("li").removeClass("active");
                    $li.addClass("active");
                },
                "click; li": function (event) {
                    var view = this;
                    var val = $(event.currentTarget).html();
                    view.$input.val(val);
                },
                "keyup; input": function(){
                    var view = this;
                    showClear.call(view);
                },
                "keydown; input": function (e) {
                    var view = this;
                    if (e.keyCode == borderKey.UP) {
                        if (view.$rebox.css('display') == 'none') {
                            reset.call(view);
                            return false;
                        }
                        view.index--;
                        if (view.index < 0) {
                            view.index = Math.abs(view.rlen) - 1;
                        }

                        changeSelect.call(view, true);
                        e.preventDefault();
                        return false;
                    } else if (e.keyCode == borderKey.DOWN) {
                        if (view.$rebox.css('display') == 'none') {
                            reset.call(view);
                            return false;
                        }
                        view.index++;
                        if (view.index >= view.rlen) {
                            view.index = 0;
                        }
                        changeSelect.call(view, true);
                        e.preventDefault();
                        return false;
                    } else if (e.keyCode == borderKey.TAB) {
                        hideResult.call(view);
                    } else if (e.keyCode == borderKey.ESC) {
                        hideResult.call(view);
                        return false;
                    } else if (e.keyCode == borderKey.ENTER) {
                        var val = view.$rebox.find("li.active").html();
                        view.$input.val(val);
                        hideResult.call(view);
                    } else{
                        setTimeout(function () {
                            getKey.call(view);
                        }, view.delay);
                    }
                },
                "btap; span":function(){
                    var view = this;
                    view.$input.val("");
                    showClear.call(view);
                },
                "focusout; input":function(){
                    var view = this;
                    showClear.call(view);
                }
            },
            docEvents: {}
        });

    //get search key
    function getKey() {
        var view = this;
        var val = $.trim(view.$input.val());

        getResult.call(view, val);

        if (!val || view.rlen <=0) {
            hideResult.call(view);
        }
    }


    function getResult(inputWord, fn) {
        var view = this;
        var htmltemp = '', htmllen = 0;

        $.each(view.list, function (i, word) {
            if (contains(word, inputWord)) {
                htmltemp += '<li>' + word + '</li>';
                htmllen++;
            }
        });
        var $input = view.$input;
        view.litop = $input.offset().top + $input.outerHeight() - 1;
        view.lileft = $input.offset().left;
        view.liwth = $input.outerWidth() - 2;
        view.$rebox.css({
            'position': 'absolute',
            'top': view.litop,
            'left': view.lileft,
            'width': view.liwth,
            'display': "block"
        }).html(htmltemp).show();

        view.index = -1;
        view.rlen = htmllen;

    }


    function changeSelect(change) {
        var view = this;
        change = change == false ? false : true;
        var obj = view.$rebox.find('li').eq(view.index);
        view.$rebox.find('li.active').removeClass('active');
        obj.addClass("active");
        if (change) {
            view.choseKey = view.backval = obj.html();
            view.$input.val(view.choseKey);
        }
    }

    function reset() {
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
        view.hidden = true;
        view.$rebox.hide();
    }

    startWith = function (key, str) {
        var reg = new RegExp("^" + str);
        return reg.test(key);
    }
    contains = function (key, str) {
        var reg = new RegExp(str);
        return reg.test(key);
    }

    function showClear(){
        var view = this;
        var val = view.$input.val();
        if(!/^\s*$/g.test(val)){
            console.log("add class");
            if(!view.$el.hasClass("clear")){
                view.$el.addClass("clear");
            }
        } else{
            console.log("remove class")
            if(view.$el.hasClass("clear")){
                view.$el.removeClass("clear");
            }
        }
    }

})(jQuery);
