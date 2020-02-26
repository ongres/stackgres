var Side = Vue.component("sg-side", {
	template: `
		<aside id="side" :class="currentNamespace">
			<div id="current-namespace">
				<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
				<h2>
					NAMESPACE<br/>
					{{ currentNamespace }}
				</h2>
			</div>
			<div id="sets">
				<div class="set clu">
					<router-link :to="'/overview/'+currentNamespace" title="Overview" class="view nav-item">
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
						<h3>Stackgres Clusters</h3>
					</router-link>

					<ul>
						<template v-for="cluster in clusters">
							<li v-if="cluster.data.metadata.namespace == currentNamespace">
								<router-link :to="'/information/'+cluster.data.metadata.namespace+'/'+cluster.name" class="item" :class="cluster.name">{{ cluster.name }}</router-link>
							</li>
						</template>
						<li><router-link to="/crd/create/cluster/" class="addnew item">Add New</router-link></li>
					</ul>

				</div>
				<!--<div class="set back">
					<h3>Backups</h3>
					<a href="#" class="addnew">+</a>

					<template v-for="backup in backups">
						<router-link :to="'/backups/'+backup.data.metadata.namespace+'/'+backup.name" class="item" :class="backup.name" v-if="backup.data.metadata.namespace == currentNamespace">{{ backup.name }}</router-link>
					</template>
				</div>-->
				<div class="set conf">
					<div class="nav-item">
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 21 20"><path d="M.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm19.09-1.905h-10.5a.953.953 0 100 1.9h10.5a.953.953 0 100-1.9zm0 6.191h-8.59v-1.9a.955.955 0 00-1.91 0v5.715a.955.955 0 001.91 0v-1.9h8.59a.953.953 0 100-1.905zm-12.409 0H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"/></svg>
						<h3>Configurations</h3>
					</div>

					<ul>
						<div class="pg set">
							<router-link :to="'/configurations/postgresql/'+currentNamespace" class="nav-item">
								<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.7 20"><path class="a" d="M10.946 18.7a.841.841 0 01-.622-.234.862.862 0 01-.234-.635v-7.817a.8.8 0 01.221-.6.834.834 0 01.608-.214h3.29a3.4 3.4 0 012.353.755 2.7 2.7 0 01.843 2.12 2.72 2.72 0 01-.843 2.126 3.379 3.379 0 01-2.353.764h-2.394v2.875a.8.8 0 01-.869.867zM14 13.637q1.778 0 1.778-1.551T14 10.535h-2.18v3.1zm11.968-.107a.683.683 0 01.494.181.625.625 0 01.191.477v2.875a1.717 1.717 0 01-.16.87 1.174 1.174 0 01-.655.414 6.882 6.882 0 01-1.242.294 9.023 9.023 0 01-1.364.107 5.252 5.252 0 01-2.527-.573 3.883 3.883 0 01-1.638-1.665 5.548 5.548 0 01-.569-2.6 5.5 5.5 0 01.569-2.575 3.964 3.964 0 011.611-1.671 4.965 4.965 0 012.455-.59 4.62 4.62 0 013.089 1.016 1.058 1.058 0 01.234.294.854.854 0 01-.087.843.479.479 0 01-.388.2.737.737 0 01-.267-.047 1.5 1.5 0 01-.281-.153 4.232 4.232 0 00-1.1-.582 3.648 3.648 0 00-1.146-.167 2.747 2.747 0 00-2.2.859 3.834 3.834 0 00-.742 2.561q0 3.477 3.049 3.477a6.752 6.752 0 001.815-.254v-2.36h-1.517a.737.737 0 01-.5-.161.664.664 0 010-.909.732.732 0 01.5-.161zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"/></svg>
								<h4>PostgreSQL</h4>
							</router-link>

							<ul>
								<template v-for="config in pgConfig">
									<li v-if="config.data.metadata.namespace == currentNamespace">
										<router-link :to="'/configurations/postgresql/'+config.data.metadata.namespace+'/'+config.name" class="item" :class="config.name">{{ config.name }}</router-link>
									</li>
								</template>
								<li><router-link to="/crd/create/pgconfig/" class="addnew item">Add New</router-link></li>
							</ul>						
						</div>

						<div class="pool set">
							<router-link :to="'/configurations/connectionpooling/'+currentNamespace" class="nav-item">
								<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.5 20"><path d="M14.305 18.749a4.7 4.7 0 01-2.388-.589 3.91 3.91 0 01-1.571-1.685 5.668 5.668 0 01-.546-2.568 5.639 5.639 0 01.548-2.561 3.916 3.916 0 011.571-1.678 4.715 4.715 0 012.388-.593 5.189 5.189 0 011.658.261 4.324 4.324 0 011.378.756.758.758 0 01.24.281.859.859 0 01.067.361.768.768 0 01-.16.495.479.479 0 01-.388.2.984.984 0 01-.548-.191 4 4 0 00-1.07-.595 3.405 3.405 0 00-1.1-.167 2.571 2.571 0 00-2.106.869 3.943 3.943 0 00-.72 2.562 3.963 3.963 0 00.716 2.568 2.568 2.568 0 002.106.869 3.147 3.147 0 001.063-.173 5.112 5.112 0 001.1-.589 2.018 2.018 0 01.267-.134.751.751 0 01.29-.048.477.477 0 01.388.2.767.767 0 01.16.494.863.863 0 01-.067.355.739.739 0 01-.24.286 4.308 4.308 0 01-1.378.757 5.161 5.161 0 01-1.658.257zm5.71-.04a.841.841 0 01-.622-.234.856.856 0 01-.234-.636v-7.824a.8.8 0 01.22-.6.835.835 0 01.609-.214h3.29a3.4 3.4 0 012.354.755 2.7 2.7 0 01.842 2.12 2.725 2.725 0 01-.842 2.127 3.386 3.386 0 01-2.354.764h-2.393v2.875a.8.8 0 01-.87.868zm3.05-5.069q1.779 0 1.779-1.552t-1.779-1.551h-2.18v3.1zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"/></svg>
								<h4>Connection Pooling</h4>
							</router-link>

							<ul>
								<template v-for="config in poolConfig">
									<li v-if="config.data.metadata.namespace == currentNamespace">
										<router-link :to="'/configurations/connectionpooling/'+config.data.metadata.namespace+'/'+config.name" class="item" :class="config.name">{{ config.name }}</router-link>
									</li>
								</template>
								<li><router-link to="/crd/create/poolconfig/" class="addnew item">Add New</router-link></li>
							</ul>
						</div>

						<div class="backup set">
							<router-link :to="'/configurations/backup/'+currentNamespace" class="nav-item">
								<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 27.1 20"><path d="M15.889 13.75a2.277 2.277 0 011.263.829 2.394 2.394 0 01.448 1.47 2.27 2.27 0 01-.86 1.885 3.721 3.721 0 01-2.375.685h-3.449a.837.837 0 01-.6-.213.8.8 0 01-.22-.6v-7.795a.8.8 0 01.22-.6.835.835 0 01.609-.214h3.306a3.679 3.679 0 012.306.648 2.165 2.165 0 01.836 1.815 2.159 2.159 0 01-.395 1.3 2.254 2.254 0 01-1.089.79zm-4.118-.585h2.179q1.779 0 1.778-1.323a1.143 1.143 0 00-.441-.989 2.267 2.267 0 00-1.337-.321h-2.179zm2.407 4.118a2.219 2.219 0 001.363-.335 1.242 1.242 0 00.428-1.042 1.271 1.271 0 00-.435-1.056 2.155 2.155 0 00-1.356-.348h-2.407v2.781zm8.929 1.457a3.991 3.991 0 01-2.941-1 3.968 3.968 0 01-1-2.927V9.984a.854.854 0 01.227-.622.925.925 0 011.23 0 .854.854 0 01.227.622V14.9a2.623 2.623 0 00.573 1.838 2.18 2.18 0 001.684.622 2.153 2.153 0 001.671-.628 2.624 2.624 0 00.573-1.832V9.984a.85.85 0 01.228-.622.924.924 0 011.229 0 .85.85 0 01.228.622v4.826a3.969 3.969 0 01-1 2.92 3.95 3.95 0 01-2.929 1.01zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"/></svg>
								<h4>Backups</h4>
							</router-link>

							<ul>
								<template v-for="config in bkConfig">
									<li v-if="config.data.metadata.namespace == currentNamespace">
										<router-link :to="'/configurations/backup/'+config.data.metadata.namespace+'/'+config.name" class="item" :class="config.name">{{ config.name }}</router-link>
									</li>
								</template>
								<li><router-link to="/crd/create/backupconfig/" class="addnew item">Add New</router-link></li>
							</ul>
						</div>

						<div class="prof set">
							<router-link :to="'/profiles/'+currentNamespace" class="nav-item">
								<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 21.3 20"><path d="M10.962 9.14a.808.808 0 00-.862.86v7.878a.86.86 0 00.235.63.83.83 0 00.624.242.82.82 0 00.872-.872V10a.842.842 0 00-.235-.624.862.862 0 00-.634-.236zm9.407.825a3.419 3.419 0 00-2.362-.758h-3.3a.842.842 0 00-.611.215.8.8 0 00-.221.6v7.851a.859.859 0 00.233.637.842.842 0 00.624.235.806.806 0 00.868-.87v-2.882h2.406a3.393 3.393 0 002.362-.767 2.729 2.729 0 00.846-2.133 2.709 2.709 0 00-.845-2.128zm-2.576 3.7H15.6v-3.116h2.192q1.785 0 1.785 1.557t-1.784 1.557zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"/></svg>
								<h4>Instance Profiles</h4>
							</router-link>

							<ul>
								<template v-for="profile in profiles">
									<li v-if="profile.data.metadata.namespace == currentNamespace">
										<router-link :to="'/profiles/'+profile.data.metadata.namespace+'/'+profile.name" class="item" :class="profile.name">{{ profile.name }}</router-link>
									</li>
								</template>
								<li><router-link to="/crd/create/profile/" class="addnew item">Add New</router-link></li>
							</ul>
						</div>
					</ul>
				</div>
				<div class="set backups">
					<router-link :to="'/backups/'+currentNamespace" title="Backups" class="nav-item">
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M10.55.55A9.454 9.454 0 001.125 9.5H.479a.458.458 0 00-.214.053.51.51 0 00-.214.671l1.621 3.382a.49.49 0 00.213.223.471.471 0 00.644-.223l1.62-3.382A.51.51 0 004.2 10a.49.49 0 00-.479-.5H3.1a7.47 7.47 0 117.449 7.974 7.392 7.392 0 01-3.332-.781.988.988 0 00-.883 1.767 9.356 9.356 0 004.215.99 9.45 9.45 0 000-18.9z"/><path class="a" d="M13.554 10a3 3 0 10-3 3 3 3 0 003-3z"/></svg>
						<h3>Backups</h3>
					</router-link>
				</div>
			</div>
			<div id="expanders">
				<div class="collapse">
					<svg xmlns="http://www.w3.org/2000/svg" width="11" height="13"><g><path d="M4.763 7.8a1 1 0 011.474 0l3.227 3.52A1 1 0 018.727 13H2.273a1 1 0 01-.737-1.676zM6.237 5.2a1 1 0 01-1.474 0L1.536 1.68A1 1 0 012.273 0h6.454a1 1 0 01.737 1.676z"/></g></svg>
					Collapse All
				</div>
				<div class="expand">
					<svg xmlns="http://www.w3.org/2000/svg" width="11" height="14"><g><path d="M6.237 13.2a1 1 0 01-1.474 0L1.536 9.68A1 1 0 012.273 8h6.454a1 1 0 01.737 1.676zM4.763.8a1 1 0 011.474 0l3.227 3.52A1 1 0 018.727 6H2.273a1 1 0 01-.737-1.676z"/></g></svg>
					Expand All
				</div>
			</div>

			<!--<footer id="credits">
				<span>STACK<strong>GRES</strong></span> coded with <span class="hearth">❤</span>︎ by Ongres
			</footer>-->
		</aside>`,
	data: function() {

		return {
			//clusters: null,
		}

	},

	computed: {
		namespaces () {
			return store.state.namespaces
		},

		clusters () {
			return store.state.clusters
		},

		backups () {
			return store.state.backups;
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

		pgConfig () {
			return store.state.pgConfig;
		},

		poolConfig () {
			return store.state.poolConfig;
		},

		bkConfig () {
			return store.state.backupConfig;
		},

		profiles () {
			return store.state.profiles;
		},

		hasClusters () {
			let index = store.state.clusters.find(c => (store.state.currentNamespace == c.data.metadata.namespace) );

			if(typeof index !== "undefined")
				return true
			else
				return false
		},

		hasPGConfig () {
			let index = store.state.pgConfig.find(c => (store.state.currentNamespace == c.data.metadata.namespace) );

			if(typeof index !== "undefined")
				return true
			else
				return false
		},

		hasPoolConfig () {
			let index = store.state.poolConfig.find(c => (store.state.currentNamespace == c.data.metadata.namespace) );

			if(typeof index !== "undefined")
				return true
			else
				return false
		},

		hasBackupConfig () {
			let index = store.state.backupConfig.find(c => (store.state.currentNamespace == c.data.metadata.namespace) );

			if(typeof index !== "undefined")
				return true
			else
				return false
		},

		hasProfiles () {
			let index = store.state.profiles.find(c => (store.state.currentNamespace == c.data.metadata.namespace) );

			if(typeof index !== "undefined")
				return true
			else
				return false
		}
	}
})