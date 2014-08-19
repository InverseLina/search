/**
 * View: CustomFilterPopup
 *
 *
 *
 */
(function($) {
	brite.registerView("CustomFilterPopup", {
		parent : ".SearchDataGrid .search-result"
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			return render("CustomFilterPopup");
		},

		postDisplay : function(data) {
			var view = this;
			var $e = view.$el;
			
			var $filterItem = $e.find(".filter-item[data-name='desiredsalary']");
			brite.display("CustomFilterNumber",$filterItem.find(".filter-item-container"), {name:"desiredsalary"});
			$filterItem = $e.find(".filter-item[data-name='iswillingtorelocate']");
			brite.display("CustomFilterBoolean",$filterItem.find(".filter-item-container"), {name:"iswillingtorelocate"});
		},
		// --------- /View Interface Implement--------- //

		// --------- Events--------- //
		events : {
			"click;.CustomFilterPopup-slider":function(){
				var view = this;
				var $e = view.$el;
				$e.toggleClass("show");
			},
			"click;.filter-item-header":function(e){
				var view = this;
				var $e = view.$el;
				var $target = $(e.currentTarget);
				var $icon = $target.find(".icon-fa");
				var $item = $target.closest(".filter-item");
				var component = $item.find(".filter-item-container").bFindFirstComponent()[0];
				if($item.hasClass("extend")){
					$item.removeClass("extend");
					$icon.removeClass("fa-chevron-down");
					$icon.addClass("fa-chevron-right");
					if(component){
						component.showMode('view');
					}
				}else{
					$item.addClass("extend");
					$icon.addClass("fa-chevron-down");
					$icon.removeClass("fa-chevron-right");
					if(component){
						component.showMode('edit');
					}
				}
			}
		}
		// --------- /Events--------- //
	});
})(jQuery);
