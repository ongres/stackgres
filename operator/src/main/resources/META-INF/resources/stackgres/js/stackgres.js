Vue.use(VueRouter);

var navItems = document.getElementsByClassName("nav-item");

var clustersList = [];
var clustersData = [];
var pgConf = [];
var poolConf = [];
var profiles = [];
var currentCluster = "";
var serverIP = "";

//Test API
//var apiURL = 'http://192.168.1.10:7978/';

//Prod API
var apiURL = "/stackgres/";


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

  	],
});

router.replace({ path: '', redirect: '/' });

router.beforeEach((to, from, next) => { 
    if (to.matched.some(record => record.meta.conditionalRoute)) { 
        // this route requires condition to be accessed
        // if not, redirect to home page.
        //var nav = document.getElementById("nav"); 

        //console.log(to);

        if (currentCluster == '' && ( from.path.includes("profiles") || from.path.includes("configurations") ) && (to.path != ('/information/'+to.params.name)) ) { 
            //check codition is false
            //console.log(from);
            next({ path: '/'}) 
        } else { 
            //check codition is true
            //console.log(from);
            next() 
        } 
    } else { 
        next() // make sure to always call next()! 
    } 

   	/*if ( ( to.path.includes("information") || to.path.includes("status") ) && ( from.path.includes("profiles") || from.path.includes("configurations") ) ){
      console.log("Cluster: "+currentCluster);
    	next('/');
    }
    else
      next()*/

    //currentCluster = document.querySelector(".clu .router-link-exact-active").text;
    //currentCluster = $(".clu .router-link-exact-active").text();
		//next();
});

/* eslint-disable no-new */
new Vue({
  	el: '#app',
  	router,
  	data: {
  		active: true,
  		//clusters: []
	},
	mounted () {

    /* Server IP */
        axios
        .get(apiURL+'kubernetes-cluster-info',
          { headers: {
            'content-type': 'application/json'
          }
        })
        .then( function(response) {
          serverIP = response.data;
          //console.log(response.data.substring(8).replace("/",""));
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

      		response.data.forEach( function(item, index){
			  clustersData[item.metadata.name] = { 
			    name: item.metadata.name,
			    data: item 
			  };

			  clustersList.push({
			  	name: item.metadata.name,
			  	data: item
			  });
			});
      		
      	});

      	/* PostgreSQL Config */
      	axios
      	.get(apiURL+'pgconfig',
      		{ headers: {
      			'content-type': 'application/json'
      		}
      	})
      	.then( function(response) {
      		response.data.forEach( function(item, index) {
      			pgConf.push({
      				"name": item.metadata.name,
	    			"data": item 
      			})
      		});
      	});

      	/* Connection Pooling Config */
      	axios
      	.get(apiURL+'connpoolconfig',
      		{ headers: {
      			'content-type': 'application/json'
      		}
      	})
      	.then( function(response) {
      		response.data.forEach( function(item, index) {
      			poolConf.push({
      				"name": item.metadata.name,
	    			"data": item 
      			})
      		});
      	});

      	/* Profiles */
      	axios
      	.get(apiURL+'profile',
      		{ headers: {
      			'content-type': 'application/json'
      		}
      	})
      	.then( function(response) {
      		response.data.forEach( function(item, index) {
      			profiles.push({
      				"name": item.metadata.name,
	    			"data": item 
      			})
      		});
      	});
  	}
})


/* jQuery */

$(document).ready(function(){

	$(document).on("click", ".clu a", function(){
		$(".clu .router-link-active:not(.router-link-exact-active)").removeClass("router-link-active");
		currentCluster = $(this).text();


		$("#nav").removeClass("disabled");
		//console.log(currentCluster);
		//console.log(router.history.current.params.name);
	});

	$(document).on("click", ".conf a, .prof a", function(){
		currentCluster = '';
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

		$(".clu a[href$='"+currentCluster+"']").addClass("router-link-active");

	});


	$("#nav .view").click(function(){
		$("#nav .tooltip.show").prop("class","tooltip").hide();
		$("#nav .top a.nav-item").removeClass("router-link-active");
		$("#nav").addClass("disabled");
		$(".clu a").removeClass("router-link-active").removeClass("router-link-exact-active");
		$(".set.active").removeClass("active");


		if(currentCluster.length) {
			//$(".clu a[href$='"+currentCluster+"']").addClass("router-link-active");
			/*$("#nav .top a").each(function(){
				$(this).attr("href", $(this).attr("href")+currentCluster);
			});*/
		}
	});

	$("#nav.disabled .top a.nav-item").click(function(){

			$("#nav .tooltip.show").prop("class","tooltip").hide();
			$(this).siblings(".tooltip").fadeIn().addClass("show");
			$("#nav .top .tooltip").addClass("pos"+($(this).index()+2));
	});

	$("#nav .bottom a.nav-item").click(function(){
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

});