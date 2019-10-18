var ClusterStatus = Vue.component("cluster-status", {
	template: `
		<div id="cluster-status">
			<header>
				<h2 class="title">STATUS</h2>
				<h3 class="subtitle">{{ $route.params.name }}</h3>
			</header>

			<div class="content">
				<div class="table">
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
					<div v-for="pod in pods" class="row">
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
	      pods: null
	    }
	},
	created () {
		this.fetchData()
	},
  	watch: {
    	'$route': 'fetchData'
  	},
  	methods: {
	    fetchData () {
	      this.pods = clustersData[this.$route.params.name].data.status.pods 
	    }
	}
})
