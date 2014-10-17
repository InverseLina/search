(function($) {

	brite.registerView("SearchDataGrid", {
		parent : ".ContentView"
	}, {

		// --------- View Interface Implement--------- //
		create : function() {
			return render("SearchDataGrid");
		},

		postDisplay : function() {
			var view = this;
			var $e = view.$el;
			
			view.$searchInput = view.$el.find(".search-input");
			view.$searchResult = view.$el.find(".search-result");
			view.tableOrderColumn = null;
			view.tableOrderType = null;
			view.labelDisable = true;
			view.showContentMessage("empty");
			brite.display("ToolBar");
			brite.display("Pagination", view.$el.find(".pagination-ctn"));
			brite.display("EstimateBar",$e.find(".estimateBar-ctn"));
			$(":text").placeholder();
			
			var searchMode = app.preference.get("searchMode","power");
			$e.find(".searchMode .btn").removeClass("active");
			$e.find(".searchMode .btn[data-mode='"+searchMode+"']").addClass("active");
			resizeGridControls.call(view);
			
			brite.display("CustomFilterPopup", null);
		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"keypress; .search-input" : function(event) {
				var view = this;
				var $this = $(event.currentTarget);
				var keyword = $this.val();
				if (event.which === 13) {
					event.preventDefault();
					if(!app.ParamsControl.isEmptySearch()){
						view.$el.trigger("DO_SEARCH", {
							search : keyword
						});
					}
				}
			},
			"keyup; .searchField .search-input" : function(event) {
				var view = this;
				if (event.keyCode !== 13) {
					view.$el.trigger("SEARCH_QUERY_CHANGE");
				}
			},
			"click; .searchMode .btn:not(.active)" : function(event) {
				var view = this;
				var $btn = $(event.currentTarget);
				var $searchMode = $btn.closest(".searchMode");
				$searchMode.find(".btn").toggleClass("active");
				view.$el.trigger("SEARCHMODE_DOSEARCH",new Boolean(true));
			},
			"click; table .locationTh span.columnName,table .contactTh span.columnName" : function(event) {
				var view = this;
				var $th = $(event.currentTarget).closest("th");
				var $desc = $(".desc", $th);
				var $asc = $(".asc", $th);
				var column = $th.attr("data-column");
				var pageIdx = view.pageIdx || 1;
				var pageSize = view.pageSize || 30;
				if (column === "company" || column === "skill" || column === "education" || column === "resume") {
					return false;
				}
				view.tableOrderColumn = column;
				if ($asc.is(":hidden")) {
					$(".desc,.asc", $th.parent()).hide();
					$asc.show();
					view.tableOrderType = "asc";
					view.$el.bComponent("MainView").$el.trigger("DO_SEARCH", {
						pageIdx : pageIdx,
						pageSize : pageSize
					});
				} else {
					$(".desc,.asc", $th.parent()).hide();
					view.tableOrderType = "desc";
					$desc.show();
					view.$el.bComponent("MainView").$el.trigger("DO_SEARCH", {
						pageIdx : pageIdx,
						pageSize : pageSize
					});
				}
				event.stopPropagation();
			},
			"click; table th[data-column]" : function(event) {
				event.preventDefault();
				event.stopPropagation();
				var view = this;
				var $e = view.$el;
				var $th = $(event.currentTarget);
				var $headerPopup = $e.closest('.SearchDataGrid').find('.HeaderPopup');
				if($headerPopup.is(":visible")){
					view.$el.trigger("POPUP_CLOSE");
				}else{
					view.$el.trigger("POPUP_CLOSE");
					brite.display("HeaderPopup", ".SearchDataGrid", {
						$target : $th
					});
				}
			},
			"click; table th[data-column] .operatorBtnGroups .btn" : function(event) {
				event.preventDefault();
				event.stopPropagation();
				var view = this;
				var $btn = $(event.currentTarget);
				if(!$btn.hasClass("active")){
					var $btn = $(event.currentTarget);
					$operatorBtnGroups = $btn.closest(".operatorBtnGroups");
					$operatorBtnGroups.find(".btn").toggleClass("active");
					var column = $operatorBtnGroups.closest("th[data-column]").attr("data-column");
					var value = $operatorBtnGroups.find(".btn.active").attr("data-value");
					app.preference.store(column+"Operator", value);
					view.$el.trigger("DO_SEARCH");
				}
			},
			"click; div.btnPopupColumns" : function(event) {
				brite.display("SelectColumns");
			},
			"click; .resume-ico.click" : function(event) {
				var cid = $(event.currentTarget).closest("i").attr("data-id");
				var sfid = $(event.currentTarget).closest("i").attr("data-sfid");
				var cname = $(event.currentTarget).closest("i").attr("data-cname");
				if (app.orgInfo.apex_resume_url && app.orgInfo.apex_resume_url.length && app.orgInfo.apex_resume_url.length > 0) {
					var url = app.orgInfo.apex_resume_url + sfid;
					window.showModalDialog(url, "dialogWidth=540px;dialogHeight=430px");
				} else {
					brite.display("ResumeView", "body", {
						id : cid,
						sfid : sfid,
						name : cname
					});
				}
			},
			"mouseover; .resume-ico.hover" : function(event) {
				var cid = $(event.currentTarget).closest("i").attr("data-id");
				var sfid = $(event.currentTarget).closest("i").attr("data-sfid");
				var cname = $(event.currentTarget).closest("i").attr("data-cname");
				if (app.orgInfo.apex_resume_url && app.orgInfo.apex_resume_url.length && app.orgInfo.apex_resume_url.length > 0) {
					var url = app.orgInfo.apex_resume_url + sfid;
					window.showModalDialog(url, "dialogWidth=540px;dialogHeight=430px");
				} else {
					brite.display("ResumeView", "body", {
						id : cid,
						sfid : sfid,
						name : cname
					});
				}
			},
			"click; table td[data-column='company'],td[data-column='skill'],td[data-column='education']" : function(event) {
				var view = this;
				var $this = $(event.currentTarget);
				var name = $this.attr("data-column");
				var value = $this.closest("td").attr("data-value") || $this.find("span").text();
				var ids = $this.closest("td").attr("data-groupedids");
				if(!ids || ids === ""){
					ids = [];
				}else{
					ids = ids.split(",");
				}
				var i, values = value.split(","), names = [];
				var separator = new RegExp(app._separator,"g");
				for(i = 0;i < values.length; i++){
					names[i] = values[i].replace(separator,",");
				}
				if (value !== "") {
					var data = {
						type : name,
						contactName : $this.closest("tr").attr("data-contractName"),
						names : names,
						ids : ids,
						pos : {
							x : event.clientX,
							y : event.clientY
						}
					};
					if (name === "company") {
						data.title = "Company";
					} else if (name === "skill") {
						data.title = "Skill";
					} else if (name === "education") {
						data.title = "Education";
					}
					brite.display("CellPopup", null, data);
				}
			},
			"click; .applyContact .selectCheckbox" : function(event) {
				var view = this;
				var $e = view.$el;
				var $this = $(event.currentTarget);
				var $tr = $this.closest("tr");

				$this.toggleClass("selected");
				if ($tr.hasClass("applySelect")) {
					$tr.removeClass("applySelect");
				} else {
					$tr.addClass("applySelect");
				}
				if (view.$el.find("tr.applySelect[data-entity]").length > 0) {
					$e.trigger("DO_TOOLBAR_ACTIVE_BUTTONS");
				} else {
					$e.trigger("DO_TOOLBAR_DEACTIVE_BUTTONS");
				}
			},
			"click; .btnSelect" : function(event) {
				var view = this;
				var $e = view.$el;
				var $this = $(event.currentTarget);

				if ($this.hasClass("selectAll")) {
					$e.find(".tableContainer tr").addClass("applySelect").find(".selectCheckbox").addClass("selected");
					view.$el.find(".btnAddToShotList").prop("disabled", false).removeClass("disabled");
					view.$el.find(".btnApply").prop("disabled", false).removeClass("disabled");
				} else {
					$e.find(".tableContainer tr").removeClass("applySelect").find(".selectCheckbox").removeClass("selected");
				}

				if (view.$el.find("tr.applySelect[data-entity]").length > 0) {
					$e.trigger("DO_TOOLBAR_ACTIVE_BUTTONS");
				} else {
					$e.trigger("DO_TOOLBAR_DEACTIVE_BUTTONS");
				}
			},
			"click; a.lineInfo.email" : function(event) {
				var href = $(event.currentTarget).attr('href');
				this.$el.trigger("EMAIL_PRESS", {
					mailTo : href.substring(href.indexOf(':') + 1)
				});
			},
			//add drag event

			"bdragstart; th[data-column]" : function(event) {
				event.stopPropagation();
				event.preventDefault();
				var $th = $(event.currentTarget);
				var view = this;
				var $e = view.$el;
				
				var $table = $("<table id='dragTable'><thead><tr></tr></thead><tbody></tbody></table>");
				var pos = $th.position();
				$table.css({
					"display" : "block",
					"position" : "absolute",
					opacity : 0.5,
					left : pos.left,
					top : pos.top,
					"cursor" : "pointer",
					"width" : $th.outerWidth()
				});

				$table.find("thead tr").append($th.clone());
				var index = $th.index();
				$e.find(".scrollTable tbody tr td:nth-child(" + (index + 2) + ")").each(function() {
					var $td = $(this);
					$table.find("tbody").append("<tr></tr>");
					$table.find("tbody tr:last").append($td.clone());
					$(this).addClass("holderSpace");
				});

				$th.addClass("holderSpace");

				view.$el.find(".tableContainer").append($table);
			},
			"bdragmove; th[data-column]" : function(e) {
				var view = this;
				var $e = view.$el;
				
				e.stopPropagation();
				e.preventDefault();
				
				$e.trigger("POPUP_CLOSE");
				
				var $dragTable = view.$el.find("#dragTable");
				var ppos = $dragTable.position();
				var pos = {
					left : ppos.left + e.bextra.deltaX,
					top : 0
				};
				$dragTable.css(pos);

				$e.find(".scrollTable tr").each(function() {
					var $tr = $(this);
					var $holder = $tr.find(".holderSpace");
					$tr.find("th[data-column],td[data-column]").each(function(idx, th) {
						var $th = $(th);
						var tpos = $th.offset();
						if (e.bextra.pageX > tpos.left && e.bextra.pageX < tpos.left + $th.outerWidth()) {
							if (e.bextra.pageX < tpos.left + $th.outerWidth() / 2) {
								$holder.insertAfter($th);
							} else {
								$holder.insertBefore($th);
							}
						}
					});
				});
			},
			"bdragend; th[data-column]" : function(e) {
				e.stopPropagation();
				e.preventDefault();
				var view = this;
				var $e = view.$el;
				
				var columns = [];
				$e.find(".scrollTable th[data-column]").each(function(idx, th) {
					var $th = $(th);
					columns.push($th.attr('data-column'));
				});
				view.$el.find("#dragTable").remove();

				$e.find(".scrollTable th, .scrollTable td").removeClass("holderSpace");

				app.columns.save(columns).done(function(){
					view.$el.trigger("DO_SEARCH");
				});
			}

		},
		// --------- /Events--------- //

		// --------- Public Methods--------- //
		refreshColumns : function(resultData, labelAssigned) {
			labelAssigned = labelAssigned || app.buildPathInfo().labelAssigned;
			resultData = resultData || [];
			var view = this;
			var $e = view.$el;
			var html = render("search-items", {
				items : resultData,
				colWidth : getColWidth.call(view),
				labelAssigned : labelAssigned
			});
			view.$searchResult.find(".tableContainer").html(html);
			
			if(resultData.length == 0){
				view.showContentMessage("empty");
				view.$el.trigger("CHECK_CLEAR_BTN");
			}

			//show desc/asc
			if (view.tableOrderColumn && view.tableOrderType) {
				$e.find("table th[data-column='" + view.tableOrderColumn + "']").find("." + view.tableOrderType).show();
			}


		},
		showContentMessage : function(cmd, extra) {
			var view = this;
			var $e = view.$el;
			view.$el.find(".search-info").addClass("hide");
			var $tabContainer = view.$searchResult.find(".tableContainer");
			if (cmd === "empty") {
				$tabContainer.html(render("search-empty", {
					colWidth : getColWidth.call(view)
				}));
				$e.trigger("REFRESH_ESTIMATE_COUNT",{count:0, exact:true});
				$e.trigger("REFRESH_PAGINATION",{pageSize:view.pageSize});
			} else if (cmd === "loading") {
				$tabContainer.html(render("search-loading"));
			} else if (cmd === "retrying") {
				$tabContainer.html(render("search-retrying"));
			} else if (cmd === "lessword") {
				$tabContainer.html(render("search-query-less-words", {
					colWidth : getColWidth.call(view),
					labelAssigned : app.buildPathInfo().labelAssigned
				}));
				view.restoreSearchParam();
			} else if (cmd === "parentheses") {
				$tabContainer.html(render("search-query-cannot-parentheses"));
				view.restoreSearchParam();
			} else if (cmd === "error") {
				var html = render("search-query-error", {
					title : extra.title,
					detail : extra.detail,
					colWidth : getColWidth.call(view)
				});
				$tabContainer.html(html);
			}
			
			view.restoreSearchParam();
			fixColWidth.call(view);
		},
		restoreSearchParam : function(filters) {
			var key, dataName, data, displayName, $html, $th, view = this;
			
			view.$el.find("table th[data-column='skill'] .operatorBtnGroups .btn").removeClass("active");
			view.$el.find("table th[data-column='skill'] .operatorBtnGroups .btn[data-value='" + app.preference.get("skillOperator", "O") + "']").addClass("active");
			view.$el.find("table th[data-column='company'] .operatorBtnGroups .btn").removeClass("active");
			view.$el.find("table th[data-column='company'] .operatorBtnGroups .btn[data-value='" + app.preference.get("companyOperator", "O") + "']").addClass("active");
			
			if (view.$el.find("table th .selectedItems .item").length > 0) {
				return;
			}

			var result = filters || app.ParamsControl.getFilterParams() || {};
			for (key in result) {
				dataName = key;
				if (key === "Contact") {
					dataName = "contact";
				}
				
				$th = view.$el.find("table thead th[data-column='{0}']".format(dataName));
				
				var item;
				data = result[key];
				if (data && data.length > 0) {
					for(var index = 0; index < data.length; index++){
						var val = data[index];
						item = {
							name : val.name,
							display: val.name
						};

						val = val.value;
						if(val.suffix){
							item.display = item.display + " ("+val.suffix+")";
						}
						
						if (val.minYears || val.minRadius) {
							item.min = val.minYears || val.minRadius;
						}
						if (item.min) {
							item.display = item.display + " (" + item.min + ")";
						}
						
						if (data.length > 1 && index !== data.length - 1) {
							item.display = item.display + ",";
						}
						

						$html = $(render("search-items-header-add-item", item));

						$th.find(".addFilter").before($html);
					}

					$th.find(".addFilter").hide();
				}
			}
			
			// for custom
			var headerCustomFilters = app.ParamsControl.getHeaderCustomFilters();
			var customColumns = app.columns.getCustomColumnsSelected();
			for(var i = 0; i < headerCustomFilters.length; i++){
				var headerCustomFilter = headerCustomFilters[i];
				var fieldName = headerCustomFilter.field;
				$th = view.$el.find("table thead th[data-column='{0}'][data-display='side']".format(fieldName));
				var type;
				for(var j = 0; j < customColumns.length; j++){
					var customColumn = customColumns[j];
					if(customColumn.name == fieldName){
						type = customColumn.type;
						break;
					}
				}
				
				if(type){
					var item = {};
					if(type.toLowerCase() == "string"){
						var isValue = headerCustomFilter.conditions["=="];
						var isValueHtml;
						if(isValue){
							isValueHtml = "Is: "
							for(var k = 0; k < isValue.length; k++){
								item = {};
								item.name = isValue[k];
								item.display = item.name;
								isValueHtml += render("search-items-header-add-single-item", item)
							}
						}
						var isNotValue = headerCustomFilter.conditions["!="];
						var isNotValueHtml;
						if(isNotValue){
							isNotValueHtml = "Is not: ";
							for(var k = 0; k < isNotValue.length; k++){
								item = {};
								item.name = isNotValue[k];
								item.display = item.name;
								isNotValueHtml += render("search-items-header-add-single-item", item)
							}
						}
						
						if(isValueHtml && isNotValueHtml){
							$html = isValueHtml + ", " + isNotValueHtml;
						}else if(isValue){
							$html = isValueHtml;
						}else{
							$html = isNotValueHtml;
						}
						$th.find(".addFilter").before("<span>"+$html+"</span>");
					}else if(type.toLowerCase() == "boolean"){
						item.name = headerCustomFilter.conditions["=="];
						item.display = item.name + "" == "true" ? "Yes" : "No";
						$html = $(render("search-items-header-add-item", item));
						$th.find(".addFilter").before($html);
					}else if(type.toLowerCase() == "date"){
						var greaterValue = headerCustomFilter.conditions[">="];
						var lessValue = headerCustomFilter.conditions["<="];
						
						if(greaterValue && lessValue){
							item.display = "Between: " + greaterValue + " and " + lessValue;
						}else if(lessValue){
							item.display = "Before: " + lessValue;
						}else{
							item.display = "After: " + greaterValue;
						}
						$html = $(render("search-items-header-add-single-item", item));
						$th.find(".addFilter").before($html);
					}else if(type.toLowerCase() == "number"){
						var greaterValue = headerCustomFilter.conditions[">="];
						var lessValue = headerCustomFilter.conditions["<="];
						
						if(typeof greaterValue != "undefined" && typeof lessValue != "undefined"){
							item.display = "Between: " + greaterValue + " and " + lessValue;
						}else if(typeof lessValue != "undefined"){
							item.display = "Less than: " + lessValue;
						}else{
							item.display = "Greater than: " + greaterValue;
						}
						$html = $(render("search-items-header-add-single-item", item));
						$th.find(".addFilter").before($html);
					}
				}
				$th.find(".addFilter").hide();
				
			}
			
			// for custom columns
			var headerCustomColumnFilters = app.ParamsControl.getHeaderCustomColumnFilters();
			var customColumns = app.columns.getCustomColumnsSelected();
			for(var k in headerCustomColumnFilters){
				var headerCustomColumnFilter = headerCustomColumnFilters[k];
				var fieldName = k.substring(2);
				$th = view.$el.find("table thead th[data-column='{0}'][data-display='column']".format(fieldName));
				if($th.length > 0){
					var isValue = headerCustomColumnFilter["=="];
					var isValueHtml;
					if (isValue) {
						isValueHtml = "Is: "
						for (var k = 0; k < isValue.length; k++) {
							item = {};
							item.name = isValue[k];
							item.display = item.name;
							isValueHtml += render("search-items-header-add-single-item", item)
						}
					}
					var isNotValue = headerCustomColumnFilter["!="];
					var isNotValueHtml;
					if (isNotValue) {
						isNotValueHtml = "Is not: ";
						for (var k = 0; k < isNotValue.length; k++) {
							item = {};
							item.name = isNotValue[k];
							item.display = item.name;
							isNotValueHtml += render("search-items-header-add-single-item", item)
						}
					}
					
					
					if(isValueHtml || isNotValueHtml){
						if (isValueHtml && isNotValueHtml) {
							$html = isValueHtml + ", " + isNotValueHtml;
						} else if (isValue) {
							$html = isValueHtml;
						} else {
							$html = isNotValueHtml;
						}
						$th.find(".addFilter").before("<span>" + $html + "</span>");
						$th.find(".addFilter").hide(); 
					}

				}
			}
			
			resizeHeight.call(view);
		},
		// --------- /Public Methods--------- //

		// --------- Document Events--------- //
		docEvents : {
			"PAGE_SIZE_CHANGE" : function(event, pageSize) {
				var view = this;
				view.pageSize = pageSize;
			},
			"SEARCHMODE_DOSEARCH" : function(event, modeChange) {
				var view = this;
				var $btn = view.$el.find(".searchMode .btn.active");
				app.preference.store("searchMode", $btn.attr("data-mode"));
				var searchModeChange;
				if(modeChange){
					searchModeChange = "true";
				}else{
					searchModeChange = "false";
				}
				view.$el.trigger("DO_SEARCH",{searchModeChange:searchModeChange,pageIdx:view.pageIdx});
			},
			"ADD_FILTER" : function(event, extra) {
				var view = this;
				app.ParamsControl.save(extra);
				view.restoreSearchParam();
				view.$el.trigger("DO_SEARCH");
				event.preventDefault();
			},
			"REMOVE_FILTER" : function(event, extra) {
				var view = this;
				app.ParamsControl.remove(extra);
				view.restoreSearchParam();
				view.$el.trigger("DO_SEARCH");
				event.preventDefault();
			},
			"UPDATE_FILTER" : function(event, extra) {
				var view = this;
				view.$el.trigger("DO_SEARCH");
				event.preventDefault();
			},
			"ON_ERROR" : function(event, extra) {
				var view = this;
				if (extra) {
					var title = extra.errorCode || "";
					var detail = extra.errorMessage || "";
					view.showContentMessage("error", {
						title : title,
						detail : detail
					});
					brite.display("MessagePanel", ".search-result", {
						message : "No Organization selected"
					});
				}
			},

			"CHANGE_TO_FAV_VIEW" : function(event, extra) {
				var view = this;
				view.$el.find(".tableContainer th").addClass("favFilter");
				view.$el.find(".search-result").addClass("favFilter");
				view.$el.find(".tableContainer tbody").addClass("favFilter");
				view.$el.find(".btnPopupColumns").addClass("favFilter");
				view.$el.find(".gridControls").hide();
				view.$el.find(".toolBarContainer ").hide();
				view.$el.find(".empty-search").hide();
				//          view.$el.find(".page").hide();
			},
			"RESTORE_SEARCH_VIEW" : function(event) {
				var view = this;
				view.$el.find(".tableContainer th").removeClass("favFilter");
				view.$el.find(".search-result").removeClass("favFilter");
				view.$el.find(".tableContainer tbody").removeClass("favFilter");
				view.$el.find(".btnPopupColumns").removeClass("favFilter");
				view.$el.find(".gridControls").show();
				view.$el.find(".toolBarContainer ").show();
				//          view.$el.find(".page").show();
			}

		},
		// --------- /Document Events--------- //

		// --------- Parent Events--------- //
		parentEvents : {

			MainView : {
				"DO_CLEAR_ORDER" : function(event){
					var view = this;
					view.tableOrderColumn = null;
					view.tableOrderType = null;
				},
				
				"SEARCH_RESULT_CHANGE" : function(event, result) {
					var labelAssigned = app.buildPathInfo().labelAssigned;
					var view = this;
					var $e = view.$el;
					
					// process filter cache when have result
					app.TopFilterItem.saveItems(result.result);
					
					$e.find(".resultTime").removeClass("hide");
					$e.find(".resultTime .count").html("c:{0}ms".format(result.countDuration));
					if(!result.searchModeChange){
						var html;
						$e.find(".resultTime .select").html("s:{0}ms".format(result.selectDuration));
	
						if (result.count > 0 || result.result.length > 0) {
							buildResult.call(view, result.result).done(function(data) {
								view.refreshColumns(data, labelAssigned);
							});
	
						} else {
							view.$searchResult.find(".tableContainer").html(render("search-query-notfound", {
								colWidth : getColWidth.call(view),
								labelAssigned : app.buildPathInfo().labelAssigned
							}));
	
						}
						
						fixColWidth.call(view);
						view.restoreSearchParam();
					}

					
					$e.trigger("REFRESH_ESTIMATE_COUNT", {count:result.count,exact: result.exactCount});
					view.pageIdx = result.pageIdx;
					view.pageSize = result.pageSize;
					var paginationData = {
						pageIdx : result.pageIdx,
						pageSize : result.pageSize,
						totalCount : result.count,
						exactCount : result.exactCount,
						hasNextPage : result.hasNextPage,
						callback : function(pageIdx, pageSize) {
							view.pageIdx = pageIdx;
							view.pageSize = pageSize;
							view.$el.trigger("DO_SEARCH", {
								pageIdx : pageIdx
							});
						}
					};
					$e.trigger("REFRESH_PAGINATION", paginationData);
					
					var pagination = view.$el.find(".Pagination").bComponent();
					if (labelAssigned) {
						$e.trigger("CHANGE_TO_FAV_VIEW");
					} else {
						$e.trigger("RESTORE_SEARCH_VIEW");
					}


				}

			}
		},
		winEvents : {
			resize : function(event) {
				var view = this;
				fixColWidth.call(view);
				resizeGridControls.call(view);
			}

		},
		// --------- /Parent Events--------- //

		// --------- Public Methods--------- //
		getSearchValues : function() {
			var view = this;
			var val = this.$searchInput.val();
			var result = {};
			result.search = val;
			if (view.tableOrderColumn && view.tableOrderType) {
				result.sort = {
					column : view.tableOrderColumn,
					order : view.tableOrderType
				};
			}
			return result;
		}

		// --------- /Public Methods--------- //

	});

	// --------- Private Methods--------- //
	function buildResult(items) {
		var result = [];
		var item;
		var columns = app.columns.get();
		var colLen = columns.length;
		var view = this;
		var dtd = $.Deferred();
		var dateFormat = app.orgInfo["local_date"] || "YYYY-MM-DD";

		for (var i = 0; i < items.length; i++) {
			item = [];
			for (var j = 0; j < columns.length; j++) {
				if (columns[j] === "skill") {
					item.push({
						name : columns[j],
						value : translate(reSortData(items[i][columns[j]], "skill")),
						groupedids : items[i]["skillgroupedids"],
						realValue : items[i][columns[j]],
						notLast : colLen - j > 1
					});

				} else if (columns[j] === "education") {
					item.push({
						name : columns[j],
						value : reSortData(items[i][columns[j]], "education"),
						groupedids : items[i]["educationgroupedids"],
						realValue : items[i][columns[j]],
						notLast : colLen - j > 1
					});
				} else if (columns[j] === "company") {
					item.push({
						name : columns[j],
						value : reSortData(items[i][columns[j]], "company"),
						groupedids : items[i]["companygroupedids"],
						realValue : items[i][columns[j]],
						notLast : colLen - j > 1
					});

				} else if (columns[j] === "CreatedDate") {
					item.push({
						name : columns[j],
						value : formateDate(items[i][columns[j]], dateFormat),
						notLast : colLen - j > 1
					});
				} else if (columns[j] === "contact") {
					var displayValue = "<a class='lineInfo name' target='_blank' href='" + app.orgInfo.instance_url + items[i]["sfid"] + "'>" + items[i]["name"] + "</a>";
					if (items[i]['resume'] !== -1) {
						var resumeBehave = app.orgInfo.contact_resume_behave || "click";
						if(resumeBehave === "click"){
							displayValue += "<i data-id='" + items[i]["resume"] + "' data-sfid='" + items[i]["sfid"] + "' data-cname='" + items[i]["name"] + "' title='View Resume.' class='resume-ico click glyphicon glyphicon-file'></i>";
						}else{
							displayValue += "<i data-id='" + items[i]["resume"] + "' data-sfid='" + items[i]["sfid"] + "' data-cname='" + items[i]["name"] + "' title='View Resume.' class='resume-ico hover glyphicon glyphicon-file'></i>";
						}
					}
					displayValue += "<div class='lineInfo title'>" + items[i]["title"] + "</div>";
					displayValue += "<a class='lineInfo email' href='mailTo:" + items[i]["email"] + "'>" + items[i]["email"] + "</a>";
					item.push({
						name : columns[j],
						value : displayValue,
						notLast : colLen - j > 1
					});
				} else {
					item.push({
						name : columns[j],
						value : items[i][columns[j]],
						notLast : colLen - j > 1
					});
				}
			}
			result.push({
				row : item,
				names : {
					id : items[i].id,
					name : items[i].name,
					sfid : items[i].sfid
				}
			});
		}
		dtd.resolve(result);

		return dtd.promise();
	}

	function getColWidth() {
		var view = this;
		var $e = view.$el;
		var colLen = $e.find(".scrollTable th[data-column]").size();
		//        return parseInt((view.$searchResult.innerWidth()-30)/colLen)-2;
		return 100 / colLen;
	}

	function fixColWidth() {
		var view = this;
		var $e = view.$el;
		var colWidth;
		var colName;
		var colLen = $e.find(".scrollTable th[data-column]").size();
		var tableWidth = view.$el.find(".tableContainer").width();
		
		//checkbox
		var applyContactCheckboxWidth = 19;
		if (!view.labelDisable) {
			tableWidth = tableWidth - 32;
		}
		if (colLen !== 0) {
			colWidth = tableWidth / colLen;
		} else {
			colWidth = tableWidth;
		}
		var realWidth;

		var $body = view.$el.find(".scrollTable tbody");
		var $head = view.$el.find(".scrollTable thead");
		var tlen = $head.find("th").length - 1;

		$head.find("th").each(function(idx, item) {
			var $item = $(item);
			colName = $item.attr("data-column");
			if ($item.hasClass("favLabel")) {
				if (!view.labelDisable) {
					realWidth = 32;
				} else {
					realWidth = 0;
				}
			} else {
				realWidth = colWidth;
			}

			if (idx === 0) {
				$item.css({
					width : 0,
					"max-width" : 0,
					"min-width" : 0
				});
				$body.find("tr td:nth-child(" + (idx + 1) + ")").css({
					width : 0,
					"max-width" : 0,
					"min-width" : 0
				});
			} else if (idx === 1) {
				$item.css({
					width : realWidth,
					"max-width" : realWidth,
					"min-width" : realWidth
				});

				$body.find("tr td:nth-child(" + (idx + 1) + ")").css({
					width : applyContactCheckboxWidth,
					"max-width" : applyContactCheckboxWidth,
					"min-width" : applyContactCheckboxWidth
				});

				$body.find("tr td:nth-child(" + (idx + 2) + ")").css({
					width : realWidth - applyContactCheckboxWidth,
					"max-width" : realWidth - applyContactCheckboxWidth,
					"min-width" : realWidth - applyContactCheckboxWidth
				}).find(" > span").css({
					width : realWidth - applyContactCheckboxWidth - 4
				});

			} else if (idx === tlen) {
				$item.css({
					width : realWidth + 50,
					"max-width" : realWidth + 50,
					"min-width" : realWidth
				});
				$body.find("tr td:nth-child(" + (idx + 2) + ")").css({
					width : realWidth,
					"max-width" : realWidth,
					"min-width" : realWidth
				}).find(" > span").css({
					width : realWidth - 4
				});
			} else {
				$item.css({
					width : realWidth,
					"max-width" : realWidth,
					"min-width" : realWidth
				});
				$body.find("tr td:nth-child(" + (idx + 2) + ")").css({
					width : realWidth,
					"max-width" : realWidth,
					"min-width" : realWidth
				}).find(" > span").css({
					width : realWidth - 4
				});
			}

		});

		if (view.labelDisable) {
			$e.find("th.favLabel").hide();
			$e.find("td.favLabel").hide();
		}

		resizeHeight.call(view);
	}

	function resizeHeight() {
		var $e = this.$el;
		var height = 0;
		$e.find(".tableContainer thead th[data-column='contact']").each(function() {
			var $th = $(this);
			var $columnName = $th.find(".columnName");
			var $indicators = $th.find(".indicators");
			var $selectedItems = $th.find(".selectedItems");
			var thHeight = $columnName.height();
			thHeight += $selectedItems.height();
			if ($indicators.length > 0) {
				thHeight += $indicators.height();
			}
			if (thHeight > height) {
				height = thHeight;
			}
		});
		height += 10;
		$e.find(".tableContainer thead .headerTh").css("height", height + "px");
		var newHeight = $e.find(".tableContainer thead .headerTh").css("min-height").replace("px", "") * 1;
		height = height > newHeight ? height : newHeight;
		$e.find(".btnPopupColumns").css("height", height + "px");
		$e.find(".btnPopupColumns").css("line-height", (height - 6) + "px");
		$e.find(".tableContainer tbody").css("top", (height + 15) + "px");
	}
	
	function resizeGridControls(){
		var view = this;
		var $e = view.$el;
		var $gridControl = $e.find(".gridControls");
		var height = $gridControl.height();
		var $seachResult = $e.find(".search-result");
		$seachResult.css("top",(height+9)+"px");
	}

	function translate(value) {
		if (!value) {
			return "";
		}
		var items = value.split(","), result = [];
		var len = items.length;
		for (var i = 0; i < len; i++) {
			if (items[i].length > 1) {
				if (items[i].substr(0, 8) === "<strong>") {
					result.push("<strong>" + items[i].substr(8, 1).toUpperCase() + items[i].substr(9).toLowerCase());
				} else {
					result.push(items[i].substr(0, 1).toUpperCase() + items[i].substr(1).toLowerCase());
				}

			} else {
				result.push(items[i]);
			}
		}
		return result.join(",");
	}

	function formateDate(date, format) {
		if (!date || date === "") {
			return "";
		}
		date = new Date(date);
		var year = date.getYear() + 1900;
		var month = date.getMonth() + 1;
		if (month < 10) {
			month = "0" + month;
		}
		var day = date.getDate();
		if (day < 10) {
			day = "0" + day;
		}
		return format.replace("YYYY", year).replace("MM", month).replace("DD", day);
	}

	/**
	 * resort data for the filter selected value,make the matched values first show with bold
	 */
	function reSortData(value, type) {
		var filters = app.ParamsControl.getFilterParams() || {};
		var filter = filters[type] || [];
		value = (value || "") + ",";
		var matched = "";
		$.each(filter, function(index, e) {
			var separator = new RegExp(app._separator,"g");
			if (value.replace(separator,",").indexOf(e.name + ",") !== -1) {
				value = value.replace(separator,",").replace(e.name + ",", "");
				matched += "<strong>" + e.name + "</strong>,";
			}
		});
		value = matched + value;
		return value;
	}

	// --------- /Private Methods--------- //

})(jQuery);
