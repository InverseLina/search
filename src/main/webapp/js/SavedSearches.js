/**
 * View: SavedSearches
 *
 *
 *
 *
 */
(function($) {
	var dao = app.SavedSearchesDaoHandler;
	brite.registerView("SavedSearches", {
		parent : ".saveSearchesContainer",
		emptyParent : true
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			var dfd = $.Deferred();
			var item= [];
			data =[];
			var $e = $(render("SavedSearches"));
			dfd.resolve($e);
			return dfd.promise();
		},

		postDisplay : function(data) {
			var width, height, pos, view = this;
			$(document).on("btap." + view.cid, function(event) {
				var $ul = view.$el.find("ul:visible");
				if ($ul.length > 0) {
					width = $ul.outerWidth();
					height = $ul.outerHeight();
					pos = $ul.offset();
					if (event.pageX > pos.left && event.pageY < pos.left + width && event.pageY > pos.top && event.pageY < pos.top + height) {
						//do nothing
					} else {
						$ul.hide();
					}
				}
			});
		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"SEARCH_RESULT_CHANGE" : function() {
				var view = this;
				checkAndChangeBtnState.call(view, true);
			},
			"keyup; input" : function() {
				var view = this;
				checkAndChangeBtnState.call(view);
			},
			"click; .btn" : function(event) {
				event.stopPropagation();
				var query, view = this;
				var $btn = $(event.currentTarget);
				var searchName = $.trim(view.$el.find("input").val());
				if ($btn.hasClass("update")) {
					searchName = searchName.substring(0, searchName.length - 2);
					view.$el.find("input").val(searchName);
					$btn.removeClass("update");
					$btn.text("Save");
				}
				enableBtn(view, false);
				view.$el.find("input").focus();
				var content = {};
				query = getSearchQuery(view);
				if (query !== "") {
					content.query = query;
				}
				if (hasSearchFilter()) {
					content.filters = app.ParamsControl.getFilterParams();
				}
				dao.save(searchName, JSON.stringify(content));
			},
			"click; .drawdown" : function(event) {
				var view = this;
				event.stopPropagation();
				event.preventDefault();
				dao.list().done(function(result) {
					var html = render("SavedSearches-list", {
						result : result
					});
					view.$el.find(".search-list").empty().html(html).show();
				});
			},
			"click; .createNew" : function(event) {
				event.stopPropagation();
				event.preventDefault();
				var view = this;
				var $input = view.$el.find("input");
				getNewName().done(function(newName) {
					$input.val(newName).select().focus();
					$input.trigger("SEARCH_QUERY_CHANGE");
					view.$el.find(".search-list").hide();
				});

			},
			"click; li .remove i" : function(event) {
				var view = this;
				event.stopPropagation();
				event.preventDefault();
				var $li = $(event.currentTarget).closest("li");
				var id = $li.attr("data-objId");
				if (id) {
					dao.del(id).done(function() {
						$li.remove();
						view.$el.trigger("SEARCH_QUERY_CHANGE");
					});
				}
			},
			"click; li[data-objId]" : function(event) {
				var view = this;
				var $li = $(event.currentTarget);
				var id = $li.attr("data-objId");
				var $input = view.$el.find("input");
				var contentView = view.$el.bView("ContentView");
				dao.get(id).done(function(result) {
					if (result) {
						$input.val(result.name).focus().select();
						enableBtn(view, false);
						view.$el.find(".search-list").hide();
						//todo should restore params
						var search = JSON.parse(result.search);
						contentView.$el.find(".search-form .search-input").val(search.query || "");
						app.ParamsControl.setFilterParams(search.filters);
						$input.trigger("DO_SEARCH");
					}
				});
			},
			"mouseleave; ul" : function() {
				var view = this;
				view.$el.find(".search-list").hide();
			}

		},
		// --------- /Events--------- //

		// --------- Document Events--------- //
		docEvents : {
			"SEARCH_QUERY_CHANGE ADD_FILTER REMOVE_FILTER" : function() {
				var view = this;
				checkAndChangeBtnState.call(view);
			}

		}
		// --------- /Document Events--------- //
	});

	// --------- Private Methods--------- //
	function enableBtn(view, status) {
		var $btn = view.$el.find(".btn");
		if (status) {
			$btn.removeClass("disabled");
		} else {
			if (!$btn.hasClass("disabled")) {
				$btn.addClass("disabled");
			}
			$btn.removeClass("update");
			$btn.text("Save");

		}
	}

	function getSearchQuery(view) {
		var contentView = $(".ContentView").bView();
		return $.trim(contentView.$el.find(".search-form .search-input").val());
	}

	function hasSearchFilter() {
		var filters = app.ParamsControl.getFilterParams();
		var hasFilter = false;
		for (var key in filters) {
			if (filters[key].length > 0) {
				hasFilter = true;
				break;
			}
		}
		return hasFilter;
	}

	function checkAndChangeBtnState(justSearch) {
		justSearch = justSearch || false;
		var view = this;
		view.$el = $(this.el);
		var $btn = view.$el.find(".btn");
		var $input = view.$el.find("input");

		var query = getSearchQuery(view);
		var searchName = $.trim(view.$el.find("input").val());

		var hasName = searchName.length > 0;
		var isUpdate = $btn.hasClass("update");
		if (/\s{1}\*$/g.test(searchName)) {
			searchName = searchName.substring(0, searchName.length - 2);
		}

		var hasFilter = hasSearchFilter();

		if (!hasName) {
			enableBtn(view, false);
		} else {
			if (hasFilter || query !== "") {
				dao.count(searchName).done(function(count) {
					if (count > 0) {
						if (justSearch) {
							if (isUpdate) {
								enableBtn(view, true);
							} else {
								enableBtn(view, false);
							}
						} else {
							enableBtn(view, true);
							if (!isUpdate) {
								$btn.addClass("update");
								$btn.text("Update");
								view.$el.find("input").val(searchName + " *");
							}
						}
					} else {
						if (isUpdate) {
							$btn.removeClass("update");
							$btn.text("Save");
							$input.val(searchName);
						} else {
							enableBtn(view, true);
						}
					}
				});

			} else {
				enableBtn(view, false);
			}
		}
	}

	function getNewName() {
		var baseName = "My search";
		var dfd = $.Deferred();
		var searchName;
		var checkExist = function(dfd, index) {
			searchName = baseName + " " + index;
			dao.count(searchName).done(function(count) {
				if (count === 0) {
					dfd.resolve(searchName);
				} else {
					index++;
					checkExist(dfd, index);
				}
			});
			return dfd.promise();
		};
		return checkExist(dfd, 1);

	}

	// --------- /Private Methods--------- //

})(jQuery);
