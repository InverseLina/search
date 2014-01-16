var app = app || {};
(function($){
    var _storeValue = {};
    var queryKey = "";
    function getMainView(){
        return app.MainView;
    }
    app.ParamsControl = {
        getParamsForSearch: function(params){
            params = params ||{};
            var   view = getMainView();
            var obj, key, newKey;
            var data, result = {};
            var searchData = result.searchValues = {};
            var contentSearchValues = view.contentView.getSearchValues();
            queryKey = $.trim(params.search||contentSearchValues.search);
            result.searchColumns = app.preference.columns().join(",");
            if(contentSearchValues.sort){
                result.orderBy = contentSearchValues.sort.column;
                result.orderType =  !!(contentSearchValues.sort.order === "asc");
            }
            if(!/^\s*$/.test(queryKey)){
                searchData.q_search = queryKey;
            }

            var pathInfo = app.buildPathInfo();
            if(pathInfo.paths && pathInfo.paths.length == 3 && pathInfo.paths[1] == "list"){
                searchData.q_labelAssigned = true;
                searchData.q_label = view.contentView.tabView.getLabelName(pathInfo.paths[2])
            }else{
                searchData.q_labelAssigned = false;
                searchData.q_label = view.contentView.tabView.getSelectLabel().name;
            }

/*            searchData.q_labelAssigned = params.labelAssigned || false;
            if(searchData.q_labelAssigned){
                searchData.q_label =  params.label ||"Favorites";
            }else{
                searchData.q_label = view.contentView.tabView.getSelectLabel().name;
            }*/


            for (key in _storeValue) {
                newKey = key.substring(0, 1).toLocaleLowerCase() + key.substring(1);
                data = [];
                $.each(_storeValue[key], function(idx, item){
                    if (newKey == "contact") {
                       data.push(item.value.value);
                    }else{
                        data.push(item.value);
                    }
                });
                if (data.length > 0) {
                    if (newKey == "company") {
                        searchData["q_companies"] = data;
                    } else {
                        searchData["q_" + newKey + "s"] = data;
                    }
                }
            }
            result.searchValues = JSON.stringify(searchData);
            result.pageIndex = view.contentView.pageIdx || 1;
            result.pageSize = view.contentView.pageSize || 15;
//         console.log(result);
            return result;
        },
        /**
         * save data {type: xx, name: xx, val:xx}
         * @param data
         */
        save: function(data){
           var store;
           if(!data && !data.type && !data.name){
               return;
           }
           if(!_storeValue[data.type]){
//              _storeValue[data.type] = {};
              _storeValue[data.type] = [];
           }
            store = _storeValue[data.type];
//            store[data.name] = data.value;
            delete data.type;
            store.push({name: data.name, value: data});
            /*if(data.minVal){
                store.minVal = data.minVal;
            }*/
        },
        /**
         * remove data format {type:xxx, name: xxx}
         * @param data
         */
        remove: function(data){
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
                for (index = 0; index < store.length; index++) {
                    obj = store[index];
                    if (obj.name == data.name) {
                        found = index;
                        break;
                    }
                }
                if (found != -1) {
//                delete store[index];
                    store.splice(index, 1);
                }
            }else{
                if(data){
                    delete _storeValue[data]
                }
            }
        },
        getFilterParams: function(){
            return _storeValue||{};
        },
        setFilterParams:function(filters){
            _storeValue = filters||{};
        },
        get: function (type, name) {
            var i, obj, data = _storeValue[type]||[];
            if (!name) {
                return data;
            } else {
                for (i = 0; i < data.length; i++) {
                    obj = data[i];
                    if (obj.name == name) {
                        return obj;
                    }
                }
            }
            return null;
        },
        getQuery: function(){
            return queryKey;
        },
        clear:function(){
        	_storeValue = {};
        }
    }

})(jQuery);