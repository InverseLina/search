var app = app || {};
(function($) {
	app.FilterViewMixIn = function() {
		var searchDao = app.SearchDaoHandler;
		var borderKey = {
			LEFT : 37,
			RIGHT : 39,
			UP : 38,
			DOWN : 40,
			TAB : 9,
			ESC : 27,
			ENTER : 13
		};

		return {
			// --------- View Interface Implement--------- //
			create : function(data, config) {
				var $e, dfd = $.Deferred();
				var view = this;
				if (type === "company" || type === "education" || type === "skill" || type === "location") {
					$e = $(render(view.name, {}));
					dfd.resolve($e);
				} else {
					$e = $(render(view.name));
					dfd.resolve($e);
				}
				return dfd.promise();
			},
			postDisplay : function(data) {
				var item, html, displayName, view = this;
				data = (data || {}).data || [];
				$.each(data, function(idx, val) {
					item = {
						name : val.name
					};
					val = val.value;
					if (val.minYears || val.minRadius) {
						item.min = val.minYears || val.minRadius;
					}
					item.groupedid = val.groupedid;
					item.locationid = val.locationid;
					item.latitude = val.latitude;
					item.longitude = val.longitude;
					item.suffix = val.suffix;
					var html = render("filterPanel-selectedItem-add", item);
					view.$el.find("span.add").before(html);

				});

				var controlData = app.ParamsControl.get(view.type);
				if (controlData && controlData.length > 0) {
					showSPline.call(view, true);
				}

				if (view.afterPostDisplay) {
					view.afterPostDisplay();
				}
				if (view.$el.find(".sliderBarContainer").length > 0) {
					var opts = {
						max : 20,
						minLabel:"-"
					};
					if (view.type === "location") {
						opts.max = 100;
						opts.min = 1;
						opts.defaultValue = 10;
						opts.minLabel = null;
					}
					brite.display("Slider", ".sliderBarContainer", opts).done(function(slider) {
						view.slider = slider;
					});
				}
				var $input = view.$el.find("input.autoComplete:first");
				$input.focus();
				view.lastQueryString = $input.val();
				var type = $input.attr("data-type");
				if (view.type !== 'Contact') {
					var listName = (type === "company" ? "companies" : (type + "s"));
					var params = JSON.parse(app.ParamsControl.getParamsForSearch().searchValues);
					delete params["q_" + listName];
					
					var autoDfd = $.Deferred();
					
					if(type == 'location' || app.ParamsControl.isEmptySearch() || app.TopFilterItem.getAutoComplete(type).list.length == 0){
						searchDao.getAutoCompleteData({
							"searchValues" : JSON.stringify(params),
							"type" : type,
							"orderByCount" : true
						}).always(function(result){
							autoDfd.resolve(result);
						});
					}else{
						autoDfd.resolve(app.TopFilterItem.getAutoComplete(type));
					}
					
					autoDfd.done(function(result) {
						if (type === "company") {
							type = "employer";
						}
						$input.closest('.popover-content').find(".autoCompleteList").html(render("filterPanel-autoComplete-list", {
							results : result["list"],
							type : type
						}));
						activeFirstItem.call(view);
						$input.closest('.popover-content').next().text("" + (result.duration || 0) + "ms");
					});
				}
				$(":text").placeholder();
			},
			// --------- /View Interface Implement--------- //
			
			// --------- Events--------- //
			events : {
				"click; span.add" : function(event) {
					var view = this;
					var $span = $(event.target);
					var autoComplete = $span.attr("data-auto-complete");
					if (autoComplete) {
						$span.addClass("hide").next().removeClass("hide");
					} else {
						view.$el.find(".save").parent().removeClass("hide");
						$span.addClass("hide");
					}
				},
				"click; .autoCompleteContainer.active .clear" : function(event) {
					var view = this;
					var $input = $(event.currentTarget).closest(".autoCompleteContainer").find("input");
					$input.val("").focus().change();
					changeAutoComplete.call(view, event);
				},
				"change; .autoComplete" : function(event) {
					var view = this;
					var $input = $(event.currentTarget);
					var val = $input.val();
					event.stopPropagation();
					if (!/^\s*$/.test(val)) {
						$input.closest("span.autoCompleteContainer").addClass("active");
					} else {
						$input.closest("span.autoCompleteContainer").removeClass("active");
					}
				},
				"keyup;.autoComplete" : function(event) {
					var view = this;
					changeAutoComplete.call(view, event);
				},
				"keydown;.autoComplete" : function(event) {
					var view = this;
					changeAutoComplete.call(view, event, true);
				},
				"SHOWSEARCHRESULT" : function(event, params) {
					var view = this;
					var $input = view.$el.find("input.autoComplete:first");
					var type = $input.attr("data-type");

					var listName = (type === "company" ? "companies" : (type + "s"));
					var paramsObj = JSON.parse(app.ParamsControl.getParamsForSearch().searchValues);
					delete paramsObj["q_" + listName];
					var keyword = $.trim($input.val());
					var orderByCount = $.trim(keyword) === "" ? true : false;

					var searchCond = {
						"searchValues" : JSON.stringify(paramsObj),
						"type" : type,
						queryString : keyword,
						orderByCount : true
					};

					if (view.slider && view.slider.getValue() > 0) {
						searchCond.min = view.slider.getValue();
					}

					searchDao.getAutoCompleteData(searchCond).done(function(data) {
						if (view && view.$el) {
							if (data.queryString === $.trim($input.val())) {
								view.$el.find(".autoCompleteList").html(render("filterPanel-autoComplete-list", {
									results : data.list,
									type : type
								}));
								activeFirstItem.call(view);
								view.$el.bView("HeaderPopup").$el.find(" .duration").text("" + (data.duration || 0) + "ms");
							}
						}
					});

				},
				"click; div.content .autoCompleteList  div[class$='Row'][class!='contactRow']" : function(event) {
					var view = this;
					var $contentText = $(event.currentTarget).find(".contentText");
					var itemData = {
						name : $contentText.attr("data-name"),
					};
					
					if ($contentText.attr("data-groupedid") != "") {
						itemData.groupedid = $contentText.attr("data-groupedid") * 1;
					}
					
					if ($contentText.attr("data-locationid") != "") {
						itemData.locationid = $contentText.attr("data-locationid") * 1;
					}
					
					if ($contentText.attr("data-latitude") != "") {
						itemData.latitude = $contentText.attr("data-latitude") * 1;
					}
					
					if ($contentText.attr("data-longitude") != "") {
						itemData.longitude = $contentText.attr("data-longitude") * 1;
					}
					
					if($contentText.attr("data-suffix") != ""){
						itemData.suffix = $contentText.attr("data-suffix");
					}
					addItem.call(view, itemData);
					view.$el.find("input").focus();

				},
				"mouseover; div.content .autoCompleteList div[class$='Row'][class!='contactRow']" : function(event) {
					var view = this;
					view.$el.find("div.content div[class$='Row'][class!='contactRow'] span").removeClass("active");
					$(event.currentTarget).find("span").addClass("active");
				},
				"click; .selectedItems span.clear" : function(event) {
					event.preventDefault();
					event.stopPropagation();
					var view = this;
					var dataName = $(event.currentTarget).closest("span[data-name]").attr("data-name");
					setTimeout(function() {
						view.$el.find(".selectedItems span[data-name='" + dataName + "']").remove();
						if (view.$el.find(".selectedItems span[data-name]").length === 0) {
							showSPline.call(view, false);
							if (view.type === "Contact") {
								view.$el.find(".selectedItems").hide();
							}
						}
						view.$el.trigger("REMOVE_FILTER", {
							name : dataName,
							type : view.type
						});
					}, 200);
					view.$el.find("input:first").focus();

				},
				SLIDER_VALUE_CHANGE : function(event) {
					var view = this;
					changeAutoComplete.call(view, event);
				},
				"CHANGEAUTOCOMPLETE" : function(event) {
					var view = this;
					changeAutoComplete.call(view, event);
				}

			},
			// --------- /Events--------- //
			// --------- Public APIs--------- //
			getValue : function() {
				var datasName = this.type.substring(0, 1).toLocaleLowerCase() + this.type.substring(1) + "s";
				if (datasName === "companys") {
					dataName = "companies";
				}
				var data = {};
				var values = data[datasName] = [], value;
				var view = this;
				view.$el.find(".item").each(function() {
					value = $(this).data("value");
					values.push(value);
				});
				return data;
			}
			// --------- /Public APIs--------- //
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
				$nextItem = $item.parent().next("div").find(".contentText");
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
				$nextItem = $item.parent().prev("div").find(".contentText");
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
			var $dataItem = view.$el.find(".selectedItems .item[data-name='" + data.name + "']");
			var len = $dataItem.length;
			if (view.slider) {
				minValue = view.slider.getValue();
			}

			if (len === 0) {
				item = {
					type : view.type,
					name : data.name
				};
				if(data.groupedid){
					item.groupedid = data.groupedid;
				}
				if(data.locationid){
					item.locationid = data.locationid;
				}
				if(data.latitude){
					item.latitude = data.latitude;
				}
				if(data.longitude){
					item.longitude = data.longitude;
				}
				if(data.suffix){
					item.suffix = data.suffix;
				}
				if (minValue > 0) {
					if (view.type === "location") {
						item['minRadius'] = minValue;
					} else {
						item['minYears'] = minValue;
					}
					view.slider.reset();
				}
				view.$el.find(".selectedItems span.add").before(render("filterPanel-selectedItem-add", {
					groupedid : data.groupedid,
					locationid : data.locationid,
					longitude : data.longitude,
					latitude : data.latitude,
					name : data.name,
					suffix : data.suffix,
					min : minValue || ""
				}));
				view.$el.trigger("ADD_FILTER", item);
				view.$el.find("input").val("").focus().change();
				showSPline.call(view, true);
			} else {
				var obj = app.ParamsControl.get(view.type, data.name);
				var oldMinValue = obj.value.minRadius || obj.value.minYears || 0;

				if (oldMinValue !== minValue) {
					if (view.type === "location") {
						obj.value['minRadius'] = minValue;
					} else {
						obj.value['minYears'] = minValue;
					}
				}

				var html = render("filterPanel-selectedItem-add", {
					groupedid : data.groupedid,
					locationid : data.locationid,
					latitude : data.latitude,
					longitude : data.longitude,
					name : data.name,
					suffix : data.suffix,
					min : minValue > 0 ? minValue : ""
				});
				$dataItem.html($(html).html());
				view.$el.trigger("UPDATE_FILTER"); 

			}
			return false;
		}

		function changeAutoComplete(event, keydown) {
			keydown = keydown || false;
			var $activeItem, view = this;
			var $input = view.$el.find("input.autoComplete:first");
			var type = $input.attr("data-type");
			var val = $input.val();
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
							var itemData = {
								name: $activeItem.attr("data-name"),
							};
							if($activeItem.attr("data-groupedid") != ""){
								itemData.groupedid = $activeItem.attr("data-groupedid") * 1;
							}
							if($activeItem.attr("data-locationid") != ""){
								itemData.locationid = $activeItem.attr("data-locationid") * 1;
							}
							if($activeItem.attr("data-latitude") != ""){
								itemData.latitude = $activeItem.attr("data-latitude") * 1;
							}
							if($activeItem.attr("data-longitude") != ""){
								itemData.longitude = $activeItem.attr("data-longitude") * 1;
							}
							if($activeItem.attr("data-suffix") != ""){
								itemData.suffix = $activeItem.attr("data-suffix");
							}
							addItem.call(view, itemData);
						}
						setTimeout(function() {
							$input.focus();
						}, 200);
						event.preventDefault();
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

	};

	// --------- /Private Methods--------- //
	
	app.mixin = function(mixinObj, component, options) {
		var merge, obj = {};
		var eventNames = ['events', 'docEvents', 'winEvents', 'parentEvents'];
		$.extend(obj, mixinObj);
		$.each(component, function(key, val) {
			if ($.inArray(key, eventNames) >= 0) {
				if (key === "parentEvents") {
					merge = obj[key] = obj[key] || {};
					$.each(val, function(k, v) {
						merge[k] = $.extend(merge[k] || {}, v);
					});

				} else {
					obj[key] = $.extend(obj[key] || {}, val);
				}
			} else {
				obj[key] = val;
			}
		});
		return obj;
	};

})(jQuery);
