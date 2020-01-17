var ClusterInfo = Vue.component("cluster-info", {
	template: `
		<div id="cluster-info">
			<header>
				<h2 class="title">INFO</h2>
				<h3 class="subtitle">{{ $route.params.name }}</h3>
			</header>

			<div class="content">
				<div class="table">
					<div class="head row">
						<div class="col">
							<h4>CPU</h4>
						</div>

						<div class="col">
							<h4>Memory</h4>
						</div>

						<div class="col">
							<h4>Disk</h4>
						</div>

						<div class="col">
							<h4>Health</h4>
						</div>
					</div>
					<div class="row" v-if="allDataReady">
						<div class="col">
							{{ cluster.data.cpuRequested }} (avg. load {{ cluster.data.averageLoad1m }} )
						</div>
						<div class="col">
							{{ cluster.data.memoryRequested }}
						</div>
						<div class="col">
							{{ cluster.data.diskUsed }} / {{ cluster.data.diskFound }}
						</div>
						<div class="col">
							{{ pods.data.podsReady }} / {{ pods.data.pods.length }}
						</div>
					</div>
				</div>
			</div>
		</div>`,
	data: function() {
		return {
	      dataReady: [ false, false ],
	      allDataReady: false,
	      polling: null
	    }
	},
	methods: {
		
		fetchAPI: function() {
			vc = this;

			/*store.commit('setCurrentCluster', vm.$route.params.name);
			console.log("Current cluster: "+store.state.currentCluster)*/

			/* Clusters Data */
		    axios
		    .get(apiURL+'cluster/status/'+vm.$route.params.namespace+'/'+vm.$route.params.name,
		    	{ headers: {
		            'content-type': 'application/json'
		          }
		        }
	      	)
	      	.then( function(response){

	        	store.commit('setCurrentCluster', { 
	              	name: vm.$route.params.name,
	              	data: response.data
              	});

	        	vc.dataReady[0] = true;
	        	vc.allDataReady = vc.dataReady[0] && vc.dataReady[1];
	      	});

			/* Pods Data */
		    axios
		    .get(apiURL+'cluster/pods/'+vm.$route.params.namespace+'/'+vm.$route.params.name,
		    	{ headers: {
		            'content-type': 'application/json'
		          }
		        }
	      	)
	      	.then( function(response){

	        	store.commit('setCurrentPods', { 
	              	name: vm.$route.params.name,
	              	data: response.data
              	});

	        	vc.dataReady[1] = true;
	        	vc.allDataReady = vc.dataReady[0] && vc.dataReady[1];
	      	});
		}

	},
	created: function() {

		if ( (store.state.currentCluster.length > 0) && (store.state.currentCluster.name == vm.$route.params.name) ) {
			this.dataReady = true;
		}
	},
	mounted: function() {

		var count = 0;

		this.fetchAPI();
	    
	    this.polling = setInterval( function(){
	    	//count++;
	      	this.fetchAPI();

	      	//console.log("Interval run #"+count);

	    }.bind(this), 5000);
	    
	},
	computed: {

		cluster () {
			//console.log(store.state.currentCluster);
			return store.state.currentCluster
		},
		pods () {
			//console.log(store.state.currentPods);
			return store.state.currentPods
		}
	},
	beforeDestroy () {
		clearInterval(this.polling);
		//console.log('Interval cleared');
	} 
})