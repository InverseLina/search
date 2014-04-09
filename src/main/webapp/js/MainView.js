(function($) {
	var searchDao = app.SearchDaoHandler;

	brite.registerView("MainView", {
		parent : "body"
	}, {
		// --------- View Interface Implement--------- //
		create : function(data) {
			data = data || {};
			var uiFlags = data.uiFlags = data.uiFlags || {};
			if ( typeof uiFlags.showHeader === "undefined") {
				uiFlags.showHeader = true;
			}
			return render("MainView", {
				jssVersion : jssVersion,
				contextPath : contextPath,
				showHeader : uiFlags.showHeader
			});
		},
		postDisplay : function(data) {
			var view = this;
			view.$el.find(".config").removeClass("hide");
			view.$el.find(".home").addClass("hide");

			if (app.cookie("userName")) {
				var userName = app.cookie("userName");
				if (userName) {
					userName = ":" + userName;
				} else {
					userName = "";
				}
				var $sfInfo = view.$el.find(".sf-info");
				$sfInfo.html((app.cookie("org") || " ") + userName.replace(/"/g, ""));
			}

			if (app.startError) {
				setTimeout(function() {
					view.$el.trigger("ERROR_PROCESS", app.startError);
				}, 100);
			} else {
				if (data && data.type === "admin") {
					brite.display("AdminMainView");
				} else {
					brite.display("ContentView", this.$el.find(".contentview-ctn")).done(function(contentView) {
						view.contentView = contentView;
					});
				}
			}

			app.MainView = view;
		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"click; [data-action='DO_SEARCH']" : function(e) {
				var view = this;
				var $this = $(e.currentTarget);
				var search;
				// if click by the search icon
				if ($this.hasClass("glyphicon-search")) {
					var $input = $this.closest(".input-wrapper").find("input[type='text']");
					search = $input.val();
				}
				if(!app.ParamsControl.isEmptySearch()){
					view.$el.trigger("DO_SEARCH", {
						search : search
					});
				}

			},
			//should trim the search value when focus out
			'focusout; input[type="text"]' : function(event) {
				var $target = $(event.currentTarget);
				$target.val($.trim($target.val()));
			},
			"click;.clear-all" : function(event) {
				clearSearch.call(this);
			},
			"click;.reSearch" : function(event) {
				$(document).trigger("DO_SEARCH",{retry:true});
			},
			"CHECK_CLEAR_BTN" : function() {
				var view = this;
				var isEmptySearch = app.ParamsControl.isEmptySearch();
				if (!isEmptySearch) {
					view.$el.find(".btnClearSearch").prop("disabled", false).removeClass("disabled");
				} else {
					view.$el.find(".btnClearSearch").prop("disabled", true).addClass("disabled");
				}
				view.$el.find(".btnAddToShotList").prop("disabled", true).addClass("disabled");
				view.$el.find(".btnApply").prop("disabled", true).addClass("disabled");
			}

		},
		// --------- /Events--------- //

		// --------- Document Events--------- //
		docEvents : {
			"DO_SET_COLUMNS" : function(event, extra) {
				var columns = ["contact", "company", "skill", "education", "location"];
				var colStr, view = this;
				if (extra.columns && extra.columns.length > 0) {
					colStr = extra.columns.join(",");
					$.each(columns, function(idx, column) {
						if (colStr.indexOf(column) < 0) {
							app.ParamsControl.remove(column === "contact" ? "Contact" : column);
						}
					});
					app.preference.columns(extra.columns);
					if(app.ParamsControl.isEmptySearch()){
						view.contentView.dataGridView.refreshColumns();
					}else{
						view.$el.trigger("DO_SEARCH");
					}
				}
			},
			"DO_SEARCH" : function(event, opts) {
				doSearch.call(this, opts);
				event.preventDefault();
			},
			"SEARCH_QUERY_CHANGE" : function(event) {
				this.$el.trigger("CHECK_CLEAR_BTN");
			},
			"CLEAR_SEARCH_QUERY" : function(event) {
				var view = this;
				clearSearch.call(view);
			},
			"ERROR_PROCESS" : function(event, data) {
				var view = this;
				if (data.errorCode === "NO_PASSCODE") {
					if ($("body").bFindComponents("PassCodeModal").length === 0) {
						brite.display("PassCodeModal");
					}
				} else if (data.errorCode === "NO_ADMIN_ACCESS") {
					if ($("body").bFindComponents("LoginModal").length === 0) {
						brite.display("LoginModal");
					}
				} else {
					brite.display("MessagePanel", ".contentview-ctn", {
						message : data.errorMessage
					});
				}
				delete app.startError;

			}

		}
		// --------- /Document Events--------- //
	});

	// --------- Private Methods--------- //
	function doSearch(opts) {
		var view = this;
		var $e = view.$el;
		opts = opts || {};
		var search = opts.search;
		if(!app.ParamsControl.isEmptySearch()){
			var searchParameter = app.ParamsControl.getParamsForSearch({
				search : search
			});
			var searchKey = app.ParamsControl.getQuery();
			var filters = app.ParamsControl.getFilterParams();
			if ($.trim(searchKey).length > 0 && $.trim(searchKey).length < 3) {
				view.contentView.dataGridView.showContentMessage("lessword");
				var labelAssigned = app.buildPathInfo().labelAssigned;
				if (labelAssigned) {
					view.$el.trigger("CHANGE_TO_FAV_VIEW");
				}
				return;
			}
			if(opts.retry){
				view.contentView.dataGridView.showContentMessage("retrying");
			}else{
				view.contentView.dataGridView.showContentMessage("loading");
			}
			view.contentView.dataGridView.restoreSearchParam();
	
			searchParameter.pageIndex = opts.pageIdx || 1;
			var searchTimes = 0;
			doSearchRequest.call(view, searchParameter, searchTimes);
		}else{
			view.contentView.dataGridView.showContentMessage("empty");
		}

	}
	
	function doSearchRequest(searchParameter, searchTimes){
		var view = this;
		searchTimes++ ;
		searchDao.search(searchParameter).done(function(result) {
			view.$el.trigger("CHECK_CLEAR_BTN");
			view.$el.trigger("SEARCH_RESULT_CHANGE", result);
		}).fail(function(result){
			if (searchTimes < 3) {
				doSearchRequest.call(view, searchParameter, searchTimes);
			} else {
				view.contentView.dataGridView.showContentMessage("error", {
					title : "Search Error",
					detail : "Server unavailable for the moment. <span class='reSearch btn btn-link'>Retry search</span>"
				});
			}
		});
	}

	function clearSearch() {
		var view = this;
		var $el = view.contentView.$el;
		app.ParamsControl.clear();
		app.preference.store("contact_filter_objectType", "All");
		app.preference.store("contact_filter_status", "All");
		view.$el.find(".contentview-ctn").bEmpty();
		brite.display("ContentView", this.$el.find(".contentview-ctn")).done(function(contentView) {
			view.contentView = contentView;
			view.$el.find(".clear-all").prop("disabled", true);
		});
	}

	// --------- /Private Methods--------- //

})(jQuery);
