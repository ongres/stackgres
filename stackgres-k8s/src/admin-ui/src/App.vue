<template>
  <div id="app">
		<NavBar></NavBar>
		<SideBar v-if="$route.params.hasOwnProperty('namespace')"></SideBar>
    <div
      id="main"
      class="loadingContainer"
      :class="[
        !$route.params.hasOwnProperty('namespace') && 'noSidebar',
        isLoading && 'loading'
      ]"
    >
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
  import store from './store'

  export default {
    components: {
      NavBar,
      SideBar, 
      HeaderSection
    },

    computed: {
      isLoading() {
        return store.state.isLoading
      }
    }
  }

</script>

<style scoped>
  #main.noSidebar {
    margin: 0 auto;
    float: none;
    width: 100%;
  }

  .loadingContainer.loading:after {
    top: 50px;
    height: calc(100% - 50px);
  }
</style>
