var app = app || {};

(function ($) {
    var cache;


    function genericHeaderRenderer(filterInfo) {
        return render("search-query-generic-render-header", filterInfo);
    }

    function genericCellRenderer(filterInfo) {
        return render("search-query-generic-render-header", filterInfo);
    }

    var getConfig = function () {
        var org = app.cookie("org");
        if (org) {
            var filters = null;
            app.getJsonData("searchuiconfig", {org: org}, {async: false}).done(function (result) {
                filters = result;
            });
            return $.map(filters, function (item) {
                item.serverName = item.name;
                item.name = item.type;
                item.display = item.title;
                if (item.type == 'custom') {
                    item.name = item.serverName;
                }
                return item;
            });
        } else {
            return null;
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
            filterRenderer: function ($content, headerInfo) {
                var data = app.ParamsControl.getFilterParams()['contract'] || [];
                return brite.display("ContactFilterView", $content, {data: data, th: headerInfo });
                // for example
            }
        },
        company: {
            filterRenderer: function ($content, headerInfo) {
                var data = app.ParamsControl.getFilterParams()['company'] || [];
                brite.display("CompanyFilterView", $content, {data: data, th: headerInfo });
            },
        },
        skill: {
            filterRenderer: function ($content, headerInfo) {
                var data = app.ParamsControl.getFilterParams()['skill'] || [];
                brite.display("SkillFilterView", $content, {data: data, th: headerInfo });
            },
        },
        education: {
            filterRenderer: function ($content, headerInfo) {
                var data = app.ParamsControl.getFilterParams()['education'] || [];
                brite.display("EducationFilterView", $content, {data: data, th: headerInfo });
            },

        },
        location: {
            filterRenderer: function ($content, headerInfo) {
                var data = app.ParamsControl.getFilterParams()['location'] || [];
                brite.display("LocationFilterView", $content, {data: data, th: headerInfo });
            }
        },

    }

    var genericRender = {
        headerRenderer: function (filterInfo) {
            return render("search-query-generic-render-header", filterInfo);
        },
        fitlerRenderer: function (headerInfo, popup) {
            // render the inside of the filter popup
            // should probably call something like
            brite.display("ContactFilterView", popup.$content, headerInfo); // for example
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
