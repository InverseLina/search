/**
 * View: Slider
 *
 *
 *
 *
 */
(function($) {
	brite.registerView("DatePicker", {
		parent : "body",
	}, {
		// --------- View Interface Implement--------- //
		create : function(data, config) {
			data = data || {};
			var view = this;
			view.target = data.target;
			return render("DatePicker");
		},

		postDisplay : function(data) {
			var view = this;
			var $e = this.$el;
			showCalendar.call(view);
			
		},
		events:{
			"btap":function(e){
				e.stopPropagation();
			},
			"click; .year":function(e){
				var view = this;
				var $e = view.$el;
				var $year = $(e.currentTarget);
				var $select = $e.find(".calendar-header select[name='yearSelect']");
				$select.removeClass("hide");
				$year.addClass("hide");
				var year = $year.attr("data-year") * 1;
				$select.empty();
				for(var i = year - 15; i < year + 15; i++){
					var selected = i == year ? "selected" : "";
					var option = "<option value='"+i+"' "+selected+">"+i+"</option>";
					$select.append($(option));
				}
			},
			"change; select[name='yearSelect']":function(e){
				var view = this;
				var $e = view.$el;
				var $select = $(e.currentTarget);
				var $year = $e.find(".calendar-header .year");
				$select.addClass("hide");
				$year.removeClass("hide");
				var year = $select.val() * 1;
				$select.empty();
				$year.attr("data-year", year).text(year);
				view.currentYear = year;
				showCalendar.call(view);
			},
			"click;.action":function(e){
				var view = this;
				var $action = $(e.currentTarget);
				var next = $action.hasClass("actionNext");
				var month;
				if(next){
					month = view.currentMonth + 1;
					if(month == 12){
						view.currentYear = view.currentYear +1;
					}
				}else{
					month = view.currentMonth - 1;
					if(month == -1){
						view.currentYear = view.currentYear -1;
					}
				}
	
				showCalendar.call(view,month);
			},
			"click;.today":function(e){
				var view = this;
				var $target = $(view.target);
				var date = new Date();
				$target.val(date.format("MM/dd/yyyy"));
			},
			"click;.date":function(e){
				var view = this;
				var $target = $(view.target);
				var $date = $(e.currentTarget);
				var $td = $date.closest("td");
				var date = $td.data("date");
				$target.val(date.format("MM/dd/yyyy"));
			}
		},
		docEvents:{
			"btap":function(e){
				var view = this;
				view.$el.bRemove();
			}
		}
		// --------- /View Interface Implement--------- //
	});

	// --------- Private Methods--------- //
	
	function showCalendar(month) {
		var view = this;
		var $e = view.$el;
		var $calendarCon = $e.find(".DatePicker-content");
		$calendarCon.empty();
		var calendar = getCalendars.call(view, month);
		$calendarCon.append(render("DatePicker-calendar", calendar));
		var $tbody = $e.find(".DatePicker-calendar-table tbody");
		for (var i = 0; i < calendar.weeks.length; i++) {
			var week = calendar.weeks[i];
			var $tr = $(render("DatePicker-calendar-tr"));
			$tbody.append($tr);
			for (var j = 0; j < week.length; j++) {
				var $td = $(render("DatePicker-calendar-td", week[j] || {}));
				$tr.append($td);
				$td.data("date",week[j].dateObj);
			}
		}
	}

	
	function getCalendars(month) {
		var view = this;
		var calendar = {};
		var firstDateOfMonth = new Date();
		if ( typeof month != "undefined") {
			firstDateOfMonth.setMonth(month);

		}
		if ( typeof view.currentYear != "undefined") {
			firstDateOfMonth.setYear(view.currentYear);
		}
		firstDateOfMonth.setDate(1);
		firstDateOfMonth.setHours(0);
		firstDateOfMonth.setMinutes(0);
		firstDateOfMonth.setSeconds(0);
		view.currentMonth = firstDateOfMonth.getMonth();
		view.currentYear = firstDateOfMonth.getFullYear();

		var endDateOfMonth = new Date(firstDateOfMonth * 1);
		endDateOfMonth.setMonth(firstDateOfMonth.getMonth() + 1);
		endDateOfMonth.setDate(0);

		var weeks = [];
		var week = new Array(7);
		for (var i = 0; i < endDateOfMonth.getDate() * 1; i++) {
			var date = new Date(firstDateOfMonth * 1 + i * 24 * 60 * 60 * 1000);
			var dataValue = date.getDate();
			var dateFormatStr = date.format("yyyy/MM/dd");
			week[date.getDay()] = {
				dataValue : dataValue,
				dateObj : date,
				dateStr : dateFormatStr,
				currentMonth:true
			};
			if (date.getDay() % 7 == 6) {
				weeks.push(week);
				if (endDateOfMonth.getDate() - i > 1) {
					week = new Array(7);
				}
			}
		}
		if (endDateOfMonth.getDay() % 7 != 6) {
			weeks.push(week);
		}
		
		// add last month dates
		for(var i = firstDateOfMonth.getDay() - 1; i >= 0; i--){
			var date = new Date(firstDateOfMonth * 1 - (firstDateOfMonth.getDay() - i) * 24 * 60 * 60 * 1000);
			var dateStr = date.getDate() <= 9 ? "0" + date.getDate() : date.getDate();
			var dataValue = date.getDate();
			var dateFormatStr = date.format("yyyy/MM/dd");
			weeks[0][i] = {
				dataValue : dataValue,
				dateObj : date,
				dateStr : dateFormatStr,
				currentMonth:false
			}
		}
		
		//add next month dates
		var newEndDate = endDateOfMonth;
		for (var i = endDateOfMonth.getDay() + 1; i <= 6; i++) {
			var date = new Date(endDateOfMonth * 1 + (i - endDateOfMonth.getDay()) * 24 * 60 * 60 * 1000);
			var dataValue = date.getDate();
			var dateFormatStr = date.format("yyyy/MM/dd");
			weeks[weeks.length - 1][i] = {
				dataValue : dataValue,
				dateObj : date,
				dateStr : dateFormatStr,
				currentMonth:false
			}
			newEndDate = date;
		}
		
		if(weeks.length < 6){
			var week = [];
			for(var i = 0; i < 7; i++){
				var date = new Date(newEndDate * 1 + (i + 1) * 24 * 60 * 60 * 1000);
				var dataValue = date.getDate();
				var dateFormatStr = date.format("yyyy/MM/dd");
				week.push({
					dataValue : dataValue,
					dateObj : date,
					dateStr : dateFormatStr,
					currentMonth:false
				});
			}
			weeks.push(week);
		}

		

		calendar.year = firstDateOfMonth.getFullYear();
		calendar.month = firstDateOfMonth.getMonth();
		calendar.monthLabel = getMonth.call(view, calendar.month);
		calendar.weeks = weeks;
		return calendar;
	}
	
	function getMonth(month){
		var months = ["January","February","March","April","May","June","July","August","September","October","November","December"];
		return months[month];
	}

	// --------- /Private Methods--------- //
	
	

})(jQuery);