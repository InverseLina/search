<script>

	[#if (errorCode)?? ]
		brite.display("LoginModal");
	[#else]
		brite.display("MainView","body",{type:"admin",uiFlags:app.uiFlags});
	[/#if]
</script>
