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
      'pgconfig',
      'connpoolconfig',
      'profile'
    ];

//Local Json "API"
//var apiURL = 'js/data/';

//Test API
//var apiURL = 'http://192.168.1.10:7978/';

//Prod API
var apiURL = '/stackgres/';

var urlParams = new URLSearchParams(window.location.search);

if( urlParams.has('localAPI') ) {
  console.log('Using Local API');
  apiURL = 'js/data/';
}

const router = new VueRouter({
  routes: [
    { 
      path: '/crd/:action/cluster', 
      component: CreateCluster,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/crd/:action/cluster/:namespace/:name', 
      component: CreateCluster,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/crd/:action/profile', 
      component: CreateProfile,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/crd/:action/profile/:namespace/:name', 
      component: CreateProfile,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/crd/:action/pgconfig', 
      component: CreatePGConfig,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/crd/:action/pgconfig/:namespace/:name', 
      component: CreatePGConfig,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/crd/:action/poolconfig', 
      component: CreatePoolConfig,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/crd/:action/poolconfig/:namespace/:name', 
      component: CreatePoolConfig,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/crd/:action/backupconfig', 
      component: CreateBackupConfig,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/crd/:action/backupconfig/:namespace/:name', 
      component: CreateBackupConfig,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/crd/:action/backup/:namespace', 
      component: CreateBackup,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/crd/:action/backup/:namespace/:cluster/:name', 
      component: CreateBackup,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/overview/:namespace', 
      component: ClusterOverview,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/configuration/:namespace/:name', 
      component: ClusterInfo,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/status/:namespace/:name', 
      component: ClusterStatus,
      meta: {
        conditionalRoute: false
      },
    },
    {  
      path: '/backups/:namespace/', 
      component: Backups,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/backups/:namespace/:name', 
      component: Backups,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/configurations/postgres/:namespace', 
      component: PgConfig,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/configurations/postgres/:namespace/:name', 
      component: PgConfig,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/configurations/connectionpooling/:namespace', 
      component: PoolConfig,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/configurations/connectionpooling/:namespace/:name', 
      component: PoolConfig,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/configurations/backup/:namespace', 
      component: BackupConfig,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/configurations/backup/:namespace/:name', 
      component: BackupConfig,
      meta: {
        conditionalRoute: false
      },
    },
    {  
      path: '/profiles/:namespace/', 
      component: SGProfiles,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/profiles/:namespace/:name', 
      component: SGProfiles,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/monitor/', 
      component: Grafana,
      meta: {
        conditionalRoute: false
      },
    },
    { 
      path: '/monitor/:namespace/:name', 
      component: Grafana,
      meta: {
        conditionalRoute: false
      },
    },
    ,
    { 
      path: '/grafana/:namespace/:name/:pod', 
      component: Grafana,
      meta: {
        conditionalRoute: false
      },
    },

  ],
});

router.replace({ path: '', redirect: '/'});

router.beforeEach((to, from, next) => { 
    if (to.matched.some(record => record.meta.conditionalRoute)) { 
        // this route requires condition to be accessed
        // if not, redirect to home page.
        //var nav = document.getElementById("nav"); 

        //console.log(to);

        if (store.state.currentCluster == {} && ( from.path.includes("profiles") || from.path.includes("configurations") ) && (to.path != ('/configuration/'+to.params.name)) ) { 
            next({ path: '/'}) 
        } else { 
            next() 
        } 
    } else { 
        next() // make sure to always call next()! 
    } 

});

const store = new Vuex.Store({
  state: {
    currentNamespace: ' ',
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
    storageClasses: []
  },
  mutations: {
    
    setCurrentNamespace (state, namespace) {
      state.currentNamespace = namespace;
    },

    updateNamespaces (state, namespace) {
      //state.namespaces.length = 0;
      state.namespaces.push(namespace);

      if( store.state.currentNamespace == ' ') {
        store.commit('setCurrentNamespace', namespace);
        router.push('/overview/'+store.state.currentNamespace);
        $('#selected--zg-ul-select').addClass('active');
      }
      
    },

    addNamespaces (state, namespacesList) {
      state.allNamespaces = [...namespacesList];
    },

    addStorageClasses (state, storageClassesList) {
      state.storageClasses = [...storageClassesList];
    },

    setCurrentCluster (state, cluster) {
      state.currentCluster = cluster;
      
      // Enable/Disable Graffana button
      /* if (cluster.data.grafanaEmbedded) {
        $("#grafana-btn").css("display","block");
      } else {
        $("#grafana-btn").css("display","none");
      } */

      let index = state.backups.find(p => (cluster.name == p.data.spec.cluster) );

      // Enable/Disable Backups button
      if ( typeof index !== "undefined" ) {
        $("#backup-btn").css("display","block");
      } else {
        $("#backup-btn").css("display","none");
      }

      //Object.assign(state.currentCluster, cluster);
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
        //console.log(cluster.name+" ya existe");
      } else {
        state.clusters.splice( cluster );    
        //console.log('Se agregó '+cluster.name);
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
  }
});

const vm = new Vue({
  el: '#app',
  router,
  data: {
    active: true,
    ip: '',
    currentCluster: '',
    currentPods: '',
    clustersData: {}
    //clusters: []
  },
  methods: {
    
    /* API Request */
    fetchAPI: function() {

      console.log("Fetching API");
      $("#loader").show();
      
      /*if(!doneInit)
        namespaces.length = 0;*/

      /* Namespaces Data */
      axios
      .get(apiURL+'namespaces',
        { headers: {
            //'content-type': 'application/json'
          }
        }
      )
      .then( function(response){

        // Check if there are any changes on API Data
        if ( checkData(response.data, store.state.allNamespaces) ) {

          if(typeof store.state.allNamespaces !== 'undefined' && response.data.length != store.state.allNamespaces.length)
            store.commit('flushAllNamespaces');

          store.commit('addNamespaces', response.data);

        }
        
      });

      /* Clusters Data */
      axios
      .get(apiURL+'cluster',
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
            
            if(store.state.namespaces.indexOf(item.metadata.namespace) === -1) {
            
              store.commit('updateNamespaces', item.metadata.namespace);
            
            } else {
              //console.log("Namespace ya existe");
            }     
              
            store.commit('updateClusters', { 
              name: item.metadata.name,
              data: item
            });

            // Set as current cluster if no other cluster has already been set
            if(!store.state.currentCluster.length) {
              // Read Cluster Data
              axios
              .get(apiURL+'cluster/status/'+item.metadata.namespace+'/'+item.metadata.name,
                  { headers: {
                      'content-type': 'application/json'
                  }
                  }
              )
              .then( function(response){

                  cluster = { 
                      name: item.metadata.name,
                      data: response.data,
                      spec: item.spec,
                      metadata: item.metadata   
                  };
                  
                  store.commit('setCurrentCluster', cluster);

              });
            }

          });

        }
        
      });

      /* Backups */
      axios
      .get(apiURL+'backup',
        { headers: {
          'content-type': 'application/json'
        }
      })
      .then( function(response) {

        if( checkData(response.data, apiData['backup']) ) {

          if(typeof apiData['backup'] !== 'undefined' && response.data.length != apiData['backup'].length)
            store.commit('flushBackups');

          apiData['backup'] = response.data;

          apiData['backup'].forEach( function(item, index) {
            
            if(store.state.namespaces.indexOf(item.metadata.namespace) === -1) {
            
              store.commit('updateNamespaces', item.metadata.namespace);

            } else {
              //console.log("Namespace ya existe");
            }     
              
            store.commit('updateBackups', { 
              name: item.metadata.name,
              data: item
            });

          });

          //console.log("Backups Data updated");

        }
      });

      /* PostgreSQL Config */
      axios
      .get(apiURL+'pgconfig',
        { headers: {
          'content-type': 'application/json'
        }
      })
      .then( function(response) {

        if( checkData(response.data, apiData['pgconfig']) ) {

          if(typeof apiData['pgconfig'] !== 'undefined' && response.data.length != apiData['pgconfig'].length)
            store.commit('flushPGConfig');

          apiData['pgconfig'] = response.data;

          apiData['pgconfig'].forEach( function(item, index) {
            
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
      });

      /* Connection Pooling Config */
      axios
      .get(apiURL+'connpoolconfig',
        { headers: {
          'content-type': 'application/json'
        }
      })
      .then( function(response) {

        if( checkData(response.data, apiData['connpoolconfig']) ) {

          if(typeof apiData['connpoolconfig'] !== 'undefined' && response.data.length != apiData['connpoolconfig'].length)
            store.commit('flushPoolConfig');

          apiData['connpoolconfig'] = response.data;

          apiData['connpoolconfig'].forEach( function(item, index) {
            
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
      });

      /* Backup Config */
      axios
      .get(apiURL+'backupconfig',
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
      });


      /* Profiles */
      axios
      .get(apiURL+'profile',
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
      });


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
        
      });



      setTimeout(function(){
        $("#loader").fadeOut(500);
        //$("#loader").hide();  
      }, 1500);

    }

  },
  mounted: function() {

   /*axios
    .get(apiURL+'kubernetes-cluster-info',
      { headers: {
        'content-type': 'application/json'
      }
    })
    .then( function(response) {
      vm.ip = response.data;
      //console.log(response.data.substring(8).replace("/",""));
    });*/
    
    this.fetchAPI();

    /*$("#loader").click(function(){
      vm.fetchAPI();
    });*/

    setInterval( function(){
      this.fetchAPI();
    }.bind(this), 10000);

  }
});

Vue.filter('params', function(params){
  params = '<strong>'+params;
  params.replace(new RegExp('\r?\n','g'), '<br /><strong>');
  params.replace('=','</strong> = ');

  return params;
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

function formatBytes (a) {
  if(0==a)return"0 Bytes";var c=1024,d=2,e=["Bytes","Ki","Mi","Gi","Ti","Pi","Ei","Zi","Yi"],f=Math.floor(Math.log(a)/Math.log(c))+1;return parseFloat((a/Math.pow(c,f)).toFixed(d))+" "+e[f];
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

function notify (message, kind = 'message') {
  //$("#notifications").addClass("active");
  $("#notifications .tooltip").append('<div class="message"><h3>'+kind+'</h3><p>'+message+'</p><span class="close">×</span><div>');
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

// Sort tables by an specific parameter
function sortTable( table, param, direction ) {
  
  table.sort((a,b) => {
    let modifier = 1;
    
    if(direction === 'desc') modifier = -1;
    
    a = eval("a."+param);
    b = eval("b."+param);

    if(a < b)
      return -1 * modifier;
    
    if(a > b)
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

  $(document).on("click", "#main, #side", function() {
    if($(this).prop("id") != "notifications") {
      $(".hasTooltip.active").removeClass("active");
      $("#selected--zg-ul-select.open").removeClass("open");
      $("#be-select.active").removeClass("active");
    } 
  });


  $(document).on("click", "#sets .nav-item", function(){
    if(!($(this).parent().hasClass("active"))) {
      $(".set.active:not(.conf)").removeClass("active");
      $(this).parent("div:not(.conf)").addClass("active");
    }

    $(".set:not(.active) > ul.show").removeClass("show");
    
  });

  $(document).on("click", ".set .item", function(){
    $(".set.active:not(.conf)").removeClass("active");
    $(this).parent().parent().parent().addClass("active");

    $(".set:not(.active) > ul.show").removeClass("show");
  });

  $(document).on("mouseover", ".set:not(.active)", function(){
    let offset = $(this).offset();
    let submenu = $(this).children("ul");
    
    if(window.innerHeight > 700)
      submenu.css("bottom","auto").css("top",offset.top)
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
    if($(this).parent().hasClass("active"))
      $(this).parent().removeClass("active");
    else{
      $(".hasTooltip.active").removeClass("active");
      $(this).parent().addClass("active");
    }      
  });

  $("#sets h3").click(function(){
    $(this).parent().toggleClass("hide");
  });

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

  $(document).on('click', '.message .close', function(){
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

  $(document).on("click", "tr.base:not(.Pending) td:not(.actions)", function(){
    $(this).parent().next().toggle()
  });

  $(document).on("click", "tr.base a.open", function(){
    $(this).parent().parent().next().toggle()
  });

  
  $(document).on("click",".toggle",function(){
    $(this).toggleClass("open");
    $(this).next().toggleClass("open");
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

  $(document).on("click", "#current-namespace h2 > strong", function(){
    $("#current-namespace").toggleClass("open");
    $("#ns-select").slideToggle();    
  });

});