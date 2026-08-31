<template>
  <div id="app">
		<AnnouncementsBar></AnnouncementsBar>
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
    <footer id="enterpriseFooter" :class="!$route.params.hasOwnProperty('namespace') && 'noSidebar'">
      <p>
        <strong>StackGres</strong> offers an Enterprise License subscription including 24x7 Support.
        <a href="https://stackgres.io/support/" target="_blank" rel="noopener noreferrer">Contact us</a>
      </p>
    </footer>
	</div>
</template>

<script>
  import AnnouncementsBar from '@/components/navbar/AnnouncementsBar.vue'
  import NavBar from '@/components/navbar/NavBar.vue'
  import SideBar from '@/components/SideBar.vue'
  import HeaderSection from '@/components/navbar/HeaderSection.vue'
  import store from './store'

  export default {
    components: {
      AnnouncementsBar,
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

  #enterpriseFooter {
    position: fixed;
    bottom: 0;
    left: 350px;
    width: calc(100% - 350px);
    right: 0;
    z-index: 9;
    margin: 0;
    height: 35px;
    min-height: 35px;
    padding: 10px;
    border-top: 1px solid var(--borderColor);
    text-align: center;
    background: var(--rowBg);
  }

  .collapsed #enterpriseFooter {
    left: 50px;
    width: calc(100% - 50px);
  }

  #enterpriseFooter.noSidebar {
    left: 0;
    width: 100%;
  }

  #enterpriseFooter a:hover {
    text-decoration: underline;
  }

  #enterpriseFooter p {
    line-height: 1;
  }

  /* Keep page content clear of the fixed footer. */
  #app #main {
    padding-bottom: 50px;
  }

  /* .darkmode #enterpriseFooter {
    background: #26252c;
    border-top-color: #35343c;
  }

  .darkmode #enterpriseFooter p {
    color: var(--gray2);
  } */
</style>
