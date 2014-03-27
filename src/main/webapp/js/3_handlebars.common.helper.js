(function($){
	/**
	 * we can use like this {{#gte 1 2}}true{{else}}false{{/gte}}
	 * means if 1 >=2, will show true, else show false;
	 */
	Handlebars.registerHelper('gte', function(a,b,options) {
		  if(a >= b) {
			return options.fn(this);
		  } else {
			return options.inverse(this);
		  }
	});
	
	/**
	 * we can use like this {{#gt 1 2}}true{{else}}false{{/gt}}
	 * means if 1 > 2, will show true, else show false;
	 */
	Handlebars.registerHelper('gt', function(a,b,options) {
		  if(a > b) {
			return options.fn(this);
		  } else {
			return options.inverse(this);
		  }
	});
	
	/**
	 * we can use like this {{#lte 1 2}}true{{else}}false{{/lte}}
	 * means if 1 <= 2, will show true, else show false;
	 */
	Handlebars.registerHelper('lte', function(a,b,options) {
		  if(a <= b) {
			return options.fn(this);
		  } else {
			return options.inverse(this);
		  }
	});
	
	/**
	 * we can use like this {{#lt 1 2}}true{{else}}false{{/lt}}
	 * means if 1 < 2, will show true, else show false;
	 */
	Handlebars.registerHelper('lt', function(a,b,options) {
		if(a < b) {
			return options.fn(this);
		} else {
			return options.inverse(this);
		}
	});
	
	/**
	 * we can use like this {{#equal 1 2}}true{{else}}false{{/equal}}
	 * means if 1 < 2, will show true, else show false;
	 */
	Handlebars.registerHelper('equal', function(a,b,options) {
		if(a == b) {
			return options.fn(this);
		} else {
			return options.inverse(this);
		}
	});
	
	/**
	 * we can use like this {{#notEqual 1 2}}true{{else}}false{{/notEqual}}
	 * means if 1 !== 2, will show true, else show false;
	 */
	Handlebars.registerHelper('notEqual', function(a,b,options) {
		if(a !== b) {
			return options.fn(this);
		} else {
			return options.inverse(this);
		}
	});
	
	/**
	 * we can use like this {{{arrayToString items showProperty}}}
	 * so we will show string "1,2,3"
	 */
	Handlebars.registerHelper('arrayToString', function(items,showProperty,options) {
		if(items && items.length > 0){
			var str = "";
			for(var i = 0; i < items.length; i++){
				if(showProperty){
					str += items[i][showProperty];
				}else{
					str += items[i];
				}
				if(i !== items.length-1){
					str += ",";
				}
			}
			return str;
		}else{
			return "";
		}
	});
	
	Handlebars.registerHelper('gtt', function(a,b,c,options) {
		  if(a - b - c > 0) {
			return options.fn(this);
		  } else {
			return options.inverse(this);
		  }
	});
	
	/**
	 */
	Handlebars.registerHelper('print', function(a) {
		console.log(a);
	});
	

	/**
	 * we can use like this {{{incl "tmpl-test" data}}}
	 */
	Handlebars.registerHelper('incl', function(template,data,options) {
		var html = Handlebars.templates[template](data);
		return html;
	});
	
	Handlebars.registerHelper('check', function (lvalue, operator, rvalue, options) {

		var operators, result;

		if (arguments.length < 3) {
			throw new Error("Handlerbars Helper 'compare' needs 2 parameters");
		}

		if (options === undefined) {
			options = rvalue;
			rvalue = operator;
			operator = "===";
		}

		operators = {
			'===': function (l, r) { return l === r; },
			'===': function (l, r) { return l === r; },
			'!==': function (l, r) { return l !== r; },
			'!==': function (l, r) { return l !== r; },
			'<': function (l, r) { return l < r; },
			'>': function (l, r) { return l > r; },
			'<=': function (l, r) { return l <= r; },
			'>=': function (l, r) { return l >= r; },
			'typeof': function (l, r) { return typeof l == r; }
		};

		if (!operators[operator]) {
			throw new Error("Handlerbars Helper 'compare' doesn't know the operator " + operator);
		}

		result = operators[operator](lvalue, rvalue);

		if (result) {
			return options.fn(this);
		} else {
			return options.inverse(this);
		}

	});

	function htmlDecode(str) {
		var s = "";
		if (str.length == 0)    return    "";
		s = str.replace(/&gt;/g, "&");
		s = s.replace(/&lt;/g, "<");
		s = s.replace(/&gt;/g, ">");
		s = s.replace(/&nbsp;/g, "    ");
		s = s.replace(/'/g, "\'");
		s = s.replace(/&quot;/g, "\"");
		s = s.replace(/<br>/g, "\n");
		return    s;
	}

	Handlebars.registerHelper("decode", function(data){
		//return $('<textarea />').html(data).val();
		return htmlDecode(data);
	});



	Handlebars.registerHelper("each_with_index", function(array, options) {
		var buffer = "";
		for (var i = 0, j = array.length; i < j; i++) {
			var item = array[i];

			// if item is already an object just add the index property
			if (typeof (item) == 'object') {
				item['index'] = i;
			} else { // make an object and add the index property
				item = {
					value: item,
					index: i
				};
			}

			// show the inside of the block
			buffer += options.fn(item);
		}

		// return the finished buffer
		return buffer;

	});
	
	
})(jQuery);