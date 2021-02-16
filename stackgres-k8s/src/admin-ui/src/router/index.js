import Vue from 'vue';
import VueRouter from 'vue-router';
import store from '../store'

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
  },/* 
  {
    path: '*',
    component: NotFound
  } */
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
});

router.beforeResolve((to, from, next) => {

  if(!to.params.hasOwnProperty('namespace')) {
    router.push('/overview/default')
  } else {
    store.commit('setCurrentPath', {
      namespace: to.params.namespace,
      name: to.params.hasOwnProperty('name') ? to.params.name : '',
      component: to.name.length ? to.name : ''
    })
  }

  next()
  
})

export default router;