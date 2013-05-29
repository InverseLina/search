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
            create: function (data, config){
                return render("Pagination");
            },

            postDisplay: function (data) {
                console.log(data);
                var view = this;
                if(data.totalCount>0){

                  renderPage.call(view, data.pageIdx||1, data.pageSize||10, data.totalCount, data.callback);
                }
            },
            events: {
              "click; a[data-page]":function(event){
                  var view = this;
                  var newpageIdx = $(event.currentTarget).attr("data-page");
                  view.page.callback(newpageIdx, view.page.pageSize);
              },
              "click; a.next":function(event){

                  var view = this;
                  var page = view.page;
                  view.page.callback(page.pageIdx + 1, page.pageSize);
              },
              "click; a.prev":function(event){
                  var view = this;
                  var page = view.page;
                  view.page.callback(page.pageIdx - 1, page.pageSize);
              },
              "change; select":function(event){
                  var view = this;
                  var page = view.page;
                  var pageSize = $(event.target).val();
                  if(pageSize >= -1){
                      page.pageSize = pageSize;
                      view.page.callback(1, page.pageSize);
                  }
              }
            },
            docEvents: {}
        });

        function calc(page) {
            page.pageCount = parseInt(page.totalCount/page.pageSize) ;
            if(page.totalCount % page.pageSize != 0 ) {
                page.pageCount += 1;
            }
            if(page.pageIdx > 5){
                page.start = page.pageIdx -1;
            }else{
                page.start = 1;
            }

            page.end = page.pageIdx + 3;
            if(page.end > page.pageCount){
                page.end =page.pageCount;
            }
            if(page.end < page.pageCount -2) {
                page.end_2 = page.pageCount - 2;
            }
            if(page.end < page.pageCount -1) {
                page.end_1 = page.pageCount - 1;
            }
//            console.log(page)
        }

    function renderPage(pageIdx, pageSize, totalCount, callback) {
        var view = this;
        var page = view.page = {pageIdx: pageIdx, pageSize: pageSize, totalCount: totalCount, callback: callback};
        calc(view.page);
        var html = render("Pagination-detail", page);
        view.$el.empty().append(html);
        view.$el.find("select").val(page.pageSize);
    }

})(jQuery);
