import Vue from 'vue';
import VueRouter from 'vue-router';
import store from '../store'
import axios from 'axios'

// Form Components
import CreateCluster from '../components/forms/CreateCluster.vue'
import CreateProfile from '../components/forms/CreateProfile.vue'
import CreatePgConfig from '../components/forms/CreatePgConfig.vue'
import CreatePoolConfig from '../components/forms/CreatePoolConfig.vue'
import CreateBackupConfig from '../components/forms/CreateBackupConfig.vue'
import CreateBackup from '../components/forms/CreateBackup.vue'
import CreateLogsServer from '../components/forms/CreateLogsServer.vue'
import CreateDbOps from '../components/forms/CreateDbOps.vue'

// Main Components
import GlobalDashboard from '../components/GlobalDashboard.vue'
import NamespaceOverview from '../components/NamespaceOverview.vue'
import ClusterOverview from '../components/ClusterOverview.vue'
import ClusterInfo from '../components/ClusterInfo.vue'
import ClusterStatus from '../components/ClusterStatus.vue'
import ClusterLogs from '../components/ClusterLogs.vue'
import ClusterEvents from '../components/ClusterEvents.vue'
import Backups from '../components/Backups.vue'
import PgConfig from '../components/PgConfig.vue'
import PoolConfig from '../components/PoolConfig.vue'
import BackupConfig from '../components/BackupConfig.vue'
import InstanceProfile from '../components/InstanceProfile.vue'
import LogsServer from '../components/LogsServer.vue'
import DbOps from '../components/DbOps.vue'
import Grafana from '../components/Grafana.vue'
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
    component: Backups,
    name: 'NamespaceBackups',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgbackup/:backupname', 
    component: Backups,
    name: 'SingleBackups',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/sgbackups', 
    component: Backups,
    name: 'ClusterBackups',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/sgbackup/:backupname', 
    component: Backups,
    name: 'SingleClusterBackups',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgpgconfigs', 
    component: PgConfig,
    name: 'PgConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgpgconfig/:name', 
    component: PgConfig,
    name: 'SinglePgConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgpoolconfigs', 
    component: PoolConfig,
    name: 'PoolConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgpoolconfig/:name', 
    component: PoolConfig,
    name: 'SinglePoolConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgbackupconfigs', 
    component: BackupConfig,
    name: 'BackupConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgbackupconfig/:name', 
    component: BackupConfig,
    name: 'SingleBackupConfig',
    meta: {
      conditionalRoute: false
    },
  },
  {  
    path: '/:namespace/sginstanceprofiles', 
    component: InstanceProfile,
    name: 'InstanceProfile',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sginstanceprofile/:name', 
    component: InstanceProfile,
    name: 'SingleInstanceProfile',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgdistributedlogs', 
    component: LogsServer,
    name: 'LogsServer',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgdistributedlog/:name', 
    component: LogsServer,
    name: 'SingleLogsServer',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgdbops', 
    component: DbOps,
    name: 'DbOps',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgdbop/:name', 
    component: DbOps,
    name: 'SingleDbOps',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:namespace/sgdbop/:name/event/:uid', 
    component: DbOps,
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
    const component = to.matched[0].components.default.name;

    if(!checkLogin()) {
      next(); 
      return;
    }

    /* Check if Namespace exist */
    if(to.params.hasOwnProperty('namespace')) {
      
      axios
      .get('/stackgres/namespaces')
      .then( function(response){
        store.commit('addNamespaces',response.data)
        if(response.data.includes(to.params.namespace)) {
          store.commit('setCurrentNamespace', to.params.namespace);
        }
        else {
          checkAuthError(error)
          notFound();
        }
      }).catch(function(error) {
        checkAuthError(error)
        notFound()
      });
    }
      
    switch(component) {

      case 'CreateCluster':
      case 'Logs':
      case 'ClusterInfo':
      case 'Grafana':
      case 'ClusterStatus':

        axios
        .get('/stackgres/sgclusters')
        .then( function(response){

          var found = false,
              stats = {};

            if(component == 'ClusterStatus') {
              /* Check for Cluster status */
              axios
              .get('/stackgres/namespaces/'+to.params.namespace+'/sgclusters/'+to.params.name+'/stats')
              .then( function(resp){
                stats = resp.data;
              }).catch(function(error) {
                checkAuthError(error)
                notFound()
              });
            } 

          response.data.forEach( function(item, index) {

            var cluster = {
              name: item.metadata.name,
              data: item,
              hasBackups: false,
              status: {},
            };

            if( to.params.hasOwnProperty('name') && (to.params.name == item.metadata.name) && (to.params.namespace == item.metadata.namespace) ) {
              cluster.status = stats;
              store.commit('setCurrentCluster', cluster);
              found = true;
            }

            store.commit('updateClusters', cluster);

          });

          if( to.params.hasOwnProperty('name') && !found)
            notFound()
          else
            next()

        }).catch(function(error) {
            checkAuthError(error)
          notFound()
        });

        break

      case 'InstanceProfile':
      case 'CreateProfile':
        /* Check if Profile exists */
        axios
        .get('/stackgres/sginstanceprofiles')
        .then( function(response){

          var found = false

          response.data.forEach( function(item, index) {
              
            store.commit('updateProfiles', { 
              name: item.metadata.name,
              data: item
            }); 

            if( to.params.hasOwnProperty('name') && (to.params.name == item.metadata.name) && (to.params.namespace == item.metadata.namespace) )
              found = true;

          });

          if( to.params.hasOwnProperty('name') && !found)
            notFound()
          else
            next()

        }).catch(function(error) {
          checkAuthError(error)
          notFound()
        });

        break

      case 'PgConfig':
      case 'CreatePgConfig':
        
        /* Check if Postgres Config exists */
        axios
        .get('/stackgres/sgpgconfigs')
        .then( function(response){

          var found = false

          response.data.forEach( function(item, index) {
              
            store.commit('updatePGConfig', { 
              name: item.metadata.name,
              data: item
            }); 

            if( to.params.hasOwnProperty('name') && (to.params.name == item.metadata.name) && (to.params.namespace == item.metadata.namespace) )
              found = true;

          });

          if( to.params.hasOwnProperty('name') && !found)
            notFound()
          else
            next()

        }).catch(function(error) {
          checkAuthError(error)
          notFound()
        });

        break;

      case 'PoolConfig':
      case 'CreatePoolConfig':

        /* Check if PgBouncer Config exists */
        axios
        .get('/stackgres/sgpoolconfigs')
        .then( function(response){

          var found = false

          response.data.forEach( function(item, index) {
              
            store.commit('updatePoolConfig', { 
              name: item.metadata.name,
              data: item
            }); 

            if( to.params.hasOwnProperty('name') && (to.params.name == item.metadata.name) && (to.params.namespace == item.metadata.namespace) )
              found = true;

          });

          if( to.params.hasOwnProperty('name') && !found)
            notFound()
          else
            next()

        }).catch(function(error) {
          checkAuthError(error)
          notFound()
        });

        break;
      
      case 'BackupConfig':
      case 'CreateBackupConfig':
        /* Check if BackupConfig Config exists */
        axios
        .get('/stackgres/sgbackupconfigs')
        .then( function(response){

          var found = false

          response.data.forEach( function(item, index) {
              
            store.commit('updateBackupConfig', { 
              name: item.metadata.name,
              data: item
            }); 

            if( to.params.hasOwnProperty('name') && (to.params.name == item.metadata.name) && (to.params.namespace == item.metadata.namespace) )
              found = true;

          });

          if( to.params.hasOwnProperty('name') && !found)
            notFound()
          else
            next()

        }).catch(function(error) {
          checkAuthError(error)
          notFound()
        });

        break;

      case 'Backups':
      case 'CreateBackup':
        /* If filtered by Cluster, first check if Cluster exists */
        if(to.name.includes('ClusterBackup')) {

          axios
          .get('/stackgres/sgclusters')
          .then( function(response){
  
            var found = false
  
            response.data.forEach( function(item, index) {
  
              var cluster = {
                name: item.metadata.name,
                data: item,
                hasBackups: false,
                status: {},
              };
                
              store.commit('updateClusters', cluster);
  
              if( to.params.hasOwnProperty('name') && (to.params.name == item.metadata.name) && (to.params.namespace == item.metadata.namespace) ) {
                store.commit('setCurrentCluster', cluster);
                found = true;
              }
  
            });
  
            if( to.params.hasOwnProperty('name') && !found)
              notFound()
            else
              next()
  
          }).catch(function(error) {
            checkAuthError(error)
            notFound()
          });

          axios
          .get('/stackgres/sgbackups')
          .then( function(response){ 
            var found = false,
                duration = '';

            if(response.data.length) {

              response.data.forEach( function(item, index) {
                  
                store.commit('updateBackups', { 
                  name: item.metadata.name,
                  data: item,
                  duration: '',
                  show: true
                });

                if( to.params.hasOwnProperty('backupname') && (to.params.backupname == item.metadata.name) && (to.params.namespace == item.metadata.namespace) )
                  found = true;

              });
            }

            if( to.params.hasOwnProperty('backupname') && !found) {
              notFound()
            }
            else {
              next()
            }
          }).catch(function(error) {
            checkAuthError(error)
            notFound()
          });

        } else {
          
          axios
          .get('/stackgres/sgbackups')
          .then( function(response){

            var found = false

            if(response.data.length) {

              response.data.forEach( function(item, index) {
                  
                store.commit('updateBackups', { 
                  name: item.metadata.name,
                  data: item,
                  duration: '',
                  show: true
                });

                if( to.params.hasOwnProperty('backupname') && (to.params.backupname == item.metadata.name) && (to.params.namespace == item.metadata.namespace) )
                  found = true;

              });
            }

            if( to.params.hasOwnProperty('backupname') && !found) {
              notFound()
            }
            else {
              next()
            }

          }).catch(function(error) {
            checkAuthError(error)
            notFound()
          });
        }
        
        break;

      case 'LogsServer':
      case 'CreateLogsServer': 

        /* Check if requested Logs Server exists */
        axios
        .get('/stackgres/sgdistributedlogs')
        .then( function(response){

          var found = false
          var logs = []

          response.data.forEach( function(item, index) {
              
            logs.push({
              name: item.metadata.name,
              data: item
            }) 

            if( to.params.hasOwnProperty('name') && (to.params.name == item.metadata.name) && (to.params.namespace == item.metadata.namespace) )
              found = true;

          });

          if( to.params.hasOwnProperty('name') && !found)
            notFound()
          else {
            store.commit('addLogsClusters', logs);
            next()
          }

        }).catch(function(error) {
          checkAuthError(error)
          notFound()
        });

        break;
      
      case 'DbOps':

        /* Check if requested Database Operation exists */
        axios
        .get('/stackgres/sgdbops')
        .then( function(response){

          var found = false
          var dbOps = [];

          response.data.forEach( function(item, index) {
              
            response.data.forEach(function(item, index){
              dbOps.push({
                name: item.metadata.name,
                data: item
              })
            })

            if( to.params.hasOwnProperty('name') && (to.params.name == item.metadata.name) && (to.params.namespace == item.metadata.namespace) )
              found = true;

          });

          if( to.params.hasOwnProperty('name') && !found)
            notFound()
          else {
            store.commit('addDbOps', dbOps);
            next()
          }

        }).catch(function(error) {
          checkAuthError(error)
          notFound()
        });

        break;
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