/**
 * View: GenericFilterView
 *
 *
 *
 *
 */
(function($) {
	var searchDao = app.SearchDaoHandler;
	var component = {
		create : function(data, config) {
			this.type = data.filterInfo.name;
			var html = render("GenericFilterView", data);
			return html;
		},

		postDisplay : function(data) {
			var item, html, displayName, view = this;
			var data = (data || {}).data || [];
			$.each(data, function(idx, val) {
				item = {
					name : val.name
				};
				val = val.value;
				if (val.minYears || val.minRadius) {
					item.min = val.minYears || val.minRadius;
				}
				var html = render("filterPanel-selectedItem-add", item);
				view.$el.find("span.add").before(html);

			});

			data = app.ParamsControl.get(view.type);
			if (data && data.length > 0) {
				showSPline.call(view, true);
			}

			if (view.afterPostDisplay) {
				view.afterPostDisplay();
			}
			if (view.$el.find(".sliderBarContainer").length > 0) {
				var opts = {
					max : 20
				};
				if (view.type ==="location") {
					opts.max = 100;
				} else if (view.type === "skill") {
					opts.max = 10;
				}
				brite.display("Slider", ".sliderBarContainer", opts).done(function(slider) {
					view.slider = slider;
				});
			}
			var $input = view.$el.find("input.autoComplete:first");
			$input.focus();
			view.lastQueryString = $input.val();
			var type = $input.attr("data-type");
			var listName = (type === "company" ? "companies" : (type + "s"));
			var params = JSON.parse(app.ParamsControl.getParamsForSearch().searchValues);
			delete params["q_" + listName];
			searchDao.getAutoCompleteData({
				"searchValues" : JSON.stringify(params),
				"type" : type,
				"orderByCount" : true
			}).always(function(result) {
				$input.closest(".rootFilterContainer").find(".autoCompleteList").html(render("filterPanel-autoComplete-list", {
					results : result["list"],
					type : type
				}));
				activeFirstItem.call(view);
				view.$el.bView("HeaderPopup").$el.find(".duration").text("" + (result.duration || 0) + "ms");
			});

		},

	};

	// --------- Private Methods--------- //
	function close() {
		var view = this;
		if (view && view.$el) {
			view.$el.bRemove();
			$(document).off("btap." + view.cid);
		}
	}

	function nextItem() {
		var $nextItem, view = this;
		var $item = view.$el.find(".contentText.active");
		if ($item.length > 0) {
			$nextItem = $item.closest("div").next("div").find(".contentText");
			if ($nextItem.length === 0) {
				$nextItem = view.$el.find(".contentText:first");
			}
		} else {
			$nextItem = view.$el.find(".contentText:first");
		}
		if ($nextItem.length > 0) {
			view.$el.find(".contentText").removeClass("active");
			$nextItem.addClass("active");
		}
		changeInput.call(view);

	}

	function activeFirstItem() {
		var view = this;
		if (view && view.$el) {
			view.$el.find(".contentText").removeClass("active");
			view.$el.find(".contentText:first").addClass("active");
		}
	}

	function prevItem() {
		var $nextItem, view = this;
		var $item = view.$el.find(".contentText.active");
		if ($item.length > 0) {
			$nextItem = $item.closest("div").prev("div").find(".contentText");
			if ($nextItem.length === 0) {
				$nextItem = view.$el.find(".contentText:last");
			}
		} else {
			$nextItem = view.$el.find(".contentText:last");
		}
		if ($nextItem.length > 0) {
			view.$el.find(".contentText").removeClass("active");
			$nextItem.addClass("active");
		}
		changeInput.call(view);
	}

	function changeInput() {
		var view = this;
		var $item = view.$el.find(".contentText.active");
		if ($item.length > 0) {
			view.$el.find("input:focus").val($item.attr("data-name")).change();
		}

	}

	function addItem(data) {
		var item, minValue = 0, view = this;
		var $dataItem = view.$el.find(".selectedItems .item[data-name='" + data + "']");
		var len = $dataItem.length;
		if (view.slider) {
			minValue = view.slider.getValue();
		}
		if (len === 0) {
			item = {
				type : view.type,
				name : data
			};
			if (minValue > 0) {
				if (view.type === "location") {
					item['minRadius'] = minValue;
				} else {
					item['minYears'] = minValue;
				}
				view.slider.reset();
			}
			view.$el.find(".selectedItems span.add").before(render("filterPanel-selectedItem-add", {
				name : data,
				min : minValue || ""
			}));
			view.$el.trigger("ADD_FILTER", item);
			view.$el.find("input").val("").focus().change();
			showSPline.call(view, true);
		} else {
			var obj = app.ParamsControl.get(view.type, data);
			var oldMinValue = obj.value.minRadius || obj.value.minYears || 0;
			if (oldMinValue !== minValue) {
				if (view.type === "location") {
					obj.value['minRadius'] = minValue;
				} else {
					obj.value['minYears'] = minValue;
				}
				var html = render("filterPanel-selectedItem-add", {
					name : data,
					min : minValue > 0 ? minValue : ""
				});
				$dataItem.html($(html).html());
				view.$el.trigger("UPDATE_FILTER");
			}
		}

	}

	function changeAutoComplete(event, keydown) {
		keydown = keydown || false;
		var $activeItem, view = this;
		var $input = view.$el.find("input.autoComplete:first");
		var type = $input.attr("data-type");
		//      var resultType = (type==="company")?"companies":(type+"s");
		var val = $input.val();
		//      var searchData;
		event.stopPropagation();
		if (!/^\s*$/.test(val)) {
			$input.closest("span.autoCompleteContainer").addClass("active");
		} else {
			$input.closest("span.autoCompleteContainer").removeClass("active");
		}

		switch (event.keyCode) {
			case borderKey.ENTER:
			case borderKey.TAB:
				if (keydown) {
					$activeItem = view.$el.find(".contentText.active");
					if ($activeItem.length === 1) {
						addItem.call(view, $activeItem.attr("data-name"));
					}
					setTimeout(function() {
						$input.focus();
					}, 200)

				}
				break;
			case borderKey.ESC:
				if (keydown) {
					close.call(view);
				}
				break;
			case borderKey.DOWN:
				if (keydown) {
					nextItem.call(view);
				}
				break;
			case borderKey.UP:
				if (keydown) {
					prevItem.call(view);
				}
				break;
			/*          case borderKey.RIGHT:
			 if(event.ctrlKey && view.slider){
			 view.slider.inc();
			 }
			 break
			 case borderKey.LEFT:
			 if(event.ctrlKey && view.slider){
			 view.slider.dec();
			 }
			 break;*/
			default:
				if (!keydown) {
					view.$el.trigger("SHOWSEARCHRESULT");
				}

		}

	}

	function showSPline(status) {
		var view = this;
		if (status) {
			view.$el.find(".separateLine").show();
		} else {
			view.$el.find(".separateLine").hide();
		}
	}


	brite.registerView("GenericFilterView", {
		emptyParent : true
	}, app.mixin(app.FilterViewMixIn(), component));
})(jQuery);
