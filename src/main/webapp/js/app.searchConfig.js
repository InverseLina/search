var app = app || {};

(function ($) {
    var cache;


    function genericHeaderRender(filterInfo){
        return render("search-query-generic-render-header", filterInfo);
    }

    var getConfig = function () {
        var org = app.cookie("org");
        if(org){
            var filters = null;
            app.getJsonData("searchuiconfig", {org: org}, {async: false}).done(function(result){
                filters = result;
            });
            return $.map(filters, function(item){
               item.serverName = item.name;
               item.name = item.type;
               item.display = item.title;
               if(item.type == 'custom'){
                   item.name = item.serverName;
               }
               return item;
            });
        }else{
            return null;
        }
    };

    app.getSearchUiConfig = function(update){
        if(update){
            cache = getConfig();
        }
        if(cache){
            return cache;
        }else{
            cache = getConfig();
            return cache;
        }
    }

    var filters = {
        contact:{
            headerRenderer: genericHeaderRender,
            filterRenderer: function(headerInfo,pos, data, fn){
                brite.display("FilterContact",".ContentView",{position:pos,type:"contact",
                    data:data,th: headerInfo }).done(function(result){
                        fn(result);
                    });
            },
            cellRenderer: function(cellInfo){
                // remember, in the cell, we should not have $elements or brite.display, because, we just
                // generate a string to be fast, so, this should only render string as part of the handlebar
                // rendering
                return render("cellRendererContact",cellInfo);
            }
        },
        employer:{
            headerRenderer: genericHeaderRender,
            filterRenderer: function(headerInfo,pos, data, fn){
                brite.display("FilterEmployer",".ContentView",{position:pos,type:"employer",
                    data:data,th: headerInfo }).done(function(result){
                        fn(result);
                    });
            },
            cellRenderer: function(cellInfo){
                // remember, in the cell, we should not have $elements or brite.display, because, we just
                // generate a string to be fast, so, this should only render string as part of the handlebar
                // rendering
                return render("cellRendererContact",cellInfo);
            }
        },
        skill:{
            headerRenderer: genericHeaderRender,
            filterRenderer: function(headerInfo,pos, data, fn){
                brite.display("FilterSkill",".ContentView",{position:pos,type:"skill",
                    data:data,th: headerInfo }).done(function(result){
                        fn(result);
                    });
            },
            cellRenderer: function(cellInfo){
                // remember, in the cell, we should not have $elements or brite.display, because, we just
                // generate a string to be fast, so, this should only render string as part of the handlebar
                // rendering
                return render("cellRendererContact",cellInfo);
            }
        },
        education:{
            headerRenderer:genericHeaderRender,
            filterRenderer: function(headerInfo,pos, data, fn){
                brite.display("FilterEducation",".ContentView",{position:pos,type:"education",
                    data:data,th: headerInfo }).done(function(result){
                        fn(result);
                    });
            },
            cellRenderer: function(cellInfo){
                // remember, in the cell, we should not have $elements or brite.display, because, we just
                // generate a string to be fast, so, this should only render string as part of the handlebar
                // rendering
                return render("cellRendererContact",cellInfo);
            }
        },
        location:{
            headerRenderer: genericHeaderRender,
            filterRenderer: function(headerInfo,pos, data, fn){
                brite.display("FilterLocation",".ContentView",{position:pos,type:"location",
                    data:data,th: headerInfo }).done(function(result){
                        fn(result);
                    });
            },
            cellRenderer: function(cellInfo){
                // remember, in the cell, we should not have $elements or brite.display, because, we just
                // generate a string to be fast, so, this should only render string as part of the handlebar
                // rendering
                return render("cellRendererContact",cellInfo);
            }
        },

    }

    var genericRender = {
        headerRenderer: genericHeaderRender,
        fitlerRenderer: function(headerInfo,popup){
            // render the inside of the filter popup
            // should probably call something like
            brite.display("ContactFilterView",popup.$content,headerInfo); // for example
        },
        cellRenderer: function(cellInfo){
            // remember, in the cell, we should not have $elements or brite.display, because, we just
            // generate a string to be fast, so, this should only render string as part of the handlebar
            // rendering
            return render("cellRendererContact",cellInfo);
        }
    }

    app.getFilterRender = function(field){
        var render = filters[field];
        if(!render){
            render = genericRender;
        }
        return render;
    }

})(jQuery)
