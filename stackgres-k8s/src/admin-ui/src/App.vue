<template>
  <div id="app">
		<NavBar></NavBar>
		<SideBar></SideBar>
		<router-view id="main" :key="$route.path"></router-view>
		<div id="contentTooltip">
			<div class="close"></div>
			<div class="info">
				<span class="close">CLOSE</span>
				<div class="content"></div>
			</div>
		</div>
	</div>
</template>

<script>
  import NavBar from '@/components/NavBar.vue'
  import SideBar from '@/components/SideBar.vue'

  /* Import CSS */
  //require('@/assets/style.css')
  
  export default {
    components: {
      NavBar,
      SideBar
    }
  }

</script>

<style>
</style>