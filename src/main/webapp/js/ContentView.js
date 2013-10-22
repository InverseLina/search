(function($) {

  brite.registerView("ContentView", {
    parent : "#contentview-ctn"
  }, {

    // --------- View Interface Implement--------- //
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

      view.empty();
      $(window).resize(function() {
        fixColWidth.call(view);
      });
      if(app.cookie("userName")){
          var userName = app.cookie("userName");
          if(userName) {
              userName = ":"+ userName
          }else{
              userName="";
          }
    	  view.$el.find(".sf-info").html((app.cookie("org")||" ")+userName.replace(/"/g,""));
      }
        brite.display("TabView").done(function(tabView){
            view.tabView = tabView;
        });

        brite.display("SavedSearches");
    },
    // --------- /View Interface Implement--------- //

    // --------- Events--------- //
    events : {
      "keypress; .search-input": function(event){
        var view = this;
        var $this = $(event.currentTarget);
        var keyword = $this.val();
        if (event.which === 13) {
          view.$el.trigger("DO_SEARCH",{search:keyword});
        }
      },
      "keyup; .big .search-input": function(event){
        var view = this;
        var $this = $(event.currentTarget);
          view.$el.find(".search-form .search-input").val($this.val());
          view.$el.trigger("SEARCH_QUERY_CHANGE");
      },
      "keyup; .search-form .search-input": function(event){
        var view = this;
        if(event.keyCode != 13){
            view.$el.trigger("SEARCH_QUERY_CHANGE");
        }
      },
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
    	  var position = {top:$th.get(0).offsetTop+$th.height() + 40,left:$th.get(0).offsetLeft+$th.width()/2-195};
//          console.log(position);
          if(position.left <  20 ){
              position.left = 20;
          }
          var popupOffset =  $(".btnPopupColumns", view.$el).offset();
          if(position.left +  400 > popupOffset.left){
              position.left =  popupOffset.left - 400;
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
              brite.display(viewName,".ContentView",{position:position,type:type, data:data,th: $th }).done(function(filterDlg){
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
          var sfid = $(event.currentTarget).closest("i").attr("data-sfid");
          var cname = $(event.currentTarget).closest("i").attr("data-cname");
          if (org.apex_resume_url && org.apex_resume_url.length && org.apex_resume_url.length > 0){
              var url = org.apex_resume_url + sfid;
              window.showModalDialog(url,"dialogWidth=540px;dialogHeight=430px");
          }else{
              brite.display("ResumeView","body", {id: cid,sfid: sfid, name:cname});
          }

      },
      "btap; tbody td.favLabel":function(event){
         var view = this;
         var $td = $(event.currentTarget);
         event.stopPropagation();
         event.preventDefault();

         var labelId = view.tabView.getSelectLabel().id;
          var contactId = $td.closest("tr").attr("data-objId");
         if(labelId){

             if($td.hasClass("hasLabel")){
                 console.log("remove class")
                 app.LabelDaoHandler.unAssign(contactId, labelId);
                 $td.removeClass("hasLabel");
             }else{
                 app.LabelDaoHandler.assign(contactId, labelId);
                 console.log("add class")
                 $td.addClass("hasLabel");
             }
         }

      },
      "btap; table td[data-column='company'],td[data-column='skill'],td[data-column='education']" : function(event) {
        var view = this;
        var $this = $(event.currentTarget);
        var name = $this.attr("data-column");
        var value = $this.closest("td").attr("data-value")||$this.find("span").text();
        $this.closest("tbody").find("tr").removeClass("select");
          $this.closest("tr").addClass("select");
          this.selectId = $this.closest("tr").attr("data-objId");
        if(value != ""){
          var data = {type: name, contactName: $this.closest("tr").attr("data-contractName"), names:value.split(","), pos: {x:event.clientX, y:event.clientY}};
          if(name=="company"){
              data.title = "Company"
          }else if(name=="skill"){
              data.title = "Skill";
          }else if(name=="education"){
              data.title = "Education";
          }
            brite.display("CellPopup", null, data);
        }
      }

    },
    // --------- /Events--------- //


    // --------- Public Methods--------- //
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
      brite.display("MessagePanel",".search-result", {message: "No Organization selected"})

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
      restoreSearchParam: function (filters){
        var key, dataName, data, displayName, $html, $th, view = this;
          if (view.selectId) {
              view.$el.find("tbody tr[data-objId='" + view.selectId + "']").addClass("select");
          }
        if(view.$el.find("table th .selectedItems .item").length > 0){
            return;
        }

        var result = filters||app.ParamsControl.getFilterParams() || {};

        for(key in result){
            dataName = key;
            if(key == "Contact"){
                dataName = "contact";
            }
            $th = view.$el.find("table thead th[data-column='{0}']".format(dataName));
            var item,  data = result[key];
            if (data && data.length > 0) {
                $.each(data, function (index, val) {
                    item = {name: val.name};

                    val = val.value;
                    if(val.minYears||val.minRadius){
                        item.min = val.minYears||val.minRadius;
                    }
                    if(item.min){
                        item.display = val.name + " (" + item.min + ")";
                    }else{
                        item.display = val.name;
                    }
                    if(data.length > 1 && index != data.length-1){
                        item.display = item.display + ",";
                    }

                    $html = $(render("search-items-header-add-item", item));

                    $th.find(".addFilter").before($html);
                });
                $th.find(".addFilter").hide();
            }
        }

    },
    // --------- /Public Methods--------- //

    // --------- Document Events--------- //
    docEvents: {
      "ADD_FILTER":function(event, extra){
          var view = this;
          app.ParamsControl.save(extra);
          view.restoreSearchParam();
          view.$el.trigger("DO_SEARCH");
          if(view.filterDlg && view.filterDlg.$el) {
              //view.filterDlg.$el.trigger("SHOWSEARCHRESULT", {});
          }
      },
      "REMOVE_FILTER":function(event, extra){
          var view = this;
          app.ParamsControl.remove(extra);
          view.restoreSearchParam();
          view.$el.trigger("DO_SEARCH");
          if(view.filterDlg && view.filterDlg.$el) {
              //view.filterDlg.$el.trigger("SHOWSEARCHRESULT", {});
          }

      },
      "UPDATE_FILTER": function(event, extra){
          var view = this;
          view.$el.trigger("DO_SEARCH");
      },
      "ON_ERROR":function(event, extra) {
          var view = this;
          if(extra){
            view.showErrorMessage(extra.errorCode||"", extra.errorMessage||"");
          }
      },
      "ERROR_PROCESS": function (event, extra) {
            var view = this;
            view.$searchInfo.empty();
            view.$el.find("input").attr("disabled", true);

            brite.display("MessagePanel", ".search-result", {message: extra.errorMessage})
        } ,
      "CHANGE_TO_FAV_VIEW": function(event, extra){
          var view = this;
          view.$el.find(".tableContainer th").addClass("favFilter");
          view.$el.find(".search-result").addClass("favFilter");
          view.$el.find(".tableContainer tbody").addClass("favFilter");
          view.$el.find(".btnPopupColumns").addClass("favFilter");
          view.$el.find(".search-form").hide();
          view.$el.find(".saveSearchesContainer ").hide();
          view.$el.find(".empty-search").hide();
          view.$el.find(".page").hide();
      },
      "RESTORE_SEARCH_VIEW": function(event){
         var view = this;
          view.$el.find(".tableContainer th").removeClass("favFilter");
          view.$el.find(".search-result").removeClass("favFilter");
          view.$el.find(".tableContainer tbody").removeClass("favFilter");
          view.$el.find(".btnPopupColumns").removeClass("favFilter");
          view.$el.find(".search-form").show();
          view.$el.find(".saveSearchesContainer ").show();
          view.$el.find(".page").show();
      },
        CHANGE_SELECT_LABEL: function(event){
            var view = this;
            var label = view.tabView.getSelectLabel();
            view.$el.find("tbody td.favLabel").attr("title", label.name);
            var $icons =  view.$el.find("tbody td.favLabel i");
            if(label.name == "Favorites"){
                $icons.removeClass("glyphicon-stop").addClass("glyphicon-star");
            }else{
                $icons.removeClass("glyphicon-star").addClass("glyphicon-stop");
            }
        }
    },
    // --------- /Document Events--------- //


    // --------- Parent Events--------- //
    parentEvents : {

      MainView : {
        "SEARCH_RESULT_CHANGE" : function(event, result) {
          var view = this;
          var $e = view.$el;
            if (org['action_add_to_sourcing'] == "true") {
                view.$el.find(".btnAddToSourcingProject").removeClass("hide");
            } else {
                view.$el.find(".btnAddToSourcingProject").addClass("hide");
            }

          var html;
          var htmlInfo = " <span class='resultTime'> (c:{0}ms, s:{1}ms)</span>".format(result.countDuration , result.selectDuration);
          htmlInfo += "<span class='resultCount' style='{2}: {3}px'>{0} match{1}</span>";

          if (result.count > 0) {
//            $e.find(".actions").show();
            buildResult.call(view, result.result).done(function(data){
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
                 callback : function(pageIdx, pageSize){
                     view.pageIdx = pageIdx;
                     view.pageSize = pageSize;
                     view.$el.trigger("DO_SEARCH",{pageIdx:pageIdx});
                 }
               }).done(function(){
                       var pagination = view.$el.find(".pagination");
                       showSearchInfo.call(view, result, htmlInfo, "left", (pagination.offset().left - view.$searchInfo.offset().left -155 ))
                   });


              //restore input values
              $e.find(".search-input").val(app.ParamsControl.getQuery());

              view.restoreSearchParam();
            });


          } else {
            $e.find(".actions").hide();
            view.$searchResult.find(".tableContainer").html(render("search-query-notfound", {
              colWidth : getColWidth.call(view)
            }));
            view.$searchResult.find(".page").empty();
            fixColWidth.call(view);
              showSearchInfo.call(view, result, htmlInfo, "right", 0);
              view.restoreSearchParam();

          }



        }

      }
    },
    // --------- /Parent Events--------- //

    // --------- Public Methods--------- //
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
    // --------- /Public Methods--------- //

  });

  // --------- Private Methods--------- //
  function buildResult(items) {
    var result = [];
    var item;
    var columns = app.preference.columns();
    var colLen = columns.length;
    var view = this;
    var dtd = $.Deferred();
    var label = view.tabView.getSelectLabel();
    var isFav = label.name == "Favorites" ? true: false;
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
                realValue: items[i][columns[j]],
  	            notLast : colLen - j > 1
  	          });
  	        } else if (columns[j] == "resume") {
              var value = "", resume = items[i][columns[j]];

               if(items[i][columns[j]] != -1){
                  value = "<i data-id='" + items[i][columns[j]] + "' data-sfid='"+items[i]["sfid"]+"' data-cname='"+items[i]["name"]+"' title='View Resume.' class='resume-ico glyphicon glyphicon-file'></i>";
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
  	           var displayValue = "<a class='lineInfo name' href='"+org.instance_url+items[i]["sfid"]+"'>"+items[i]["name"]+"</a>";
  	           displayValue += "<div class='lineInfo title'>"+items[i]["title"]+"</div>";
  	           displayValue += "<a class='lineInfo email' href='mailTo:"+items[i]["email"]+"'>"+items[i]["email"]+"</a>";
  	           //displayValue += "<div class='lineInfo phone'>"+items[i]["phone"]+"</div>";
  	            item.push({
  	                name : columns[j],
  	                value : displayValue,
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
  	        row : item,
            names: {id: items[i].id, name: items[i].name},
            hasLabel: items[i].haslabel,
            isFav: isFav,
            labelName: label.name
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
      } else if ($item.hasClass("favLabel")) {
        realWidth = 32;
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
        //fix for ie
        $body.find("td[data-column='" + colName + "'] > span").css({
            width : Math.floor(realWidth - 4),
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
  // --------- /Private Methods--------- //


})(jQuery); 