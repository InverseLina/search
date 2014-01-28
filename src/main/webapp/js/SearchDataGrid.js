(function($) {

  brite.registerView("SearchDataGrid", {
    parent : ".ContentView"
  }, {

    // --------- View Interface Implement--------- //
    create : function() {
      return render("SearchDataGrid");
    },

    postDisplay : function() {
      var view = this;
      view.$searchInput = view.$el.find(".search-input");
      view.$searchResult = view.$el.find(".search-result");
      view.tableOrderColumn = null;
      view.tableOrderType = null;
      if(typeof(app.orgInfo)!='undefined'){
    	  view.labelDisable = app.orgInfo.config_userlistFeature == 'false' ? true : false;
      }
      view.showContentMessage("empty");
      
        brite.display("TabView",null, {hide: view.labelDisable}).done(function(tabView){
            view.tabView = tabView;
        });

        brite.display("ToolBar");
        if(app.startError){
           setTimeout(function(){
           view.$el.find(".search-info").empty();
            view.$el.find("input").attr("disabled", true);
            if(app.startError.errorCode == "NO_PASSCODE"){
                if($("body").bFindComponents("PassCodeModal").length ==0){
                    brite.display("PassCodeModal");
                }
            }else{
                brite.display("MessagePanel", ".search-result", {message: app.startError.errorMessage})
            }

            delete app.startError;
           }, 100);
        }
        $(":text").placeholder();
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
          view.$el.find(".searchField .search-input").val($this.val());
          view.$el.trigger("SEARCH_QUERY_CHANGE");
      },
      "keyup; .searchField .search-input": function(event){
        var view = this;
        if(event.keyCode != 13){
            view.$el.trigger("SEARCH_QUERY_CHANGE");
        }
      },
      "click; table .locationTh span.columnName,table .contactTh span.columnName" : function(event) {
        var view = this;
        var $th = $(event.currentTarget).closest("th");
        var $desc = $(".desc", $th);
        var $asc = $(".asc", $th);
        var column = $th.attr("data-column");
        var bPagination = view.$el.bComponent("SearchDataGrid");
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
      "click; table th[data-column] .selectedItems" : function(event) {
          event.preventDefault();
          event.stopPropagation();
    	  var view = this;
    	  var $th = $(event.currentTarget).parent().parent();
          view.$el.trigger("POPUP_CLOSE");
          brite.display("HeaderPopup", ".SearchDataGrid", {
              $target : $th
          });
          
      },
      "click; div.btnPopupColumns" : function(event) {
        brite.display("SelectColumns");
      },
      "click; .resume-ico" : function(event) {
          var cid = $(event.currentTarget).closest("i").attr("data-id");
          var sfid = $(event.currentTarget).closest("i").attr("data-sfid");
          var cname = $(event.currentTarget).closest("i").attr("data-cname");
          if (app.orgInfo.apex_resume_url && app.orgInfo.apex_resume_url.length && app.orgInfo.apex_resume_url.length > 0){
              var url = app.orgInfo.apex_resume_url + sfid;
              window.showModalDialog(url,"dialogWidth=540px;dialogHeight=430px");
          }else{
              brite.display("ResumeView","body", {id: cid,sfid: sfid, name:cname});
          }

      },
      "click; tbody td.favLabel":function(event){
         var view = this;
         var $td = $(event.currentTarget);
         event.stopPropagation();
         event.preventDefault();

         var labelId = view.tabView.getSelectLabel().id;
          var contactId = $td.closest("tr").attr("data-objId");
         if(labelId){

             if($td.hasClass("hasLabel")){
                 app.LabelDaoHandler.unAssign(contactId, labelId);
                 $td.removeClass("hasLabel");
             }else{
                 app.LabelDaoHandler.assign(contactId, labelId);
                 $td.addClass("hasLabel");
             }
         }

      },
      "click; table td[data-column='company'],td[data-column='skill'],td[data-column='education']" : function(event) {
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
      },
      "click; .applyContact input": function(event) {
          var view = this;
          var $e = view.$el;
          var $tr = $(event.currentTarget).closest("tr");
          if($tr.hasClass("applySelect")){
              $tr.removeClass("applySelect");
          }else{
              $tr.addClass("applySelect");
          }
          if(view.$el.find("tr.applySelect").length > 0){
              $e.trigger("DO_TOOLBAR_ACTIVE_BUTTONS");
          }else{
              $e.trigger("DO_TOOLBAR_DEACTIVE_BUTTONS");
          }
      },
        //add drag event

        "bdragstart; th[data-column]": function (event) {
            event.stopPropagation();
            event.preventDefault();
            var $th = $(event.currentTarget);
            var view = this;
            var $e = view.$el;
            var $table = $("<table id='dragTable'><thead><tr></tr></thead><tbody></tbody></table>");
            var pos = $th.position();
            $table.css({"display": "block","position":"absolute", opacity:0.5, left: pos.left, top: pos.top,
                "cursor": "pointer", "width":$th.outerWidth()});

            $table.find("thead tr").append($th.clone());
            var index = $th.index();
            $e.find(".scrollTable tbody tr td:nth-child("+(index + 1)+")").each(function(){
                var $td = $(this);
                $table.find("tbody").append("<tr></tr>");
                $table.find("tbody tr:last").append($td.clone());
                $(this).addClass("holderSpace");
            });

            $th.addClass("holderSpace");

            view.$el.find(".tableContainer").append($table);
        },
        "bdragmove; th[data-column]":function(e){
            var view = this;
            var $e = view.$el;
            e.stopPropagation();
            e.preventDefault();
            var $dragTable = view.$el.find("#dragTable");
            var ppos = $dragTable.position();
            var pos = {left: ppos.left + e.bextra.deltaX, top: 0}
            $dragTable.css(pos);

            $e.find(".scrollTable tr").each(function(){
                var $tr = $(this);
                var $holder = $tr.find(".holderSpace");
                $tr.find("th[data-column],td[data-column]").each(function(idx, th){
                    var $th = $(th);
                    var tpos = $th.offset();
                    if(e.bextra.pageX > tpos.left && e.bextra.pageX  < tpos.left + $th.outerWidth()){
                        if(e.bextra.pageX < tpos.left + $th.outerWidth() / 2){
                            $holder.insertAfter($th);
                        }else{
                            $holder.insertBefore($th);
                        }
                    }
                });
            });
        },
        "bdragend; th[data-column]":function(e){
            e.stopPropagation();
            e.preventDefault();
            var view = this;
            var $e = view.$el;

            var columns = [];
            $e.find(".scrollTable th[data-column]").each(function(idx, th){
                var $th = $(th);
                columns.push($th.attr('data-column'));
            });
            view.$el.find("#dragTable").remove();

            $e.find(".scrollTable th, .scrollTable td").removeClass("holderSpace");

            app.getJsonData("perf/save-user-pref", {value: JSON.stringify(columns)},"Post").done(function(){
                app.getFilterOrders(columns);
                view.$el.trigger("DO_SEARCH");
            });
        }

    },
    // --------- /Events--------- //


    // --------- Public Methods--------- //
    showContentMessage : function(cmd,extra){
    	var view = this;
    	view.$el.find(".search-info").empty();
    	var $tabContainer = view.$searchResult.find(".tableContainer");
    	if(cmd == "empty"){
		    $tabContainer.html(render("search-empty", {
		      colWidth : getColWidth.call(view)
		    }));
    	}else if(cmd == "loading"){
	      	$tabContainer.html(render("search-loading"));
    	}else if(cmd == "lessword"){
	        $tabContainer.html(render("search-query-less-words", {
	          colWidth : getColWidth.call(view),
	          labelAssigned: app.buildPathInfo().labelAssigned
	        }));
		    view.restoreSearchParam();
    	}else if(cmd == "error"){
	        var html = render("search-query-error", {
		        title : extra.title,
		        detail : extra.detail,
		        colWidth : getColWidth.call(view)
		      });
		      $tabContainer.html(html);
		      brite.display("MessagePanel",".search-result", {message: "No Organization selected"});
    	}
	    view.$searchResult.find(".page").empty();
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
          event.preventDefault();
      },
      "REMOVE_FILTER":function(event, extra){
          var view = this;
          app.ParamsControl.remove(extra);
          view.restoreSearchParam();
          view.$el.trigger("DO_SEARCH");
          event.preventDefault();
      },
      "UPDATE_FILTER": function(event, extra){
          var view = this;
          view.$el.trigger("DO_SEARCH");
          event.preventDefault();
      },
      "ON_ERROR":function(event, extra) {
          var view = this;
          if(extra){
          	var title = extra.errorCode||"";
          	var detail = extra.errorMessage || "";
            view.showContentMessage("error",{title:title, detail:detail});
          }
      },
      "ERROR_PROCESS": function (event, extra) {

            var view = this;
            view.$el.find(".search-info").empty();
            view.$el.find("input").attr("disabled", true);
            view.$el.find(".TabView").hide();
            view.$el.find(".gridControls").hide();
            if(extra.errorCode == "NO_PASSCODE"){
                if($("body").bFindComponents("PassCodeModal").length ==0){
                    brite.display("PassCodeModal");
                }

            }else {
                brite.display("MessagePanel", ".search-result", {message: extra.errorMessage})
            }
        } ,
      "CHANGE_TO_FAV_VIEW": function(event, extra){
          var view = this;
          view.$el.find(".tableContainer th").addClass("favFilter");
          view.$el.find(".search-result").addClass("favFilter");
          view.$el.find(".tableContainer tbody").addClass("favFilter");
          view.$el.find(".btnPopupColumns").addClass("favFilter");
          view.$el.find(".gridControls").hide();
          view.$el.find(".toolBarContainer ").hide();
          view.$el.find(".empty-search").hide();
//          view.$el.find(".page").hide();
//          console.log(extra);
      },
      "RESTORE_SEARCH_VIEW": function(event){
         var view = this;
          view.$el.find(".tableContainer th").removeClass("favFilter");
          view.$el.find(".search-result").removeClass("favFilter");
          view.$el.find(".tableContainer tbody").removeClass("favFilter");
          view.$el.find(".btnPopupColumns").removeClass("favFilter");
          view.$el.find(".gridControls").show();
          view.$el.find(".toolBarContainer ").show();
//          view.$el.find(".page").show();
      },
        CHANGE_SELECT_LABEL: function(event){
            var view = this;
            var label = view.tabView.getSelectLabel();
            if (label) {
                view.$el.find("tbody td.favLabel").attr("title", label.name);
                var $icons = view.$el.find("tbody td.favLabel i");
                if (label.name == "Favorites") {
                    $icons.removeClass("glyphicon-stop").addClass("glyphicon-star");
                } else {
                    $icons.removeClass("glyphicon-star").addClass("glyphicon-stop");
                }
                var cids = [];
                view.$el.find("tbody tr[data-objId]").each(function (idx, tr) {
                    cids.push($(tr).attr("data-objId"));
                });
                view.$el.find("tbody td.favLabel").removeClass("hasLabel");
                if(cids.length > 0){
                    app.LabelDaoHandler.getLabelStatus("[" + cids.join(",") + "]", label.id).done(function(result){
                        $.each(result, function(idx, obj){
                           if(obj.haslabel){
                               view.$el.find("tbody tr[data-objId='" + obj.id + "'] td.favLabel").addClass("hasLabel");
                           }
                        });
                    });
                }
            }
        }
    },
    // --------- /Document Events--------- //


    // --------- Parent Events--------- //
    parentEvents : {

      MainView : {
        "SEARCH_RESULT_CHANGE" : function(event, result) {
          var labelAssigned = app.buildPathInfo().labelAssigned;
          var view = this;
          var $e = view.$el;

          var html;
          $e.find(".resultTime").html("(c:{0}ms, s:{1}ms)".format(result.countDuration , result.selectDuration));

          if (result.count > 0) {
            buildResult.call(view, result.result).done(function(data){

        	   html = render("search-items", {
                 items : data,
                 colWidth : getColWidth.call(view),
                 labelAssigned: labelAssigned
               });
               view.$searchResult.find(".tableContainer").html(html);

               //show desc/asc
               if (view.tableOrderColumn && view.tableOrderType) {
                 $e.find("table th[data-column='" + view.tableOrderColumn + "']").find("." + view.tableOrderType).show();
               }
               fixColWidth.call(view);

               brite.display("Pagination", view.$el.find(".pagination-ctn"), {
                 pageIdx : result.pageIdx,
                 pageSize : result.pageSize,
                 totalCount : result.count,
                 callback : function(pageIdx, pageSize){
                     view.pageIdx = pageIdx;
                     view.pageSize = pageSize;
                     view.$el.trigger("DO_SEARCH",{pageIdx:pageIdx});
                 }
               }).done(function(){
                       var $pagination = view.$el.find(".pagination-ctn");
                       var pagination = view.$el.find(".Pagination").bComponent();
                       if(labelAssigned){
                           $e.trigger("CHANGE_TO_FAV_VIEW");
                           pagination.hide();
                       }else{
                           $e.trigger("RESTORE_SEARCH_VIEW");
                           pagination.show();
                       }

                       $e.find(".resultCount").html("{0} match{1}".format(result.count, result.count > 1 ? "es":""));
                   });


              //restore input values
              $e.find(".search-input").val(app.ParamsControl.getQuery());

              view.restoreSearchParam();

            });


          } else {
              view.$searchResult.find(".tableContainer").html(render("search-query-notfound", {
              colWidth : getColWidth.call(view),
              labelAssigned: app.buildPathInfo().labelAssigned
              }));
              view.$searchResult.find(".page").empty();
              fixColWidth.call(view);
              $e.find(".resultCount").html("{0} match{1}".format(result.count, result.count > 1 ? "es":""));
	          view.restoreSearchParam();
	          if(labelAssigned){
	              $e.trigger("CHANGE_TO_FAV_VIEW");
	          }else{
	              $e.trigger("RESTORE_SEARCH_VIEW");
	          }

          }



        }

      }
    },
    winEvents : {
        resize: function (event) {
            var view = this;
            fixColWidth.call(view);
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
      var oldColumns = app.preference.columns();
      //reorder columns
      var filterOrders = app.getFilterOrders();
      var columns = [];

      $.each(filterOrders, function (idx, colName) {
          if ($.inArray(colName, oldColumns) >= 0) {
              columns.push(colName);
          }
      });

      $.each(oldColumns, function(idx, colName){
          if($.inArray(colName, columns) <0){
              columns.push(colName);
          }
      });
      if (columns.length > 0 && columns.length != filterOrders.length) {
          app.getFilterOrders(columns);
      }

    var colLen = columns.length;
    var view = this;
    var dtd = $.Deferred();
    var label = view.tabView.getSelectLabel();
    var isFav = label.name == "Favorites" ? true: false;
	 $.ajax({
			url: contextPath +"/config/get/local_date",
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
  	            value : translate(reSortData(items[i][columns[j]],"skill")),
                realValue: items[i][columns[j]],
  	            notLast : colLen - j > 1
  	          });
  	          
  	        }else if (columns[j] == "education") {
	          item.push({
	  	            name : columns[j],
	  	            value : reSortData(items[i][columns[j]],"education"),
	                realValue: items[i][columns[j]],
	  	            notLast : colLen - j > 1
  	          });
		   }else if (columns[j] == "company") {
			  item.push({
				  name : columns[j],
				  value : reSortData(items[i][columns[j]],"company"),
					  realValue: items[i][columns[j]],
					  notLast : colLen - j > 1
			  });
			  
		   }else if (columns[j] == "resume") {
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
  	           var displayValue = "<a class='lineInfo name' href='"+app.orgInfo.instance_url+items[i]["sfid"]+"'>"+items[i]["name"]+"</a>";
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
            names: {id: items[i].id, name: items[i].name, sfid: items[i].sfid},
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
    var $e = view.$el;
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
    tableWidth = tableWidth - 32;
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
      } else if ($item.hasClass("applyContact")) {
        realWidth = 32;
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

    });
    
    if(view.labelDisable){
    	$e.find("th.favLabel").hide();
    	$e.find("td.favLabel").hide();
    }
  }



  function translate(value) {
    if (!value) {
      return "";
    }
    var items = value.split(","), result = [];
    var len = items.length;
    for (var i = 0; i < len; i++) {
      if (items[i].length > 1) {
    	  if(items[i].substr(0, 8)=="<strong>"){
    		  result.push("<strong>"+items[i].substr(8, 1).toUpperCase() + items[i].substr(9).toLowerCase());
    	  }else{
    		  result.push(items[i].substr(0, 1).toUpperCase() + items[i].substr(1).toLowerCase());
    	  }
        
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

  /**
   * resort data for the filter selected value,make the matched values first show with bold
   */
  function reSortData(value,type){
	  var filters = app.ParamsControl.getFilterParams()||{};
	  var filter = filters[type]||[];
	  value= (value||"")+",";
	  var matched = "";
	  $.each(filter,function(index,e){
		if(value.indexOf(e.name+",")!=-1){
			value.replace(e.name+",", "");
			matched+="<strong>"+e.name+"</strong>,"
		}
	  });
	  value=matched+value;
	  return value;
  }
  // --------- /Private Methods--------- //


})(jQuery); 