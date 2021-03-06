var app = app || {};
(function($) {
	var _storeValue = {};
	var queryKey = "";
	
	var _headerCustomColumnFilters = {};
	var _headerCustomFilters = [];
	var _sideCustomAdvancedFilters = [];
	function getMainView() {
		return app.MainView;
	}

	app.ParamsControl = {
		getParamsForSearch : function(params) {
			params = params || {};
			searchModeChange = params.searchModeChange || "false";
			var view = getMainView();
			var obj, key, newKey;
			var data, result = {};
			var searchData = result.searchValues = {};
			var contentSearchValues = view.contentView.dataGridView.getSearchValues();
			queryKey = $.trim(params.search || contentSearchValues.search);
			result.searchColumns = app.columns.get().join(",");
			
			if (contentSearchValues.sort) {
				if(new RegExp(contentSearchValues.sort.column,"i").test(app.columns.get())){//only the sort header shown 
					result.orderBy = contentSearchValues.sort.column;
					result.orderType = contentSearchValues.sort.order === "asc";
				}
			}
			if (!/^\s*$/.test(queryKey)) {
				searchData.q_search = queryKey;
			}

			for (key in _storeValue) {
				newKey = key.substring(0, 1).toLocaleLowerCase() + key.substring(1);
				data = [];
				for(var idx = 0; idx < _storeValue[key].length; idx++){
					var item = _storeValue[key][idx];
					if (newKey === "contact") {
						data.push(item.value.value);
					} else {
						data.push(item.value);
					}
				}

				if (data.length > 0) {
					if (newKey === "company") {
						searchData["q_companies"] = data;
					} else {
						searchData["q_" + newKey + "s"] = data;
					}
				}

			}
			searchData["q_objectType"] = app.preference.get("contact_filter_objectType", "All");
			searchData["q_status"] = app.preference.get("contact_filter_status", "All");
			//for custom fields
			searchData["q_customFields"] = $.extend([], _headerCustomFilters, _sideCustomAdvancedFilters);
			
			//for custom columns
			searchData = $.extend({}, searchData, _headerCustomColumnFilters);
			
			result.searchValues = JSON.stringify(searchData);
			result.searchMode = app.preference.get("searchMode", "power");
			result.searchModeChange = searchModeChange;
			
			//for skill
			result.skillOperator = app.preference.get("skillOperator", "O");
			
			//for company
			result.companyOperator = app.preference.get("companyOperator", "O");
			
			result.pageIndex = view.contentView.dataGridView.pageIdx || 1;
			result.pageSize = view.contentView.dataGridView.pageSize || 15;
			
			
			return result;
		},
		restoreSearch:function(search){
			var view = getMainView();
			var searchValues = JSON.parse(search.searchValues);
			app.ParamsControl.clear();
			app.columns.save(search.searchColumns);
			app.preference.store("skillOperator", searchValues["skillOperator"]);
			app.preference.store("companyOperator", searchValues["companyOperator"]);
			app.preference.store("searchMode", searchValues["searchMode"]);
			
			for(var key in searchValues){
				if(key == "q_search"){
					view.contentView.dataGridView.$el.find(".search-input").val(searchValues[key]);
				}else if(key == "q_objectType"){
					app.preference.store("contact_filter_objectType", searchValues[key]);
				}else if(key == "q_status"){
					app.preference.store("contact_filter_status", searchValues[key]);
				}else if(key == "q_customFields"){
					var fields = searchValues[key];
					var customAdvancedFilter = app.columns.getCustomColumnsNotSelected();
					for(var i = 0; i < customAdvancedFilter.length; i++){
						for(var j = 0; j < fields.length; j++){
							if(customAdvancedFilter[i].name == fields[j].field){
								app.ParamsControl.saveSideCustomAdvancedFilter(fields[j]);
							}
						}
					}
					
					var customColumns = app.columns.getCustomColumnsSelected();
					for(var i = 0; i < customColumns.length; i++){
						for(var j = 0; j < fields.length; j++){
							if(customColumns[i].name == fields[j].field){
								if(customColumns[i].display == "column"){
									app.ParamsControl.saveHeaderCustomColumnFilter(fields[j].field, fields[j].conditions);
								}else{
									app.ParamsControl.saveHeaderCustomFilter(fields[j]);
								}
							}
						}
					}
					
				}else if(key == "q_contacts"){
					var type = "contact";
					var valueArr = searchValues[key];
					for (var i = 0; i < valueArr.length; i++) {
						var value = valueArr[i];
						var displayName = app.getContactDisplayName(value);
						app.ParamsControl.save({
							name : displayName,
							type : type,
							value : value
						});
					}

				}else if(key == "q_companies"){
					var type = "company";
					var valueArr = searchValues[key];
					for (var i = 0; i < valueArr.length; i++) {
						var value = valueArr[i];
						value.type = "company";
						app.ParamsControl.save(value);
					}

				}else if(key == "q_skills" || key == "q_locations" || key == "q_educations"){
					var type = key.substring(2, key.length - 1);
					var valueArr = searchValues[key];
					for (var i = 0; i < valueArr.length; i++) {
						var value = valueArr[i];
						value.type = type;
						app.ParamsControl.save(value);
					}
				}else{
					var type = key.substring(2, key.length);
					var value = searchValues[key];
					var columns = app.columns.get().join(",");
					if(columns.indexOf(type) == -1){
						this.saveSideCustomAdvancedFilter({field:type, conditions:value});
					}else{
						this.saveHeaderCustomColumnFilter(type,value);
					}
				}
			}


			var customFilterPopup = view.$el.find(".CustomFilterPopup").bView();
			if (customFilterPopup) {
				customFilterPopup.refreshFilterViews();
			}

			if(!searchValues.q_search){
				view.contentView.dataGridView.$el.find(".search-input").val("");
			}
		},
		/**
		 * save data {type: xx, name: xx, val:xx}
		 * @param data
		 */
		save : function(data) {
			var store;
			if (!data && !data.type && !data.name) {
				return;
			}
			if (!_storeValue[data.type]) {
				//              _storeValue[data.type] = {};
				_storeValue[data.type] = [];
			}
			store = _storeValue[data.type];
			//            store[data.name] = data.value;
			delete data.type;
			store.push({
				name : data.name,
				value : data
			});
		},
		/**
		 * remove data format {type:xxx, name: xxx}
		 * @param data
		 */
		remove : function(data) {
			var index, obj, store, found = -1;
			if ($.isPlainObject(data)) {
				if (!data && !data.type && !data.name) {
					return;
				}
				if (!_storeValue[data.type]) {
					_storeValue[data.type] = [];
				}
				store = _storeValue[data.type];
				//delete store[data.name];
				for ( index = 0; index < store.length; index++) {
					obj = store[index];
					if (obj.name === data.name) {
						found = index;
						break;
					}
				}
				if (found !== -1) {
					//                delete store[index];
					store.splice(index, 1);
				}
			} else {
				if (data) {
					delete _storeValue[data];
				}
			}
		},
		getFilterParams : function() {
			return _storeValue || {};
		},
		setFilterParams : function(filters) {
			_storeValue = filters || {};
		},
		get : function(type, name) {
			var i, obj, data = _storeValue[type] || [];
			if (!name) {
				return data;
			} else {
				for ( i = 0; i < data.length; i++) {
					obj = data[i];
					if (obj.name === name) {
						return obj;
					}
				}
			}
			return null;
		},
		getQuery : function() {
			return queryKey;
		},
		clear : function() {
			_storeValue = {};
			_headerCustomFilters = [];
			_headerCustomColumnFilters = {};
			_sideCustomAdvancedFilters = [];
		},
		isEmptySearch:function(){
			var searchParams = this.getParamsForSearch() || {};
			if(searchParams.searchValues){
				var searchValues = JSON.parse(searchParams.searchValues);
				if(searchValues.q_search){
					return false;
				}
				
				if(searchValues.q_objectType && searchValues.q_objectType != "All"){
					return false;
				}
				
				if(searchValues.q_status && searchValues.q_status != "All"){
					return false;
				}
				
				if(searchValues.q_contacts && searchValues.q_contacts.length > 0){
					return false;
				}
				
				if(searchValues.q_skills && searchValues.q_skills.length > 0){
					return false;
				}
				
				if(searchValues.q_educations && searchValues.q_educations.length > 0){
					return false;
				}
				
				if(searchValues.q_companies && searchValues.q_companies.length > 0){
					return false;
				}
				
				if(searchValues.q_locations && searchValues.q_locations.length > 0){
					return false;
				}
				
			}
			
			//for custom fields
			if(_sideCustomAdvancedFilters.length > 0){
				return false;
			}
			
			if(_headerCustomFilters.length > 0){
				return false;
			}
			
			// custom columns
			var hasColumnFilter = false;
			for(var k in _headerCustomColumnFilters){
				var obj = _headerCustomColumnFilters[k];
				if(obj){
					if((obj["=="] && obj["=="].length > 0) || (obj["!="] && obj["!="].length > 0)){
						hasColumnFilter = true;
						break;
					}
				}
			}
			
			if(hasColumnFilter){
				return false;
			}
			
			return true;
		},
		
		saveHeaderCustomFilter:function(headerCustomFilter){
			if(!headerCustomFilter){
				return;
			}
			var index = -1;
			for(var i = 0; i < _headerCustomFilters.length; i++){
				var filter = _headerCustomFilters[i];
				if(filter.field == headerCustomFilter.field){
					index = i;
				}
			}
			if(index == -1){
				if(headerCustomFilter.conditions){
					_headerCustomFilters.push(headerCustomFilter);
				}
			}else{
				if(headerCustomFilter.conditions){
					_headerCustomFilters.splice(index, 1, headerCustomFilter);
				}else{
					_headerCustomFilters.splice(index, 1);
				}
			}
		},
		
		getHeaderCustomFilters : function(){
			return $.extend([], _headerCustomFilters);
		},
		
		getHeaderCustomFilter : function(fieldName){
			for(var i = 0; i < _headerCustomFilters.length; i++){
				var filter = _headerCustomFilters[i];
				if(fieldName == filter.field){
					return $.extend({}, filter);
				}
			}
			return null;
		},
		
		saveHeaderCustomColumnFilter : function(name, data) {
			if(data){
				_headerCustomColumnFilters["q_" + name] = data;
			}else{
				delete _headerCustomColumnFilters["q_" + name];
			}
		},
		
		getHeaderCustomColumnFilters : function() {
			return $.extend([], _headerCustomColumnFilters);
		},
		
		getHeaderCustomColumnFilter : function(name) {
			var filters = this.getHeaderCustomColumnFilters();
			return filters["q_" + name] || {};
		},
		
		saveSideCustomAdvancedFilter:function(sideCustomAdvancedFilter){
			if(!sideCustomAdvancedFilter){
				return;
			}
			var index = -1;
			for(var i = 0; i < _sideCustomAdvancedFilters.length; i++){
				var filter = _sideCustomAdvancedFilters[i];
				if(filter.field == sideCustomAdvancedFilter.field){
					index = i;
				}
			}
			if(index == -1){
				if(sideCustomAdvancedFilter.conditions){
					_sideCustomAdvancedFilters.push(sideCustomAdvancedFilter);
				}
			}else{
				if(sideCustomAdvancedFilter.conditions){
					_sideCustomAdvancedFilters.splice(index, 1, sideCustomAdvancedFilter);
				}else{
					_sideCustomAdvancedFilters.splice(index, 1);
				}
			}
		},
		
		getSideCustomAdvancedFilters : function(){
			return $.extend([], _sideCustomAdvancedFilters);
		},
		
		getSideCustomAdvancedFilter : function(fieldName){
			for(var i = 0; i < _sideCustomAdvancedFilters.length; i++){
				var filter = _sideCustomAdvancedFilters[i];
				if(fieldName == filter.field){
					return $.extend({}, filter);
				}
			}
			return null;
		}

	};

})(jQuery); 