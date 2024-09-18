import Vue from 'vue'
import Vuex, { Store } from 'vuex'
import axios from 'axios'

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    theme: 'light',
    loginToken: '',
    authType: 'JWT',
    showLogs: false,
    notFound: false,
    isLoading: false,
    currentPath: {
      namespace: '',
      name: '',
      component: '',
    },
    currentNamespace: '',
    ready: false,
    currentCluster: {},
    currentShardedCluster: {},
    currentPods: [],
    namespaces: [],
    allNamespaces: [],
    roles: [],
    clusterroles: [],
    users: [],
    sgclusters: null,
    sgshardedclusters: null,
    sgstreams: null,
    sgbackups: null,
    sgpgconfigs: null,
    sgpoolconfigs: null,
    sginstanceprofiles: null,
    sgobjectstorages: null,
    sgscripts: null,
    storageClasses: [],
    logs: [],
    sgdistributedlogs: null,
    sgdbops: null,
    postgresVersions: {},
    applications: [],
    dashboardsList: [],
    sgconfigs: null,
    cloneCRD: {},
    timezone: 'local',
    view: 'normal',
    interval: 1,
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

    loading (state, isLoading) {
      state.isLoading = isLoading;
    },
    
    setPermissions (state, permissions) {
      state.permissions.allowed = permissions;
    },

    flushPermissions (state) {
      state.permissions = {
        allowed: {
          namespaced: [],
          unnamespaced: {}
        },
        forbidden: []
      };
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

      if(token != 'OIDC') {
        axios.defaults.headers.common['Authorization'] = 'Bearer ' + token;
      }
    },

    setAuthType(state, authType) {
      state.authType = authType;
    },

    setTheme (state, theme = '') {
      state.theme = theme.length ? theme : ( (state.theme === 'light') ? 'dark' : 'light');
      document.cookie = "sgTheme=" + state.theme + "; Path=/; expires=Fri, 31 Dec 9999 23:59:59 GMT; SameSite=Strict;";
      $("body").toggleClass("darkmode");
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

    setRoles (state, roles) {
      state.roles = [...roles];
    },

    setClusterRoles (state, roles) {
      state.clusterroles = [...roles];
    },

    setUsers (state, users) {
      state.users = [...users];
    },

    initKind(state, kind) {
      if(state[kind] === null) {
        state[kind] = [];
      }
    },

    addLogsClusters (state, logsClusters) {
      state.sgdistributedlogs = [...logsClusters];
    },

    addDbOps (state, dbOps) {
      state.sgdbops = [...dbOps];
    },

    addSgStreams (state, sgStreams) {
      state.sgstreams = [...sgStreams];
    },

    addStorageClasses (state, storageClassesList) {
      state.storageClasses = (storageClassesList === null) ? null : [...storageClassesList];
    },

    setCurrentCluster (state, cluster) {
      state.currentCluster = cluster;      
    },

    setCurrentShardedCluster (state, cluster) {
      state.currentShardedCluster = cluster;      
    },

    setCurrentPods (state, pods) {
      state.currentPods = pods;
    },

    updateClusters ( state, cluster ) {

      let index = state.sgclusters.find(c => (cluster.data.metadata.name == c.name) && (cluster.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = cluster.data;
      } else {
        state.sgclusters.push( cluster );    
      }

    },

    updateShardedClusters ( state, cluster ) {

      let index = state.sgshardedclusters.find(c => (cluster.data.metadata.name == c.name) && (cluster.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = cluster.data;
      } else {
        state.sgshardedclusters.push( cluster );    
      }

    },

    updateClusterStats (state, clusterStats) {

      let cluster = state.sgclusters.find(c => (clusterStats.name == c.name) && (clusterStats.namespace == c.data.metadata.namespace) ); 

      if ( typeof cluster !== "undefined" )
        cluster.status = clusterStats.stats
      
    },

    updateBackups ( state, backup ) {

        let index = state.sgbackups.find(p => (backup.data.metadata.name == p.name) && (backup.data.metadata.namespace == p.data.metadata.namespace) ); 

        if ( typeof index !== "undefined" ) {
          index.data = backup.data;
        } else {
          state.sgbackups.push( backup );    
        }

    },
    
    showBackup ( state, show ) {

      state.sgbackups[show.pos].show = show.isVisible;

    },

    showDbOp ( state, show ) {

      state.sgdbops[show.pos]['show'] = show.isVisible;

    },

    updatePGConfig ( state, config ) {

      let index = state.sgpgconfigs.find(c => (config.data.metadata.name == c.name) && (config.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = config.data;
      } else {
        state.sgpgconfigs.push( config );    
      }

    },
    updatePoolConfig ( state, config ) {

      let index = state.sgpoolconfigs.find(c => (config.data.metadata.name == c.name) && (config.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = config.data;
      } else {
        state.sgpoolconfigs.push( config );    
      }

    },
    updateObjectStorages ( state, config ) {

      let index = state.sgobjectstorages.find(c => (config.data.metadata.name == c.name) && (config.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = config.data;
      } else {
        state.sgobjectstorages.push( config );    
      }

    },
    updateScripts ( state, config ) {

      let index = state.sgscripts.find(c => (config.data.metadata.name == c.name) && (config.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = config.data;
      } else {
        state.sgscripts.push( config );    
      }

    },
    updateProfiles ( state, profile ) {

      let index = state.sginstanceprofiles.find(p => (profile.data.metadata.name == p.name) && (profile.data.metadata.namespace == p.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = profile.data;
      } else {
        state.sginstanceprofiles.push( profile );    
      }

    },

    setCurrentNamespace (state, namespace) {
      state.currentNamespace = namespace;
    },

    flushResource(state, resource) {
      state[resource].length = 0;
    },

    setDeleteItem (state, item) {
      
      if(!item.kind.length) { // Item has been deleted succesfuly, remove from store
        let kind = state.deleteItem.kind;
        state[kind] = state[kind].filter( el => !(
            (el.hasOwnProperty('name') ? el.name : el.metadata.name) == state.deleteItem.name && 
            (el.hasOwnProperty('data') ? el.data.metadata.namespace : el.metadata.namespace) == state.deleteItem.namespace
          )
        );
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

    setApplications (state, applications) {
      state.applications = applications;
    },

    setDashboardsList (state, dashboardsList) {
      state.dashboardsList = dashboardsList;
    },
    
    setSGConfigs (state, configs) {
      state.sgconfigs = configs;
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

    setReloadInterval (state, interval) {
      state.interval = interval;
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
