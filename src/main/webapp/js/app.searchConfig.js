var app = app || {};

(function ($) {
    var cache;


    function genericHeaderFilterRenderer(filterInfo) {
        return render("search-query-generic-filter-render-header", filterInfo);
    }

    var getConfig = function () {
        var org = app.cookie("org");
        if (org) {
            var filters = null;
            app.getJsonData("searchuiconfig", {org: org}, {async: false}).done(function (result) {
                filters = result;
            });
            var temp = [];
            $.each(filters||{}, function (idx, item) {
//                item.serverName = item.name;
//                item.name = item.type;
                item.display = item.title;
//                if (item.type == 'custom') {
//                    item.name = item.serverName;
//                }
                temp.push(item);
                temp[item.name] = item;
            });
            return temp;
        } else {
            return {};
        }
    };

    app.getSearchUiConfig = function (update) {
        if (update) {
            cache = getConfig();
        }
        if (cache) {
            return cache;
        } else {
            cache = getConfig();
            return cache;
        }
    }


    var filters = {
        contact: {
            headerRenderer: genericHeaderFilterRenderer,
            filterRenderer: function ($content, headerInfo) {
                var data = app.ParamsControl.getFilterParams()['contract'] || [];
                return brite.display("ContactFilterView", $content, {data: data, th: headerInfo });
                // for example
            }
        },
        company: {
            headerRenderer: genericHeaderFilterRenderer,
            filterRenderer: function ($content, headerInfo) {
                var data = app.ParamsControl.getFilterParams()['company'] || [];
                brite.display("CompanyFilterView", $content, {data: data, th: headerInfo });
            },
        },
        skill: {
            headerRenderer: genericHeaderFilterRenderer,
            filterRenderer: function ($content, headerInfo) {
                var data = app.ParamsControl.getFilterParams()['skill'] || [];
                brite.display("SkillFilterView", $content, {data: data, th: headerInfo });
            },
        },
        education: {
            headerRenderer: genericHeaderFilterRenderer,
            filterRenderer: function ($content, headerInfo) {
                var data = app.ParamsControl.getFilterParams()['education'] || [];
                brite.display("EducationFilterView", $content, {data: data, th: headerInfo });
            },

        },
        location: {
            headerRenderer: genericHeaderFilterRenderer,
            filterRenderer: function ($content, headerInfo) {
                var data = app.ParamsControl.getFilterParams()['location'] || [];
                brite.display("LocationFilterView", $content, {data: data, th: headerInfo });
            }
        }

    }
    //for generic not default filter render
    var genericRender = {
        headerRenderer: function (filterInfo) {
            return render("search-query-generic-render-header", filterInfo);
        },
        filterRenderer: function ($content, headerInfo) {

            var type = headerInfo.attr("data-column");
            var data = app.ParamsControl.getFilterParams()[type] || [];
            var filterInfo = app.getSearchFilter(type);
            brite.display("GenericFilterView", $content,  {data: data, th: headerInfo, filterInfo: filterInfo }); // for example
        },
        cellRenderer: function (cellInfo) {
            return render("search-query-generic-render-cell", cellInfo);
        }
    }

    function getRender(field, renderName) {
        var renderer = filters[field];

        if (!renderer) {
            renderer = genericRender;
        } else {
            if (!renderer[renderName]) {
                renderer = genericRender
            }
        }
        return renderer[renderName];
    }

    app.getFilterRender = function (field) {
        return getRender(field, "filterRenderer");
    }
    app.getHeaderRender = function (field) {
        return getRender(field, "headerRenderer");
    }
    app.getCellRender = function (field) {
        return getRender(field, "cellRenderer");
    }

})(jQuery);
