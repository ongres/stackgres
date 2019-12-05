var ClusterStatus = Vue.component("cluster-status", {
	template: `
		<div id="cluster-status">
			<header>
				<h2 class="title">STATUS</h2>
				<h3 class="subtitle">{{ $route.params.name }}</h3>
			</header>

			<div class="content">
				<div class="table" v-if="dataReady">
					<div class="head row">
						<div class="col text">
							<h4>Pod Name</h4>
						</div>

						<div class="col status">
							<h4>Status</h4>
						</div>

						<!--<div class="col text">
							<h4>View Report</h4>
						</div>-->

						<div class="col">
							<h4>Containers</h4>
						</div>
					</div>
					<div v-for="pod in cluster.data.pods" class="row">
						<div class="col text">
							{{ pod.name }}
						</div>
						<div :class="'col status '+pod.status.toLowerCase()">
							<span>{{ pod.status.charAt(0) }}</span> {{ pod.role }}
						</div>
						<!--<div class="col link">
							{{ pod.ip }}:{{ pod.port }}
						</div>-->
						<div class="col">
							{{ pod.containers_ready }} / {{ pod.containers }}
						</div>
					</div>
				</div>
			</div>
			<ul class="status-legend">
				STATUS LEGEND:

				<li class="status running">
					<span>R</span> Running Pod
				</li>

				<li class="status pending">
					<span>P</span> Pending Pod
				</li>

				<li class="status failed">
					<span>F</span> Failed Pod
				</li>
			</ul>
		</div>`,
	data: function() {
		return {
	      dataReady: false,
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
		    .get(apiURL+'clusters/status/'+vm.$route.params.namespace+'/'+vm.$route.params.name,
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

	        	vc.dataReady = true;

	      	});
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

	    //$('.clu .'+vm.$route.params.name).addClass("active");
	    
	},
	computed: {

		cluster () {
			//console.log(store.state.currentCluster);
			return store.state.currentCluster
		}
	},
	beforeDestroy () {
		clearInterval(this.polling);
		//console.log('Interval cleared');
	} 
})
