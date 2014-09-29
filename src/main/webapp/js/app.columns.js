var app = app || {};
(function($) {
	var filterOrders;
	
	app.columns = {
		getSelectedColumns : function(){
			var columns = this.get();
			var allColumns = this.listAll();
			var displays = [];
			$.each(columns, function(idx, colName) {
				$.each(allColumns, function(didx, item) {
					if (item.name === colName) {
						displays.push(item);
					}
				});

			});
			return displays;

		},
		get : function() {
			var columns = app.preference.get("columns", null);
			if(columns){
				return columns.split(",");
			}else{
				app.getJsonData("perf/get-user-pref", {}, {
					async : false
				}).done(function(result) {
					if (result && result['val_text'] && result['val_text'] != "") {
						columns = JSON.parse(result['val_text']);
					} else {
						columns = defaultColumns();
					}
				});
				return columns;
			}
		},
		save : function(newColumns){
			var dfd = $.Deferred();
			if (newColumns && $.type(newColumns) === "array") {
				if (newColumns.length > 0) {
					app.cookie("columns", newColumns.join(","));
				}
				
				app.getJsonData("perf/save-user-pref", {
					value : JSON.stringify(newColumns)
				}, "Post").done(function() {
					dfd.resolve();
				});
			}else{
				dfd.reject();
			}
			return dfd.promise();
		},
		
		listAll: function() {
			var columns = app.getSearchUiConfig();
			var fields = app.getCustomFields();
	
			for (var i = 0; i < fields.length; i++) {
				var colObj = fields[i];
				colObj.title = colObj.label;
				colObj.display = colObj.label;
				colObj.custom = true;
				columns.push(colObj);
			}
			return columns;
		},
		
		listAllWithOrder: function() {
			var columns = this.listAll();
			var orders = this.get();
			var orderColumns = [];
			var ids = [];
			
			$.each(orders, function(idx, name) {
				$.each(columns, function(idx, item) {
					if (item.name === name) {
						orderColumns.push(item);
						ids.push(idx);
					}
				});

			});

			for ( i = 0; i < columns.length; i++) {
				if ($.inArray(i, ids) < 0) {
					orderColumns.push(columns[i]);
				}
			}
			return orderColumns;
		},
		
		getCustomColumnsNotSelected: function() {
			var columns = this.get();
			var customFields = app.getCustomFields();
			
			var customColumns = [];
			$.each(customFields, function(i, item) {
				var exist = false;
				$.each(columns, function(j, name) {
					if (item.name === name) {
						exist = true;
					}
				});
				
				if(!exist){
					customColumns.push(item);
				}
			});
			return customColumns;
		},
		
		getCustomColumnsSelected: function() {
			var columns = this.get();
			var customFields = app.getCustomFields();
			
			var customColumns = [];
			$.each(customFields, function(i, item) {
				var exist = false;
				$.each(columns, function(j, name) {
					if (item.name === name) {
						exist = true;
					}
				});
				
				if(exist){
					customColumns.push(item);
				}
			});
			return customColumns;
		}
	};
	
	function defaultColumns(){
		var filters = app.getSearchUiConfig();
		var displays = $.grep(filters, function(item, idx) {
			return item.show;
		});

		return $.map(displays, function(item) {
			return item.type;
		});
	};

})(jQuery); 