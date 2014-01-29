<script>
	if("${login?string("true","false")}"=="false"){
		brite.display("LoginModal");
	}else{
		brite.display("MainView","body",{type:"admin",uiFlags:app.uiFlags});
	}
</script>
