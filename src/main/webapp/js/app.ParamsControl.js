var app = app || {};
(function($){
    var _storeValue = {};
    function getMainView(){
        return $("body .MainView").bView("MainView");
    }
    app.ParamsControl = {
        getParamsForSearch: function(){
            var view = getMainView();
//            console.log(_storeValue);
            var obj, key, newKey;
            var data, result = {};
            var searchData = result.searchValues = {};
            var contentSearchValues = view.contentView.getSearchValues();
            result.searchColumns = app.preference.columns().join(",");
            if(contentSearchValues.sort){
                result.orderBy = contentSearchValues.sort.column;
                result.orderType =  !!(contentSearchValues.sort.order === "asc");
            }
            if(!/^\s*$/.test(contentSearchValues.search)){
                searchData.q_search = $.trim(contentSearchValues.search)
            }

            for (key in _storeValue) {
                newKey = key.substring(0,1).toLocaleLowerCase() + key.substring(1);
                if (newKey == "contact") {
                    data = searchData["q_contacts"] = []; // = view._searchValues[key];

                } else {
                    if(newKey == "company"){
                       data = searchData["q_companies"] = {};
                    }else{
                        data = searchData["q_" + newKey + "s"] = {};
                    }
                    data = data.values = [];
                }
                $.each(_storeValue[key], function(idx, item){
                    data.push(item.value);
                });
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
            store.push({name: data.name, value: data.value});
        },
        /**
         * remove data format {type:xxx, name: xxx}
         * @param data
         */
        remove: function(data){
            var index, obj, store, found = -1;
            if(!data && !data.type && !data.name){
                return;
            }
            if(!_storeValue[data.type]){
                _storeValue[data.type] = [];
            }
            store = _storeValue[data.type];
            //delete store[data.name];
            for (index = 0; index < store.length; index++) {
                obj = store[index];
                if(obj.name == data.name){
                    found = index;
                    break;
                }
            }
            if(found != -1){
//                delete store[index];
                store.splice(index, 1);
            }
        },
        getFilterParams: function(){
            return _storeValue||{};
        }
    }

})(jQuery);