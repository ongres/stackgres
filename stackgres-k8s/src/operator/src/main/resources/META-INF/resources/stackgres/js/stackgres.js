Vue.use(VueRouter);

var navItems = document.getElementsByClassName("nav-item");

var clustersList = [],
    //clustersData = [],
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
//var apiURL = './js/data/';

//Test API
//var apiURL = 'http://192.168.1.10:7978/';

//Prod API
var apiURL = '/stackgres/';

const router = new VueRouter({
  routes: [
      { 
        path: '/overview/:namespace', 
        component: ClusterOverview,
        meta: {
          conditionalRoute: false
        },
      },
      { 
        path: '/information/:namespace/:name', 
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
        path: '/configurations/postgresql/:namespace', 
        component: PgConfig,
        meta: {
          conditionalRoute: false
        },
      },
      { 
        path: '/configurations/postgresql/:namespace/:name', 
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
        path: '/grafana/', 
        component: Grafana,
        meta: {
          conditionalRoute: false
        },
      },
      { 
        path: '/grafana/:name', 
        component: Grafana,
        meta: {
          conditionalRoute: false
        },
      },
      ,
      { 
        path: '/grafana/:name/:pod', 
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

        if (store.state.currentCluster == {} && ( from.path.includes("profiles") || from.path.includes("configurations") ) && (to.path != ('/information/'+to.params.name)) ) { 
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
    clusters: [],
    pgConfig: [],
    poolConfig: [],
    backupConfig: [],
    profiles: []
  },
  mutations: {
    
    setCurrentNamespace (state, namespace) {
      state.currentNamespace = namespace;
    },

    updateNamespaces (state, namespace) {
      //state.namespaces.length = 0;
      state.namespaces.push(namespace);
    },

    setCurrentCluster (state, cluster) {
      state.currentCluster = cluster;
      //Object.assign(state.currentCluster, cluster);
    },

    updateClusters ( state, cluster ) {

      let index = state.clusters.find(c => (cluster.data.metadata.name == c.name) && (cluster.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = cluster.data;
        console.log(cluster.name+" ya existe");
      } else {
        state.clusters.push( cluster );    
        console.log('Se agregó '+cluster.name);
      }

    },
    updatePGConfig ( state, config ) {

      let index = state.pgConfig.find(c => (config.data.metadata.name == c.name) && (config.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = config.data;
        console.log(config.name+" ya existe");
      } else {
        state.pgConfig.push( config );    
        console.log('Se agregó '+config.name);
      }

    },
    updatePoolConfig ( state, config ) {

      let index = state.poolConfig.find(c => (config.data.metadata.name == c.name) && (config.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = config.data;
        console.log(config.name+" ya existe");
      } else {
        state.poolConfig.push( config );    
        console.log('Se agregó '+config.name);
      }

    },
    updateBackupConfig ( state, config ) {

      let index = state.backupConfig.find(c => (config.data.metadata.name == c.name) && (config.data.metadata.namespace == c.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = config.data;
        console.log(config.name+" ya existe");
      } else {
        state.backupConfig.push( config );    
        console.log('Se agregó '+config.name);
      }

    },
    updateProfiles ( state, profile ) {

      let index = state.profiles.find(p => (profile.data.metadata.name == p.name) && (profile.data.metadata.namespace == p.data.metadata.namespace) ); 

      if ( typeof index !== "undefined" ) {
        index.data = profile.data;
        console.log(profile.name+" ya existe");
      } else {
        state.profiles.push( profile );    
        console.log('Se agregó '+profile.name);
      }

    },

    flushClusters (state ) {
      state.clusters.length = 0;
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
  }
});

var vm = new Vue({
    el: '#app',
    router,
    data: {
      active: true,
      ip: '',
      currentCluster: '',
      clustersData: {}
      //clusters: []
    },
  methods: {
    
    /* API Request */
    fetchAPI: function() {

      $("#loader").show();
      
      /*if(!doneInit)
        namespaces.length = 0;*/

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

              if(store.state.currentNamespace == ' ') {
                store.commit('setCurrentNamespace', item.metadata.namespace);
                router.push('/overview/'+store.state.currentNamespace);
                $('#selected--zg-ul-select').text(store.state.currentNamespace).addClass('active');
              }
            
            } else {
              //console.log("Namespace ya existe");
            }     
              
            store.commit('updateClusters', { 
              name: item.metadata.name,
              data: item
            });

          });
          
          console.log('Clusters Data updated');
          
          /*if(doneInit)
            notify('Clusters Data updated');
          else
            doneInit = true;*/

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

          console.log("PGconf Data updated");

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

          console.log("PoolConf Data updated");

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

          console.log("BackupConfig Data updated");

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
            store.commit('flushProfile');

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

          console.log("Profiles Data updated");

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

Vue.filter('prettyCRON', function (value) {
  
  //console.log(value);
  return prettyCron.toString(value)
  
});

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

function notify (message) {
  //$("#notifications").addClass("active");
  $("#notifications .tooltip").append('<p>'+message+'</p>');
  $("#notifications .count").text(parseInt($("#notifications .count").text(),10)+1);

  if(parseInt($("#notifications .count").text(),10) > 0) {
    $("#notifications").addClass("active");
    $("#notifications .count").removeClass("zero");
    $("#notifications .tooltip .zero").remove();
  } else {
    if (!$("#notifications .tooltip .zero").length)
      $("#notifications .tooltip").append('<p class="zero">There are no new notifications.</p>');
  }
}


/* jQuery */

$(document).ready(function(){

  $("#namespaces li:first-of-type a").click();

  $(document).on("click", "a.namespace", function(){
    store.commit('setCurrentNamespace',$(this).text());
  });

  $(document).on("click", ".clu a", function(){
    $(".clu .router-link-active:not(.router-link-exact-active)").removeClass("router-link-active");
    //store.commit('setCurrentClusterName', $(this).text());


    $("#nav").removeClass("disabled");
    //console.log(currentCluster);
    //console.log(router.history.current.params.name);
  });

  $(document).on("click", ".conf a, .prof a", function(){
    store.commit('setCurrentCluster', ' ');
    $("#nav").addClass("disabled");
  });

  $(document).on("click", ".box h4", function(){
    $(this).parent().toggleClass("show");
  });

  $(document).on("click", "#main, #side", function() {
    $(".tooltip.show").removeClass("show").hide();
  });


  $(document).on("click", ".set a", function(){
    $("#sets .set.active").removeClass("active");
    $(this).parents(".set").addClass("active");
  });

  $(document).on("click", "#nav:not(.disabled) .top a.nav-item", function(){

    $(".clu a[href$='"+store.state.currentCluster+"']").addClass("router-link-active");

  });

  $("#notifications").click(function(){
    $(this).toggleClass("active");
    $("#notifications .tooltip").toggleClass("show");
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

  $(".help").click(function(){
    $("#nav .tooltip.show").prop("class","tooltip").hide();
    $(this).siblings(".tooltip").fadeIn().addClass("show");
    $("#nav .top .tooltip").addClass("pos"+($(this).index()+2));
  });

  $("#sets h3").click(function(){
    $(this).parent().toggleClass("hide");
  });

  $(".clu .item").click(function(){
    $("#nav").removeClass("disabled");
  });

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

    if (!ul.hasClass('zg-ul-select')) {
      ul.addClass('zg-ul-select');
    }
    
    $('li:first-of-type', this).addClass('active');

    //ul.before('<div id="selected--zg-ul-select">');
    var selected = $('#selected--zg-ul-select');
      
    
    $(document).on('click', '#selected--zg-ul-select', function(){

      $(this).toggleClass('open');
      ul.toggleClass('active');
      
      // Remove active class from any <li> that has it...
      //ul.children().removeClass('active');
      
      // And add the class to the <li> that gets clicked
      //$(this).toggleClass('active');

      var selectedText = $(this).text();
      if (ul.hasClass('active')) {
        selected.text(selectedText).addClass('active');
      }
      else {
        //selected.text('').removeClass('active'); 
        $('li.active', ul);
      }
    });

    $(document).on('click', '#be-select li a', function(){
      selected.text($(this).text()).removeClass('open');
      ul.removeClass('active');
    });

    // Close the faux select menu when clicking outside it 
    $(document).on('click', function(event){
      /*if($('ul.zg-ul-select').length) {
        if(!$('ul.zg-ul-select').has(event.target).length == 0) {
          return;
        }
        else {
          $('ul.zg-ul-select').removeClass('active');
          //$('#selected--zg-ul-select').removeClass('active').text('');
          $('ul.zg-ul-select li.active');
        } 
      }*/
    });
  }

  // Run
  $('#be-select').ulSelect();

});