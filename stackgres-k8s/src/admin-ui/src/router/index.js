import Vue from 'vue';
import VueRouter from 'vue-router';
import store from '../store'
import axios from 'axios'

// Form Components
import CreateCluster from '../components/forms/CreateSGClusters.vue'
import CreateProfile from '../components/forms/CreateSGInstanceProfiles.vue'
import CreatePgConfig from '../components/forms/CreateSGPgConfigs.vue'
import CreatePoolConfig from '../components/forms/CreateSGPoolConfigs.vue'
import CreateBackupConfig from '../components/forms/CreateSGBackupConfigs.vue'
import CreateBackup from '../components/forms/CreateSGBackups.vue'
import CreateLogsServer from '../components/forms/CreateSGDistributedLogs.vue'
import CreateDbOps from '../components/forms/CreateSGDbOps.vue'

// Main Components
import GlobalDashboard from '../components/GlobalDashboard.vue'
import NamespaceOverview from '../components/NamespaceOverview.vue'
import ClusterOverview from '../components/ClusterOverview.vue'
import ClusterInfo from '../components/ClusterInfo.vue'
import ClusterStatus from '../components/ClusterStatus.vue'
import ClusterLogs from '../components/ClusterLogs.vue'
import ClusterEvents from '../components/ClusterEvents.vue'
import SGBackups from '../components/SGBackups.vue'
import SGPgConfigs from '../components/SGPgConfigs.vue'
import SGPoolConfigs from '../components/SGPoolConfigs.vue'
import SGBackupConfigs from '../components/SGBackupConfigs.vue'
import SGInstanceProfiles from '../components/SGInstanceProfiles.vue'
import SGDistributedLogs from '../components/SGDistributedLogs.vue'
import SGDbOps from '../components/SGDbOps.vue'
import Grafana from '../components/ClusterMonitoring.vue'
import NotFound from '../components/NotFound.vue'

// Applications
import BabelfishCompass from '../components/applications/BabelfishCompass.vue'


Vue.use(VueRouter);

const routes = [
  { 
    path: '/:namespace/sgclusters/new', 
    component: CreateCluster,
    name: 'CreateCluster',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/edit', 
    component: CreateCluster,
    name: 'EditCluster',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sginstanceprofiles/new', 
    component: CreateProfile,
    name: 'CreateProfile',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sginstanceprofile/:name/edit',
    component: CreateProfile,
    name: 'EditProfile',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgpgconfigs/new', 
    component: CreatePgConfig,
    name: 'CreatePgConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgpgconfig/:name/edit', 
    component: CreatePgConfig,
    name: 'EditPgConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgpoolconfigs/new', 
    component: CreatePoolConfig,
    name: 'CreatePoolConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgpoolconfig/:name/edit', 
    component: CreatePoolConfig,
    name: 'EditPoolConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgbackupconfigs/new', 
    component: CreateBackupConfig,
    name: 'CreateBackupConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgbackupconfig/:name/edit', 
    component: CreateBackupConfig,
    name: 'EditBackupConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgbackups/new', 
    component: CreateBackup,
    name: 'CreateBackups',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgcluster/:cluster/sgbackups/new', 
    component: CreateBackup,
    name: 'CreateClusterBackup',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgbackup/:backupname/edit', 
    component: CreateBackup,
    name: 'EditBackup',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgcluster/:cluster/sgbackup/:backupname/edit', 
    component: CreateBackup,
    name: 'EditClusterBackup',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgdistributedlogs/new', 
    component: CreateLogsServer,
    name: 'CreateLogsServer',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgdistributedlog/:name/edit', 
    component: CreateLogsServer,
    name: 'EditLogsServer',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgdbops/new', 
    component: CreateDbOps,
    name: 'CreateDbOps',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/', 
    component: GlobalDashboard,
    name: 'GlobalDashboard',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/index.html', 
    component: GlobalDashboard,
    name: 'GlobalDashboardIndex',
    meta: {
      conditionalRoute: false
    },
  },
  {
    path: '/:namespace',
    component: NamespaceOverview,
    name: 'NamespaceOverview',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/sgclusters', 
    component: ClusterOverview,
    name: 'ClusterOverviewEmpty',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgclusters', 
    component: ClusterOverview,
    name: 'ClusterOverview',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/config', 
    component: ClusterInfo,
    name: 'ClusterInfo',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgcluster/:name', 
    component: ClusterStatus,
    name: 'ClusterStatus',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/logs', 
    component: ClusterLogs,
    name: 'ClusterLogs',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/events', 
    component: ClusterEvents,
    name: 'ClusterEvents',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/event/:uid', 
    component: ClusterEvents,
    name: 'SingleClusterEvents',
    meta: {
      conditionalRoute: false
    },
  },
  {  
    path: '/:namespace/sgbackups', 
    component: SGBackups,
    name: 'NamespaceBackups',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgbackup/:backupname', 
    component: SGBackups,
    name: 'SingleBackups',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/sgbackups', 
    component: SGBackups,
    name: 'ClusterBackups',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/sgbackup/:backupname', 
    component: SGBackups,
    name: 'SingleClusterBackups',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgpgconfigs', 
    component: SGPgConfigs,
    name: 'PgConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgpgconfig/:name', 
    component: SGPgConfigs,
    name: 'SinglePgConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgpoolconfigs', 
    component: SGPoolConfigs,
    name: 'PoolConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgpoolconfig/:name', 
    component: SGPoolConfigs,
    name: 'SinglePoolConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgbackupconfigs', 
    component: SGBackupConfigs,
    name: 'BackupConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgbackupconfig/:name', 
    component: SGBackupConfigs,
    name: 'SingleBackupConfig',
    meta: {
      conditionalRoute: false
    },
  },
  {  
    path: '/:namespace/sginstanceprofiles', 
    component: SGInstanceProfiles,
    name: 'InstanceProfile',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sginstanceprofile/:name', 
    component: SGInstanceProfiles,
    name: 'SingleInstanceProfile',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgdistributedlogs', 
    component: SGDistributedLogs,
    name: 'LogsServer',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgdistributedlog/:name', 
    component: SGDistributedLogs,
    name: 'SingleLogsServer',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgdbops', 
    component: SGDbOps,
    name: 'DbOps',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgdbop/:name', 
    component: SGDbOps,
    name: 'SingleDbOps',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgdbop/:name/event/:uid', 
    component: SGDbOps,
    name: 'SingleDbOpsEvents',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/monitor', 
    component: Grafana,
    name: 'ClusterMonitor',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/monitor/:pod', 
    component: Grafana,
    name: 'SingleClusterMonitor',
    meta: {
      conditionalRoute: false
    },
  },
  {
    path: '/:namespace/sgcluster/:name/monitor/:pod/:range', 
    component: Grafana,
    name: 'SingleClusterMonitorRange',
    meta: {
      conditionalRoute: false
    },
  },
  {
    path: '/:namespace/application/babelfish-compass/', 
    component: BabelfishCompass,
    name: 'BabelfishCompass',
    meta: {
      conditionalRoute: false
    },
  },
  {
    path: '*',
    name: 'NotFound',
    component: NotFound
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
});

function getCookie(cname) {
  var name = cname + "=";
  var ca = document.cookie.split(';');
  for(var i = 0; i < ca.length; i++) {
    var c = ca[i];
    while (c.charAt(0) == ' ') {
      c = c.substring(1);
    }
    if (c.indexOf(name) == 0) {
      return c.substring(name.length, c.length);
    }
  }
  return "";
}

function notFound() {
  store.commit('notFound',true)
}

function checkLogin() {
  let loginToken = getCookie('sgToken');

  if (!loginToken.length) {
    if(!store.state.loginToken.length) {
      $('#signup').addClass('login').fadeIn();
      return false;
    } else {
      return true;
    }
  } else if ( !store.state.loginToken.length && (loginToken.length > 0) ) {
    $('#signup').hide();
    store.commit('setLoginToken', loginToken);
    return true;
  }
}


function checkAuthError(error) {
  if(error.response) {
    if(error.response.status == 401 ) {
      document.cookie = 'sgToken=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT; SameSite=Strict;';
      store.commit('setLoginToken');
      window.location.replace('/admin/')
      process.exit(1);
    } else if(error.response.status == 403 ) {
      
      // Little hack to store the right plural kind to validate RBAC permissions
      if(error.response.config.url.includes('sgcluster/stats'))
        kind = 'stats'
      else
        kind = error.response.config.url.replace('/stackgres/','')+'s';
      
      store.commit('setNoPermissions', kind);
    }
  }
  
}

router.beforeResolve((to, from, next) => {

  // If loading CRD from direct URL validate if CRD exists on the API before loading
  if( from.path == '/') {
    let kind = ( 
      to.matched[0].components.default.name.startsWith('Cluster') ? 
        'sgclusters' : 
        ( 
          to.matched[0].components.default.name.startsWith('Create') ? 
            to.matched[0].components.default.name.replace('Create', '') : 
            to.matched[0].components.default.name 
        ) 
    );

    if(!checkLogin()) {
      next(); 
      return;
    }

    /* First check if Namespace exist */
    if(to.params.hasOwnProperty('namespace')) {

      let namespaceName = to.params.namespace;
      
      axios
      .get('/stackgres/namespaces')
      .then( function(response){
        store.commit('addNamespaces', response.data)
        
        if(response.data.includes(namespaceName)) {
          
          store.commit('setCurrentNamespace', namespaceName);

          let resourceName = ( to.params.hasOwnProperty('name') ? to.params.name : ( to.params.hasOwnProperty('backupname') ? to.params.backupname : '' ) );
          
          // Then check if requested resource exists
          if(resourceName.length) {

            // If requesting for backups inside a single cluster
            if(to.name.includes('ClusterBackup')) {

              if(to.params.hasOwnProperty('backupname')) {
                axios
                .get('/stackgres/namespaces/' + namespaceName + '/' + kind.toLowerCase() + '/' + to.params.backupname )
                .catch(function(error) {
                  checkAuthError(error);
                  notFound();
                  return false;
                });
              }

              kind = 'sgclusters';

            }
    
            axios
            .get('/stackgres/namespaces/' + namespaceName + '/' + kind.toLowerCase() + '/' + resourceName )
            .catch(function(error) {
              checkAuthError(error)
              notFound()
            });
            
          }

        } else {
          notFound();
        }
      }).catch(function(error) {
        checkAuthError(error)
        notFound()
      });
      
    }

  }

  if(to.name !== 'NotFound')
    store.commit('notFound',false)


  if (to.matched.some(record => record.meta.conditionalRoute)) { 
      if (store.state.currentCluster == {} && ( from.path.includes("profiles") || from.path.includes("configurations") ) && (to.path != ('/admin/configuration/'+to.params.name)) ) { 
          next({ path: '/admin/'}) 
      } else { 
          next() 
      } 
  } else { 
      next() // make sure to always call next()! 
  } 

})

export default router;


// Prevent router "Navigation Duplicated" error
const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push(location, onResolve, onReject) {
  if (onResolve || onReject)
    return originalPush.call(this, location, onResolve, onReject)
  return originalPush.call(this, location).catch((err) => {
    if (VueRouter.isNavigationFailure(err)) {
      // resolve err
      return err
    }
    // rethrow error
    return Promise.reject(err)
  })
}