<template>
  <div id="app">
		<NavBar></NavBar>
		<SideBar v-if="$route.params.hasOwnProperty('namespace')"></SideBar>
    <div id="main" :class="!$route.params.hasOwnProperty('namespace') && 'noSidebar'">
      <HeaderSection></HeaderSection>
      <router-view :key="$route.path"></router-view>
    </div>
    <input type="text" value="" id="copyText">
	</div>
</template>

<script>
  import NavBar from '@/components/navbar/NavBar.vue'
  import SideBar from '@/components/SideBar.vue'
  import HeaderSection from '@/components/navbar/HeaderSection.vue'

  export default {
    components: {
      NavBar,
      SideBar, 
      HeaderSection
    }
  }

</script>

<style scoped>
  #main.noSidebar {
    margin: 0 auto;
    float: none;
    width: 100%;
  }
</style>
