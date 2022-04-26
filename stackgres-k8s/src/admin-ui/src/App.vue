<template>
  <div id="app">
		<NavBar></NavBar>
		<SideBar v-if="!$route.name.includes('GlobalDashboard')"></SideBar>
		<router-view id="main" :key="$route.path" :class="$route.name.includes('GlobalDashboard') ? 'noSidebar' : ''"></router-view>
    <input type="text" value="" id="copyText">
	</div>
</template>

<script>
  import NavBar from '@/components/navbar/NavBar.vue'
  import SideBar from '@/components/SideBar.vue'

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
