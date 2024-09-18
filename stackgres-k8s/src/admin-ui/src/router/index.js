import Vue from 'vue';
import VueRouter from 'vue-router';
import store from '../store'
import sgApi from '../api/sgApi'

// Form Components
import CreateNamespace from '../components/forms/CreateNamespaces.vue'
import CreateCluster from '../components/forms/CreateSGClusters.vue'
import CreateShardedCluster from '../components/forms/CreateSGShardedClusters.vue'
import CreateStream from '../components/forms/CreateSGStreams.vue'
import CreateProfile from '../components/forms/CreateSGInstanceProfiles.vue'
import CreatePgConfig from '../components/forms/CreateSGPgConfigs.vue'
import CreatePoolConfig from '../components/forms/CreateSGPoolConfigs.vue'
import CreateObjectStorage from '../components/forms/CreateSGObjectStorages.vue'
import CreateScript from '../components/forms/CreateSGScripts.vue'
import CreateBackup from '../components/forms/CreateSGBackups.vue'
import CreateLogsServer from '../components/forms/CreateSGDistributedLogs.vue'
import CreateDbOps from '../components/forms/CreateSGDbOps.vue'

// Main Components
import GlobalDashboard from '../components/GlobalDashboard.vue'
import NamespaceOverview from '../components/NamespaceOverview.vue'

// SGClusters
import ClusterOverview from '../components/ClusterOverview.vue'
import ClusterInfo from '../components/ClusterInfo.vue'
import ClusterStatus from '../components/ClusterStatus.vue'
import ClusterLogs from '../components/ClusterLogs.vue'
import ClusterEvents from '../components/ClusterEvents.vue'
import Grafana from '../components/ClusterMonitoring.vue'

// SGShardedsClusters
import ShardedClusterOverview from '../components/crds/sgshardedcluster/SGShardedClusterOverview.vue'
import ShardedClusterStatus from '../components/crds/sgshardedcluster/SGShardedClusterStatus.vue'
import ShardedClusterConfig from '../components/crds/sgshardedcluster/SGShardedClusterConfig.vue'
import ShardedClusterMonitoring from '../components/crds/sgshardedcluster/SGShardedClusterMonitoring.vue'

// Other CRDs
import SGBackups from '../components/SGBackups.vue'
import SGPgConfigs from '../components/SGPgConfigs.vue'
import SGPoolConfigs from '../components/SGPoolConfigs.vue'
import SGObjectStorages from '../components/SGObjectStorages'
import SGScripts from '../components/SGScripts'
import SGInstanceProfiles from '../components/SGInstanceProfiles.vue'
import SGDistributedLogs from '../components/SGDistributedLogs.vue'
import SGDbOps from '../components/SGDbOps.vue'
import SGStreams from '../components/crds/sgstream/SGStreams.vue'

// Misc
import NotFound from '../components/NotFound.vue'

// Applications
import BabelfishCompass from '../components/applications/BabelfishCompass.vue'

// Settings
import SGConfig from '../components/SGConfig.vue'
import EditSGConfig from '../components/forms/EditSGConfig.vue'

// Users
import UserManagement from '../components/users/UserManagement.vue'
import CreateRole from '../components/forms/CreateRole.vue'
import CreateUser from '../components/forms/CreateUser.vue'
import SGUser from '../components/users/SGUser.vue'
import SGRole from '../components/users/SGRole.vue'

Vue.use(VueRouter);

const routes = [
  { 
    path: '/:namespace/sgclusters/new', 
    component: CreateCluster,
    name: 'CreateCluster',
    meta: {
      componentName: 'SGCluster' 
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/edit', 
    component: CreateCluster,
    name: 'EditCluster',
    meta: {
      componentName: 'SGCluster'
    },
  },
  { 
    path: '/:namespace/sgshardedclusters/new', 
    component: CreateShardedCluster,
    name: 'CreateShardedCluster',
    meta: {
      componentName: 'SGShardedCluster' 
    },
  },
  { 
    path: '/:namespace/sgshardedcluster/:name/edit', 
    component: CreateShardedCluster,
    name: 'EditShardedCluster',
    meta: {
      componentName: 'SGShardedCluster'
    },
  },
  { 
    path: '/:namespace/sgstreams/new', 
    component: CreateStream,
    name: 'CreateStream',
    meta: {
      componentName: 'SGStream' 
    },
  },
  { 
    path: '/:namespace/sgstream/:name/edit', 
    component: CreateStream,
    name: 'EditStream',
    meta: {
      componentName: 'SGStream'
    },
  },
  { 
    path: '/:namespace/sginstanceprofiles/new', 
    component: CreateProfile,
    name: 'CreateProfile',
    meta: {
      componentName: 'SGInstanceProfile'
    },
  },
  { 
    path: '/:namespace/sginstanceprofile/:name/edit',
    component: CreateProfile,
    name: 'EditProfile',
    meta: {
      componentName: 'SGInstanceProfile'
    },
  },
  { 
    path: '/:namespace/sgpgconfigs/new', 
    component: CreatePgConfig,
    name: 'CreatePgConfig',
    meta: {
      componentName: 'SGPgConfig', 
      customComponentName: 'SGPostgresConfig'
    },
  },
  { 
    path: '/:namespace/sgpgconfig/:name/edit', 
    component: CreatePgConfig,
    name: 'EditPgConfig',
    meta: {
      componentName: 'SGPgConfig', 
      customComponentName: 'SGPostgresConfig'
    },
  },
  { 
    path: '/:namespace/sgpoolconfigs/new', 
    component: CreatePoolConfig,
    name: 'CreatePoolConfig',
    meta: {
      componentName: 'SGPoolConfig', 
      customComponentName: 'SGPoolingConfig'
    },
  },
  { 
    path: '/:namespace/sgpoolconfig/:name/edit', 
    component: CreatePoolConfig,
    name: 'EditPoolConfig',
    meta: {
      componentName: 'SGPoolConfig', 
      customComponentName: 'SGPoolingConfig'
    },
  },
  { 
    path: '/:namespace/sgobjectstorages/new', 
    component: CreateObjectStorage,
    name: 'CreateObjectStorage',
    meta: {
      componentName: 'SGObjectStorage'
    },
  },
  { 
    path: '/:namespace/sgobjectstorage/:name/edit', 
    component: CreateObjectStorage,
    name: 'EditObjectStorage',
    meta: {
      componentName: 'SGObjectStorage'
    },
  },
  { 
    path: '/:namespace/sgscripts/new', 
    component: CreateScript,
    name: 'CreateScript',
    meta: {
      componentName: 'SGScript'
    },
  },
  { 
    path: '/:namespace/sgscript/:name/edit', 
    component: CreateScript,
    name: 'EditScript',
    meta: {
      componentName: 'SGScript'
    },
  },
  { 
    path: '/:namespace/sgbackups/new', 
    component: CreateBackup,
    name: 'CreateBackups',
    meta: {
      componentName: 'SGBackup'
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/sgbackups/new', 
    component: CreateBackup,
    name: 'CreateClusterBackup',
    meta: {
      componentName: 'SGCluster'
    },
  },
  { 
    path: '/:namespace/sgshardedcluster/:name/sgbackups/new', 
    component: CreateBackup,
    name: 'CreateShardedClusterBackup',
    meta: {
      componentName: 'SGShardedCluster'
    },
  },
  { 
    path: '/:namespace/sgbackup/:backupname/edit', 
    component: CreateBackup,
    name: 'EditBackup',
    meta: {
      componentName: 'SGBackup'
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/sgbackup/:backupname/edit', 
    component: CreateBackup,
    name: 'EditClusterBackup',
    meta: {
      componentName: 'SGCluster'
    },
  },
  { 
    path: '/:namespace/sgshardedcluster/:name/sgbackup/:backupname/edit', 
    component: CreateBackup,
    name: 'EditShardedClusterBackup',
    meta: {
      componentName: 'SGShardedCluster'
    },
  },
  { 
    path: '/:namespace/sgdistributedlogs/new', 
    component: CreateLogsServer,
    name: 'CreateLogsServer',
    meta: {
      componentName: 'SGDistributedLog'
    },
  },
  { 
    path: '/:namespace/sgdistributedlog/:name/edit', 
    component: CreateLogsServer,
    name: 'EditLogsServer',
    meta: {
      componentName: 'SGDistributedLog'
    },
  },
  { 
    path: '/:namespace/sgdbops/new', 
    component: CreateDbOps,
    name: 'CreateDbOps',
    meta: {
      componentName: 'SGDbOp'
    },
  },
  { 
    path: '/', 
    component: GlobalDashboard,
    name: 'GlobalDashboard',
  },
  { 
    path: '/index.html', 
    component: GlobalDashboard,
    name: 'GlobalDashboardIndex',
  },
  {
    path: '/:namespace',
    component: NamespaceOverview,
    name: 'NamespaceOverview',
  },
  {
    path: '/namespaces/new',
    component: CreateNamespace,
    name: 'CreateNamespace',
    meta: {
      componentName: 'Namespace'
    },
  },
  { 
    path: '/:namespace/sgclusters', 
    component: ClusterOverview,
    name: 'ClusterOverview',
    meta: {
      componentName: 'SGCluster'
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/config', 
    component: ClusterInfo,
    name: 'ClusterInfo',
    meta: {
      componentName: 'SGCluster'
    },
  },
  { 
    path: '/:namespace/sgcluster/:name', 
    component: ClusterStatus,
    name: 'ClusterStatus',
    meta: {
      componentName: 'SGCluster'
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/logs', 
    component: ClusterLogs,
    name: 'ClusterLogs',
    meta: {
      componentName: 'SGCluster'
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/events', 
    component: ClusterEvents,
    name: 'ClusterEvents',
    meta: {
      componentName: 'SGCluster'
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/event/:uid', 
    component: ClusterEvents,
    name: 'SingleClusterEvents',
    meta: {
      componentName: 'SGCluster'
    },
  },
  { 
    path: '/:namespace/sgshardedclusters', 
    component: ShardedClusterOverview,
    name: 'ShardedClusterOverview',
    meta: {
      componentName: 'SGShardedCluster'
    },
  },
  { 
    path: '/:namespace/sgshardedcluster/:name/config', 
    component: ShardedClusterConfig,
    name: 'ShardedClusterConfig',
    meta: {
      componentName: 'SGShardedCluster'
    },
  },
  { 
    path: '/:namespace/sgshardedcluster/:name', 
    component: ShardedClusterStatus,
    name: 'ShardedClusterStatus',
    meta: {
      componentName: 'SGShardedCluster'
    },
  },
  { 
    path: '/:namespace/sgshardedcluster/:name/logs', 
    component: ClusterLogs,
    name: 'ShardedClusterLogs',
    meta: {
      componentName: 'SGShardedCluster'
    },
  },
  { 
    path: '/:namespace/sgshardedcluster/:name/events', 
    component: ClusterEvents,
    name: 'ShardedClusterEvents',
    meta: {
      componentName: 'SGShardedCluster'
    },
  },
  { 
    path: '/:namespace/sgshardedcluster/:name/event/:uid', 
    component: ClusterEvents,
    name: 'SingleShardedClusterEvents',
    meta: {
      componentName: 'SGShardedCluster'
    },
  },
  { 
    path: '/:namespace/sgstreams', 
    component: SGStreams,
    name: 'SGStream',
    meta: {
      componentName: 'SGStream'
    },
  },
  { 
    path: '/:namespace/sgstream/:name', 
    component: SGStreams,
    name: 'SingleSGStream',
    meta: {
      componentName: 'SGStream'
    },
  },
  {  
    path: '/:namespace/sgbackups', 
    component: SGBackups,
    name: 'NamespaceBackups',
    meta: {
      componentName: 'SGBackup'
    },
  },
  { 
    path: '/:namespace/sgbackup/:backupname', 
    component: SGBackups,
    name: 'SingleBackups',
    meta: {
      componentName: 'SGBackup'
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/sgbackups', 
    component: SGBackups,
    name: 'ClusterBackups',
    meta: {
      componentName: 'SGCluster'
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/sgbackup/:backupname', 
    component: SGBackups,
    name: 'SingleClusterBackups',
    meta: {
      componentName: 'SGCluster'
    },
  },
  { 
    path: '/:namespace/sgshardedcluster/:name/sgbackups', 
    component: SGBackups,
    name: 'ShardedClusterBackups',
    meta: {
      componentName: 'SGShardedCluster'
    },
  },
  { 
    path: '/:namespace/sgshardedcluster/:name/sgbackup/:backupname', 
    component: SGBackups,
    name: 'SingleShardedClusterBackups',
    meta: {
      componentName: 'SGShardedCluster'
    },
  },
  { 
    path: '/:namespace/sgpgconfigs', 
    component: SGPgConfigs,
    name: 'PgConfig',
    meta: {
      componentName: 'SGPgConfig', 
      customComponentName: 'SGPostgresConfig'
    },
  },
  { 
    path: '/:namespace/sgpgconfig/:name', 
    component: SGPgConfigs,
    name: 'SinglePgConfig',
    meta: {
      componentName: 'SGPgConfig', 
      customComponentName: 'SGPostgresConfig'
    },
  },
  { 
    path: '/:namespace/sgpoolconfigs', 
    component: SGPoolConfigs,
    name: 'PoolConfig',
    meta: {
      componentName: 'SGPoolConfig', 
      customComponentName: 'SGPoolingConfig'
    },
  },
  { 
    path: '/:namespace/sgpoolconfig/:name', 
    component: SGPoolConfigs,
    name: 'SinglePoolConfig',
    meta: {
      componentName: 'SGPoolConfig', 
      customComponentName: 'SGPoolingConfig'
    },
  },
  { 
    path: '/:namespace/sgobjectstorages', 
    component: SGObjectStorages,
    name: 'SGObjectStorages',
    meta: {
      componentName: 'SGObjectStorage'
    },
  },
  { 
    path: '/:namespace/sgobjectstorage/:name', 
    component: SGObjectStorages,
    name: 'SingleObjectStorages',
    meta: {
      componentName: 'SGObjectStorage'
    },
  },
  { 
    path: '/:namespace/sgscripts', 
    component: SGScripts,
    name: 'SGScripts',
    meta: {
      componentName: 'SGScript'
    },
  },
  { 
    path: '/:namespace/sgscript/:name', 
    component: SGScripts,
    name: 'SingleScripts',
    meta: {
      componentName: 'SGScript'
    },
  },
  {  
    path: '/:namespace/sginstanceprofiles', 
    component: SGInstanceProfiles,
    name: 'InstanceProfile',
    meta: {
      componentName: 'SGInstanceProfile'
    },
  },
  { 
    path: '/:namespace/sginstanceprofile/:name', 
    component: SGInstanceProfiles,
    name: 'SingleInstanceProfile',
    meta: {
      componentName: 'SGInstanceProfile'
    },
  },
  { 
    path: '/:namespace/sgdistributedlogs', 
    component: SGDistributedLogs,
    name: 'LogsServer',
    meta: {
      componentName: 'SGDistributedLog'
    },
  },
  { 
    path: '/:namespace/sgdistributedlog/:name', 
    component: SGDistributedLogs,
    name: 'SingleLogsServer',
    meta: {
      componentName: 'SGDistributedLog'
    },
  },
  { 
    path: '/:namespace/sgdbops', 
    component: SGDbOps,
    name: 'DbOps',
    meta: {
      componentName: 'SGDbOp'
    },
  },
  { 
    path: '/:namespace/sgdbop/:name', 
    component: SGDbOps,
    name: 'SingleDbOps',
    meta: {
      componentName: 'SGDbOp'
    },
  },
  { 
    path: '/:namespace/sgdbop/:name/event/:uid', 
    component: SGDbOps,
    name: 'SingleDbOpsEvents',
    meta: {
      componentName: 'SGDbOp'
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/monitor', 
    component: Grafana,
    name: 'ClusterMonitor',
    meta: {
      componentName: 'SGCluster'
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/monitor/:pod', 
    component: Grafana,
    name: 'SingleClusterMonitor',
    meta: {
      componentName: 'SGCluster'
    },
  },
  { 
    path: '/:namespace/sgcluster/:name/monitor/:pod/:dashboard', 
    component: Grafana,
    name: 'SingleClusterMonitorDashboard',
    meta: {
      componentName: 'SGCluster'
    },
  },
  {
    path: '/:namespace/sgcluster/:name/monitor/:pod/:range', 
    component: Grafana,
    name: 'SingleClusterMonitorRange',
    meta: {
      componentName: 'SGCluster'
    },
  },
  {
    path: '/:namespace/sgcluster/:name/monitor/:pod/:dashboard/:range', 
    component: Grafana,
    name: 'SingleClusterMonitorDashboardRange',
    meta: {
      componentName: 'SGCluster'
    },
  },
  { 
    path: '/:namespace/sgshardedcluster/:name/monitor', 
    component: ShardedClusterMonitoring,
    name: 'ShardedClusterMonitor',
    meta: {
      componentName: 'SGShardedCluster'
    },
  },
  { 
    path: '/:namespace/sgshardedcluster/:name/monitor/:pod', 
    component: ShardedClusterMonitoring,
    name: 'SingleShardedClusterMonitor',
    meta: {
      componentName: 'SGShardedCluster'
    },
  },
  { 
    path: '/:namespace/sgshardedcluster/:name/monitor/:pod/:dashboard', 
    component: ShardedClusterMonitoring,
    name: 'SingleShardedClusterMonitorDashboard',
    meta: {
      componentName: 'SGShardedCluster'
    },
  },
  {
    path: '/:namespace/sgshardedcluster/:name/monitor/:pod/:range', 
    component: ShardedClusterMonitoring,
    name: 'SingleShardedClusterMonitorRange',
    meta: {
      componentName: 'SGShardedCluster'
    },
  },
  {
    path: '/:namespace/sgshardedcluster/:name/monitor/:pod/:dashboard/:range', 
    component: ShardedClusterMonitoring,
    name: 'SingleShardedClusterMonitorDashboardRange',
    meta: {
      componentName: 'SGShardedCluster'
    },
  },
  {
    path: '/:namespace/application/babelfish-compass/', 
    component: BabelfishCompass,
    name: 'BabelfishCompass',
    meta: { 
      componentName: 'Application'
    },
  },
  {
    path: '/sgconfig/:name', 
    component: SGConfig,
    name: 'SGConfig',
    meta: { 
      componentName: 'SGConfig'
    },
  },
  ,
  {
    path: '/sgconfig/:name/edit', 
    component: EditSGConfig,
    name: 'EditSGConfig',
    meta: { 
      componentName: 'SGConfig'
    },
  },
  {
    path: '/manage/users', 
    component: UserManagement,
    name: 'UserManagement',
    meta: { 
      componentName: 'User'
    },
  },
  {
    path: '/manage/user/:name', 
    component: SGUser,
    name: 'SingleUser',
    meta: { 
      componentName: 'User'
    },
  },
  {
    path: '/manage/users/new', 
    component: CreateUser,
    name: 'CreateUser',
    meta: { 
      componentName: 'User'
    },
  },
  {
    path: '/manage/user/:name/edit', 
    component: CreateUser,
    name: 'EditUser',
    meta: { 
      componentName: 'User'
    },
  },
  {
    path: '/manage/roles/new', 
    component: CreateRole,
    name: 'CreateRole',
    meta: { 
      componentName: 'Role'
    },
  },
  {
    path: '/manage/role/:name/edit', 
    component: CreateRole,
    name: 'EditRole',
    meta: { 
      componentName: 'Role'
    },
  },
  {
    path: '/manage/role/:name', 
    component: SGRole,
    name: 'SingleRole',
    meta: { 
      componentName: 'Role'
    },
  },
  {
    path: '/manage/clusterroles/new', 
    component: CreateRole,
    name: 'CreateClusterRole',
    meta: { 
      componentName: 'ClusterRole'
    },
  },
  {
    path: '/manage/clusterrole/:name/edit', 
    component: CreateRole,
    name: 'EditClusterRole',
    meta: { 
      componentName: 'ClusterRole'
    },
  },
  {
    path: '/manage/clusterrole/:name', 
    component: SGRole,
    name: 'SingleClusterRole',
    meta: { 
      componentName: 'ClusterRole'
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

function iCan( action = 'any', kind, namespace = '' ) {
  if(kind === 'sgstreams')
    return true;
  
  if(namespace.length) { // If filtered by namespace

    if(namespace === 'all') {
      return (store.state.permissions.allowed.namespaced.filter(n => 
          (n.resources[kind].includes(action)) 
        ).length == store.state.permissions.allowed.namespaced.length
      );
    } else if(namespace === 'any') {
      return (typeof store.state.permissions.allowed.namespaced.find(n => 
          n.resources[kind].includes(action)
        ) !== 'undefined'
      );
    } else {
      let permissions = store.state.permissions.allowed.namespaced.find(p => (p.namespace == namespace));
      return (
        (typeof permissions != 'undefined') &&
          (
            ( (action == 'any') && permissions.resources[kind].length) ||
            ( (action != 'any') && permissions.resources[kind].includes(action) 
          )
        )
      )
    }

  } else {
    return store.state.permissions.allowed.unnamespaced.hasOwnProperty(kind) && 
      ( (action === 'any') || store.state.permissions.allowed.unnamespaced[kind].includes(action) )
  }
}

function actionForbidden() {
  const content = `
                <h2>Action Forbidden</h2>
                <p>You don't have enough permissions to access the requested resource. For more information on authorization management on StackGres, click the button below.</p>
                <br/><br/><a class="btn" href="https://stackgres.io/doc/latest/api/rbac/" target="_blank" title="https://stackgres.io/doc/latest/api/rbac/">More Info</a>
              `;
  const tooltip = `<div class="contentTooltip show">
    <div class="close"></div>
    <div class="info center">
      <span class="close">CLOSE</span>
      <div class="content">` + content + `</div>
    </div>
  </div>`;

  $('#main').append(tooltip);
}


router.beforeResolve((to, from, next) => {

  // If loading CRD from direct URL validate if CRD exists on the API before loading
  if( from.path == '/') {
    
    let kind = 
      to.matched[0].meta.hasOwnProperty('componentName')
        ? (to.matched[0].meta.componentName.toLowerCase() + 's')
        : (
          to.matched[0].components.default.name.startsWith('Cluster')
            ? 'sgclusters'
            : ( 
              to.matched[0].components.default.name.startsWith('Create') 
                ? to.matched[0].components.default.name.replace('Create', '')
                : to.matched[0].components.default.name 
            )
        );

    if(!checkLogin()) {
      next(); 
      return;
    }

    // Read and set user permissions first
    sgApi
    .get('can_i')
    .then( function(response) {
      store.commit('setPermissions', response.data);
    })
    .then( function() {
      /* First check if Namespace exist */
      if(to.params.hasOwnProperty('namespace') && !['applications','users','BabelfishCompass', 'roles', 'clusterroles'].includes(kind)) {

        let namespaceName = to.params.namespace;
        
        sgApi
        .get('namespaces')
        .then( function(response){
          store.commit('addNamespaces', response.data)
          
          if(response.data.includes(namespaceName)) {
            
            store.commit('setCurrentNamespace', namespaceName);

            if(to.params.hasOwnProperty('name') || to.params.hasOwnProperty('backupname')) {
              
              if ( 
                  ( 
                    ( to.params.hasOwnProperty('name') || to.params.hasOwnProperty('backupname') ) && 
                    to.name.startsWith('Edit') && !iCan('patch', kind.toLowerCase(), to.params.namespace) 
                  ) ||
                  ( to.name.startsWith('Create') && !iCan('create', kind.toLowerCase(), to.params.namespace) )                  
              ) {
                actionForbidden();
                router.push('/' + to.params.namespace);
              } else {
                let resourceName = ( to.params.hasOwnProperty('name') ? to.params.name : ( to.params.hasOwnProperty('backupname') ? to.params.backupname : '' ) );
              
                // Then check if requested resource exists
                if(resourceName.length) {

                  // If requesting for backups inside a single cluster
                  if(to.name.includes('ClusterBackup')) {

                    if(to.params.hasOwnProperty('backupname')) {
                      sgApi
                      .getResourceDetails('sgbackups', namespaceName, to.params.backupname)
                      .catch(function(error) {
                        checkAuthError(error);
                        notFound();
                        return false;
                      });
                    }

                    kind = 'sgclusters';

                  }
          
                  sgApi
                  .getResourceDetails(kind.toLowerCase(), namespaceName, resourceName)
                  .catch(function(error) {
                    checkAuthError(error)
                    notFound()
                  });
                }
              }
              
            } else if ( to.meta.hasOwnProperty('componentName') && !to.params.hasOwnProperty('name') && !to.params.hasOwnProperty('backupname') && !iCan('list', kind.toLowerCase(), to.params.namespace) ) {
              actionForbidden();
              router.push('/' + to.params.namespace);
            }
              
          } else {
            notFound();
          }
        }).catch(function(error) {
          checkAuthError(error)
          notFound()
        });
        
      }
    })
    .catch(function(err) {
      console.log(err);
      checkAuthError(err);
    });

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