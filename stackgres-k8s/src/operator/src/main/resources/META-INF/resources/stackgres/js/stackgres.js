Vue.use(VueRouter);

var navItems = document.getElementsByClassName("nav-item");

var clustersList = [],
    clustersData = [],
    pgConf = [],
    poolConf = [],
    profiles = [],
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
var apiURL = './js/data/';

//Test API
//var apiURL = 'http://192.168.1.10:7978/';
//var apiURL = '/stackgres/';

//Prod API
//var apiURL = "/";

const router = new VueRouter({
  routes: [
      { 
        path: '/overview/', 
        component: ClusterOverview,
        meta: {
          conditionalRoute: false
        },
      },
      { 
        path: '/information/:name', 
        component: ClusterInfo,
        meta: {
          conditionalRoute: true
        },
      },
      { 
        path: '/status/:name', 
        component: ClusterStatus,
        meta: {
          conditionalRoute: true
        },
      },
      { 
        path: '/configurations/postgresql', 
        component: PgConfig,
        meta: {
          conditionalRoute: false
        },
      },
      { 
        path: '/configurations/postgresql/:name', 
        component: PgConfig,
        meta: {
          conditionalRoute: false
        },
      },
      { 
        path: '/configurations/connectionpooling', 
        component: PoolConfig,
        meta: {
          conditionalRoute: false
        },
      },
      { 
        path: '/configurations/connectionpooling/:name', 
        component: PoolConfig,
        meta: {
          conditionalRoute: false
        },
      },
      {  
        path: '/profiles/', 
        component: SGProfiles,
        meta: {
          conditionalRoute: false
        },
      },
      { 
        path: '/profiles/:name', 
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

router.replace({ path: '', redirect: '/' });

router.beforeEach((to, from, next) => { 
    if (to.matched.some(record => record.meta.conditionalRoute)) { 
        // this route requires condition to be accessed
        // if not, redirect to home page.
        //var nav = document.getElementById("nav"); 

        //console.log(to);

        if (vm.currentCluster == '' && ( from.path.includes("profiles") || from.path.includes("configurations") ) && (to.path != ('/information/'+to.params.name)) ) { 
            next({ path: '/'}) 
        } else { 
            next() 
        } 
    } else { 
        next() // make sure to always call next()! 
    } 

});

var vm = new Vue({
    el: '#app',
    router,
    data: {
      active: true,
      ip: '',
      currentCluster: '',
      //clusters: []
  },
  methods: {
    
    /* API Request */
    fetchAPI: function() {

      $("#loader").show();

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

          apiData['cluster'] = response.data;

          clustersData.length = 0;
          clustersList.length = 0;
          
          apiData['cluster'].forEach( function(item, index) {
            clustersData[item.metadata.name] = { 
              name: item.metadata.name,
              data: item 
            };

            clustersList.push({
              name: item.metadata.name,
              data: item
            });
          });

          
          console.log('Clusters Data updated');
          
          if(doneInit){
            //vm.$router.go();
            notify('Clusters Data updated');
          }
          else
            doneInit = true;
          //vm.$forceUpdate();
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

          apiData['pgconfig'] = response.data;
          pgConf.length = 0;

          apiData['pgconfig'].forEach( function(item, index) {
            pgConf.push({
              "name": item.metadata.name,
              "data": item 
            })
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
         
          apiData['connpoolconfig'] = response.data;
          poolConf.length = 0;

          apiData['connpoolconfig'].forEach( function(item, index) {
            poolConf.push({
              "name": item.metadata.name,
            "data": item 
            })
          });

          console.log("PoolConfig Data updated");

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

          apiData['profile'] = response.data;
          profiles.length = 0;

          apiData['profile'].forEach( function(item, index) {
            profiles.push({
              "name": item.metadata.name,
            "data": item 
            })
          });

          console.log("Profiles Data updated");

        }

      });

      setTimeout(function(){
        $("#loader").fadeOut(2000);  
      }, 1500);
      
    }

  },
  mounted: function() {

   axios
    .get(apiURL+'kubernetes-cluster-info',
      { headers: {
        'content-type': 'application/json'
      }
    })
    .then( function(response) {
      vm.ip = response.data;
      //console.log(response.data.substring(8).replace("/",""));
    });
    
    this.fetchAPI();

    setInterval( function(){
      this.fetchAPI();
    }.bind(this), 10000);

  }
})

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

  $(document).on("click", ".clu a", function(){
    $(".clu .router-link-active:not(.router-link-exact-active)").removeClass("router-link-active");
    vm.currentCluster = $(this).text();


    $("#nav").removeClass("disabled");
    //console.log(currentCluster);
    //console.log(router.history.current.params.name);
  });

  $(document).on("click", ".conf a, .prof a", function(){
    vm.currentCluster = '';
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

    $(".clu a[href$='"+vm.currentCluster+"']").addClass("router-link-active");

  });

  $("#notifications").click(function(){
    $(this).toggleClass("active");
    $("#notifications .tooltip").toggleClass("show");
  });


  $("#nav .view").click(function(){
    $("#nav .tooltip.show").prop("class","tooltip").hide();
    $("#nav .top a.nav-item").removeClass("router-link-active");
    $("#nav").addClass("disabled");
    $(".clu a").removeClass("router-link-active").removeClass("router-link-exact-active");
    $(".set.active").removeClass("active");


    if(vm.currentCluster.length) {
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

});