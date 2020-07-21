Vue.use(VueRouter);

var navItems = document.getElementsByClassName("nav-item");

var clustersList = [],
    //clustersData = [],
    backups = [],
    pgConf = [],
    poolConf = [],
    profiles = [],
    namespaces = [],
    //currentCluster = "",
    currentPod = "",
    apiData = [],
    pods = [],
    doneInit = false,
    apiEndpoints = [
      'cluster',
      'sgpgconfig',
      'sgpoolconfig',
      'profile'
    ];

//Local Json "API"
//var apiURL = 'js/data/';

//Test API
//var apiURL = 'http://192.168.1.10:7978/';

//Prod API
var apiURL = '/stackgres/';

var urlParams = new URLSearchParams(window.location.search);

const router = new VueRouter({
  mode: 'history',
  routes: [
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

  ],
});


router.beforeEach((to, from, next) => { 

  const component = to.matched[0].components.default.options.name;
  
  // If loading CRD from direct URL validate if CRD exists on the API before loading
  if( from.path == '/') {

    checkLogin();

    /* Check if Namespace exist */
    if(to.params.hasOwnProperty('namespace')) {
      axios
      .get(apiURL+'namespace')
      .then( function(response){
        store.commit('addNamespaces',response.data)
        if(response.data.includes(to.params.namespace)) {
          store.commit('setCurrentNamespace', to.params.namespace);
        }
        else
          notFound();
      }).catch(function(err) {
        notFound()
      });
    }
      
    switch(component) {

      case 'CreateCluster':
      case 'Logs':
      case 'Grafana':    

        axios
        .get(apiURL+'sgcluster')
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

        }).catch(function(err) {
          notFound()
        });

        break

      case 'ClusterStatus':
      case 'ClusterInfo':
        /* Check if Cluster exists */
        axios
        .get(apiURL+'sgcluster/stats/'+to.params.namespace+'/'+to.params.name)
        .then( function(response){
          next()
        }).catch(function(err) {
          notFound()
        });

        break;

      case 'InstanceProfile':
      case 'CreateProfile':
        /* Check if Profile exists */
        axios
        .get(apiURL+'sginstanceprofile')
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

        }).catch(function(err) {
          notFound()
        });

        break

      case 'PostgresConfig':
      case 'CreatePgConfig':
        
        /* Check if Postgres Config exists */
        axios
        .get(apiURL+'sgpgconfig')
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

        }).catch(function(err) {
          notFound()
        });

        break;

      case 'PoolConfig':
      case 'CreatePoolConfig':

        /* Check if PgBouncer Config exists */
        axios
        .get(apiURL+'sgpoolconfig')
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

        }).catch(function(err) {
          notFound()
        });

        break;
      
      case 'BackupConfig':
      case 'CreateBackupConfig':
        /* Check if BackupConfig Config exists */
        axios
        .get(apiURL+'sgbackupconfig')
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

        }).catch(function(err) {
          notFound()
        });

        break;

      case 'Backups':
      case 'CreateBackup':
        /* If filtered by Cluster, first check if Cluster exists */
        if(to.params.hasOwnProperty('cluster')) {

          axios
          .get(apiURL+'sgcluster')
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
  
          }).catch(function(err) {
            notFound()
          });

          axios
          .get(apiURL+'sgbackup')
          .then( function(response){ 
            var found = false

            if(response.data.length) {

              response.data.forEach( function(item, index) {
                  
                if( (item.status !== null) && item.status.hasOwnProperty('process')) {
                  if( item.status.process.status === 'Completed' ) {
                    //console.log('setting duration');
                    start = moment(item.status.process.timing.start);
                    finish = moment(item.status.process.timing.stored);
                    duration = new Date(moment.duration(finish.diff(start))).toISOString();
                  } 
                } else {
                  //console.log('duration not set');
                  duration = '';
                }
                
                  
                store.commit('updateBackups', { 
                  name: item.metadata.name,
                  data: item,
                  duration: duration,
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
          }).catch(function(err) {
            notFound()
          });

        } else {
          
          axios
          .get(apiURL+'sgbackup')
          .then( function(response){

            var found = false

            if(response.data.length) {

              response.data.forEach( function(item, index) {
                  
                if( (item.status !== null) && item.status.hasOwnProperty('process')) {
                  if( item.status.process.status === 'Completed' ) {
                    //console.log('setting duration');
                    start = moment(item.status.process.timing.start);
                    finish = moment(item.status.process.timing.stored);
                    duration = new Date(moment.duration(finish.diff(start))).toISOString();
                  } 
                } else {
                  //console.log('duration not set');
                  duration = '';
                }
                
                  
                store.commit('updateBackups', { 
                  name: item.metadata.name,
                  data: item,
                  duration: duration,
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

          }).catch(function(err) {
            notFound()
          });
        }
        
        break;

    }

  }
  
  // If entering a Cluster, setup as current
  if ( to.params.cluster === "cluster" ) {

    let cluster = store.state.clusters.find(c => ( (to.params.name == c.name) && (to.params.namespace == c.data.metadata.namespace) ) );
    
    if ( typeof cluster !== "undefined" ) {
      
      store.commit('setCurrentCluster', cluster);
    }  else {
      //alert("Not found");
      //window.location.href = '/admin/not-found.html';
    }

    $('.clu li.current').removeClass('current');
	  $('li.cluster-'+store.state.currentNamespace+'-'+store.state.currentCluster.name).addClass('current');
    
  }

  if (to.matched.some(record => record.meta.conditionalRoute)) { 
      // this route requires condition to be accessed
      // if not, redirect to home page.
      //var nav = document.getElementById("nav"); 

      //console.log(to);

	    //console.log(router.history.current);


      if (store.state.currentCluster == {} && ( from.path.includes("profiles") || from.path.includes("configurations") ) && (to.path != ('/admin/configuration/'+to.params.name)) ) { 
          next({ path: '/admin/'}) 
      } else { 
          next() 
      } 
  } else { 
      next() // make sure to always call next()! 
  } 
    
});

const store = new Vuex.Store({
  state: {
    theme: 'light',
    loginToken: '',
    showLogs: false,
    notFound: false,
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
    cloneCRD: {},
    permissions: {
      allowed: {
        namespaced: [],
        unnamespaced: []
      },
      forbidden: []
    },
    tooltips: {
      description: 'Click on a question mark to get help and tips about that field.', 
      SGCluster: {},
      SGBackup: {},
      SGPostgresConfig: {},
      SGBackupConfig: {},
      SGPoolingConfig: {},
      SGInstanceProfile: {},
      SGDistributedLogs: {}
    },
    deleteItem: {
      kind: '',
      namespace: '',
      name: '',
      redirect: ''
    },
    confirmDeleteName: ''
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
      axios.defaults.headers.common['Authorization'] = 'Bearer '+store.state.loginToken;
    },

    setTheme (state, theme) {
      state.theme = theme;
    },
    
    setCurrentNamespace (state, namespace) {
      state.currentNamespace = namespace;
    },

    updateNamespaces (state, namespace) {
      //state.namespaces.length = 0;
      state.namespaces.push(namespace);

      /* if( (store.state.currentNamespace == ' ') && (router.history.current.params.namespace == namespace) ) {   
        $('#selected--zg-ul-select').addClass('active');
      } */
      
    },

    addNamespaces (state, namespacesList) {
      state.allNamespaces = [...namespacesList];
    },

    addLogsClusters (state, logsClusters) {
      state.logsClusters = [...logsClusters];
    },

    addStorageClasses (state, storageClassesList) {
      state.storageClasses = [...storageClassesList];
    },

    setCurrentCluster (state, cluster) {

      state.currentCluster = cluster;
      
    },

    setCurrentPods (state, pods) {
      state.currentPods = pods;
      //Object.assign(state.currentPods, pods);
    },

    updateClusters ( state, cluster ) {

      let index = state.clusters.find(c => (cluster.data.metadata.name == c.name) && (cluster.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = cluster.data;
        //console.log(cluster.name+" ya existe");
      } else {
        state.clusters.push( cluster );    
        //console.log('Se agregó '+cluster.name);
      }

    },

    /*removeCluster ( state, name, namespace ) {

      let index = state.clusters.find(c => (cluster.data.metadata.name == c.name) && (cluster.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = cluster.data;
      } else {
        state.clusters.splice( cluster );    
      }

    },*/


    updateBackups ( state, backup ) {

        let index = state.backups.find(p => (backup.data.metadata.name == p.name) && (backup.data.metadata.namespace == p.data.metadata.namespace) ); 

        if ( typeof index !== "undefined" ) {
          index.data = backup.data;
          //console.log(backup.name+" ya existe");
        } else {
          state.backups.push( backup );    
          // console.log('Se agregó '+backup.name);
        }

    },
    
    showBackup ( state, show ) {

      state.backups[show.pos].show = show.isVisible;

    },

    updatePGConfig ( state, config ) {

      let index = state.pgConfig.find(c => (config.data.metadata.name == c.name) && (config.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = config.data;
        // console.log(config.name+" ya existe");
      } else {
        state.pgConfig.push( config );    
        // console.log('Se agregó '+config.name);
      }

    },
    updatePoolConfig ( state, config ) {

      let index = state.poolConfig.find(c => (config.data.metadata.name == c.name) && (config.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = config.data;
        // console.log(config.name+" ya existe");
      } else {
        state.poolConfig.push( config );    
        // console.log('Se agregó '+config.name);
      }

    },
    updateBackupConfig ( state, config ) {

      let index = state.backupConfig.find(c => (config.data.metadata.name == c.name) && (config.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = config.data;
        // console.log(config.name+" ya existe");
      } else {
        state.backupConfig.push( config );    
        // console.log('Se agregó '+config.name);
      }

    },
    updateProfiles ( state, profile ) {

      let index = state.profiles.find(p => (profile.data.metadata.name == p.name) && (profile.data.metadata.namespace == p.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = profile.data;
        // console.log(profile.name+" ya existe");
      } else {
        state.profiles.push( profile );    
        // console.log('Se agregó '+profile.name);
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
      state.deleteItem = item;
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
      state.tooltips[tooltips.kind] = tooltips.description;
    },

    setTooltipDescription (state, description) {
      state.tooltips.description = description;
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
    
  }
});

// Set "default" as default landing Namespace
if( (window.location.pathname === '/admin/index.html') && ( (store.state.loginToken.length) || (getCookie('sgToken')) )  ) {
  store.commit('setCurrentNamespace', 'default');
  router.push('/admin/overview/default');
}  

Vue.mixin({
  data: function(){
    return {
    }
  },
  computed: {
   
  },
  methods: {

    iCan( action, kind, namespace = '' ) {
      
      if(namespace.length) {
        var iCan = false;
        
        store.state.permissions.allowed.namespaced.forEach(function( ns ){
          if( (ns.namespace == namespace) && ns.resources.hasOwnProperty(kind) && (ns.resources[kind].includes(action)) ) {
            iCan = true;
            return false
          }
        });
        
        return iCan
      } else {
        return !store.state.permissions.forbidden.includes(kind)
      }

    },

    iCant ( ) {
      return "<p>You don't have enough permissions to access this resource</p>"
    },

    parseParams: function(params) {
      params = params.replace(/=/g, ':</strong> ');
      params = '<li><strong class="label">'+params+'</li>';
      params = params.replace(/(?:\r\n|\r|\n)/g, '</li><li><strong class="label">')
      
      return params;
    },

    sort: function(s) {
			
			//if s == current sort, reverse
			if(s === this.currentSort) {
			  this.currentSortDir = this.currentSortDir==='asc'?'desc':'asc';
			}
			this.currentSort = s;

		},

    cancelDelete: function(){
      $("#delete").removeClass("active");
      $("#delete .warning").hide();
      store.commit('setConfirmDeleteName', '');
    },
    
    deleteCRD: function( kind, namespace, name, redirect ) {

      //console.log("Open delete");
      $('#delete input').val('');
      $("#delete").addClass("active");
      $(".filter > .open").removeClass("open");

      store.commit("setDeleteItem", {
        kind: kind,
        namespace: namespace,
        name: name,
        redirect: redirect
      });

    },

		confirmDelete: function( confirmName ) {

      //console.log("Name: "+confirmName);

      const item = store.state.deleteItem;

			if(confirmName == item.name) { 
				$("#delete .warning").fadeOut();

				const res = axios
				.delete(
				  apiURL+item.kind, 
            {
              data: {
                "metadata": {
                  "name": item.name,
                  "namespace": item.namespace
                }
              }
            }
				)
				.then(function (response) {
					console.log("DELETED");
					notify('<span class="capitalize">'+item.kind+'</span> <strong>'+item.name+'</strong> deleted successfully', 'message', item.kind);
          
          $('.'+item.kind+'-'+item.namespace+'-'+item.name).addClass("hide");
          //$('.'+item.kind+'-'+item.namespace+'-'+item.name+'.hide').remove();
          vm.fetchAPI(item.kind);

					if( (typeof item.redirect !== 'undefined') && item.redirect.length)
            router.push(item.redirect);
          
          store.commit("setDeleteItem", {
            kind: '',
            namespace: '',
            name: '',
            redirect: ''
          });

					$("#delete").removeClass("active");
				})
				.catch(function (error) {
				  console.log(error);
          notify(error.response.data,'error',item.kind);
          checkAuthError(error)
				});
			} else {
				$("#delete .warning").fadeIn();
			}
			
    },

    loadTooltips: function( kind, lang = 'EN' ) {

      if( !store.state.tooltips[kind].hasOwnProperty('metadata.name') ) {
        //console.log("Reading "+kind+" tooltips");

        fetch('/admin/js/components/forms/help/crd-'+kind+'-EN.json')
        .then(response => response.json())
        .then(data => 
            store.commit('setTooltips', { 
              kind: kind, 
              description: data 
            })
          );
        
      }

    },

    showTooltip: function( kind, field ) {

      const label = $("[for='"+field+"']").text();
      const crd = store.state.tooltips[kind];

      $("#help .title").html(label);

      let param = crd;

      if(field == 'spec.postgresql.conf') {
        param =  crd.spec.properties['postgresql.conf']
      } else if (field == 'spec.pgBouncer.pgbouncer.ini') {
        param =  crd.spec.properties.pgBouncer.properties['pgbouncer.ini']
      } else {
        params = field.split('.');
        params.forEach(function(item, index){
          if( !index ) // First level
            param = param[item]
          else
            param = param.properties[item]
        })
      }

      store.commit("setTooltipDescription", param['description']);

    },

    cloneCRD: function(kind, namespace, name) {

      var crd = {};

      switch(kind) {
        case 'SGCluster':
          crd = JSON.parse(JSON.stringify(store.state.clusters.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
          break;
        
        case 'SGBackupConfig':
          crd = JSON.parse(JSON.stringify(store.state.backupConfig.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
          break;

        case 'SGBackup':
          crd = JSON.parse(JSON.stringify(store.state.backups.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
          break;
        
        case 'SGInstanceProfile':
          crd = JSON.parse(JSON.stringify(store.state.profiles.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
          break;
        
        case 'SGPoolingConfig':
          crd = JSON.parse(JSON.stringify(store.state.poolConfig.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
          break;
        
        case 'SGPostgresConfig':
          crd = JSON.parse(JSON.stringify(store.state.pgConfig.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
          break;
        
        case 'SGDistributedLogs':
          crd = JSON.parse(JSON.stringify(store.state.logsClusters.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
          break;
      }
      
      if(typeof crd !== 'undefined') {
        crd.kind = kind;
        if($('#cloneName').val() !== crd.data.metadata.name)
          crd.data.metadata.name = 'copy-of-'+crd.data.metadata.name;

        store.commit('setCloneCRD', crd);
        
        $('#cloneName').val(crd.data.metadata.name);
        $('#cloneNamespace').val(crd.data.metadata.namespace);
        $("#notifications.hasTooltip.active").removeClass("active");
        $("#notifications.hasTooltip .message.show").removeClass("show");
        $('#clone').fadeIn().addClass('show');
      }
    }
  }
});

Vue.use(VueMarkdown);

// Check URL Params

if( urlParams.has('localAPI') ) {
  console.log('Using Local API');
  apiURL = 'js/data/';
}

if( urlParams.has('showLogs') ) {
  console.log('Showing Centralized Log Mock');
  store.commit('showLogs', true);
}

if( urlParams.has('darkmode') ) {
  console.log('Switching to darkmode');
  store.commit('setTheme', 'dark');
  $('body').addClass('darkmode');
  $('#darkmode').addClass('active');
}

const vm = new Vue({
  el: '#app',
  router,
  //mixins: [mixin],
  data: {
    active: true,
    ip: '',
    currentCluster: '',
    currentPods: '',
    clustersData: {},
    pooling: '',
    init: false
    //clusters: []
  },
  methods: {
    
    /* API Request */
    fetchAPI: function(kind = '', params = '') {

      let loginToken = getCookie('sgToken');
      //console.log("TOKEN: "+loginToken)

      if(store.state.loginToken.search('Authentication Error') !== -1) {
        clearInterval(this.pooling);
      } else if (!loginToken.length) {
        if(!store.state.loginToken.length) {
          $('#signup').addClass('login').fadeIn();
          return false;
        }
      } else if ( !store.state.loginToken.length && (loginToken.length > 0) ) {
        $('#signup').hide();
        store.commit('setLoginToken', loginToken);
      } else if ( urlParams.has('localAPI') ) {
        $('#signup').hide();
        store.commit('setLoginToken', 'localAPI');
      }

      //console.log("Fetching API");
      //$("#loader").show();
      $('#reload').addClass('active');

      // Read and set user permissions
      axios
      .get(apiURL+'auth/rbac/can-i')
      .then( function(response) {
        //console.log(response.data)
        store.commit('setPermissions', response.data);
      }).catch(function(err) {
        console.log(err);
        checkAuthError(err);
      });
      
      if ( !store.state.permissions.forbidden.includes('namespaces') && ( !kind.length || (kind == 'namespaces') ) ) {
        /* Namespaces Data */
        axios
        .get(apiURL+'namespace')
        .then( function(response){

          // Check if there are any changes on API Data
          if ( checkData(response.data, store.state.allNamespaces) ) {

            if(typeof store.state.allNamespaces !== 'undefined' && response.data.length != store.state.allNamespaces.length)
              store.commit('flushAllNamespaces');

            store.commit('addNamespaces', response.data);

          }
          
        }).catch(function(err) {
          console.log(err);
          checkAuthError(err);
        });
      }

      if ( !store.state.permissions.forbidden.includes('sgclusters') && ( !kind.length || (kind == 'cluster') ) ){
        /* Clusters Data */
        axios
        .get(apiURL+'sgcluster',
          { headers: {
              'content-type': 'application/json'
            }
          }
        )
        .then( function(response){

          // Check if there are any changes on API Data
          if ( checkData(response.data, apiData['cluster']) ) {

            if(typeof apiData['cluster'] !== 'undefined' && response.data.length != apiData['cluster'].length)
              store.commit('flushClusters');

            apiData['cluster'] = response.data;
      
            apiData['cluster'].forEach( function(item, index) {

              var cluster = {
                name: item.metadata.name,
                data: item,
                hasBackups: false,
                status: {},
              };
              
              
              if(store.state.namespaces.indexOf(item.metadata.namespace) === -1)
                store.commit('updateNamespaces', item.metadata.namespace);

              axios
                .get(apiURL+'sgcluster/stats/'+item.metadata.namespace+'/'+item.metadata.name,
                    { headers: {
                        'content-type': 'application/json'
                    }
                    }
                )
                .then( function(response){
                  //console.log(response.data);
                  cluster.status = response.data;
                }).catch(function(err) {
                  console.log(err);
                  checkAuthError(err);
                });
                
              store.commit('updateClusters', cluster);

              // Set as current cluster if no other cluster has already been set
              if(!store.state.currentCluster)              
                store.commit('setCurrentCluster', cluster);

            });

          }
          
        }).catch(function(err) {
          console.log(err);
          checkAuthError(err);
        });

      }

      if (!store.state.permissions.forbidden.includes('sgbackups') && ( !kind.length || (kind == 'backup') )) {
        
        /* Backups */
        axios
        .get(apiURL+'sgbackup',
          { headers: {
            'content-type': 'application/json'
          }
        })
        .then( function(response) {

          if( checkData(response.data, apiData['backup']) ) {
            var start, finish, duration;

            if(typeof apiData['backup'] !== 'undefined' && response.data.length != apiData['backup'].length)
              store.commit('flushBackups');

            apiData['backup'] = response.data;

            apiData['backup'].forEach( function(item, index) {
              
              if(store.state.namespaces.indexOf(item.metadata.namespace) === -1) {
              
                store.commit('updateNamespaces', item.metadata.namespace);

              } else {
                //console.log("Namespace ya existe");
              }

              //console.log(item);
              if( (item.status !== null) && item.status.hasOwnProperty('process')) {
                if( item.status.process.status === 'Completed' ) {
                  //console.log('setting duration');
                  start = moment(item.status.process.timing.start);
                  finish = moment(item.status.process.timing.stored);
                  duration = new Date(moment.duration(finish.diff(start))).toISOString();
                } else {
                  //console.log('duration not set');
                  duration = '';
                }
                
              }
                
              store.commit('updateBackups', { 
                name: item.metadata.name,
                data: item,
                duration: duration,
                show: true
              });

            });

            store.state.clusters.forEach(function(cluster, index){
              let backups = store.state.backups.find(b => ( (cluster.name == b.data.spec.sgCluster) && (cluster.data.metadata.namespace == b.data.metadata.namespace) ) );
      
              if ( typeof backups !== "undefined" )
                cluster.hasBackups = true; // Enable/Disable Backups button

            });

            

            //console.log("Backups Data updated");

          }
        }).catch(function(err) {
          console.log(err);
          checkAuthError(err);
        });
      }

      if ( !store.state.permissions.forbidden.includes('sgpgconfigs') && (!kind.length || (kind == 'sgpgconfig') ) ){

        /* PostgreSQL Config */
        axios
        .get(apiURL+'sgpgconfig',
          { headers: {
            'content-type': 'application/json'
          }
        })
        .then( function(response) {

          if( checkData(response.data, apiData['sgpgconfig']) ) {

            if(typeof apiData['sgpgconfig'] !== 'undefined' && response.data.length != apiData['sgpgconfig'].length)
              store.commit('flushPGConfig');

            apiData['sgpgconfig'] = response.data;

            apiData['sgpgconfig'].forEach( function(item, index) {
              
              if(store.state.namespaces.indexOf(item.metadata.namespace) === -1) {
              
                store.commit('updateNamespaces', item.metadata.namespace);

              } else {
                //console.log("Namespace ya existe");
              }     
                
              store.commit('updatePGConfig', { 
                name: item.metadata.name,
                data: item
              });

            });

            // console.log("PGconf Data updated");

          }
        }).catch(function(err) {
          console.log(err);
          checkAuthError(err);
        });
      }

      if (!store.state.permissions.forbidden.includes('sgpoolonfigs') && ( !kind.length || (kind == 'sgpoolconfig') ) ){

        /* Connection Pooling Config */
        axios
        .get(apiURL+'sgpoolconfig',
          { headers: {
            'content-type': 'application/json'
          }
        })
        .then( function(response) {

          if( checkData(response.data, apiData['sgpoolconfig']) ) {

            if(typeof apiData['sgpoolconfig'] !== 'undefined' && response.data.length != apiData['sgpoolconfig'].length)
              store.commit('flushPoolConfig');

            apiData['sgpoolconfig'] = response.data;

            apiData['sgpoolconfig'].forEach( function(item, index) {
              
              if(store.state.namespaces.indexOf(item.metadata.namespace) === -1) {
              
                store.commit('updateNamespaces', item.metadata.namespace);

              } else {
                //console.log("Namespace ya existe");
              }     
                
              store.commit('updatePoolConfig', { 
                name: item.metadata.name,
                data: item
              });

            });

            // console.log("PoolConf Data updated");

          }
        }).catch(function(err) {
          console.log(err);
          checkAuthError(err);
        });
      }

      if ( !store.state.permissions.forbidden.includes('sgbackupconfigs') && ( !kind.length || (kind == 'sgbackupconfig')) ) {

        /* Backup Config */
        axios
        .get(apiURL+'sgbackupconfig',
          { headers: {
            'content-type': 'application/json'
          }
        })
        .then( function(response) {

          if( checkData(response.data, apiData['backupconfig']) ) {

            if(typeof apiData['backupconfig'] !== 'undefined' && response.data.length != apiData['backupconfig'].length)
              store.commit('flushBackupConfig');

            apiData['backupconfig'] = response.data;

            apiData['backupconfig'].forEach( function(item, index) {
              
              if(store.state.namespaces.indexOf(item.metadata.namespace) === -1) {
              
                store.commit('updateNamespaces', item.metadata.namespace);

              } else {
                //console.log("Namespace ya existe");
              }     
                
              store.commit('updateBackupConfig', { 
                name: item.metadata.name,
                data: item
              });

            });

            // console.log("BackupConfig Data updated");

          }
        }).catch(function(err) {
          console.log(err);
          checkAuthError(err);
        });
      }

      if ( !store.state.permissions.forbidden.includes('sginstanceprofiles') && (!kind.length || (kind == 'sginstanceprofile') ) ) {

        /* Profiles */
        axios
        .get(apiURL+'sginstanceprofile',
          { headers: {
            'content-type': 'application/json'
          }
        })
        .then( function(response) {

          if( checkData(response.data, apiData['profile']) ) {

            if(typeof apiData['profile'] !== 'undefined' && response.data.length != apiData['profile'].length)
              store.commit('flushProfiles');

            apiData['profile'] = response.data;

            apiData['profile'].forEach( function(item, index) {
              
              if(store.state.namespaces.indexOf(item.metadata.namespace) === -1) {
              
                store.commit('updateNamespaces', item.metadata.namespace);

              } else {
                //console.log("Namespace ya existe");
              }     
                
              store.commit('updateProfiles', { 
                name: item.metadata.name,
                data: item
              });

            });

            // console.log("Profiles Data updated");

          }
        }).catch(function(err) {
          console.log(err);
          checkAuthError(err);
        });
      }

      if (!store.state.permissions.forbidden.includes('storageclasss') && ( !kind.length || (kind == 'storageclass') )) {
        /* Storage Classes Data */
        axios
        .get(apiURL+'storageclass',
          { headers: {
              //'content-type': 'application/json'
            }
          }
        )
        .then( function(response){

          // Check if there are any changes on API Data
          if ( checkData(response.data, store.state.storageClasses) ) {

            if(typeof store.state.storageClasses !== 'undefined' && response.data.length != store.state.storageClasses.length)
              store.commit('flushStorageClasses');

            store.commit('addStorageClasses', response.data);

          }
          
        }).catch(function(err) {
          console.log(err);
          checkAuthError(err);
        });
      }

      if (!store.state.permissions.forbidden.includes('sgdistributedlogs') && ( !kind.length || (kind == 'sgdistributedlogs') ) ){
        /* Distribude Logs Data */
        axios
        .get(apiURL+'sgdistributedlogs',
          { headers: {
              //'content-type': 'application/json'
            }
          }
        )
        .then( function(response){

          // Check if there are any changes on API Data
          if ( checkData(response.data, store.state.logsClusters) ) {

            if(typeof store.state.logsClusters !== 'undefined' && response.data.length != store.state.logsClusters.length)
              store.commit('flushLogsClusters');

            var logs = [];

            response.data.forEach(function(item, index){
              logs.push({
                name: item.metadata.name,
                data: item
              })
            })

            store.commit('addLogsClusters', logs);

          }
          
        }).catch(function(err) {
          console.log(err);
          checkAuthError(err);
        });
      }


      if (!store.state.permissions.forbidden.includes('sgbackups') && ( (kind === 'backup') || (kind === 'cluster') ) ) {
        // Check if current cluster has backups
        let currentClusterBackups = store.state.backups.find(b => ( (store.state.currentCluster.name == b.data.spec.cluster) && (store.state.currentCluster.data.metadata.namespace == b.data.metadata.namespace) ) );
          
        if ( typeof currentClusterBackups !== "undefined" )
          store.state.currentCluster.hasBackups = true; // Enable/Disable Backups button
      }

      setTimeout(function(){
        //$("#loader").fadeOut(500);
        $("#reload").removeClass("active");
        this.init = true;
        //$("#loader").hide();  
      }, 2000);

      if(!store.state.ready)
        store.commit('setReady',true)

    },

  },
  mounted: function() {
    
    this.fetchAPI();

    if(store.state.loginToken.length > 0) {
      this.pooling = setInterval( function(){
        this.fetchAPI();
      }.bind(this), 10000);
    }

  }
});


Vue.filter('prettyCRON', function (value) {
  
  //console.log(value);
  return prettyCron.toString(value)
  
});

Vue.filter('formatBytes',function(a){

  // Bytes Formatter via https://stackoverflow.com/questions/15900485/correct-way-to-convert-size-in-bytes-to-kb-mb-gb-in-javascript
  if(0==a)return"0 Bytes";var c=1024,d=2,e=["Bytes","Ki","Mi","Gi","Ti","Pi","Ei","Zi","Yi"],f=Math.floor(Math.log(a)/Math.log(c));return parseFloat((a/Math.pow(c,f)).toFixed(d))+" "+e[f];

});

Vue.filter('prefix',function(s, l = 2){
  return s.substring(0, l);
});


Vue.filter('formatTimestamp',function(t, part){

    if(part == 'date')
      return t.substr(0, 10);
    else if (part == 'time')
      return t.substring( 11, 19);
    else if (part == 'ms') {
      var ms = '.'+t.substring( 20, t.length - 1);
      
      if (ms == '.Z')
        ms = '.000';
      else if (ms.length == 2)
        ms += '00';
      else if (ms.length == 4)
        ms += '0';
      
      return ms.substring(0,4);
    }
      
});

function notFound() {
  store.commit('notFound',true)
  //console.log('notfound')
  router.push('/admin/not-found.html')
}

function checkLogin() {
  let loginToken = getCookie('sgToken');
  //console.log("TOKEN: "+loginToken)

  if (!loginToken.length) {
    if(!store.state.loginToken.length) {
      $('#signup').addClass('login').fadeIn();
      return false;
    }
  } else if ( !store.state.loginToken.length && (loginToken.length > 0) ) {
    $('#signup').hide();
    store.commit('setLoginToken', loginToken);
  } else if ( urlParams.has('localAPI') ) {
    $('#signup').hide();
    store.commit('setLoginToken', 'localAPI');
  }
}


function checkAuthError(error) {
  if(error.response) {
    if(error.response.status == 401 ) {
      document.cookie = 'sgToken=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;';
      store.commit('setLoginToken');
      router.push('/admin/index.html');
      clearInterval(vm.pooling);
      //store.replaceState({})
      $('#signup').addClass('login').fadeIn();
        
    } else if(error.response.status == 403 ) {
      //console.log(error.response)
      
      // Little hack to store the right plural kind to validate RBAC permissions
      if(error.response.config.url.includes('sgcluster/stats'))
        kind = 'stats'
      else
        kind = error.response.config.url.replace('/stackgres/','')+'s';
      
      store.commit('setNoPermissions', kind);
    }
  }
  
}

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

function formatBytes (a) {
  if(0==a)return"0 Bytes";var c=1024,d=2,e=["Bytes","Ki","Mi","Gi","Ti","Pi","Ei","Zi","Yi"],f=Math.floor(Math.log(a)/Math.log(c))+1;return parseFloat((a/Math.pow(c,f)).toFixed(d))+" "+e[f];
}

function getBytes (text) {
  var powers = {'Ki': 1, 'Mi': 2, 'Gi': 3, 'Ti': 4, 'Pi': 5};
  var regex = /(\d+(?:\.\d+)?)\s?(Ki|Mi|Gi|Ti|Pi)?b?/i;

  var res = regex.exec(text);

  return res[1] * Math.pow(1024, powers[res[2]]);
}

function arraysMatch (arr1, arr2) {

  // Check if the arrays are the same length
  if (arr1.length !== arr2.length) return false;

  // Check if all items exist and are in the same order
  for (var i = 0; i < arr1.length; i++) {
    if (arr1[i] !== arr2[i]) return false;
  }

  // Otherwise, return true
  return true;

}

function checkData (newData, currentData) {
  return (JSON.stringify(newData) != JSON.stringify(currentData))
}

/*function checkAPI( response, current ){
  if ( !response.length || !current.length ) 
        return false;

    // compare lengths - can save a lot of time 
    if (reponse.length != current.length)
        return false;

    for (var i = 0, l=response.length; i < l; i++) {
        // Check if we have nested arrays
        if (response[i] instanceof Array && current[i] instanceof Array) {
            // recurse into the nested arrays
            if (! checkAPI(response[i], current[i] ) )
                return false;       
        }           
        else if (response[i] != current[i]) { 
            // Warning - two different object instances will never be equal: {x:20} != {x:20}
            return false;   
        }           
    }       
    return true;
}*/

function notify (message, kind = 'message', crd = 'general') {
  //$("#notifications").addClass("active");

  $("#delete").removeClass("active");

  let details = '';
  let icon = '';

  $(".form .alert").removeClass("alert");

  switch (crd) {
    case 'general':
      icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M13.193 10A3.193 3.193 0 1010 13.2a3.2 3.2 0 003.193-3.2zm-1.809 0A1.384 1.384 0 1110 8.614 1.386 1.386 0 0111.384 10z"/><path class="a" d="M16.961 12.835a.443.443 0 01.44-.246 2.6 2.6 0 000-5.2h-.136a.4.4 0 01-.318-.157.988.988 0 00-.055-.164.427.427 0 01.122-.486A2.6 2.6 0 1013.3 2.937a.414.414 0 01-.287.116.4.4 0 01-.292-.12.455.455 0 01-.123-.357 2.591 2.591 0 00-.762-1.84 2.659 2.659 0 00-3.675 0 2.6 2.6 0 00-.76 1.84v.137a.406.406 0 01-.158.318 1.078 1.078 0 00-.163.055.41.41 0 01-.465-.1l-.076-.077a2.5 2.5 0 00-1.853-.729 2.576 2.576 0 00-1.822.8 2.632 2.632 0 00.1 3.71.434.434 0 01.058.5.423.423 0 01-.422.265 2.6 2.6 0 000 5.2h.133a.41.41 0 01.285.117.43.43 0 01-.035.629l-.079.079v.005A2.61 2.61 0 003 17.135a2.479 2.479 0 001.853.728 2.614 2.614 0 001.847-.827.429.429 0 01.5-.057.419.419 0 01.264.42 2.6 2.6 0 105.2 0v-.132a.414.414 0 01.116-.284.421.421 0 01.3-.126.356.356 0 01.278.113l.1.1a2.731 2.731 0 001.852.728 2.6 2.6 0 002.55-2.65 2.611 2.611 0 00-.825-1.857.4.4 0 01-.081-.444zm-6.2 4.422v.143a.691.691 0 01-.69.691.718.718 0 01-.692-.788 2.289 2.289 0 00-1.457-2.095 2.274 2.274 0 00-.919-.2 2.427 2.427 0 00-1.7.728.7.7 0 01-.5.213.652.652 0 01-.482-.194.676.676 0 01-.208-.477.749.749 0 01.217-.53l.064-.064a2.323 2.323 0 00-1.654-3.938H2.6a.692.692 0 01-.489-1.18.755.755 0 01.587-.2A2.286 2.286 0 004.788 7.9a2.306 2.306 0 00-.467-2.556l-.069-.069a.693.693 0 01.478-1.191.655.655 0 01.5.213l.069.071a2.257 2.257 0 002.334.536.92.92 0 00.27-.071 2.312 2.312 0 001.4-2.121v-.134a.687.687 0 01.2-.489.705.705 0 01.977 0 .751.751 0 01.2.571 2.3 2.3 0 00.705 1.64 2.331 2.331 0 001.649.665 2.369 2.369 0 001.652-.713.691.691 0 011.181.488.753.753 0 01-.259.547 2.253 2.253 0 00-.538 2.334.932.932 0 00.072.274 2.313 2.313 0 002.119 1.4h.139a.691.691 0 01.69.692.717.717 0 01-.768.691 2.312 2.312 0 00-2.113 1.395 2.345 2.345 0 00.533 2.619.693.693 0 01-.45 1.192.749.749 0 01-.506-.19l-.1-.1a2.4 2.4 0 00-1.653-.654 2.325 2.325 0 00-2.283 2.312zM5.5 4.177z"/></svg>';
      break;
    case 'sgcluster':
      icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"></path></svg>';
      break;
    case 'sgpgconfig':
      icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.7 20"><path d="M10.946 18.7a.841.841 0 01-.622-.234.862.862 0 01-.234-.635v-7.817a.8.8 0 01.221-.6.834.834 0 01.608-.214h3.29a3.4 3.4 0 012.353.755 2.7 2.7 0 01.843 2.12 2.72 2.72 0 01-.843 2.126 3.379 3.379 0 01-2.353.764h-2.394v2.875a.8.8 0 01-.869.867zM14 13.637q1.778 0 1.778-1.551T14 10.535h-2.18v3.1zm11.968-.107a.683.683 0 01.494.181.625.625 0 01.191.477v2.875a1.717 1.717 0 01-.16.87 1.174 1.174 0 01-.655.414 6.882 6.882 0 01-1.242.294 9.023 9.023 0 01-1.364.107 5.252 5.252 0 01-2.527-.573 3.883 3.883 0 01-1.638-1.665 5.548 5.548 0 01-.569-2.6 5.5 5.5 0 01.569-2.575 3.964 3.964 0 011.611-1.671 4.965 4.965 0 012.455-.59 4.62 4.62 0 013.089 1.016 1.058 1.058 0 01.234.294.854.854 0 01-.087.843.479.479 0 01-.388.2.737.737 0 01-.267-.047 1.5 1.5 0 01-.281-.153 4.232 4.232 0 00-1.1-.582 3.648 3.648 0 00-1.146-.167 2.747 2.747 0 00-2.2.859 3.834 3.834 0 00-.742 2.561q0 3.477 3.049 3.477a6.752 6.752 0 001.815-.254v-2.36h-1.517a.737.737 0 01-.5-.161.664.664 0 010-.909.732.732 0 01.5-.161zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z" class="a"></path></svg>';
      break;
    case 'sgpoolconfig' || 'sgpoolingconfig':
      icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.5 20"><path d="M14.305 18.749a4.7 4.7 0 01-2.388-.589 3.91 3.91 0 01-1.571-1.685 5.668 5.668 0 01-.546-2.568 5.639 5.639 0 01.548-2.561 3.916 3.916 0 011.571-1.678 4.715 4.715 0 012.388-.593 5.189 5.189 0 011.658.261 4.324 4.324 0 011.378.756.758.758 0 01.24.281.859.859 0 01.067.361.768.768 0 01-.16.495.479.479 0 01-.388.2.984.984 0 01-.548-.191 4 4 0 00-1.07-.595 3.405 3.405 0 00-1.1-.167 2.571 2.571 0 00-2.106.869 3.943 3.943 0 00-.72 2.562 3.963 3.963 0 00.716 2.568 2.568 2.568 0 002.106.869 3.147 3.147 0 001.063-.173 5.112 5.112 0 001.1-.589 2.018 2.018 0 01.267-.134.751.751 0 01.29-.048.477.477 0 01.388.2.767.767 0 01.16.494.863.863 0 01-.067.355.739.739 0 01-.24.286 4.308 4.308 0 01-1.378.757 5.161 5.161 0 01-1.658.257zm5.71-.04a.841.841 0 01-.622-.234.856.856 0 01-.234-.636v-7.824a.8.8 0 01.22-.6.835.835 0 01.609-.214h3.29a3.4 3.4 0 012.354.755 2.7 2.7 0 01.842 2.12 2.725 2.725 0 01-.842 2.127 3.386 3.386 0 01-2.354.764h-2.393v2.875a.8.8 0 01-.87.868zm3.05-5.069q1.779 0 1.779-1.552t-1.779-1.551h-2.18v3.1zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"></path></svg>';
      break;
    case 'sgbackupconfig':
      icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 27.1 20"><path d="M15.889 13.75a2.277 2.277 0 011.263.829 2.394 2.394 0 01.448 1.47 2.27 2.27 0 01-.86 1.885 3.721 3.721 0 01-2.375.685h-3.449a.837.837 0 01-.6-.213.8.8 0 01-.22-.6v-7.795a.8.8 0 01.22-.6.835.835 0 01.609-.214h3.306a3.679 3.679 0 012.306.648 2.165 2.165 0 01.836 1.815 2.159 2.159 0 01-.395 1.3 2.254 2.254 0 01-1.089.79zm-4.118-.585h2.179q1.779 0 1.778-1.323a1.143 1.143 0 00-.441-.989 2.267 2.267 0 00-1.337-.321h-2.179zm2.407 4.118a2.219 2.219 0 001.363-.335 1.242 1.242 0 00.428-1.042 1.271 1.271 0 00-.435-1.056 2.155 2.155 0 00-1.356-.348h-2.407v2.781zm8.929 1.457a3.991 3.991 0 01-2.941-1 3.968 3.968 0 01-1-2.927V9.984a.854.854 0 01.227-.622.925.925 0 011.23 0 .854.854 0 01.227.622V14.9a2.623 2.623 0 00.573 1.838 2.18 2.18 0 001.684.622 2.153 2.153 0 001.671-.628 2.624 2.624 0 00.573-1.832V9.984a.85.85 0 01.228-.622.924.924 0 011.229 0 .85.85 0 01.228.622v4.826a3.969 3.969 0 01-1 2.92 3.95 3.95 0 01-2.929 1.01zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"></path></svg>';
      break;
    case 'sginstanceprofile':
      icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 21.3 20"><path d="M10.962 9.14a.808.808 0 00-.862.86v7.878a.86.86 0 00.235.63.83.83 0 00.624.242.82.82 0 00.872-.872V10a.842.842 0 00-.235-.624.862.862 0 00-.634-.236zm9.407.825a3.419 3.419 0 00-2.362-.758h-3.3a.842.842 0 00-.611.215.8.8 0 00-.221.6v7.851a.859.859 0 00.233.637.842.842 0 00.624.235.806.806 0 00.868-.87v-2.882h2.406a3.393 3.393 0 002.362-.767 2.729 2.729 0 00.846-2.133 2.709 2.709 0 00-.845-2.128zm-2.576 3.7H15.6v-3.116h2.192q1.785 0 1.785 1.557t-1.784 1.557zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"></path></svg>';
      break;
    case 'sgbackup':
      icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10.55.55A9.454 9.454 0 001.125 9.5H.479a.458.458 0 00-.214.053.51.51 0 00-.214.671l1.621 3.382a.49.49 0 00.213.223.471.471 0 00.644-.223l1.62-3.382A.51.51 0 004.2 10a.49.49 0 00-.479-.5H3.1a7.47 7.47 0 117.449 7.974 7.392 7.392 0 01-3.332-.781.988.988 0 00-.883 1.767 9.356 9.356 0 004.215.99 9.45 9.45 0 000-18.9z" class="a"></path><path d="M13.554 10a3 3 0 10-3 3 3 3 0 003-3z" class="a"></path></svg>';
      break;
    case 'sgdistributedlogs':
      icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M19,15H5c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,14.6,19.6,15,19,15z"/><path class="a" d="M1,15L1,15c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,14.6,1.6,15,1,15z"/><path class="a" d="M19,11H5c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,10.6,19.6,11,19,11z"/><path class="a" d="M1,11L1,11c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,10.6,1.6,11,1,11z"/><path class="a" d="M19,7H5C4.4,7,4,6.6,4,6v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,6.6,19.6,7,19,7z"/><path d="M1,7L1,7C0.4,7,0,6.6,0,6v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,6.6,1.6,7,1,7z"/></svg>';
      break;
  }

  if(kind === 'error') {
    details = `
      <div class="message show">
        <span class="remove">
          <svg xmlns="http://www.w3.org/2000/svg" width="13.5" height="15" viewBox="0 0 13.5 15"><g transform="translate(-61 -90)"><path d="M73.765,92.7H71.513a.371.371,0,0,1-.355-.362v-.247A2.086,2.086,0,0,0,69.086,90H66.413a2.086,2.086,0,0,0-2.072,2.094V92.4a.367.367,0,0,1-.343.3H61.735a.743.743,0,0,0,0,1.486h.229a.375.375,0,0,1,.374.367v8.35A2.085,2.085,0,0,0,64.408,105h6.684a2.086,2.086,0,0,0,2.072-2.095V94.529a.372.372,0,0,1,.368-.34h.233a.743.743,0,0,0,0-1.486Zm-7.954-.608a.609.609,0,0,1,.608-.607h2.667a.6.6,0,0,1,.6.6v.243a.373.373,0,0,1-.357.371H66.168a.373.373,0,0,1-.357-.371Zm5.882,10.811a.61.61,0,0,1-.608.608h-6.67a.608.608,0,0,1-.608-.608V94.564a.375.375,0,0,1,.375-.375h7.136a.375.375,0,0,1,.375.375Z" transform="translate(0)"/><path d="M68.016,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,68.016,98.108Z" transform="translate(-1.693 -3.214)"/><path d="M71.984,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,71.984,98.108Z" transform="translate(-2.807 -3.214)"/></g></svg>
        </span>
        <span class="icon `+kind+`">
          `+icon+`
        </span>
        <span class="kind `+kind+`">
          `+kind+`
        </span>
        <h4 class="title">`+message.title+`</h4>
        <p class="detail">`+message.detail+`</p>`;

      if(message.title.search('Authentication Error') == -1)
        details += `<a href="`+message.type+`" title="More Info" target="_blank" class="doclink">More Info <svg xmlns="http://www.w3.org/2000/svg" width="15.001" height="12.751" viewBox="0 0 15.001 12.751"><g transform="translate(167.001 -31.5) rotate(90)"><path d="M37.875,168.688a.752.752,0,0,1-.53-.219l-5.625-5.626a.75.75,0,0,1,0-1.061l2.813-2.813a.75.75,0,0,1,1.06,1.061l-2.283,2.282,4.566,4.566,4.566-4.566-2.283-2.282a.75.75,0,0,1,1.06-1.061l2.813,2.813a.75.75,0,0,1,0,1.061l-5.625,5.626A.752.752,0,0,1,37.875,168.688Z" transform="translate(0 -1.687)" fill="#00adb5"/><path d="M42.156,155.033l-2.813-2.813a.752.752,0,0,0-1.061,0l-2.813,2.813a.75.75,0,1,0,1.06,1.061l1.533-1.534v5.3a.75.75,0,1,0,1.5,0v-5.3l1.533,1.534a.75.75,0,1,0,1.06-1.061Z" transform="translate(-0.937 0)" fill="#00adb5"/></g></svg></a>`;
      
      details += `</div>`;
    
    if(!!message.fields) {
      message.fields.forEach( function(item, index) {
        $(".form [data-field='"+item+"']").addClass("alert");
      });
      //$(".form [data-field='"+message.fields[0]+"']").addClass("alert");
    }
  } else {
    details = `
    <div class="message show">
        <span class="remove">
          <svg xmlns="http://www.w3.org/2000/svg" width="13.5" height="15" viewBox="0 0 13.5 15"><g transform="translate(-61 -90)"><path d="M73.765,92.7H71.513a.371.371,0,0,1-.355-.362v-.247A2.086,2.086,0,0,0,69.086,90H66.413a2.086,2.086,0,0,0-2.072,2.094V92.4a.367.367,0,0,1-.343.3H61.735a.743.743,0,0,0,0,1.486h.229a.375.375,0,0,1,.374.367v8.35A2.085,2.085,0,0,0,64.408,105h6.684a2.086,2.086,0,0,0,2.072-2.095V94.529a.372.372,0,0,1,.368-.34h.233a.743.743,0,0,0,0-1.486Zm-7.954-.608a.609.609,0,0,1,.608-.607h2.667a.6.6,0,0,1,.6.6v.243a.373.373,0,0,1-.357.371H66.168a.373.373,0,0,1-.357-.371Zm5.882,10.811a.61.61,0,0,1-.608.608h-6.67a.608.608,0,0,1-.608-.608V94.564a.375.375,0,0,1,.375-.375h7.136a.375.375,0,0,1,.375.375Z" transform="translate(0)"/><path d="M68.016,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,68.016,98.108Z" transform="translate(-1.693 -3.214)"/><path d="M71.984,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,71.984,98.108Z" transform="translate(-2.807 -3.214)"/></g></svg>
        </span>
        <span class="icon message">
          `+icon+`
        </span>
        <span class="kind message">
          Message
        </span>
        <h4 class="title">`+message+`</h4>
      </div>
    `;
  }

  $("#notifications .tooltip").append(details);
  $("#notifications .count").text(parseInt($("#notifications .count").text(),10)+1);

  if(parseInt($("#notifications .count").text(),10) > 0) {
    $("#notifications").addClass("active");
    $("#notifications .count").removeClass("zero");
    $("#notifications .tooltip .zero").hide();
  } else {
    $("#notifications .tooltip .zero").show();
  }
}

function trimString(string) {
  return string.trim();
}

function getJSON( value ) {
  //e.preventDefault();
  var text = value.split("\n").map(trimString);
  var json = {};

  text.forEach(function(e, i, a){
    var param = e.split("=").map(trimString);
    json[''+param[0].toString()+''] = param[1];
  });

  // alert(json);
  // console.log(json);
  
  return(json);
}

function showFields( fields ) {
  $(fields).slideDown();
}

function hideFields( fields ) {
  $(fields).slideUp();
}

function hasParams( obj, params) {
  var valid = true;

  for ( var i=0; i < params.length; i++){
    if( !obj || !obj.hasOwnProperty(params[i]) ) {
      return valid = false
    }
    else
      obj = obj[params[i]]
  }

  return valid
}

// Sort tables by an specific parameter
function sortTable( table, param, direction ) {

  var backupFixedParams = ['data.spec.subjectToRetentionPolicy','data.metadata.name','data.spec.sgCluster','data.status.process.status'];
  
  table.sort((a,b) => {
    let modifier = 1;
    
    if(direction === 'desc') modifier = -1;

    // If sorting backups first validate its state
    if(a.data.hasOwnProperty('status') && a.hasOwnProperty('duration')) {
      // If record is not sortable by the provided param
      if(a.data.status !== null) {
        if( (a.data.status.process.status == 'Failed') && !backupFixedParams.includes(param)){
          //console.log('failed');
          return 1
        } else if ((a.data.status.process.status == 'Running') && !backupFixedParams.includes(param)){
          return -1
        }
      }
      
    }  

    if(hasParams( a, param.split(".")))
      a = eval("a."+param);
    else
      a = '';

    if(hasParams( b, param.split(".")))
      b = eval("b."+param);
    else
      b = '';
    
    if( a < b )
      return -1 * modifier;
    
    if( a > b )
      return 1 * modifier;
    
    return 0;
  });

  return table;

}

function discoverText(e) {
  var search = $("#keyword").val();
  console.log(search);

  if (/[a-zA-Z]/.test(search) || e.keyCode == "8" || e.keyCode == "46") {
      
      $("table tr").each(function () {

          if ( ($(this).text().toLowerCase().indexOf(search.toLowerCase()) !== -1) )
              $(this).fadeIn();
          else
              $(this).fadeOut();

      });
  }
}


/* jQuery Init */

$(document).ready(function(){

  $("#namespaces li:first-of-type a").click();

  $(document).on("click", "a.namespace", function(){
    store.commit('setCurrentNamespace',$(this).text());
    $("#backup-btn, #graffana-btn").css("display","none");
  });

  /* $(document).on("click", ".clu a", function(){
    $(".clu .router-link-active:not(.router-link-exact-active)").removeClass("router-link-active");
    $("#grafana-button").css("display", "none");
    //store.commit('setCurrentClusterName', $(this).text());


    $("#nav").removeClass("disabled");
    //console.log(currentCluster);
    //console.log(router.history.current.params.name);
  }); */
/* 
  $(document).on("click", ".conf a, .prof a", function(){
    store.commit('setCurrentCluster', {});
    $("#nav").addClass("disabled");
  }); */

  $(document).on("click", ".box h4", function() {
    
    $(this).parents(".box").toggleClass("show");

    // Look for toggle button
    var btn = $(this).parents(".table").find(".details .btn");
    
    if(btn.length) {
      if(btn.text() == 'Details')
        btn.text(' Close ');
      else
        btn.text('Details');  
    }
  
  });

  $(document).on("click", ".details .btn", function(){

    $(this).parents(".box").toggleClass("show");

    if($(this).text() == 'Details')
      $(this).text(' Close ');
    else
      $(this).text('Details');
  
  });

  $(document).on("click", "#main, #side", function(e) {

    if($(this).prop("id") != "notifications") {
      $("#notifications.hasTooltip.active").removeClass("active");
      $("#notifications.hasTooltip .message.show").removeClass("show");
      $("#selected--zg-ul-select.open").removeClass("open");
      $("#be-select.active").removeClass("active");
    }

    if (!$(e.target).parents().addBack().is('.cloneCRD') && $('#clone').hasClass('show'))
      $('#clone.show').fadeOut().removeClass('show');

  });
  
  $("#clone").click(function(e){
    e.stopPropagation();
  });


  $(document).on("click", "#sets .nav-item", function(){
   /*  if(!$(this).parents().hasClass("clu"))
        $('.clu.active').removeClass('active');
    
    if(!($(this).parent().hasClass("active"))) {
      $(".set.active:not(.conf)").removeClass("active");
      $(this).parent("div:not(.conf)").addClass("active");
    } */
    $("#current-namespace").removeClass('open');
    $('#ns-select').slideUp();

    $(".set:not(.active) > ul.show").removeClass("show");
    
  });

  $(document).on("click", ".set .item", function(){
   /*  $(".set.active:not(.conf)").removeClass("active");
    $(this).parent().parent().parent().addClass("active");

    if(!$(this).parents().hasClass("clu")) {
      $(".set.active:not(.conf)").removeClass("active");
      $('.clu.active').removeClass('active');
    } */
    
    $(".set:not(.active) > ul.show").removeClass("show");
  });

  $(document).on("mouseover", ".set:not(.active)", function(){
    let offset = $(this).offset();
    let submenu = $(this).children("ul");
    
    if(window.innerHeight > 700)
      submenu.css("bottom","auto").css("top",offset.top - window.scrollY)
    else
      submenu.css("top", "auto").css("bottom",window.innerHeight - $(this).height() - offset.top)
      
    submenu.addClass("show");
  });

  $(document).on("mouseleave", ".set:not(.active) ul.show", function(){
    $(this).removeClass("show");
  });

  $(document).on("mouseleave", ".set:not(.active)", function(){
    $(this).children("ul.show").removeClass("show");
  });

  $(document).on("click", "#nav:not(.disabled) .top a.nav-item", function(){
    $(".clu a[href$='"+store.state.currentCluster+"']").addClass("router-link-active");
  });

  $(".expand").click(function(){
    $(".set").addClass("active");
    $("#sets").addClass("expanded");
  });

  $(".collapse").click(function(){
    $(".set").removeClass("active");
    $("#sets").removeClass("expanded");
  });

  $("#nav .view").click(function(){
    $("#nav .tooltip.show").prop("class","tooltip").hide();
    $("#nav .top a.nav-item").removeClass("router-link-active");
    //$("#nav").addClass("disabled");
    //$(".clu a").removeClass("router-link-active").removeClass("router-link-exact-active");
    //$(".set.active").removeClass("active");


    if(store.state.currentCluster.length) {
      //$(".clu a[href$='"+currentCluster+"']").addClass("router-link-active");
      /*$("#nav .top a").each(function(){
        $(this).attr("href", $(this).attr("href")+currentCluster);
      });*/
    }
  });

  $("#nav.disabled .top a.nav-item").click(function(){
      $("#nav .tooltip.show").prop("class","tooltip").hide();
      $(this).siblings(".tooltip").fadeIn().addClass("show");
      $("#nav .top .tooltip").addClass("pos"+($(this).index()+1));
  });

  $(".hasTooltip > a").click(function(){
    if($(this).parent().hasClass("active")) {
      $(this).parent().removeClass("active");
      $(this).parent().find("div.message").removeClass("show");
    }
    else{
      $(".hasTooltip.active").removeClass("show");
      $(this).parent().addClass("active");
      $(this).parent().find("div.message").addClass("show");
    }      
  });

  /* $("#sets h3").click(function(){
    $(this).parent().toggleClass("hide");
  }); */

/*   $(".clu .item").click(function(){
    $("#nav").removeClass("disabled");
  });
 */
  /* Disable Grafana KEY functions */
  $(".grafana iframe").contents().find("body").keyup( function(e) {
    switch (e.keyCode) {
      case 27: // 'Esc'
        event.returnValue = false;
        event.keyCode = 0;
        alert("ESC");
        break;
    }
  });

  /*$(".grafana iframe").load( function() {

    setTimeout(function(){
      $(".grafana iframe").contents().find("head")
      .append($("<style type='text/css' id='hideBars'>  .navbar, .sidemenu {display:none !important;}  </style>"));
    }, 3000);
    
  });*/


  $.fn.ulSelect = function(){
    var ul = $(this);

    if (!ul.hasClass('zg-ul-select'))
      ul.addClass('zg-ul-select');
    
    $('li:first-of-type', this).addClass('active');

    var selected = $('#selected--zg-ul-select');
      
    
    $(document).on('click', '#selected--zg-ul-select', function(){

      $(this).toggleClass('open');
      ul.toggleClass('active');

      var selectedText = $(this).text();
      if (ul.hasClass('active')) {
        selected.addClass('active');
      }
      else {
        //selected.text('').removeClass('active'); 
        $('li.active', ul);
      }
    });

    $(document).on('click', '#be-select li a', function(){
      selected.removeClass('open');
      ul.removeClass('active');
      $(".set.backups.active").removeClass('active');
    });

  }

  // Run
  $('#be-select').ulSelect();

  // Uncheck checkboxes by default
  //$('.switch input[type="checkbox"]').removeAttr('checked');

  $(document).on('click', '.message .remove', function(){
    $(this).parent().detach();
   
    $("#notifications .count").text(parseInt($("#notifications .count").text(),10)-1);
    
    if(parseInt($("#notifications .count").text(),10) == 0)
      $("#notifications .tooltip .zero").show();
  });

  $("form").submit(function(e){
    e.preventDefault(); 
  });

  $(document).on("click", ".sort th", function(){
    /* if( $(this).hasClass("sorted") ){
      if($(this).hasClass("asc"))
        $(this).removeClass("asc").addClass("desc");
      else
        $(this).removeClass("desc").addClass("asc");
    } else {
      $(".sorted").removeClass("sorted asc desc");
      $(this).addClass("sorted");
    } */

    $(".sorted").removeClass("sorted");
    $(this).addClass("sorted");
    $(".sort th").toggleClass("desc asc")   
  });

/*   $(document).on("click", "table.backups tr.base td:not(.actions), table.profiles tr.base td:not(.actions), table.pgConfig tr.base td:not(.actions), table.poolConfig tr.base td:not(.actions)", function(){
    $(this).parent().next().toggle().addClass("open");
    $(this).parent().toggleClass("open");
  });

 */
  /* $(document).on('focus','.filter.open .options', function(){
    if( $('.filter.open').find('.active').length )
      $('.filter.open').addClass('filtered');
    else
      $('.filter.open').removeClass('filtered');
    
      $('.filter.open').removeClass("open");
  }); */

  // Show configurations details when the row is clicked
  $(document).on('click', 'table:not(.backups):not(.logs) tr.base > td:not(.actions)', function(){    
    const table = $(this).parents('table');
    if(!$(this).parent().hasClass('open')) {
      if(table.hasClass('configurations'))
        router.push('/admin/configurations/'+table.prop('id')+'/'+store.state.currentNamespace+'/'+$(this).parent().data('name'))
      else
        router.push('/admin/'+table.prop('id')+'/'+store.state.currentNamespace+'/'+$(this).parent().data('name'))
    } else {
      if(table.hasClass('configurations'))
        router.push('/admin/configurations/'+table.prop('id')+'/'+store.state.currentNamespace)
      else
        router.push('/admin/'+$(this).parents('table').prop('id')+'/'+store.state.currentNamespace)
    }
  })

  $(document).mouseup(function(e) {
    var container = $(".filter.open");

    // if the target of the click isn't the container nor a descendant of the container
    if (!container.is(e.target) && container.has(e.target).length === 0) {
      if( $('.filter.open').find('.active').length )
        $('.filter.open').addClass('filtered');
      else
        $('.filter.open').removeClass('filtered');
      
        $('.filter.open').removeClass("open");
    }
  });
 
  $(document).on("click",".toggle:not(.date)",function(e){
    e.stopPropagation();
    $(this).parent().toggleClass("open");
    //$(this).next().toggleClass("open");
  })

  /* $(document).on("change","form input, form select, form textarea",function(){

    if($(this).val().length) {
      $(this).addClass("changed");
      $(this).parents("fieldset, div").first().addClass("changed");
    } else {
      $(this).removeClass("changed");
      $(this).parents("fieldset, div").first().removeClass("changed");
    }
    
  }); */

  $(document).on("change","#advancedMode",function(){
    $(this).prop("disabled","disabled")
  });

  $(document).on("click", "#current-namespace , #ns-select a", function(){
    $("#current-namespace").toggleClass("open");
    $("#ns-select").slideToggle();
    $(".set.active:not(.conf)").removeClass('active');
  });

  $("#darkmode").click(function(){
    $("body").toggleClass("darkmode");
    $(this).toggleClass("active");

    if($("body").hasClass("darkmode")) {
      store.commit('setTheme', 'dark');
      
      if($('#grafana').length)
        $('#grafana').attr("src", $('#grafana').attr("src").replace('light','dark'));
    }
    else {
      store.commit('setTheme', 'light');

      if($('#grafana').length)
        $('#grafana').attr("src", $('#grafana').attr("src").replace('dark','light'));
    }

    
  });

  $("#reload").click(function(){
    vm.fetchAPI();
    $(this).addClass("active");
  });

  $(document).on("click","[data-active]", function(){
    $($(this).data("active")).addClass("active");
  });

  $(document).on("click",".openConfig", function(){
    $(this).toggleClass('open');
    $(this).parents('tr').toggleClass('open');
    $(this).parents('tr').find('.parameters').toggleClass('open');
  });

  $(document).on('click', 'ul.select .selected', function(){
    $(this).parent().toggleClass('active');
  });

  $(document).on('click', '.set > .addnew', function(){
    if(!$(this).parent().hasClass('active')) {
      $('.set.active:not(.conf)').removeClass('active');
      $('.set ul.show').removeClass('show');
      $(this).parent().addClass('active');
    }
  });

  $('form.noSubmit').on('submit',function(e){
    e.preventDefault
  });

  onmousemove = function (e) {

    if( (window.innerWidth - e.clientX) > 420 ) {
      $('#nameTooltip').css({
        "top": e.clientY+20, 
        "right": "auto",
        "left": e.clientX+20
      })
    } else {
      $('#nameTooltip').css({
        "top": e.clientY+20, 
        "left": "auto",
        "right": window.innerWidth - e.clientX + 20
      })
    }
  }
  
  $(document).on('mouseenter', 'td.hasTooltip', function(){
    c = $(this).children('span');
    if(c.width() > $(this).width()){
      $('#nameTooltip .info').text(c.text());
      $('#nameTooltip').addClass('show');
    }
      
  });

  $(document).on('mouseleave', 'td.hasTooltip', function(){ 
    $('#nameTooltip .info').text('');
    $('#nameTooltip').removeClass('show');
  });


});
