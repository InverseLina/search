var app = app || {};

(function($) {

	function genericHeaderFilterRenderer(filterInfo) {
		return render("search-query-generic-filter-render-header", filterInfo);
	}

	var filters = {
		contact : {
			headerRenderer : function(filterInfo) {
				var cookiePrefix = "contact_filter_";
				filterInfo.objectType = app.preference.get(cookiePrefix + "objectType") || "All";
				filterInfo.status = app.preference.get(cookiePrefix + "status") || "All";
				return render("search-query-contact-filter-render-header", filterInfo);
			},
			filterRenderer : function($content, headerInfo) {
				var data = app.ParamsControl.getFilterParams()['contract'] || [];
				return brite.display("ContactFilterView", $content, {
					data : data,
					th : headerInfo
				});
				// for example
			}
		},
		company : {
			headerRenderer : function(filterInfo) {
				return render("search-query-filter-operator-render-header", filterInfo);
			},
			filterRenderer : function($content, headerInfo) {
				var data = app.ParamsControl.getFilterParams()['company'] || [];
				brite.display("CompanyFilterView", $content, {
					data : data,
					th : headerInfo
				});
			},

		},
		skill : {
			headerRenderer : function(filterInfo) {
				return render("search-query-filter-operator-render-header", filterInfo);
			},
			filterRenderer : function($content, headerInfo) {
				var data = app.ParamsControl.getFilterParams()['skill'] || [];
				brite.display("SkillFilterView", $content, {
					data : data,
					th : headerInfo
				});
			},

		},
		education : {
			headerRenderer : genericHeaderFilterRenderer,
			filterRenderer : function($content, headerInfo) {
				var data = app.ParamsControl.getFilterParams()['education'] || [];
				brite.display("EducationFilterView", $content, {
					data : data,
					th : headerInfo
				});
			},

		},
		location : {
			headerRenderer : genericHeaderFilterRenderer,
			filterRenderer : function($content, headerInfo) {
				var data = app.ParamsControl.getFilterParams()['location'] || [];
				brite.display("LocationFilterView", $content, {
					data : data,
					th : headerInfo
				});
			}
		},
		custom : {
			headerRenderer : genericHeaderFilterRenderer,
			filterRenderer : function($content, headerInfo) {
				var $target = $(headerInfo);
				var type = $target.attr("data-type");
				var name = $target.attr("data-column");
				var viewName;
				if (type.toLowerCase() == 'number') {
					viewName = "CustomFilterNumber";
				} else if (type.toLowerCase() == 'string') {
					viewName = "CustomFilterString";
				} else if (type.toLowerCase() == 'date') {
					viewName = "CustomFilterDate";
				} else if (type.toLowerCase() == 'boolean') {
					viewName = "CustomFilterBoolean";
				}
				
				if (viewName) {
					brite.display(viewName, $content, {
						name : name
					}).done(function(custonFilterView){
						custonFilterView.showMode("edit");
					});
				}
			},

		},

	};
	//for generic not default filter render
	var genericRender = {
		headerRenderer : function(filterInfo) {
			return render("search-query-generic-render-header", filterInfo);
		},
		filterRenderer : function($content, headerInfo) {
			//do nothing for now
		},
		cellRenderer : function(cellInfo) {
			if(typeof cellInfo.value != 'undefined' && cellInfo.value != null){
				if(cellInfo.value.replace){
					var separator = new RegExp(app._separator,"g");
					cellInfo.value = cellInfo.value.replace(separator,",");
				}
			}
			return render("search-query-generic-render-cell", cellInfo);
		}

	};

	function getRender(field, renderName) {
		var renderer = filters[field];

		if (!renderer) {
			renderer = genericRender;
		} else {
			if (!renderer[renderName]) {
				renderer = genericRender;
			}
		}
		return renderer[renderName];
	}


	app.getFilterRender = function(field) {
		return getRender(field, "filterRenderer");
	};


	app.getHeaderRender = function(field) {
		return getRender(field, "headerRenderer");
	};


	app.getCellRender = function(field) {
		return getRender(field, "cellRenderer");
	};

})(jQuery);
