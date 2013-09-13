(function($) {

  brite.registerView("ContentView", {
    parent : "#contentview-ctn"
  }, {
    create : function() {
      return render("ContentView");
    },

    postDisplay : function() {
      var view = this;
      view.$searchInput = view.$el.find(".search-input");
      view.$searchResult = view.$el.find(".search-result");
      view.$searchInfo = view.$el.find(".search-info");
      view.tableOrderColumn = null;
      view.tableOrderType = null;

      view.$searchInput.on("keypress", function(event) {
        if (event.which === 13) {
          view.$el.trigger("DO_SEARCH");
        }
      });
      view.empty();
      $(window).resize(function() {
        fixColWidth.call(view);
      });

    },
    events : {
      "btap; table .locationTh span.columnName,table .contactTh span.columnName" : function(event) {
        var view = this;
        var $th = $(event.currentTarget).closest("th");
        var $desc = $(".desc", $th);
        var $asc = $(".asc", $th);
        var column = $th.attr("data-column");
        var bPagination = view.$el.bComponent("ContentView");
        var pageIdx = bPagination.pageIdx || 1;
        var pageSize = bPagination.pageSize || 30;
        if(column=="company"||column=="skill"||column=="education"||column=="resume"){
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
      "btap; table th[data-column]" : function(event) {
          event.preventDefault();
          event.stopPropagation();
    	  var view = this;
    	  //$("[data-b-view^='Filter']",view.$el).bRemove();
    	  var $th = $(event.currentTarget);
    	  var position = {top:$th.get(0).offsetTop+$th.height() + 40,left:$th.get(0).offsetLeft+$th.width()/2-175};
//          console.log(position);
          if(position.left <  20 ){
              position.left = 20;
          }
    	  var type = $th.attr("data-column"); 

          var data = app.ParamsControl.getFilterParams()[type]||[];

          if(type=="company") {
              type= "employer";
          }
          if(type=="contact") {
              data = app.ParamsControl.getFilterParams()["Contact"]||[];
          }
          var viewName = "Filter"+type.substring(0, 1).toUpperCase()+type.substring(1);
    	  if(type=="skill"||type=="contact"||type=="education"||type=="employer"||type=="location"){
              if(view.filterDlg){
                  view.filterDlg.close();
              }
              brite.display(viewName,".ContentView",{position:position,type:type, data:data}).done(function(filterDlg){
                   view.filterDlg =  filterDlg
              });
           }
      },
      "change; .tableContainer td input[type='checkbox']" : function(event) {
        var view = this;
        var checkLen = view.$el.find(".tableContainer td input:not(:checked)").length;
        if (checkLen > 0) {
          view.$el.find("label.selectAll.checkbox").removeClass("selected");
          view.$el.find("label.selectAll.checkbox input").prop("checked", false);
        } else {
          view.$el.find("label.selectAll.checkbox").removeClass("selected").addClass("selected");
          view.$el.find("label.selectAll.checkbox input").prop("checked", true);
        }
      },
      "btap; div.btnPopupColumns" : function(event) {
        brite.display("SelectColumns");
      },
      "change; label.selectAll.checkbox" : function(event) {
        event.stopPropagation();
        var view = this;
        var $target = $(event.currentTarget);
        if ($target.hasClass("selected")) {
          $target.removeClass("selected");
          $target.find("input").prop("checked", false);
          view.$el.find(".tableContainer input[type='checkbox']").prop("checked", false);
        } else {
          $target.addClass("selected");
          $target.find("input").prop("checked", true);
          view.$el.find(".tableContainer input[type='checkbox']").prop("checked", true);
        }
      },
      "btap; .btnAddToSourcingProject" : function(event) {
        event.stopPropagation();
        alert("Not implement yet. (waiting for API)");
      },
      "btap; .resume-ico" : function(event) {
          var cid = $(event.currentTarget).closest("i").attr("data-id");
          brite.display("ResumeView","body", {id: cid})
      },

      "keypress;.search-input" : function(event) {
        var view = this;
        if (event.which === 13) {
          view.$el.trigger("DO_SEARCH");
        }
      }

    },
    showErrorMessage : function(title, detail) {
      var view = this;
      view.$searchInfo.empty();
      var html = render("search-query-error", {
        title : title,
        detail : detail,
        colWidth : getColWidth.call(view)
      });
      view.$searchResult.find(".tableContainer").html(html);
      view.$searchResult.find(".page").empty();
      fixColWidth.call(view);

    },
    empty : function() {
      var view = this;
      view.$searchInfo.empty();
      view.$searchResult.find(".tableContainer").html(render("search-empty", {
        colWidth : getColWidth.call(view)
      }));
      view.$searchResult.find(".page").empty();
      fixColWidth.call(view);
    },
    loading : function() {
      var view = this;
      view.$searchInfo.empty();
      view.$searchResult.find(".page").empty();
      view.$searchResult.find(".tableContainer").html(render("search-loading"));
      fixColWidth.call(view);
    },
    docEvents: {
      "ADD_FILTER":function(event, extra){
          var type, view = this;
//          console.log(extra);
          if(extra) {
             type = extra.type.substring(0,1).toLocaleLowerCase() + extra.type.substring(1);
          }
          var $th = view.$el.find("th[data-column='" + type + "']");
          $th.find("span.addFilter").before(render("search-items-header-add-item", {name: extra.name}));
          var ele = $th.find(".selectedItems span.item[data-name='" + extra.name + "']");
//          ele.data("value", extra.value);
//          c
//          console.log(extra);

          var $th = view.$el.find("th[data-column='" + type + "']");
          if($th.find(".selectedItems .item").length > 0) {
              $th.find(".selectedItems .addFilter").hide();
          }else{
              $th.find(".selectedItems .addFilter").show();
          }
          app.ParamsControl.save(extra);
          view.$el.trigger("DO_SEARCH");
          if(view.filterDlg && view.filterDlg.$el) {
              //view.filterDlg.$el.trigger("SHOWSEARCHRESULT", {});
          }
      },
      "REMOVE_FILTER":function(event, extra){
          var type, view = this;
          if(extra) {
              if(extra.type == "Contact"){
                  type = "contact";
              }else{
                  type = extra.type.substring(0,1).toLocaleLowerCase() + extra.type.substring(1);
              }
          }
          view.$el.find("th[data-column='" + type + "']").find(".selectedItems span.item[data-name='" + extra.name + "']").remove();
/*          setTimeout(function(){
              view.$el.trigger("SEARCH_PARAMS_CHANGE");
          }, 200);*/

          var $th = view.$el.find("th[data-column='" + type + "']");
          if($th.find(".selectedItems .item").length > 0) {
              $th.find(".selectedItems .addFilter").hide();
          }else{
              $th.find(".selectedItems .addFilter").show();
          }
          app.ParamsControl.remove(extra);
          view.$el.trigger("DO_SEARCH");
          if(view.filterDlg && view.filterDlg.$el) {
              //view.filterDlg.$el.trigger("SHOWSEARCHRESULT", {});
          }

      },
      UPDATE_FILTER: function(event, extra){

      }
    },
    parentEvents : {

      MainView : {
        "SEARCH_RESULT_CHANGE" : function(event, result) {
          var view = this;
          var $e = view.$el;
          $.ajax({
  			url:"/config/get/action_add_to_sourcing",
  			type:"Get",
  			dataType:'json'
	  	  }).done(function(data){
	  		if(data.success){
	  			if(data.result[0]&&data.result[0].value=="true"){
	  				view.$el.find(".btnAddToSourcingProject").removeClass("hide");
	  			}else{
	  				view.$el.find(".btnAddToSourcingProject").addClass("hide");
	  			}
	  		}
	  	  });


          var html;
          var htmlInfo = " <span class='resultTime'> (c:{0}ms, s:{1}ms)</span>".format(result.countDuration , result.selectDuration);
          htmlInfo += "<span class='resultCount' style='{2}: {3}px'>{0} match{1}</span>";

          if (result.count > 0) {
//            $e.find(".actions").show();
            buildResult(result.result).done(function(data){
        	   html = render("search-items", {
                 items : data,
                 colWidth : getColWidth.call(view)
               });
               view.$searchResult.find(".tableContainer").html(html);

               //show desc/asc
               if (view.tableOrderColumn && view.tableOrderType) {
                 $e.find("table th[data-column='" + view.tableOrderColumn + "']").find("." + view.tableOrderType).show();
               }
               fixColWidth.call(view);

               brite.display("Pagination", view.$el.find(".page"), {
                 pageIdx : result.pageIdx,
                 pageSize : result.pageSize,
                 totalCount : result.count,
                 callback : result.callback
               }).done(function(){
                       var pagination = view.$el.find(".pagination");
                       showSearchInfo.call(view, result, htmlInfo, "left", (pagination.offset().left - view.$searchInfo.offset().left -155 ))
                   });
            });
          } else {
            $e.find(".actions").hide();
            view.$searchResult.find(".tableContainer").html(render("search-query-notfound", {
              colWidth : getColWidth.call(view)
            }));
            view.$searchResult.find(".page").empty();
            fixColWidth.call(view);
              showSearchInfo.call(view, result, htmlInfo, "right", 0);
          }

        }

      }
    },

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

  });

  function buildResult(items) {
    var result = [];
    var item;
    var columns = app.preference.columns();
    var colLen = columns.length;
    var view = this;
    var dtd = $.Deferred();
	 $.ajax({
			url:"/config/get/local_date",
			type:"Get",
			dataType:'json'
  	  }).done(function(config){
  		var dateFormat = "YYYY-MM-DD";
  		if(config.result&&config.result[0]){
  			dateFormat = config.result[0].value;
  		}
  	    for (var i = 0; i < items.length; i++) {
  	      item = [];
  	      for (var j = 0; j < columns.length; j++) {
  	        if (columns[j] == "skill") {
  	          item.push({
  	            name : columns[j],
  	            value : translate(items[i][columns[j]]),
  	            notLast : colLen - j > 1
  	          });
  	        } else if (columns[j] == "resume") {
              var value = "", resume = items[i][columns[j]];

               if(items[i][columns[j]] != -1){
                  value = "<i data-id='" + items[i][columns[j]] + "' title='View Resume.' class='resume-ico glyphicon glyphicon-file'></i>";
               }

  	          item.push({
  	            name : columns[j],
  	            value : value,
  	            notLast : colLen - j > 1
  	          });
  	        } else if (columns[j] == "CreatedDate") {
  	            item.push({
  	                name : columns[j],
  	                value : formateDate(items[i][columns[j]],dateFormat),
  	                notLast : colLen - j > 1
  	              });
  	         } else if (columns[j] == "contact") {
  	            item.push({
  	                name : columns[j],
  	                value : items[i]["name"]+"<br/>"+items[i]["email"]+"<br/>"+items[i]["title"],
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
  	        row : item
  	      });
  	    }
  	    dtd.resolve(result);
  	  });
   return dtd.promise();
  }

  function getColWidth() {
    var view = this;
    var colLen = app.preference.columns().length;
    //        return parseInt((view.$searchResult.innerWidth()-30)/colLen)-2;
    return 100 / colLen;
  }

  function fixColWidth() {
    var view = this;
    var colWidth;
    var colName;
    var columns = app.preference.columns();
    var colLen = columns.length;
    var tableWidth = view.$el.find(".tableContainer").width() - 20;
      var excludes = ["id", "CreatedDate","title","email", "resume"];
    if ($.inArray("id", columns) >= 0) {
      tableWidth = tableWidth - 80;
      colLen--;
    }
    if ($.inArray("CreatedDate", columns) >= 0) {
      tableWidth = tableWidth - 110;
      colLen--;
    }
    if ($.inArray("resume", columns) >= 0) {
        tableWidth = tableWidth - 65;
        colLen--;
      }
    //checkbox
    tableWidth = tableWidth - 30;
    if (colLen != 0) {
      colWidth = tableWidth / colLen;
    } else {
      colWidth = tableWidth;
    }
    var realWidth;

    var $body = view.$el.find("tbody");
    var $head = view.$el.find("thead");
    var tlen = $head.find("th").length - 1;

    $head.find("th").each(function(idx, item) {
      var $item  = $(item);
      colName = $item.attr("data-column");
      if (colName == "id") {
        realWidth = 80;
        if (idx == tlen) {
          realWidth = colWidth + 80;
        }
      } else if (colName == "CreatedDate") {
        realWidth = 110;
        if (idx == tlen) {
          realWidth = colWidth + 110;
        }
      } else if ($item.hasClass("checkboxCol")) {
        realWidth = 30;
      } else if (colName=="resume") {
        realWidth = 65;
      } else {
        realWidth = colWidth;
      }
      if (idx == tlen) {
          $item.css({
          width : realWidth + 50,
          "max-width" : realWidth + 50,
          "min-width" : realWidth
        });
      } else {
          $item.css({
          width : realWidth,
          "max-width" : realWidth,
          "min-width" : realWidth
        });
      }
      $body.find("td[data-column='" + colName + "']").css({
        width : realWidth,
        "max-width" : realWidth,
        "min-width" : realWidth
      });
        //hide filter
        if($.inArray(colName, excludes) >= 0){
            $item.find(".addFilter").hide();
        }

    })
  }



  function translate(value) {
    if (!value) {
      return "";
    }
    var items = value.split(","), result = [];
    var len = items.length;
    for (var i = 0; i < len; i++) {
      if (items[i].length > 1) {
        result.push(items[i].substr(0, 1).toUpperCase() + items[i].substr(1).toLowerCase());
      } else {
        result.push(items[i]);
      }
    }
    return result.join(",");
  }

  function formateDate(date,format){
	  if(!date||date==""){
		  return "";
	  }
	  date = new Date(date);
	  var year = date.getYear()+1900;
	  var month = date.getMonth()+1;
	  if(month<10){
		  month="0"+month;
	  }
	  var day = date.getDate();
	  if(day<10){
		  day="0"+day;
	  }
	  return format.replace("YYYY",year).replace("MM",month).replace("DD",day);
  }

  function showSearchInfo(result, htmlInfo, direct,  offset){
       var view = this;
      view.$searchInfo.html(htmlInfo.format(result.count, result.count > 1 ? "es":"", direct, offset));
  }
})(jQuery); 