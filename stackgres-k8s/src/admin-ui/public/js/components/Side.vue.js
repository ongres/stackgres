var Side = Vue.component("sg-side", {
	template: `
		<aside id="side" :class="currentNamespace" v-if="loggedIn && isReady">
			<div id="current-namespace">
				<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
				<h2>
					<strong>NAMESPACE <span>▾</span></strong>
					<br/>
					{{ currentNamespace }}
				</h2>
			</div>

			<ul id="ns-select" tabindex="0" class="set namespaces">
				<template v-for="namespace in namespaces">
					<li v-bind:class="{'active':(namespace == currentNamespace)}">
						<router-link :to="'/admin/overview/'+namespace" class="item namespace" :class="(namespace == currentNamespace) ? 'router-link-exact-active' : ''">{{ namespace }}</router-link>
					</li>
				</template>
			</ul>
			<div id="sets">
				<div class="set clu" :class="$route.params.hasOwnProperty('cluster') ? 'active' : ''">
					<router-link :to="'/admin/overview/'+currentNamespace" title="Overview" class="view nav-item">
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
						<h3>Stackgres Clusters</h3>
					</router-link>

					<router-link to="/admin/crd/create/cluster/" class="addnew"><svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 19 19"><g transform="translate(-573 -706)"><g transform="translate(573 706)" fill="none" stroke="#00adb5" stroke-width="2"><circle cx="9.5" cy="9.5" r="9.5" stroke="none"/><circle cx="9.5" cy="9.5" r="8.5" fill="none"/></g><g transform="translate(-30.5 28.8)"><g transform="translate(609 686)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g><g transform="translate(613.7 682.7) rotate(90)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g></g></g></svg></router-link>
					<ul>
						<template v-for="cluster in clusters">
							<li v-if="cluster.data.metadata.namespace == currentNamespace" :class="'sgcluster-'+cluster.data.metadata.namespace+'-'+cluster.name">
								<router-link :to="'/admin/cluster/status/'+cluster.data.metadata.namespace+'/'+cluster.name" class="item cluster" :title="cluster.name">{{ cluster.name }}</router-link>
							</li>
						</template>
					</ul>

				</div>
				<div class="prof set" :class="$route.matched[0].components.default.options.name == 'InstanceProfile' ? 'active' : ''">
					<router-link :to="'/admin/profiles/'+currentNamespace" class="nav-item">
					<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"><g transform="translate(0 -242)"><path d="M19.649,256.971l-1.538-1.3a.992.992,0,1,0-1.282,1.514l.235.2-6.072,2.228v-4.373l.266.154a.974.974,0,0,0,.491.132.99.99,0,0,0,.862-.506,1.012,1.012,0,0,0-.369-1.372l-1.75-1.013a.983.983,0,0,0-.984,0l-1.75,1.013a1.012,1.012,0,0,0-.369,1.372.985.985,0,0,0,1.353.374l.266-.154v4.353l-6.07-2.21.233-.2a.992.992,0,1,0-1.282-1.514l-1.538,1.3a.992.992,0,0,0-.337.925l.342,1.987a.992.992,0,0,0,.977.824.981.981,0,0,0,.169-.015.992.992,0,0,0,.81-1.145l-.052-.3,7.4,2.694A1.011,1.011,0,0,0,10,262c.01,0,.02,0,.03-.005s.02.005.03.005a1,1,0,0,0,.342-.061l7.335-2.691-.051.3a.992.992,0,0,0,.811,1.145.953.953,0,0,0,.168.015.992.992,0,0,0,.977-.824l.341-1.987A.992.992,0,0,0,19.649,256.971Z" fill="#00adb5"/><path d="M20,246.25a.99.99,0,0,0-.655-.93l-9-3.26a1,1,0,0,0-.681,0l-9,3.26a.99.99,0,0,0-.655.93.9.9,0,0,0,.016.1c0,.031-.016.057-.016.089v5.886a1.052,1.052,0,0,0,.992,1.1,1.052,1.052,0,0,0,.992-1.1v-4.667l7.676,2.779a1.012,1.012,0,0,0,.681,0l7.675-2.779v4.667a1,1,0,1,0,1.984,0v-5.886c0-.032-.014-.058-.016-.089A.9.9,0,0,0,20,246.25Zm-10,2.207L3.9,246.25l6.1-2.206,6.095,2.206Z" fill="#00adb5"/></g></svg>
						<h4>Instance Profiles</h4>
					</router-link>

					<router-link to="/admin/crd/create/profile/" class="addnew"><svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 19 19"><g transform="translate(-573 -706)"><g transform="translate(573 706)" fill="none" stroke="#00adb5" stroke-width="2"><circle cx="9.5" cy="9.5" r="9.5" stroke="none"/><circle cx="9.5" cy="9.5" r="8.5" fill="none"/></g><g transform="translate(-30.5 28.8)"><g transform="translate(609 686)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g><g transform="translate(613.7 682.7) rotate(90)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g></g></g></svg></router-link>
					<ul>
						<template v-for="profile in profiles">
							<li v-if="profile.data.metadata.namespace == currentNamespace" :class="'profile-'+profile.data.metadata.namespace+'-'+profile.name">
								<router-link :to="'/admin/profiles/'+profile.data.metadata.namespace+'/'+profile.name" class="item" :title="profile.name">{{ profile.name }}</router-link>
							</li>
						</template>
					</ul>
					
				</div>
				<div class="set conf active">
					<div class="nav-item">
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 21 20"><path d="M.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm19.09-1.905h-10.5a.953.953 0 100 1.9h10.5a.953.953 0 100-1.9zm0 6.191h-8.59v-1.9a.955.955 0 00-1.91 0v5.715a.955.955 0 001.91 0v-1.9h8.59a.953.953 0 100-1.905zm-12.409 0H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"/></svg>
						<h3>Configurations</h3>
					</div>

					<ul>
						<div class="pg set" :class="$route.matched[0].components.default.options.name == 'PostgresConfig' ? 'active' : ''">
							<router-link :to="'/admin/configurations/postgres/'+currentNamespace" class="nav-item">
								<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.7 20"><path class="a" d="M10.946 18.7a.841.841 0 01-.622-.234.862.862 0 01-.234-.635v-7.817a.8.8 0 01.221-.6.834.834 0 01.608-.214h3.29a3.4 3.4 0 012.353.755 2.7 2.7 0 01.843 2.12 2.72 2.72 0 01-.843 2.126 3.379 3.379 0 01-2.353.764h-2.394v2.875a.8.8 0 01-.869.867zM14 13.637q1.778 0 1.778-1.551T14 10.535h-2.18v3.1zm11.968-.107a.683.683 0 01.494.181.625.625 0 01.191.477v2.875a1.717 1.717 0 01-.16.87 1.174 1.174 0 01-.655.414 6.882 6.882 0 01-1.242.294 9.023 9.023 0 01-1.364.107 5.252 5.252 0 01-2.527-.573 3.883 3.883 0 01-1.638-1.665 5.548 5.548 0 01-.569-2.6 5.5 5.5 0 01.569-2.575 3.964 3.964 0 011.611-1.671 4.965 4.965 0 012.455-.59 4.62 4.62 0 013.089 1.016 1.058 1.058 0 01.234.294.854.854 0 01-.087.843.479.479 0 01-.388.2.737.737 0 01-.267-.047 1.5 1.5 0 01-.281-.153 4.232 4.232 0 00-1.1-.582 3.648 3.648 0 00-1.146-.167 2.747 2.747 0 00-2.2.859 3.834 3.834 0 00-.742 2.561q0 3.477 3.049 3.477a6.752 6.752 0 001.815-.254v-2.36h-1.517a.737.737 0 01-.5-.161.664.664 0 010-.909.732.732 0 01.5-.161zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"/></svg>
								<h4>Postgres</h4>
							</router-link>

							<router-link to="/admin/crd/create/pgconfig/" class="addnew"><svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 19 19"><g transform="translate(-573 -706)"><g transform="translate(573 706)" fill="none" stroke="#00adb5" stroke-width="2"><circle cx="9.5" cy="9.5" r="9.5" stroke="none"/><circle cx="9.5" cy="9.5" r="8.5" fill="none"/></g><g transform="translate(-30.5 28.8)"><g transform="translate(609 686)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g><g transform="translate(613.7 682.7) rotate(90)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g></g></g></svg></router-link>			
							<ul>
								<template v-for="config in pgConfig">
									<li v-if="config.data.metadata.namespace == currentNamespace" :class="'sgpgconfig-'+config.data.metadata.namespace+'-'+config.name">
										<router-link :to="'/admin/configurations/postgres/'+config.data.metadata.namespace+'/'+config.name" class="item pgconfig" :title="config.name">{{ config.name }}</router-link>
									</li>
								</template>
							</ul>			
						</div>

						<div class="pool set" :class="$route.matched[0].components.default.options.name == 'PoolConfig' ? 'active' : ''">
							<router-link :to="'/admin/configurations/connectionpooling/'+currentNamespace" class="nav-item">
								<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.5 20"><path d="M14.305 18.749a4.7 4.7 0 01-2.388-.589 3.91 3.91 0 01-1.571-1.685 5.668 5.668 0 01-.546-2.568 5.639 5.639 0 01.548-2.561 3.916 3.916 0 011.571-1.678 4.715 4.715 0 012.388-.593 5.189 5.189 0 011.658.261 4.324 4.324 0 011.378.756.758.758 0 01.24.281.859.859 0 01.067.361.768.768 0 01-.16.495.479.479 0 01-.388.2.984.984 0 01-.548-.191 4 4 0 00-1.07-.595 3.405 3.405 0 00-1.1-.167 2.571 2.571 0 00-2.106.869 3.943 3.943 0 00-.72 2.562 3.963 3.963 0 00.716 2.568 2.568 2.568 0 002.106.869 3.147 3.147 0 001.063-.173 5.112 5.112 0 001.1-.589 2.018 2.018 0 01.267-.134.751.751 0 01.29-.048.477.477 0 01.388.2.767.767 0 01.16.494.863.863 0 01-.067.355.739.739 0 01-.24.286 4.308 4.308 0 01-1.378.757 5.161 5.161 0 01-1.658.257zm5.71-.04a.841.841 0 01-.622-.234.856.856 0 01-.234-.636v-7.824a.8.8 0 01.22-.6.835.835 0 01.609-.214h3.29a3.4 3.4 0 012.354.755 2.7 2.7 0 01.842 2.12 2.725 2.725 0 01-.842 2.127 3.386 3.386 0 01-2.354.764h-2.393v2.875a.8.8 0 01-.87.868zm3.05-5.069q1.779 0 1.779-1.552t-1.779-1.551h-2.18v3.1zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"/></svg>
								<h4>Connection Pooling</h4>
							</router-link>

							<ul>
								<template v-for="config in poolConfig">
									<li v-if="config.data.metadata.namespace == currentNamespace" :class="'sgpoolconfig-'+config.data.metadata.namespace+'-'+config.name">
										<router-link :to="'/admin/configurations/connectionpooling/'+config.data.metadata.namespace+'/'+config.name" class="item" :title="config.name">{{ config.name }}</router-link>
									</li>
								</template>
							</ul>
							<router-link to="/admin/crd/create/connectionpooling/" class="addnew"><svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 19 19"><g transform="translate(-573 -706)"><g transform="translate(573 706)" fill="none" stroke="#00adb5" stroke-width="2"><circle cx="9.5" cy="9.5" r="9.5" stroke="none"/><circle cx="9.5" cy="9.5" r="8.5" fill="none"/></g><g transform="translate(-30.5 28.8)"><g transform="translate(609 686)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g><g transform="translate(613.7 682.7) rotate(90)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g></g></g></svg></router-link>
						</div>

						<div class="backup set" :class="$route.matched[0].components.default.options.name == 'BackupConfig' ? 'active' : ''">
							<router-link :to="'/admin/configurations/backup/'+currentNamespace" class="nav-item">
							<svg xmlns="http://www.w3.org/2000/svg" width="26.5" height="18.747" viewBox="0 0 26.5 18.747"><g transform="translate(-60 -212.633)"><path d="M60.955,217.4h10.5a.953.953,0,1,0,0-1.906h-10.5a.953.953,0,1,0,0,1.906Z" fill="#00adb5"/><path d="M74.795,220.258a.953.953,0,0,0,.955-.953V217.4h4.295a.953.953,0,1,0,0-1.906H75.75v-1.907a.954.954,0,0,0-1.909,0V219.3A.953.953,0,0,0,74.795,220.258Z" fill="#00adb5"/><path d="M60.955,223.6H65.25V225.5a.954.954,0,0,0,1.909,0v-5.719a.954.954,0,0,0-1.909,0v1.906H60.955a.954.954,0,1,0,0,1.907Z" fill="#00adb5"/><path d="M67.636,227.884H60.955a.954.954,0,1,0,0,1.907h6.681a.954.954,0,1,0,0-1.907Z" fill="#00adb5"/><path d="M78.073,222.114h0a.852.852,0,0,0-.668-.293.944.944,0,0,0-.86.667L74.2,227.5l-2.354-5.011a.959.959,0,0,0-.883-.669.834.834,0,0,0-.663.3,1.09,1.09,0,0,0-.238.726v7.568a1.037,1.037,0,0,0,.22.692.776.776,0,0,0,.624.278.787.787,0,0,0,.631-.284,1.038,1.038,0,0,0,.225-.686V226.1l1.568,3.248a1.318,1.318,0,0,0,.355.5.819.819,0,0,0,1.012-.01,1.458,1.458,0,0,0,.35-.486l1.557-3.3v4.361a1.037,1.037,0,0,0,.22.692.776.776,0,0,0,.623.278.823.823,0,0,0,.632-.272,1.009,1.009,0,0,0,.235-.7v-7.568A1.081,1.081,0,0,0,78.073,222.114Z" fill="#00adb5"/><path d="M86.1,227.268a2.6,2.6,0,0,1,.4,1.469,2.388,2.388,0,0,1-.77,1.885,3.09,3.09,0,0,1-2.12.681H80.531a.7.7,0,0,1-.543-.214.849.849,0,0,1-.2-.6V222.7a.851.851,0,0,1,.2-.6.7.7,0,0,1,.543-.214h2.96a3.041,3.041,0,0,1,2.06.648,2.274,2.274,0,0,1,.746,1.811,2.354,2.354,0,0,1-.352,1.3,2.047,2.047,0,0,1-.973.8A2.038,2.038,0,0,1,86.1,227.268Zm-4.806-1.417h1.947q1.587,0,1.587-1.322a1.2,1.2,0,0,0-.393-.99,1.872,1.872,0,0,0-1.194-.32H81.294Zm3.367,3.782a1.311,1.311,0,0,0,.382-1.042,1.349,1.349,0,0,0-.387-1.056,1.782,1.782,0,0,0-1.213-.347H81.294v2.779h2.149A1.828,1.828,0,0,0,84.661,229.633Z" fill="#00adb5"/></g></svg>
								<h4>Managed Backups</h4>
							</router-link>

							<router-link to="/admin/crd/create/backupconfig/" class="addnew"><svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 19 19"><g transform="translate(-573 -706)"><g transform="translate(573 706)" fill="none" stroke="#00adb5" stroke-width="2"><circle cx="9.5" cy="9.5" r="9.5" stroke="none"/><circle cx="9.5" cy="9.5" r="8.5" fill="none"/></g><g transform="translate(-30.5 28.8)"><g transform="translate(609 686)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g><g transform="translate(613.7 682.7) rotate(90)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g></g></g></svg></router-link>
							<ul>
								<template v-for="config in bkConfig">
									<li v-if="config.data.metadata.namespace == currentNamespace" :class="'sgbackupconfig-'+config.data.metadata.namespace+'-'+config.name">
										<router-link :to="'/admin/configurations/backup/'+config.data.metadata.namespace+'/'+config.name" class="item" :title="config.name">{{ config.name }}</router-link>
									</li>
								</template>
							</ul>
						</div>
					</ul>
				</div>
				<div class="set backups">
					<router-link :to="'/admin/backups/'+currentNamespace" title="Backups" class="nav-item">
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M10.55.55A9.454 9.454 0 001.125 9.5H.479a.458.458 0 00-.214.053.51.51 0 00-.214.671l1.621 3.382a.49.49 0 00.213.223.471.471 0 00.644-.223l1.62-3.382A.51.51 0 004.2 10a.49.49 0 00-.479-.5H3.1a7.47 7.47 0 117.449 7.974 7.392 7.392 0 01-3.332-.781.988.988 0 00-.883 1.767 9.356 9.356 0 004.215.99 9.45 9.45 0 000-18.9z"/><path class="a" d="M13.554 10a3 3 0 10-3 3 3 3 0 003-3z"/></svg>
						<h3>Cluster Backups</h3>
					</router-link>
					<router-link :to="'/admin/crd/create/backup/'+currentNamespace" class="addnew"><svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 19 19"><g transform="translate(-573 -706)"><g transform="translate(573 706)" fill="none" stroke="#00adb5" stroke-width="2"><circle cx="9.5" cy="9.5" r="9.5" stroke="none"/><circle cx="9.5" cy="9.5" r="8.5" fill="none"/></g><g transform="translate(-30.5 28.8)"><g transform="translate(609 686)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g><g transform="translate(613.7 682.7) rotate(90)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g></g></g></svg></router-link>
				</div>
				<div class="set logs" v-if="showLogs">
					<router-link :to="'/admin/logs/'" title="Centralized Logs" class="nav-item">
					<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M19,15H5c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,14.6,19.6,15,19,15z"/><path class="a" d="M1,15L1,15c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,14.6,1.6,15,1,15z"/><path class="a" d="M19,11H5c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,10.6,19.6,11,19,11z"/><path class="a" d="M1,11L1,11c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,10.6,1.6,11,1,11z"/><path class="a" d="M19,7H5C4.4,7,4,6.6,4,6v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,6.6,19.6,7,19,7z"/><path d="M1,7L1,7C0.4,7,0,6.6,0,6v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,6.6,1.6,7,1,7z"/></svg>
						<h3>Centralized Logs</h3>
					</router-link>
				</div>
			</div>
			<!--
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
			-->

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
			return store.state.allNamespaces
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

		currentCluster () {
			return store.state.currentCluster
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
		},

		loggedIn () {
			if (typeof store.state.loginToken !== 'undefined')
				return store.state.loginToken.length > 0
			else
				return false
		},

		showLogs () {
			return store.state.showLogs
		},

		notFound () {
			return store.state.notFound
		},

		isReady () {
			return store.state.ready
		}
	}
})