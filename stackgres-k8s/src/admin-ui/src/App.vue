<template>
  <div id="app">
		<NavBar></NavBar>
		<SideBar v-if="!$route.name.includes('GlobalDashboard')"></SideBar>
		<router-view id="main" :key="$route.path" :class="$route.name.includes('GlobalDashboard') ? 'noSidebar' : ''"></router-view>
		<div class="contentTooltip">
			<div class="close"></div>
			<div class="info">
				<span class="close">CLOSE</span>
				<div class="content"></div>
			</div>
		</div>
    <input type="text" value="" id="copyText">
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

<style scoped>
  #main.noSidebar {
    margin: 0 auto;
    float: none;
    width: 100%;
  }
</style>>
