var ClusterInfo = Vue.component("cluster-info", {
	template: `
		<div id="cluster-info">
			<header>
				<h2 class="title">INFO</h2>
				<h3 class="subtitle">{{ cluster.name }}</h3>
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
					<div class="row">
						<div class="col">
							{{ cluster.data.status.cpuRequested + ' (avg. load ' + cluster.data.status.averageLoad1m + ')' }}
						</div>
						<div class="col">
							{{ cluster.data.status.memoryRequested }}
						</div>
						<div class="col">
							{{ cluster.data.status.diskUsed + '/' + cluster.data.spec.volumeSize }}
						</div>
						<div class="col">
							{{ cluster.data.status.podsReady + '/' + cluster.data.spec.instances }}
						</div>
					</div>
				</div>
			</div>
		</div>`,
	data: function() {
		return {
	      cluster: null
	    }
	},
	created () {
		this.fetchData()
		currentCluster = this.$route.params.name;
	},
  	watch: {
    	'$route': 'fetchData'
  	},
  	methods: {
	    fetchData () {
	      this.cluster = clustersData[this.$route.params.name]  
	    }
	}	
})
