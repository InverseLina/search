(function($) {

	/**
	 * remove the continues comma,like:
	 * ",,,,c1,,,,"===>"c1"
	 * ",,,,,,,,,,"===>""
	 * ",,,c1,,,,,c2"===>"c1,c2"
	 */
	Handlebars.registerHelper('rmComma', function(src, options) {
		if(typeof src == 'boolean'){
			return new Handlebars.SafeString(src); 
		}
		if (src) {
			src = src + "";
		} else {
			return new Handlebars.SafeString("");
		}
		src = src.replace(/\,{2,}/g, ",");
		if (src.length > 0 && src.substring(0, 1) === ",") {
			src = src.substring(1);
		}
		if (src.length > 0 && src.substring(src.length - 1) === ",") {
			src = src.substring(0, src.length - 1);
		}
		return new Handlebars.SafeString(src);
	});

	/**
	 * check the var is exist or not
	 */
	Handlebars.registerHelper('ifExist', function(src, options) {
		if ( typeof (src) === 'undefined') {
			return options.inverse(this);
		} else {
			return options.fn(this);
		}
	});

})(jQuery); 