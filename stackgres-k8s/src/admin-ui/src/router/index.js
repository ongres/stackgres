import Vue from 'vue';
import VueRouter from 'vue-router';

// Form Components
import CreateCluster from '../components/forms/CreateCluster.vue'
import CreateProfile from '../components/forms/CreateProfile.vue'
import CreatePGConfig from '../components/forms/CreatePGConfig.vue'
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
import SGProfiles from '../components/SGProfiles.vue'
import LogsServer from '../components/LogsServer.vue'
import Grafana from '../components/Grafana.vue'
import NotFound from '../components/NotFound.vue'


Vue.use(VueRouter);

const routes = [
  { 
    path: '/admin/crd/:action/cluster/:namespace', 
    component: CreateCluster,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/crd/:action/cluster/:namespace/:name', 
    component: CreateCluster,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/crd/:action/profile/:namespace', 
    component: CreateProfile,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/crd/:action/profile/:namespace/:name', 
    component: CreateProfile,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/crd/:action/pgconfig/:namespace', 
    component: CreatePGConfig,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/crd/:action/pgconfig/:namespace/:name', 
    component: CreatePGConfig,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/crd/:action/connectionpooling/:namespace', 
    component: CreatePoolConfig,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/crd/:action/connectionpooling/:namespace/:name', 
    component: CreatePoolConfig,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/crd/:action/backupconfig/:namespace', 
    component: CreateBackupConfig,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/crd/:action/backupconfig/:namespace/:name', 
    component: CreateBackupConfig,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/crd/:action/backup/:namespace', 
    component: CreateBackup,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/crd/create/backup/:namespace/:cluster', 
    component: CreateBackup,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/crd/:action/backup/:namespace/:uid', 
    component: CreateBackup,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/crd/:action/logs/:namespace', 
    component: CreateLogsServer,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/crd/:action/logs/:namespace/:name', 
    component: CreateLogsServer,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin', 
    component: ClusterOverview,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/overview/:namespace', 
    component: ClusterOverview,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/:cluster/configuration/:namespace/:name', 
    component: ClusterInfo,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/:cluster/status/:namespace/:name', 
    component: ClusterStatus,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/:cluster/logs/:namespace/:name', 
    component: Logs,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/:cluster/logs/:namespace/:name/:time/:index', 
    component: Logs,
    meta: {
      conditionalRoute: false
    },
  },
  {  
    path: '/admin/backups/:namespace/', 
    component: Backups,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/backups/:namespace/:name/:uid', 
    component: Backups,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/:cluster/backups/:namespace/:name', 
    component: Backups,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/:cluster/backups/:namespace/:name/:uid', 
    component: Backups,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/configurations/postgres/:namespace', 
    component: PgConfig,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/configurations/postgres/:namespace/:name', 
    component: PgConfig,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/configurations/connectionpooling/:namespace', 
    component: PoolConfig,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/configurations/connectionpooling/:namespace/:name', 
    component: PoolConfig,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/configurations/backup/:namespace', 
    component: BackupConfig,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/configurations/backup/:namespace/:name', 
    component: BackupConfig,
    meta: {
      conditionalRoute: false
    },
  },
  {  
    path: '/admin/profiles/:namespace/', 
    component: SGProfiles,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/profiles/:namespace/:name', 
    component: SGProfiles,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/logs/:namespace', 
    component: LogsServer,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/logs/:namespace/:name', 
    component: LogsServer,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/monitor/', 
    component: Grafana,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/:cluster/monitor/:namespace/:name', 
    component: Grafana,
    meta: {
      conditionalRoute: false
    },
  },
  { 
    path: '/admin/:cluster/monitor/:namespace/:name/:pod', 
    component: Grafana,
    meta: {
      conditionalRoute: false
    },
  },
  {
    path: '*',
    component: NotFound
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
});
