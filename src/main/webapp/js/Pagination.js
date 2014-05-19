/**
 * View: Pagination
 *  param: pageIdx int current page no
 *  param: pageSize int
 *  param: totalCount int
 *  param: callback callback fuction
 *
 */
(function($) {
	brite.registerView("Pagination", {
		emptyParent : true
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			var dfd = $.Deferred();
			var $el = $(render("Pagination"));
			if (data.totalCount > 0) {
				var view = this;
				var page = view.page = {
					pageIdx : data.pageIdx || 1,
					pageSize : data.pageSize || 15,
					totalCount : data.totalCount,
					callback : data.callback
				};
				calc(view.page);
				var html = render("Pagination-detail", page);
				$el.empty().append(html);
				$el.find("select").val(page.pageSize);
			}
			return dfd.resolve($el).promise();
		},
		postDisplay:function(){
			var view = this;
			if(view.page){
				var pageSize =  view.page.pageSize;
				setSelectValue.call(view, pageSize);
			}
		},
		show : function() {
			this.$el.show();
		},
		hide : function() {
			this.$el.hide();
		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"click; a[data-page]" : function(event) {
				event.stopPropagation();
				var view = this;
				var newpageIdx = $(event.currentTarget).attr("data-page");
				view.page.callback(newpageIdx, view.page.pageSize);
			},
			"click; li:not(.disabled) a.first" : function(event) {
				event.stopPropagation();
				var view = this;
				var page = view.page;
				view.page.callback(1, view.page.pageSize);
			},
			"click; li:not(.disabled) a.next" : function(event) {
				event.stopPropagation();
				var view = this;
				var page = view.page;
				if(page.pageIdx < page.pageCount){
					view.page.callback(page.pageIdx + 1, view.page.pageSize);
				}	
			},
			"click; li:not(.disabled) a.prev" : function(event) {
				event.stopPropagation();
				var view = this;
				var page = view.page;
				if(page.pageIdx > 1){}
				view.page.callback(page.pageIdx - 1, view.page.pageSize);
			},
			"click; li:not(.disabled) a.last" : function(event) {
				event.stopPropagation();
				var view = this;
				var page = view.page;
				view.page.callback(page.pageCount, view.page.pageSize);
			},			
			"click; .pageSelect .option" : function(event) {
				event.stopPropagation();
				var view = this;
				var $e = view.$el;
				var page = view.page;
				var pageSize = $(event.target).attr("data-value");
				
				if (pageSize >= -1) {
					page.pageSize = pageSize;
				}
				view.page.callback(1, view.page.pageSize);
			},
			"click; .pageSelect" : function(event) {
				event.stopPropagation();
				var view = this;
				var $e = view.$el;
				var value = $e.find(".pageSelect .value").attr("data-value");
				$e.find(".pageSelect .options").show();
				$e.find(".pageSelect .options .option").show();
				$e.find(".pageSelect .options .option[data-value='"+value+"']").hide();
			}

		},
		docEvents:{
			"click":function(){
				var view = this;
				view.$el.find(".pageSelect .options").hide();
			}
		}
		// --------- /Events--------- //
	});

	// --------- Private Methods--------- //
	// process the page info
	function calc(page) {
		page.pageCount = parseInt(page.totalCount / page.pageSize);
		if (page.totalCount % page.pageSize !== 0) {
			page.pageCount += 1;
		}
	}

	//redender and show page info
	function renderPage(pageIdx, pageSize, totalCount, callback) {
		var view = this;
		if (view.$el) {
			var page = view.page = {
				pageIdx : pageIdx,
				pageSize : pageSize,
				totalCount : totalCount,
				callback : callback
			};
			calc(view.page);
			var html = render("Pagination-detail", page);
			view.$el.empty().append(html);
			view.$el.find("select").val(page.pageSize);
		}
	}

	function setSelectValue(pageSize){
		var view = this;
		var $e = view.$el;
		if($e.find(".pageSelect .option[data-value='"+pageSize+"']").size() == 0){
			pageSize = $e.find(".pageSelect .option:first").attr("data-value");
		}
		$e.find(".pageSelect .value").html(pageSize).attr("data-value",pageSize);
	}
	// --------- /Private Methods--------- //

})(jQuery);
