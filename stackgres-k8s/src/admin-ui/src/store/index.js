import Vue from 'vue'
import Vuex, { Store } from 'vuex'
import axios from 'axios'

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    theme: 'light',
    loginToken: '',
    showLogs: false,
    notFound: false,
    currentPath: {
      namespace: '',
      name: '',
      component: '',
    },
    currentNamespace: '',
    ready: false,
    currentCluster: {},
    currentPods: [],
    namespaces: [],
    allNamespaces: [],
    clusters: [],
    backups: [],
    pgConfig: [],
    poolConfig: [],
    backupConfig: [],
    profiles: [],
    storageClasses: [],
    logs: [],
    logsClusters: [],
    dbOps: [],
    postgresVersions: {},
    cloneCRD: {},
    timezone: 'local',
    view: 'normal',
    permissions: {
      allowed: {
        namespaced: [],
        unnamespaced: {}
      },
      forbidden: []
    },
    tooltipsText: 'Click on a question mark to get help and tips about that field.',
    tooltips: {},
    deleteItem: {
      kind: '',
      namespace: '',
      name: '',
      redirect: ''
    },
    confirmDeleteName: '',
    restartCluster: {},
    notifications: {
      showAll: false,
      messages: []
    }
  },

  mutations: {

    notFound (state, notFound) {
      state.notFound = notFound;
    },
    
    setPermissions (state, permissions) {
      state.permissions.allowed = permissions;
    },

    setNoPermissions (state, kind) {
      if(!state.permissions.forbidden.includes(kind))
        state.permissions.forbidden.push(kind)
    },

    setReady (state, ready) {
      state.ready = ready;
    }, 

    setLoginToken (state, token = '') {
      state.loginToken = token;
      axios.defaults.headers.common['Authorization'] = 'Bearer ' + token;
    },

    setTheme (state, theme) {
      state.theme = theme;
      document.cookie = "sgTheme="+theme+"; Path=/; expires=Fri, 31 Dec 9999 23:59:59 GMT; SameSite=Strict;";
    },

    setCurrentPath (state, path) {
      state.currentPath = path
    },
    
    setCurrentNamespace (state, namespace) {
      state.currentNamespace = namespace;
    },

    updateNamespaces (state, namespace) {
      state.namespaces.push(namespace);
    },

    addNamespaces (state, namespacesList) {
      state.allNamespaces = [...namespacesList];
    },

    addLogsClusters (state, logsClusters) {
      state.logsClusters = [...logsClusters];
    },

    addDbOps (state, dbOps) {
      state.dbOps = [...dbOps];
    },

    addStorageClasses (state, storageClassesList) {
      state.storageClasses = [...storageClassesList];
    },

    setCurrentCluster (state, cluster) {

      state.currentCluster = cluster;
      
    },

    setCurrentPods (state, pods) {
      state.currentPods = pods;
    },

    updateClusters ( state, cluster ) {

      let index = state.clusters.find(c => (cluster.data.metadata.name == c.name) && (cluster.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = cluster.data;
      } else {
        state.clusters.push( cluster );    
      }

    },

    updateClusterStats (state, clusterStats) {

      let cluster = state.clusters.find(c => (clusterStats.name == c.name) && (clusterStats.namespace == c.data.metadata.namespace) ); 

      if ( typeof cluster !== "undefined" )
        cluster.status = clusterStats.stats
      
    },

    updateBackups ( state, backup ) {

        let index = state.backups.find(p => (backup.data.metadata.name == p.name) && (backup.data.metadata.namespace == p.data.metadata.namespace) ); 

        if ( typeof index !== "undefined" ) {
          index.data = backup.data;
        } else {
          state.backups.push( backup );    
        }

    },
    
    showBackup ( state, show ) {

      state.backups[show.pos].show = show.isVisible;

    },

    showDbOp ( state, show ) {

      state.dbOps[show.pos]['show'] = show.isVisible;

    },

    updatePGConfig ( state, config ) {

      let index = state.pgConfig.find(c => (config.data.metadata.name == c.name) && (config.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = config.data;
      } else {
        state.pgConfig.push( config );    
      }

    },
    updatePoolConfig ( state, config ) {

      let index = state.poolConfig.find(c => (config.data.metadata.name == c.name) && (config.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = config.data;
      } else {
        state.poolConfig.push( config );    
      }

    },
    updateBackupConfig ( state, config ) {

      let index = state.backupConfig.find(c => (config.data.metadata.name == c.name) && (config.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = config.data;
      } else {
        state.backupConfig.push( config );    
      }

    },
    updateProfiles ( state, profile ) {

      let index = state.profiles.find(p => (profile.data.metadata.name == p.name) && (profile.data.metadata.namespace == p.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = profile.data;
      } else {
        state.profiles.push( profile );    
      }

    },

    setCurrentNamespace (state, namespace) {
      state.currentNamespace = namespace;
    },

    flushAllNamespaces (state) {
      state.allNamespaces.length = 0;
    },

    flushClusters (state ) {
      state.clusters.length = 0;
    },

    flushBackups (state ) {
    	state.backups.length = 0;
    },

    flushPoolConfig (state ) {
      state.poolConfig.length = 0;
    },

    flushPGConfig (state ) {
      state.pgConfig.length = 0;
    },

    flushBackupConfig (state ) {
      state.backupConfig.length = 0;
    },

    flushProfiles (state ) {
      state.profiles.length = 0;
    },

    flushStorageClasses (state ) {
      state.storageClasses.length = 0;
    },

    flushLogsClusters (state ) {
      state.logsClusters.length = 0;
    },

    setDeleteItem (state, item) {
      
      if(!item.kind.length) { // Item has been deleted succesfuly, remove from store

        let kind = '';

        switch(state.deleteItem.kind) {
          
          case 'sgclusters':
            kind = 'clusters'
            break;
          
          case 'sgbackups':
            kind = 'backups'
            break;

          case 'sgpgconfigs':
            kind = 'pgConfig'
            break;

          case 'sgpoolconfigs':
            kind = 'poolConfig'
            break;

          case 'sgbackupconfigs':
            kind = 'backupConfig'
            break;
          
          case 'sginstanceprofiles':
            kind = 'profiles'
            break;
          
          case 'sgdistributedlogs':
            kind = 'logsClusters'
            break;
          
          case 'sgdbops':
            kind = 'dbOps'
            break;
        }

        state[kind].splice(state[kind].findIndex( el => (el.name == state.deleteItem.name) && (el.data.metadata.namespace == state.deleteItem.namespace) ), 1)

      }

      state.deleteItem = item;
    },

    removeResource(state, resource) {
      state[resource.kind].splice(state[resource.kind].findIndex( el => (el.name == resource.name) && (el.data.metadata.namespace == resource.namespace) ), 1)
    },

    setConfirmDeleteName (state, name) {
      state.confirmDeleteName = name;
    },

    setLogs (state, logs) {
      state.logs = logs;
    },

    appendLogs (state, logs) {
      state.logs = state.logs.concat(logs);
    },

    setTooltips (state, tooltips) {
      Object.keys(tooltips).forEach(function(key){
        state.tooltips['sg' + key.replace('Dto','').toLowerCase()] = tooltips[key];
      })
    },

    setTooltipsText (state, tooltipsText) {
      state.tooltipsText = tooltipsText;
    },

    showLogs (state, show) {
      state.showLogs = show;
    },

    setCloneCRD (state, crd) {
      state.cloneCRD = crd;
    },

    setCloneName (state, name) {
      state.cloneCRD.data.metadata.name = name;
    },

    setCloneNamespace (state, namespace) {
      state.cloneCRD.data.metadata.namespace = namespace;
    },

    setPostgresVersions (state, versions) {
      state.postgresVersions = versions;
    },

    toggleTimezone (state) {
      state.timezone = (state.timezone == 'local') ? 'utc' : 'local';
      document.cookie = "sgTimezone=" + state.timezone + "; Path=/; expires=Fri, 31 Dec 9999 23:59:59 GMT; SameSite=Strict;";
    },

    toggleView (state) {
      state.view = (state.view == 'normal') ? 'collapsed' : 'normal';
      $('body').toggleClass('collapsed')
      document.cookie = "sgView=" + state.view + "; Path=/; expires=Fri, 31 Dec 9999 23:59:59 GMT; SameSite=Strict;";
    },

    setRestartCluster (state, cluster) {
      if( cluster.namespace.length && cluster.name.length ) {
        state.restartCluster = cluster;
      } else {
        state.restartCluster = {};
      }
    },

    addNotification(state, notification) {
      state.notifications.messages.unshift(notification);
    },

    toggleNotifications(state, showAll = false) {
      let now = new Date();
      now = new Date(now.setMilliseconds(now.getMilliseconds() - 100));
      state.notifications.showAll = showAll;
      state.notifications.messages.forEach( (m) => {
        if (now > new Date(m.timestamp)) { 
          m.show  = false 
        } 
      });
    }
    
  }
});
