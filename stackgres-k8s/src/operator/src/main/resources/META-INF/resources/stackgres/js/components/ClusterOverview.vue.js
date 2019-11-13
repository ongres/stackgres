var ClusterOverview = Vue.component("cluster-overview", {
	template: `
		<div id="cluster-overview">
			<header>
				<h2 class="title">OVERVIEW</h2>
				<!--<h3 class="subtitle">K8S Cluster: {{ serverIP }}</h3>-->
			</header>

			<div class="content">
				<div class="table">
					<div class="head row">
						<div class="col text">
							<h4>StackGres Cluster</h4>
						</div>

						<div class="col">
							<h4>Instances</h4>
						</div>

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
					<div v-for="cluster in clusters" class="row">
						<div class="col text">
							{{ cluster.name }}
						</div>
						<div class="col">
							{{ cluster.data.spec.instances }}
						</div>
						<div class="col">
							{{ cluster.data.status.cpu_requested }}
						</div>
						<div class="col">
							{{ cluster.data.status.memory_requested }}
						</div>
						<div class="col">
							{{ cluster.data.spec.volume_size }}
						</div>
						<div class="col">
							{{ cluster.data.status.pods_ready + '/' + cluster.data.spec.instances }}
						</div>
					</div>
				</div>
				<!--<div class="cta">
					<a href="#" class="btn">Create New Cluster</a>
				</div>-->
			</div>
		</div>`,
	data: function() {
		
		return {
	      clusters: null
	    }
	},
	created: function() {
		this.clusters = clustersList;
	}
})
