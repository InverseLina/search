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
            show:function(){
            	this.$el.show();
            },
            hide:function(){
            	this.$el.hide();
            },
            // --------- /View Interface Implement--------- //


            // --------- Events--------- //
            events: {
                "click; a[data-page]": function (event) {
                    event.stopPropagation();
                    var view = this;
                    var newpageIdx = $(event.currentTarget).attr("data-page");
                    view.page.callback(newpageIdx, view.page.pageSize);
                },
                "click; a.next": function (event) {
                    event.stopPropagation();
                    var view = this;
                    var page = view.page;
                    view.page.callback(page.pageIdx + 1, view.page.pageSize);
                },
                "click; a.prev": function (event) {
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
        
        if(page.pageCount > 7){
	        page.start = page.pageIdx - 1;
	        page.end = page.pageIdx + 1;
	        if(page.end <= 5){
	        	page.start = 2;
	        	page.end = 5;
	        }else{
	        	page.startEllipsis = true;
	        }
	        
	        if(page.start >= page.pageCount - 4){
	        	page.start = page.pageCount - 4;
	        	page.end = page.pageCount - 1;
	        }else{
	        	page.endEllipsis = true;
	        }
	        
	        if(page.pageIdx <= 3){
	        	page.end_1 = page.pageCount - 1;
	        	page.end = page.end - 1;
	        }
        }else{
        	page.start = 2;
	        page.end = page.pageCount;
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
