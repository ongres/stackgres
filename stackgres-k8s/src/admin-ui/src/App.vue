<template>
  <div id="app">
		<NavBar></NavBar>
		<SideBar v-if="$route.params.hasOwnProperty('namespace')"></SideBar>
    <div
      id="main"
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

  #main.loading:after {
    display: block;
    content: " ";
    position: absolute;
    top: 50px;
    left: 0;
    width: 100%;
    height: calc(100% - 50px);
    background: url('/assets/img/loader.gif') center no-repeat rgba(0, 0, 0, .35);
    background-size: 30px;
  }
</style>
