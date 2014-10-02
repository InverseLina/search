/**
 * View: SavedSearchesSide
 *
 *
 *
 */
(function($) {
	
	
	brite.registerView("SavedSearchesSide", {
		parent : ".saved-content"
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			var view = this;
			data = data || {};
			view.paramName = data.name;
			return render("SavedSearchesSide");
		},

		postDisplay : function(data) {
			var view = this;
			var $e = view.$el;
			
			showSearches.call(view);
		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"keydown; .valueInput":function(e) {
				var view = this;
				if(e.which == 13){
					saveSearch.call(view);
				}
			},
			"click; .saved-item":function(e){
				var view = this;
				var $e = view.$el;
				var $item = $(e.currentTarget);
				restoreSearchForUI.call(view, $item.data("content"));
			},
			"click; .clearSavedItem":function(e){
				e.stopPropagation();
				var view = this;
				var $e = view.$el;
				var $item = $(e.currentTarget).closest(".saved-item");
				deleteSearch.call(view, $item.attr("data-id"));
			},
			"click; .btnSave":function(e){
				var view = this;
				var $e = view.$el;
				saveSearch.call(view);
			}
		},
		// --------- /Events--------- //
	});
	
	// --------- Private Methods--------- //
	function saveSearch(){
		var view = this;
		var $e = view.$el;
		var $input = $e.find(".input-containers .valueInput");
		if($input.val() == ""){
			return;
		}
		var content = JSON.stringify(app.ParamsControl.getParamsForSearch());
		app.SavedSearchesDaoHandler.save($input.val(),content).done(function(){
			$input.val("");
			showSearches.call(view);
		});
	}
	
	function showSearches(){
		var view = this;
		var $e = view.$el;
		var $cb = $e.find(".savedItems .cb");
		$e.find(".savedItems .saved-item").remove();
		app.SavedSearchesDaoHandler.list().done(function(val){
			var list = val;
			for(var i = 0; i < list.length; i++){
				var $item = $(render("SavedSearchesSide-saved-item", list[i]));
				$item.insertBefore($cb);
				$item.data("content", list[i].search);
			}
			checkEmpty.call(view);
		});
	}
	
	function deleteSearch(id){
		var view = this;
		app.SavedSearchesDaoHandler.del(id).done(function(){
			showSearches.call(view);
		});
	}
	
	function restoreSearchForUI(content){
		var view = this;
		var search = JSON.parse(content);
		app.ParamsControl.restoreSearch(search);
		view.$el.trigger("DO_SEARCH");
	}
	
	function checkEmpty(){
		var view = this;
		var $e = view.$el;
		var $savedCon = $e.find(".wrap-items");
		var size = $savedCon.find(".saved-item").size();
		if(size > 0){
			$savedCon.removeClass("empty");
		}else{
			$savedCon.addClass("empty");
		}
	}
	
	// --------- /Private Methods--------- //
})(jQuery);
