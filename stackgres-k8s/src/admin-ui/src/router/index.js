import Vue from 'vue';
import VueRouter from 'vue-router';
import store from '../store'
import axios from 'axios'
import moment from 'moment'

// Form Components
import CreateCluster from '../components/forms/CreateCluster.vue'
import CreateProfile from '../components/forms/CreateProfile.vue'
import CreatePgConfig from '../components/forms/CreatePgConfig.vue'
import CreatePoolConfig from '../components/forms/CreatePoolConfig.vue'
import CreateBackupConfig from '../components/forms/CreateBackupConfig.vue'
import CreateBackup from '../components/forms/CreateBackup.vue'
import CreateLogsServer from '../components/forms/CreateLogsServer.vue'

// Main Components
import ClusterOverview from '../components/ClusterOverview.vue'
import ClusterInfo from '../components/ClusterInfo.vue'
import ClusterStatus from '../components/ClusterStatus.vue'
import Logs from '../components/Logs.vue'
import Backups from '../components/Backups.vue'
import PgConfig from '../components/PgConfig.vue'
import PoolConfig from '../components/PoolConfig.vue'
import BackupConfig from '../components/BackupConfig.vue'
import InstanceProfile from '../components/InstanceProfile.vue'
import LogsServer from '../components/LogsServer.vue'
import Grafana from '../components/Grafana.vue'
import NotFound from '../components/NotFound.vue'


Vue.use(VueRouter);

const routes = [
  { 
    path: '/crd/:action/cluster/:namespace', 
    component: CreateCluster,
    name: 'CreateCluster',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/crd/:action/cluster/:namespace/:name', 
    component: CreateCluster,
    name: 'EditCluster',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/crd/:action/profile/:namespace', 
    component: CreateProfile,
    name: 'CreateProfile',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/crd/:action/profile/:namespace/:name', 
    component: CreateProfile,
    name: 'EditProfile',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/crd/:action/pgconfig/:namespace', 
    component: CreatePgConfig,
    name: 'CreatePgConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/crd/:action/pgconfig/:namespace/:name', 
    component: CreatePgConfig,
    name: 'EditPgConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/crd/:action/connectionpooling/:namespace', 
    component: CreatePoolConfig,
    name: 'CreatePoolConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/crd/:action/connectionpooling/:namespace/:name', 
    component: CreatePoolConfig,
    name: 'EditPoolConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/crd/:action/backupconfig/:namespace', 
    component: CreateBackupConfig,
    name: 'CreateBackupConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/crd/:action/backupconfig/:namespace/:name', 
    component: CreateBackupConfig,
    name: 'EditBackupConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/crd/:action/backup/:namespace', 
    component: CreateBackup,
    name: 'CreateBackups',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/crd/create/backup/:namespace/:cluster', 
    component: CreateBackup,
    name: 'CreateClusterBackup',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/crd/:action/backup/:namespace/:uid', 
    component: CreateBackup,
    name: 'EditBackup',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/crd/:action/logs/:namespace', 
    component: CreateLogsServer,
    name: 'CreateLogsServer',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/crd/:action/logs/:namespace/:name', 
    component: CreateLogsServer,
    name: 'EditLogsServer',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/', 
    component: ClusterOverview,
    name: 'BaseUrl',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/overview', 
    component: ClusterOverview,
    name: 'ClusterOverviewEmpty',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/overview/:namespace', 
    component: ClusterOverview,
    name: 'ClusterOverview',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:cluster/configuration/:namespace/:name', 
    component: ClusterInfo,
    name: 'ClusterInfo',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:cluster/status/:namespace/:name', 
    component: ClusterStatus,
    name: 'ClusterStatus',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:cluster/logs/:namespace/:name', 
    component: Logs,
    name: 'Logs',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:cluster/logs/:namespace/:name/:time/:index', 
    component: Logs,
    name: 'SingleLogs',
    meta: {
      conditionalRoute: false
    },
  },
  {  
    path: '/backups/:namespace/', 
    component: Backups,
    name: 'NamespaceBackups',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/backups/:namespace/:name/:uid', 
    component: Backups,
    name: 'SingleBackups',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:cluster/backups/:namespace/:name', 
    component: Backups,
    name: 'ClusterBackups',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:cluster/backups/:namespace/:name/:uid', 
    component: Backups,
    name: 'SingleClusterBackups',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/configurations/postgres/:namespace', 
    component: PgConfig,
    name: 'PgConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/configurations/postgres/:namespace/:name', 
    component: PgConfig,
    name: 'SinglePgConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/configurations/connectionpooling/:namespace', 
    component: PoolConfig,
    name: 'PoolConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/configurations/connectionpooling/:namespace/:name', 
    component: PoolConfig,
    name: 'SinglePoolConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/configurations/backup/:namespace', 
    component: BackupConfig,
    name: 'BackupConfig',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/configurations/backup/:namespace/:name', 
    component: BackupConfig,
    name: 'SingleBackupConfig',
    meta: {
      conditionalRoute: false
    },
  },
  {  
    path: '/profiles/:namespace/', 
    component: InstanceProfile,
    name: 'InstanceProfile',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/profiles/:namespace/:name', 
    component: InstanceProfile,
    name: 'SingleInstanceProfile',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/logs/:namespace', 
    component: LogsServer,
    name: 'LogsServer',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/logs/:namespace/:name', 
    component: LogsServer,
    name: 'SingleLogsServer',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:cluster/monitor/:namespace/:name', 
    component: Grafana,
    name: 'ClusterMonitor',
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/:cluster/monitor/:namespace/:name/:pod', 
    component: Grafana,
    name: 'ClusterSingleMonitor',
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
  router.push('/not-found')
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
      document.cookie = 'sgToken=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;';
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
      .get('/stackgres/namespace')
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
        .get('/stackgres/sgcluster')
        .then( function(response){

          var found = false,
              stats = {};

            if(component == 'ClusterStatus') {
              /* Check for Cluster status */
              axios
              .get('/stackgres/sgcluster/stats/'+to.params.namespace+'/'+to.params.name)
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
        .get('/stackgres/sginstanceprofile')
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
        .get('/stackgres/sgpgconfig')
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
        .get('/stackgres/sgpoolconfig')
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
        .get('/stackgres/sgbackupconfig')
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
        if(to.params.hasOwnProperty('cluster')) {

          axios
          .get('/stackgres/sgcluster')
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
          .get('/stackgres/sgbackup')
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

                if( to.params.hasOwnProperty('uid') && (to.params.uid == item.metadata.uid) && (to.params.namespace == item.metadata.namespace) )
                  found = true;

              });
            }

            if( to.params.hasOwnProperty('uid') && !found) {
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
          .get('/stackgres/sgbackup')
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

                if( to.params.hasOwnProperty('uid') && (to.params.uid == item.metadata.uid) && (to.params.namespace == item.metadata.namespace) )
                  found = true;

              });
            }

            if( to.params.hasOwnProperty('uid') && !found) {
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
    }

  }

  // Redirect to default namespace overview if in base url
  if(to.name == 'BaseUrl') {
    router.push('/overview/default')
    store.commit('setCurrentNamespace', 'default');
    store.commit('setCurrentPath', {
      namespace: 'default',
      name: '',
      component: 'ClusterOverview'
    })
  } else {

    // Setup currentPath for sidebar use
    store.commit('setCurrentPath', {
      namespace: to.params.hasOwnProperty('namespace') ? to.params.namespace : '',
      name: to.params.hasOwnProperty('name') ? to.params.name : '',
      component: to.name.length ? to.name : ''
    })

    if(to.name !== 'NotFound')
      store.commit('notFound',false)
  }

  // If entering a Cluster, setup as current
  if ( to.params.cluster === "cluster" ) {

    let cluster = store.state.clusters.find(c => ( (to.params.name == c.name) && (to.params.namespace == c.data.metadata.namespace) ) );
    
    if ( typeof cluster !== "undefined" ) { 
      store.commit('setCurrentCluster', cluster);
    }

    $('.clu li.current').removeClass('current');
	  $('li.cluster-'+store.state.currentNamespace+'-'+store.state.currentCluster.name).addClass('current');
    
  }

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