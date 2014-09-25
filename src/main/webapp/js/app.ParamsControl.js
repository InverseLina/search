var app = app || {};
(function($) {
	var _storeValue = {};
	var queryKey = "";
	
	var _headerCustomFilters = [];
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
			result.searchColumns = app.preference.columns().join(",");
			if (contentSearchValues.sort) {
				if(new RegExp(contentSearchValues.sort.column,"i").test(app.preference.columns())){//only the sort header shown 
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
			var customFilterPopup = view.$el.find(".CustomFilterPopup").bView();
			if(customFilterPopup){
				var customFields = customFilterPopup.getValues();
				searchData["q_customFields"] = customFields;
			}
			
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
			var view = getMainView();
			var customFilterPopup = view.$el.find(".CustomFilterPopup").bView();
			if(customFilterPopup){
				var customFields = customFilterPopup.getValues();
				if(customFields && customFields.length > 0){
					return false;
				}
			}
			
			return true;
		},
		
		saveHeaderCustomFilter:function(headerCustomFilter){
			var index = 0;
			for(var i = 0; i < _headerCustomFilters.length; i++){
				var filter = _headerCustomFilters[i];
				if(filter.field == _headerCustomFilters.field){
					index = i;
				}
			}
			_headerCustomFilters.splice(index, 1, headerCustomFilter);
		}

	};

})(jQuery); 