var app = app || {};
(function($) {
	var _top = {
		skills:[],
		employers:[],
		educations:[]
	};
	
	function TopFilterItem(){};
	
	TopFilterItem.prototype.getAutoComplete = function(type){
		var container = getFilterContainer(type);
		return {
			list : container.slice(0,7),
			duration : 0
		}
	};
	
	TopFilterItem.prototype.saveItems = function(contacts){
		this.clear();
		if(contacts.length > 0){
			for(var i = 0; i < contacts.length; i++){
				var contact = contacts[i];
				pushToContainer(contact, 'company');
				pushToContainer(contact, 'skill');
				pushToContainer(contact, 'education');
			}
		}
	};
	
	TopFilterItem.prototype.clear = function(){
		_top.skills = [];
		_top.employers = [];
		_top.educations = [];
	};
	
	function pushToContainer(contact, type){
		var container = getFilterContainer(type);
		var groupedids, names;
		if(type === "company"){
			names = contact.company;
			groupedids = contact.companygroupedids;
		}else if(type === "education"){
			names = contact.education;
			groupedids = contact.educationgroupedids;
		}else if(type === "skill"){
			names = contact.skill;
			groupedids = contact.skillgroupedids;
		}
		
		if(names && names.length > 0){
			var groupedIdsArr = groupedids.split(",");
			var nameArr = names.split(",");
			for (var i = 0; i < groupedIdsArr.length; i++) {
				var isExist = false;
				for (var j = 0; j < container.length; j++) {
					var item = container[j];
					if (groupedIdsArr[i] == item.groupedid) {
						item.count = item.count + 1;
						isExist = true;
						break;
					}
				}
				if (!isExist) {
					var newItem = {
						count : 1,
						name : nameArr[i],
						groupedid : groupedIdsArr[i]
					}

					container.push(newItem);
				}
			}

		}
		
		container.sort(function(a,b){
			if(a.count > b.count){
				return 1;
			}else{
				if(a.name > b.name){
					return 1;
				}
			}
			
			return -1;
		});
	}
	
	function getFilterContainer(type){
		var containKey = "";
		if(type === "company"){
			containKey = "employers";
		}else if(type === "education"){
			containKey = "educations";
		}else if(type === "skill"){
			containKey = "skills";
		}
		return _top[containKey];
	}

	app.TopFilterItem = new TopFilterItem();
})(jQuery); 