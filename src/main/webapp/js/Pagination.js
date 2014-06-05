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
			return $(render("Pagination"));
		},
		postDisplay:function(){
			var view = this;
			
			refresh.call(view);
			
			if(view.page){
				var pageSize =  view.page.pageSize;
				setSelectValue.call(view, pageSize);
			}
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
				view.page.callback(page.pageIdx + 1, view.page.pageSize);
			},
			"click; li:not(.disabled) a.prev" : function(event) {
				event.stopPropagation();
				var view = this;
				var page = view.page;
				if(page.pageIdx > 1){
					view.page.callback(page.pageIdx - 1, view.page.pageSize);
				}
			},
			"click; li:not(.disabled) a.last" : function(event) {
				event.stopPropagation();
				var view = this;
				var page = view.page;
				if(view.exactCount){
					view.page.callback(page.pageCount, view.page.pageSize);
				}else{
					view.page.callback(page.pageIdx + 1, view.page.pageSize);
				}
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
				setSelectValue.call(view, page.pageSize);
				$e.trigger("PAGE_SIZE_CHANGE", page.pageSize);
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
			},
			"REFRESH_PAGINATION":function(view,extra){
				var view = this;
				refresh.call(view, extra);
			}
		}
		// --------- /Events--------- //
	});

	// --------- Private Methods--------- //
	
	function refresh(data){
		var view = this;
		var $e = view.$el;
		data = data || {};
		view.exactCount = data.exactCount || false;
		view.page = view.page || {};
		var page = {
			pageIdx : data.pageIdx || 1,
			pageSize : data.pageSize || 15,
			totalCount : data.totalCount || 0,
			hasNextPage : data.hasNextPage || false,
			callback : data.callback || function() {},
		};
		
		view.page = page;
		calc(view.page);
		var html = render("Pagination-detail", page);
		$e.empty().append(html);
		setSelectValue.call(view,page.pageSize);
	}
	
	// process the page info
	function calc(page) {
		page.pageCount = parseInt(page.totalCount / page.pageSize);
		if (page.totalCount % page.pageSize !== 0) {
			page.pageCount += 1;
		}
	}

	function setSelectValue(pageSize){
		var view = this;
		var $e = view.$el;
		if($e.find(".pageSelect .option[data-value='"+pageSize+"']").size() == 0){
			pageSize = $e.find(".pageSelect .option:first").attr("data-value");
		}
		$e.find(".pageSelect .value").html(pageSize).attr("data-value",pageSize);
		$e.find(".pageSelect .options").hide();
	}
	
	// --------- /Private Methods--------- //

})(jQuery);
