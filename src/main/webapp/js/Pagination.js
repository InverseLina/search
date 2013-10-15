/**
 * View: Pagination
 *  param: pageIdx int current page no
 *  param: pageSize int
 *  param: totalCount int
 *  param: callback callback fuction
 *
 */
(function ($) {
    brite.registerView("Pagination", {emptyParent: true},
        {
            // --------- View Interface Implement--------- //
            create: function (data, config) {
                var dfd = $.Deferred();
                var $el = $(render("Pagination"));
                if (data.totalCount > 0) {
                    var view = this;
                    var page = view.page = {pageIdx: data.pageIdx||1, pageSize: data.pageSize||10, totalCount: data.totalCount, callback: data.callback};
                    calc(view.page);
                    var html = render("Pagination-detail", page);
                    $el.empty().append(html);
                    $el.find("select").val(page.pageSize);
                }
                return dfd.resolve($el).promise();
            },
            // --------- /View Interface Implement--------- //


            // --------- Events--------- //
            events: {
                "btap; a[data-page]": function (event) {
                    event.stopPropagation();
                    var view = this;
                    var newpageIdx = $(event.currentTarget).attr("data-page");
                    view.page.callback(newpageIdx, view.page.pageSize);
                },
                "btap; a.next": function (event) {
                    event.stopPropagation();
                    var view = this;
                    var page = view.page;
                    view.page.callback(page.pageIdx + 1, view.page.pageSize);
                },
                "btap; a.prev": function (event) {
                    event.stopPropagation();
                    var view = this;
                    var page = view.page;
                    view.page.callback(page.pageIdx - 1, view.page.pageSize);
                },
                "change; select": function (event) {
                    event.stopPropagation();
                    var view = this;
                    var page = view.page;
                    var pageSize = $(event.target).val();
                    if (pageSize >= -1) {
                        page.pageSize = pageSize;

                    }
                    view.page.callback(1, view.page.pageSize);
                }
            }
            // --------- /Events--------- //
        });

    // --------- Private Methods--------- //
    // process the page info
    function calc(page) {
        page.pageCount = parseInt(page.totalCount / page.pageSize);
        if (page.totalCount % page.pageSize != 0) {
            page.pageCount += 1;
        }
        if (page.pageIdx > 5) {
            page.start = page.pageIdx - 1;
        } else {
            page.start = 1;
        }

        page.end = page.pageIdx + 3;
        if (page.end > page.pageCount) {
            page.end = page.pageCount;
        }
        if (page.end < page.pageCount - 2) {
            page.end_2 = page.pageCount - 2;
        }
        if (page.end < page.pageCount - 1) {
            page.end_1 = page.pageCount - 1;
        }
    }

    //redender and show page info
    function renderPage(pageIdx, pageSize, totalCount, callback) {
        var view = this;
        if (view.$el) {
            var page = view.page = {pageIdx: pageIdx, pageSize: pageSize, totalCount: totalCount, callback: callback};
            calc(view.page);
            var html = render("Pagination-detail", page);
            view.$el.empty().append(html);
            view.$el.find("select").val(page.pageSize);
        }
    }

    // --------- /Private Methods--------- //

})(jQuery);
