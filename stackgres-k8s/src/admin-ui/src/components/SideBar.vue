<template>
	<aside id="side" :class="currentPath.namespace" v-if="loggedIn && isReady">
		<div id="ns-set">
			<ul>
				<li id="current-namespace">
					<span><svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg></span>
					<h2>
						<template v-if="!isCollapsed">
							<strong>{{ !currentPath.namespace.length ? 'SELECT' : '' }} NAMESPACE <span>▾</span></strong>
							<br/>

							<template v-if="!notFound">
								{{ currentPath.namespace }}
							</template>
						</template>
					</h2>
				</li>

				<li><ul id="ns-select" tabindex="0" class="set namespaces">
					<li>
						<router-link :to="'/'" class="item">
							Stackgres Namespaces
						</router-link>
					</li>
					<template v-for="namespace in namespaces">
						<li :class="{'active':(namespace == currentPath.namespace)}">
							<router-link :to="'/' + namespace" class="item namespace" :class="(namespace == currentPath.namespace) ? 'router-link-exact-active' : ''" :title="namespace">{{ namespace }}</router-link>
						</li>
					</template>
				</ul></li>
			</ul>
		</div>

		<div id="sets" v-if="!notFound && currentPath.namespace.length">
			<div v-if="iCan('','sgclusters')" class="set clu" :class="currentPath.component.includes('Cluster') ? 'active' : ''" >
				<ul>
					<li class="crdName">
						<router-link :to="'/' + currentPath.namespace + '/sgclusters'" title="Clusters Overview" class="view nav-item">
							<span><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg></span>
							<h3 :class="isCollapsed ? 'submenuTitle' : ''">Stackgres Clusters</h3>
						</router-link>

						<router-link :to="'/' + currentPath.namespace + '/sgclusters/new'" class="addnew" v-if="iCan('create', 'sgclusters', currentPath.namespace)">
							<svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 19 19"><g transform="translate(-573 -706)"><g transform="translate(573 706)" fill="none" stroke="#00adb5" stroke-width="2"><circle cx="9.5" cy="9.5" r="9.5" stroke="none"/><circle cx="9.5" cy="9.5" r="8.5" fill="none"/></g><g transform="translate(-30.5 28.8)"><g transform="translate(609 686)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g><g transform="translate(613.7 682.7) rotate(90)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g></g></g></svg>
						</router-link>
					</li>

					<li><ul v-if="iCan('list', 'sgclusters', currentPath.namespace)" class="crdSubmenu">
						<template v-for="cluster in clusters">
							<li v-if="cluster.data.metadata.namespace == currentPath.namespace" :class="'sgcluster-'+cluster.data.metadata.namespace+'-'+cluster.name">
								<router-link :to="'/' + cluster.data.metadata.namespace + '/sgcluster/' + cluster.name" class="item cluster" :title="cluster.name" :class="(currentPath.component.includes('Cluster') && (currentPath.name == cluster.name)) ? 'router-link-exact-active' : ''">
									{{ cluster.name }}
									<template v-if="hasProp(cluster, 'data.status.conditions')">
										<template v-for="condition in cluster.data.status.conditions" v-if="( (condition.type == 'PendingRestart') && (condition.status == 'True') )">
											<div class="helpTooltip alert onHover" data-tooltip="A restart operation is pending for this cluster"></div>
										</template>
									</template>
								</router-link>
							</li>
						</template>
					</ul></li>
				</ul>
			</div>

			<div v-if="iCan('','sginstanceprofiles')" class="prof set" :class="currentPath.component.includes('Profile') ? 'active' : ''">
				<ul>
					<li class="crdName">
						<router-link :to="'/' + currentPath.namespace + '/sginstanceprofiles'" class="nav-item">
							<span><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"><g transform="translate(0 -242)"><path d="M19.649,256.971l-1.538-1.3a.992.992,0,1,0-1.282,1.514l.235.2-6.072,2.228v-4.373l.266.154a.974.974,0,0,0,.491.132.99.99,0,0,0,.862-.506,1.012,1.012,0,0,0-.369-1.372l-1.75-1.013a.983.983,0,0,0-.984,0l-1.75,1.013a1.012,1.012,0,0,0-.369,1.372.985.985,0,0,0,1.353.374l.266-.154v4.353l-6.07-2.21.233-.2a.992.992,0,1,0-1.282-1.514l-1.538,1.3a.992.992,0,0,0-.337.925l.342,1.987a.992.992,0,0,0,.977.824.981.981,0,0,0,.169-.015.992.992,0,0,0,.81-1.145l-.052-.3,7.4,2.694A1.011,1.011,0,0,0,10,262c.01,0,.02,0,.03-.005s.02.005.03.005a1,1,0,0,0,.342-.061l7.335-2.691-.051.3a.992.992,0,0,0,.811,1.145.953.953,0,0,0,.168.015.992.992,0,0,0,.977-.824l.341-1.987A.992.992,0,0,0,19.649,256.971Z" fill="#00adb5"/><path d="M20,246.25a.99.99,0,0,0-.655-.93l-9-3.26a1,1,0,0,0-.681,0l-9,3.26a.99.99,0,0,0-.655.93.9.9,0,0,0,.016.1c0,.031-.016.057-.016.089v5.886a1.052,1.052,0,0,0,.992,1.1,1.052,1.052,0,0,0,.992-1.1v-4.667l7.676,2.779a1.012,1.012,0,0,0,.681,0l7.675-2.779v4.667a1,1,0,1,0,1.984,0v-5.886c0-.032-.014-.058-.016-.089A.9.9,0,0,0,20,246.25Zm-10,2.207L3.9,246.25l6.1-2.206,6.095,2.206Z" fill="#00adb5"/></g></svg></span>
							<h3 :class="isCollapsed ? 'submenuTitle' : ''">Instance Profiles</h3>
						</router-link>

						<router-link :to="'/' + currentPath.namespace + '/sginstanceprofiles/new'" class="addnew" v-if="iCan('create', 'sginstanceprofiles', currentPath.namespace)">
							<svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 19 19"><g transform="translate(-573 -706)"><g transform="translate(573 706)" fill="none" stroke="#00adb5" stroke-width="2"><circle cx="9.5" cy="9.5" r="9.5" stroke="none"/><circle cx="9.5" cy="9.5" r="8.5" fill="none"/></g><g transform="translate(-30.5 28.8)"><g transform="translate(609 686)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g><g transform="translate(613.7 682.7) rotate(90)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g></g></g></svg>
						</router-link>
					</li>

					<li><ul v-if="iCan('list', 'sginstanceprofiles', currentPath.namespace)" class="crdSubmenu">
						<template v-for="profile in profiles">
							<li v-if="profile.data.metadata.namespace == currentPath.namespace" :class="'profile-'+profile.data.metadata.namespace+'-'+profile.name">
								<router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + profile.name" class="item" :title="profile.name" :class="(currentPath.component.includes('Profile') && (currentPath.name == profile.name)) ? 'router-link-exact-active' : ''">{{ profile.name }}</router-link>
							</li>
						</template>
					</ul></li>
				</ul>
			</div>
			
			<div v-if="!isCollapsed" class="set conf">
				<div class="nav-item noHoverPointer">
					<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 21 20"><path d="M.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm19.09-1.905h-10.5a.953.953 0 100 1.9h10.5a.953.953 0 100-1.9zm0 6.191h-8.59v-1.9a.955.955 0 00-1.91 0v5.715a.955.955 0 001.91 0v-1.9h8.59a.953.953 0 100-1.905zm-12.409 0H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"/></svg>
					<h3 class="noHoverPointer">Configurations</h3>
				</div>
			</div>
				
			<div v-if="iCan('','sgpgconfigs')" class="pg set subset" :class="currentPath.component.includes('PgConfig') ? 'active' : ''" >
				<ul>
					<li class="crdName">
						<router-link :to="'/' + currentPath.namespace + '/sgpgconfigs'" class="nav-item">
							<span><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.7 20"><path class="a" d="M10.946 18.7a.841.841 0 01-.622-.234.862.862 0 01-.234-.635v-7.817a.8.8 0 01.221-.6.834.834 0 01.608-.214h3.29a3.4 3.4 0 012.353.755 2.7 2.7 0 01.843 2.12 2.72 2.72 0 01-.843 2.126 3.379 3.379 0 01-2.353.764h-2.394v2.875a.8.8 0 01-.869.867zM14 13.637q1.778 0 1.778-1.551T14 10.535h-2.18v3.1zm11.968-.107a.683.683 0 01.494.181.625.625 0 01.191.477v2.875a1.717 1.717 0 01-.16.87 1.174 1.174 0 01-.655.414 6.882 6.882 0 01-1.242.294 9.023 9.023 0 01-1.364.107 5.252 5.252 0 01-2.527-.573 3.883 3.883 0 01-1.638-1.665 5.548 5.548 0 01-.569-2.6 5.5 5.5 0 01.569-2.575 3.964 3.964 0 011.611-1.671 4.965 4.965 0 012.455-.59 4.62 4.62 0 013.089 1.016 1.058 1.058 0 01.234.294.854.854 0 01-.087.843.479.479 0 01-.388.2.737.737 0 01-.267-.047 1.5 1.5 0 01-.281-.153 4.232 4.232 0 00-1.1-.582 3.648 3.648 0 00-1.146-.167 2.747 2.747 0 00-2.2.859 3.834 3.834 0 00-.742 2.561q0 3.477 3.049 3.477a6.752 6.752 0 001.815-.254v-2.36h-1.517a.737.737 0 01-.5-.161.664.664 0 010-.909.732.732 0 01.5-.161zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"/></svg></span>
							<h4 :class="isCollapsed ? 'submenuTitle' : ''">Postgres</h4>
						</router-link>

						<router-link :to="'/' + currentPath.namespace + '/sgpgconfigs/new'" class="addnew" v-if="iCan('create', 'sgpgconfigs', currentPath.namespace)">
							<svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 19 19"><g transform="translate(-573 -706)"><g transform="translate(573 706)" fill="none" stroke="#00adb5" stroke-width="2"><circle cx="9.5" cy="9.5" r="9.5" stroke="none"/><circle cx="9.5" cy="9.5" r="8.5" fill="none"/></g><g transform="translate(-30.5 28.8)"><g transform="translate(609 686)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g><g transform="translate(613.7 682.7) rotate(90)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g></g></g></svg>
						</router-link>
					</li>

					<li><ul v-if="iCan('list', 'sgpgconfigs', currentPath.namespace)" class="crdSubmenu">
						<template v-for="config in pgConfig">
							<li v-if="config.data.metadata.namespace == currentPath.namespace" :class="'sgpgconfig-'+config.data.metadata.namespace+'-'+config.name">
								<router-link :to="'/' + config.data.metadata.namespace + '/sgpgconfig/' + config.name" class="item pgconfig" :title="config.name" :class="(currentPath.component.includes('PgConfig') && (currentPath.name == config.name)) ? 'router-link-exact-active' : ''">{{ config.name }}</router-link>
							</li>
						</template>
					</ul></li>
				</ul>		
			</div>

			<div v-if="iCan('','sgpoolconfigs')" class="pool set subset" :class="currentPath.component.includes('PoolConfig') ? 'active' : ''" >
				<ul>
					<li class="crdName">
						<router-link :to="'/' + currentPath.namespace + '/sgpoolconfigs'" class="nav-item">
							<span><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.5 20"><path d="M14.305 18.749a4.7 4.7 0 01-2.388-.589 3.91 3.91 0 01-1.571-1.685 5.668 5.668 0 01-.546-2.568 5.639 5.639 0 01.548-2.561 3.916 3.916 0 011.571-1.678 4.715 4.715 0 012.388-.593 5.189 5.189 0 011.658.261 4.324 4.324 0 011.378.756.758.758 0 01.24.281.859.859 0 01.067.361.768.768 0 01-.16.495.479.479 0 01-.388.2.984.984 0 01-.548-.191 4 4 0 00-1.07-.595 3.405 3.405 0 00-1.1-.167 2.571 2.571 0 00-2.106.869 3.943 3.943 0 00-.72 2.562 3.963 3.963 0 00.716 2.568 2.568 2.568 0 002.106.869 3.147 3.147 0 001.063-.173 5.112 5.112 0 001.1-.589 2.018 2.018 0 01.267-.134.751.751 0 01.29-.048.477.477 0 01.388.2.767.767 0 01.16.494.863.863 0 01-.067.355.739.739 0 01-.24.286 4.308 4.308 0 01-1.378.757 5.161 5.161 0 01-1.658.257zm5.71-.04a.841.841 0 01-.622-.234.856.856 0 01-.234-.636v-7.824a.8.8 0 01.22-.6.835.835 0 01.609-.214h3.29a3.4 3.4 0 012.354.755 2.7 2.7 0 01.842 2.12 2.725 2.725 0 01-.842 2.127 3.386 3.386 0 01-2.354.764h-2.393v2.875a.8.8 0 01-.87.868zm3.05-5.069q1.779 0 1.779-1.552t-1.779-1.551h-2.18v3.1zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"/></svg></span>
							<h4 :class="isCollapsed ? 'submenuTitle' : ''">Connection Pooling</h4>
						</router-link>

						<router-link :to="'/' + currentPath.namespace + '/sgpoolconfigs/new'" class="addnew" v-if="iCan('create', 'sgpoolconfigs', currentPath.namespace)">
							<svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 19 19"><g transform="translate(-573 -706)"><g transform="translate(573 706)" fill="none" stroke="#00adb5" stroke-width="2"><circle cx="9.5" cy="9.5" r="9.5" stroke="none"/><circle cx="9.5" cy="9.5" r="8.5" fill="none"/></g><g transform="translate(-30.5 28.8)"><g transform="translate(609 686)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g><g transform="translate(613.7 682.7) rotate(90)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g></g></g></svg>
						</router-link>
					</li>

					<li><ul v-if="iCan('list', 'sgpoolconfigs', currentPath.namespace)" class="crdSubmenu">
						<template v-for="config in poolConfig">
							<li v-if="config.data.metadata.namespace == currentPath.namespace" :class="'sgpoolconfig-'+config.data.metadata.namespace+'-'+config.name">
								<router-link :to="'/' + config.data.metadata.namespace + '/sgpoolconfig/' + config.name" class="item" :title="config.name" :class="(currentPath.component.includes('PoolConfig') && (currentPath.name == config.name)) ? 'router-link-exact-active' : ''">{{ config.name }}</router-link>
							</li>
						</template>
					</ul></li>
				</ul>
			</div>

			<div v-if="iCan('','sgbackupconfigs')" class="backup set subset" :class="currentPath.component.includes('BackupConfig') ? 'active' : ''" >
				<ul>
					<li class="crdName">
						<router-link :to="'/' + currentPath.namespace + '/sgbackupconfigs'" class="nav-item">
							<span><svg xmlns="http://www.w3.org/2000/svg" width="26.5" height="18.747" viewBox="0 0 26.5 18.747"><g transform="translate(-60 -212.633)"><path d="M60.955,217.4h10.5a.953.953,0,1,0,0-1.906h-10.5a.953.953,0,1,0,0,1.906Z" fill="#00adb5"/><path d="M74.795,220.258a.953.953,0,0,0,.955-.953V217.4h4.295a.953.953,0,1,0,0-1.906H75.75v-1.907a.954.954,0,0,0-1.909,0V219.3A.953.953,0,0,0,74.795,220.258Z" fill="#00adb5"/><path d="M60.955,223.6H65.25V225.5a.954.954,0,0,0,1.909,0v-5.719a.954.954,0,0,0-1.909,0v1.906H60.955a.954.954,0,1,0,0,1.907Z" fill="#00adb5"/><path d="M67.636,227.884H60.955a.954.954,0,1,0,0,1.907h6.681a.954.954,0,1,0,0-1.907Z" fill="#00adb5"/><path d="M78.073,222.114h0a.852.852,0,0,0-.668-.293.944.944,0,0,0-.86.667L74.2,227.5l-2.354-5.011a.959.959,0,0,0-.883-.669.834.834,0,0,0-.663.3,1.09,1.09,0,0,0-.238.726v7.568a1.037,1.037,0,0,0,.22.692.776.776,0,0,0,.624.278.787.787,0,0,0,.631-.284,1.038,1.038,0,0,0,.225-.686V226.1l1.568,3.248a1.318,1.318,0,0,0,.355.5.819.819,0,0,0,1.012-.01,1.458,1.458,0,0,0,.35-.486l1.557-3.3v4.361a1.037,1.037,0,0,0,.22.692.776.776,0,0,0,.623.278.823.823,0,0,0,.632-.272,1.009,1.009,0,0,0,.235-.7v-7.568A1.081,1.081,0,0,0,78.073,222.114Z" fill="#00adb5"/><path d="M86.1,227.268a2.6,2.6,0,0,1,.4,1.469,2.388,2.388,0,0,1-.77,1.885,3.09,3.09,0,0,1-2.12.681H80.531a.7.7,0,0,1-.543-.214.849.849,0,0,1-.2-.6V222.7a.851.851,0,0,1,.2-.6.7.7,0,0,1,.543-.214h2.96a3.041,3.041,0,0,1,2.06.648,2.274,2.274,0,0,1,.746,1.811,2.354,2.354,0,0,1-.352,1.3,2.047,2.047,0,0,1-.973.8A2.038,2.038,0,0,1,86.1,227.268Zm-4.806-1.417h1.947q1.587,0,1.587-1.322a1.2,1.2,0,0,0-.393-.99,1.872,1.872,0,0,0-1.194-.32H81.294Zm3.367,3.782a1.311,1.311,0,0,0,.382-1.042,1.349,1.349,0,0,0-.387-1.056,1.782,1.782,0,0,0-1.213-.347H81.294v2.779h2.149A1.828,1.828,0,0,0,84.661,229.633Z" fill="#00adb5"/></g></svg></span>
							<h4 :class="isCollapsed ? 'submenuTitle' : ''">Managed Backups</h4>
						</router-link>

						<router-link :to="'/' + currentPath.namespace + '/sgbackupconfigs/new'" class="addnew" v-if="iCan('create', 'sgbackupconfigs', currentPath.namespace)">
							<svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 19 19"><g transform="translate(-573 -706)"><g transform="translate(573 706)" fill="none" stroke="#00adb5" stroke-width="2"><circle cx="9.5" cy="9.5" r="9.5" stroke="none"/><circle cx="9.5" cy="9.5" r="8.5" fill="none"/></g><g transform="translate(-30.5 28.8)"><g transform="translate(609 686)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g><g transform="translate(613.7 682.7) rotate(90)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g></g></g></svg>
						</router-link>
					</li>
					
					<li><ul v-if="iCan('list', 'sgbackupconfigs', currentPath.namespace)" class="crdSubmenu">
						<template v-for="config in bkConfig">
							<li v-if="config.data.metadata.namespace == currentPath.namespace" :class="'sgbackupconfig-'+config.data.metadata.namespace+'-'+config.name">
								<router-link :to="'/' + config.data.metadata.namespace + '/sgbackupconfig/' + config.name" class="item" :title="config.name" :class="(currentPath.component.includes('BackupConfig') && (currentPath.name == config.name)) ? 'router-link-exact-active' : ''">{{ config.name }}</router-link>
							</li>
						</template>
					</ul></li>
				</ul>
			</div>
			
			<div v-if="iCan('', 'sgdistributedlogs')" class="set logs" :class="currentPath.component.includes('LogsServer') ? 'active' : ''" >
				<ul>
					<li class="crdName">
						<router-link :to="'/' + currentPath.namespace + '/sgdistributedlogs'" title="Logs Servers" class="nav-item">
							<span><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M19,15H5c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,14.6,19.6,15,19,15z"/><path class="a" d="M1,15L1,15c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,14.6,1.6,15,1,15z"/><path class="a" d="M19,11H5c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,10.6,19.6,11,19,11z"/><path class="a" d="M1,11L1,11c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,10.6,1.6,11,1,11z"/><path class="a" d="M19,7H5C4.4,7,4,6.6,4,6v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,6.6,19.6,7,19,7z"/><path d="M1,7L1,7C0.4,7,0,6.6,0,6v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,6.6,1.6,7,1,7z"/></svg></span>
							<h3 :class="isCollapsed ? 'submenuTitle' : ''">Logs Servers</h3>
						</router-link>

						<router-link :to="'/' + currentPath.namespace + '/sgdistributedlogs/new'" class="addnew" v-if="iCan('create', 'sgdistributedlogs', currentPath.namespace)">
							<svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 19 19"><g transform="translate(-573 -706)"><g transform="translate(573 706)" fill="none" stroke="#00adb5" stroke-width="2"><circle cx="9.5" cy="9.5" r="9.5" stroke="none"/><circle cx="9.5" cy="9.5" r="8.5" fill="none"/></g><g transform="translate(-30.5 28.8)"><g transform="translate(609 686)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g><g transform="translate(613.7 682.7) rotate(90)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g></g></g></svg>
						</router-link>
					</li>

					<li><ul v-if="iCan('list', 'sgdistributedlogs', currentPath.namespace)" class="crdSubmenu">
						<template v-for="log in logsClusters">
							<li v-if="log.data.metadata.namespace == currentPath.namespace" :class="'sgdistributed-log-'+log.data.metadata.namespace+'-'+log.name">
								<router-link :to="'/' + log.data.metadata.namespace + '/sgdistributedlog/' + log.name" class="item" :title="log.name">{{ log.name }}</router-link>
							</li>
						</template>
					</ul></li>
				</ul>
			</div>

			<div v-if="iCan('','sgbackups')" class="set backups" :class="( currentPath.component.includes('Backups') && !currentPath.component.includes('Cluster') ) ? 'active' : ''" >
				<ul>
					<li class="crdName">	
						<router-link :to="'/' + currentPath.namespace + '/sgbackups'" title="Backups" class="nav-item">
							<span><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M10.55.55A9.454 9.454 0 001.125 9.5H.479a.458.458 0 00-.214.053.51.51 0 00-.214.671l1.621 3.382a.49.49 0 00.213.223.471.471 0 00.644-.223l1.62-3.382A.51.51 0 004.2 10a.49.49 0 00-.479-.5H3.1a7.47 7.47 0 117.449 7.974 7.392 7.392 0 01-3.332-.781.988.988 0 00-.883 1.767 9.356 9.356 0 004.215.99 9.45 9.45 0 000-18.9z"/><path class="a" d="M13.554 10a3 3 0 10-3 3 3 3 0 003-3z"/></svg></span>
							<h3 :class="isCollapsed ? 'submenuTitle' : ''">Cluster Backups</h3>
						</router-link>

						<router-link :to="'/' + currentPath.namespace + '/sgbackups/new'" class="addnew" v-if="iCan('create', 'sgbackups', currentPath.namespace)">
							<svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 19 19"><g transform="translate(-573 -706)"><g transform="translate(573 706)" fill="none" stroke="#00adb5" stroke-width="2"><circle cx="9.5" cy="9.5" r="9.5" stroke="none"/><circle cx="9.5" cy="9.5" r="8.5" fill="none"/></g><g transform="translate(-30.5 28.8)"><g transform="translate(609 686)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g><g transform="translate(613.7 682.7) rotate(90)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g></g></g></svg>
						</router-link>
					</li>
				</ul>
			</div>

			<div class="set dbops" :class="currentPath.component.includes('DbOps') ? 'active' : ''" >
				<ul>
					<li class="crdName">
						<router-link :to="'/' + currentPath.namespace + '/sgdbops'" title="Database Operations" class="nav-item">
							<span><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><g fill="#36A8FF"><path d="M17.1 20c-.6 0-1-.5-1-1 0-1.6-1.3-2.8-2.8-2.8H6.6c-1.6 0-2.8 1.3-2.8 2.8 0 .6-.5 1-1 1s-1-.5-1-1c0-2.7 2.2-4.8 4.8-4.8h6.7c2.7 0 4.8 2.2 4.8 4.8.1.5-.4 1-1 1zM9.9 9.4c-1.4 0-2.5-1.1-2.5-2.5s1.1-2.5 2.5-2.5 2.5 1.1 2.5 2.5c.1 1.4-1.1 2.5-2.5 2.5zm0-3.3c-.4 0-.8.3-.8.8 0 .4.3.8.8.8.5-.1.8-.4.8-.8 0-.5-.3-.8-.8-.8z"/><path d="M10 13.7h-.2c-1-.1-1.8-.8-1.8-1.8v-.1h-.1l-.1.1c-.8.7-2.1.6-2.8-.2s-.7-1.9 0-2.6l.1-.1H5c-1.1 0-2-.8-2.1-1.9 0-1.2.8-2.1 1.8-2.2H5v-.1c-.7-.8-.7-2 .1-2.8.8-.7 1.9-.7 2.7 0 .1 0 .1 0 .2-.1 0-.6.3-1.1.7-1.4.8-.7 2.1-.6 2.8.2.2.3.4.7.4 1.1v.1h.1c.8-.7 2.1-.6 2.8.2.6.7.6 1.9 0 2.6l-.1.1v.1h.1c.5 0 1 .1 1.4.5.8.7.9 2 .2 2.8-.3.4-.8.6-1.4.7h-.3c.4.4.6 1 .6 1.5-.1 1.1-1 1.9-2.1 1.9-.4 0-.9-.2-1.2-.5l-.1-.1v.1c0 1.1-.9 1.9-1.9 1.9zM7.9 10c1 0 1.8.8 1.8 1.7 0 .1.1.2.2.2s.2-.1.2-.2c0-1 .8-1.8 1.8-1.8.5 0 .9.2 1.3.5.1.1.2.1.3 0s.1-.2 0-.3c-.7-.7-.7-1.8 0-2.5.3-.3.8-.5 1.3-.5h.1c.1 0 .2 0 .2-.1 0 0 .1-.1.1-.2s0-.1-.1-.2c0 0-.1-.1-.2-.1h-.2c-.7 0-1.4-.4-1.6-1.1 0-.1 0-.1-.1-.2-.2-.6-.1-1.3.4-1.8.1-.1.1-.2 0-.3s-.2-.1-.3 0c-.3.3-.8.5-1.2.5-1 0-1.8-.8-1.8-1.8 0-.1-.1-.2-.2-.2s-.1 0-.2.1c.1.1 0 .2 0 .3 0 .7-.4 1.4-1.1 1.7-.1 0-.1 0-.2.1-.6.2-1.3 0-1.8-.4-.1-.1-.2-.1-.3 0-.1.1-.1.2 0 .3.3.3.5.7.5 1.2.1 1-.7 1.9-1.7 1.9h-.2c-.1 0-.1 0-.2.1 0-.1 0 0 0 0 0 .1.1.2.2.2h.2c1 0 1.8.8 1.8 1.8 0 .5-.2.9-.5 1.2-.1.1-.1.2 0 .3s.2.1.3 0c.3-.2.7-.4 1.1-.4h.1z"/></g></svg></span>
							<h3 :class="isCollapsed ? 'submenuTitle' : ''">Database Operations</h3>
						</router-link>

						<router-link :to="'/' + currentPath.namespace + '/sgdbops/new'" class="addnew">
							<svg xmlns="http://www.w3.org/2000/svg" width="19" height="19" viewBox="0 0 19 19"><g transform="translate(-573 -706)"><g transform="translate(573 706)" fill="none" stroke="#00adb5" stroke-width="2"><circle cx="9.5" cy="9.5" r="9.5" stroke="none"/><circle cx="9.5" cy="9.5" r="8.5" fill="none"/></g><g transform="translate(-30.5 28.8)"><g transform="translate(609 686)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g><g transform="translate(613.7 682.7) rotate(90)" fill="#00adb5" stroke="#00adb5" stroke-width="1"><rect width="8" height="1.4" rx="0.7" stroke="none"/><rect x="0.5" y="0.5" width="7" height="0.4" rx="0.2" fill="none"/></g></g></g></svg>
						</router-link>
					</li>
				</ul>
			</div>
			
		</div>
	</aside>
</template>

<script>
	import store from '../store'
	import { mixin } from './mixins/mixin'

    export default {
        name: 'SideBar',

		mixins: [mixin],

		data: function() {

			return {
			}

		},

		created: function() {
			$(document).on("click", "#current-namespace , #ns-select a", function(){
				$("#current-namespace").toggleClass("open");
				$("#ns-select").slideToggle();
				$(".set.active").removeClass('active');
			});

			$(document).on("mouseover", ".set:not(.active), .collapsed .set", function(){
				let offset = $(this).offset();
				let submenu = $(this).find(".crdSubmenu");
				
				if($(this).is('.collapsed .set')) {
					if(!submenu.length || !submenu.children().length)
						$(this).find('.submenuTitle').addClass('emptySubmenu')
					else {
						if(window.innerHeight > 700)
							submenu.css("bottom","auto").css("top",offset.top - window.scrollY).css("max-height",window.innerHeight - (offset.top - window.scrollY) - 50)
						else
							submenu.css("top", "auto").css("bottom",window.innerHeight - $(this).height() - offset.top)
						
						submenu.addClass("show");
					}

					$(this).find('.submenuTitle').addClass('displayMe');
					$(this).find('.addnew').addClass('displayMe');
					$(this).find('.crdName').addClass('hovered');
				
				} else {
					if(submenu.length && submenu.children().length) {
						if(window.innerHeight > 700)
							submenu.css("bottom","auto").css("top",offset.top - window.scrollY).css("max-height",window.innerHeight - (offset.top - window.scrollY))
						else
							submenu.css("top", "auto").css("bottom",window.innerHeight - $(this).height() - offset.top)
						
						submenu.addClass("show");
					}
				}
			});

			$(document).on("mouseleave", ".set:not(.active), .collapsed .set", function(){
				let submenu = $(this).find(".crdSubmenu");

				submenu.removeClass("show")
				submenu.removeAttr('style');

				if($(this).is('.collapsed .set')) {
					$(this).find('.submenuTitle').removeClass('displayMe emptySubmenu');
					$(this).find('.addnew').removeClass('displayMe');
					$(this).find('.crdName').removeClass('hovered');
				}
			});

			$(document).on("mouseleave", ".set .crdSubmenu.show", function(){
				$(this).removeClass("show")
				$(this).removeAttr('style');
			});

			$(document).on("click", ".nav-item, .collapsed .nav-item", function(){
				$("#current-namespace").removeClass('open');
				$('#ns-select').slideUp();
				$(".crdSubmenu").removeClass("show");
				$(".crdSubmenu").removeAttr('style'); 

				if($(this).is('.collapsed .nav-item')) {
					$(this).children('.submenuTitle').removeClass('displayMe');
					$(this).siblings('.addnew').removeClass('displayMe');
					$(this).parent().removeClass('hovered');
				}
			});

			$(document).on("click", ".set .item", function(){    
				$(".crdSubmenu").removeClass("show");
				$(".crdSubmenu").removeAttr('style'); 
			});

			$(document).on('mouseover', '.set .addnew', function() {
				$(this).siblings('.nav-item').children('.submenuTitle').addClass('hovBg')
			});

			$(document).on('mouseleave', '.set .addnew', function() {
				$(this).siblings('.nav-item').children('.submenuTitle').removeClass('hovBg')
			});

			$(document).on('click', '.set .addnew, .collapsed .set .addnew', function(){
				let parentSet = $(this).parents('.set')

				$('.crdSubmenu').removeClass('show');
				$(".crdSubmenu").removeAttr('style');

				if(!parentSet.hasClass('active')) {
					$('.set.active').removeClass('active');
					parentSet.addClass('active');
				}

				if($(this).is('.collapsed .set .addnew')) {
					$(this).removeClass('displayMe');
					parentSet.find('.submenuTitle').removeClass('displayMe')
					$(this).parent().removeClass('hovered');
				}
			});

			$(document).on("mouseover", ".collapsed #ns-set", function(){
				$("#current-namespace").addClass("open");
				$("#ns-select").show();
			});

			$(document).on("mouseleave", ".collapsed #ns-set", function(){
				$("#current-namespace").removeClass("open");
				$("#ns-select").hide();
			});

		},

		computed: {
			namespaces () {
				return store.state.allNamespaces
			},

			clusters () {
				return store.state.clusters
			},

			logsClusters () {
				return store.state.logsClusters
			},

			backups () {
				return store.state.backups;
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

			currentPath () {
				return store.state.currentPath
			},

			notFound () {
				return store.state.notFound
			},

			isCollapsed () {
				return store.state.view == 'collapsed'
			}
		}
	}
	
</script>

<style scoped>
	/*Expanded Sidebar*/
	#ns-select {
		list-style: none;
		line-height: 50px;
		display: none;
		margin-top: -1px;
		border-bottom: 1px solid var(--borderColor);
	}

	#ns-select a {
		padding-left: 50px;
		display: block;
		display: block;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	#ns-select li {
		position: relative;
	}

	#ns-select li:hover {
		margin-left: -50px;
		padding-left: 50px;
	}

	h3, h4 {
		display: inline-block;
		margin-bottom: 20px;
		position: relative;
		cursor: pointer;
		text-transform: capitalize;
	}

	h4 {
		margin-bottom: 10px;
	}

	h3 { 
		-webkit-touch-callout: none; /* iOS Safari */
		-webkit-user-select: none; /* Safari */
		-khtml-user-select: none; /* Konqueror HTML */
		-moz-user-select: none; /* Old versions of Firefox */
			-ms-user-select: none; /* Internet Explorer/Edge */
				user-select: none; /* Non-prefixed version, currently
									supported by Chrome, Opera and Firefox */
	} 

	ul {
		list-style: none;
	}

	.set {
		height: auto;
		font-size: 1.1rem;
		line-height: 60px;
		transition: max-height 1s ease-out;
		position: relative;
	}

	.set a {
		color: inherit;
	}

	a.router-link-exact-active {
		font-weight: 600;
	}

	.crdName, .crdSubmenu li {
		height: 50px;
		line-height: 50px;
	}

	.helpTooltip.alert {
		width: 55px;
		height: 50px;
		top: 0;
		left: auto;
		right: 0;
	}

	.nav-item {
		color: inherit;
		display: flex;
		height: 50px;
		line-height: 50px;
		font-size: 14px;
		cursor: pointer;
		padding: 0 20px 0 50px
	}

	.subset .nav-item, .crdSubmenu a {
		padding-left: 80px;
		padding-right: 55px;
	}

	.subset .crdSubmenu a {
		padding: 0 55px 0 110px;
	}

	.addnew {
		position: absolute;
		top: 0;
		right: 0;
		background: transparent !important;
		display: none;
		height: 52px;
		padding: 0 20px;
	}

	body:not(.collapsed) .set:hover .addnew, body:not(.collapsed) .set.active .addnew {
		display: block;
	}

	.crdSubmenu {
		/*min-height: 50px;*/
		max-height: calc(100vh - 580px);
		overflow: auto;
	}

	.item {
		display: block;
		cursor: pointer;
		position: relative;
		height: 50px;
		line-height: 50px;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
	
	.crdSubmenu .item:before {
		display: block;
		content: " ";
		position: absolute;
		width: 9px;
		height: 2px;
		border-radius: 2px;
		background: var(--blue);
		top: 23px;
		margin-left: -27px;
	}

	.set.active, .crdName:hover, .crdSubmenu li:hover, a.router-link-exact-active, #ns-select li:hover, .submenuTitle:hover:before, .hovered span  {
		background-color: rgba(211, 211, 211, .15);
	}

	.collapsed .submenuTitle:hover, .hovBg {
		background: rgb(246, 246, 246);
	}

	.collapsed .submenuTitle:hover {
		color: var(--blue) !important;
	}

	.conf, .subset {
		background-color: rgba(211, 211, 211, .1);
	}

	.set:not(.active) .crdSubmenu {
		position: absolute;
		width: 350px;
		left: 100%;
		list-style: none;
		top: 0;
		display: none;
		background: transparent;
		padding-left: 10px;
		border-radius: 0 5px 5px 0;
	}

	.set:not(.active) .crdSubmenu.show, .collapsed .set.active .crdSubmenu.show {
		display: block;
		position: fixed;
		left: 350px;
		z-index: 2;
		overflow: auto;
		padding-left: 0;
		border: 1px solid var(--borderColor);
		border-left: none;
		background-color: var(--bgColor);
		box-shadow: 2px 2px 5px rgb(0 0 0 / 10%);
		border-radius: 0 5px 5px 0;
	}

	.set:not(.active) .crdSubmenu.show .item:before, .collapsed .set.active .crdSubmenu.show .item:before {
		display: none;
	}

	.set:not(.active) .crdSubmenu.show a, .collapsed .set.active .crdSubmenu.show a {
		padding: 0 55px 0 20px;
	}

	.set li a:hover {
		color: var(--baseColor);
	}


	/*Collapsed Sidebar*/	
	
	.collapsed #current-namespace {
		height: 70px;
	}

	.collapsed #current-namespace svg {
		margin: 0;
		transform: scale(0.8);
		position: relative;
		left: -5px;
		top: 2px;
	}

	.collapsed #ns-select {
		position: fixed;
		list-style: none;
		width: 350px;
		left: 50px;
		z-index: 999;
		top: 50px;
		height: auto;
		max-height: calc(100vh - 50px);
		overflow: auto;
		padding-left: 0;
		border-radius: 0 5px 5px 0;
		border: 1px solid var(--borderColor);
		border-left: none;
		background-color: var(--bgColor);
		box-shadow: 2px 2px 5px rgba(0,0,0,.1);
	}

	.collapsed #ns-select li:first-of-type {
		font-weight: bold;
		font-size: 1.1rem;
		border-bottom: 1px solid var(--borderColor);
	}

	.collapsed #ns-select a {
		padding-left: 20px;
	}

	.collapsed #side {
		width: 50px;
	}

	.collapsed .submenuTitle, .collapsed .addnew, .collapsed .set.active .addnew, .collapsed .set.active .crdSubmenu  {
		display: none;
	}

	.collapsed #sets {
		overflow: initial;
	}

	.collapsed .nav-item, .collapsed .subset .nav-item {
		padding: 0px;
	}

	.collapsed .nav-item span {
		padding: 15px;
	}

	.collapsed #side .nav-item svg, .collapsed #sets .subset .nav-item svg {
		width: 18px;
		height: 18px;
		margin: 0;
		position: relative;
		top: -10px;
	}

	.collapsed .crdName:hover {
		border-top: 1px solid var(--borderColor);
		border-bottom: 1px solid var(--borderColor);
	}

	.collapsed #side .crdName:hover svg {
		top: -11px;
	}

	.collapsed #sets .subset .nav-item svg {
		width: 20px;
		height: 20px;
	}

	.hovered .nav-item {
		width: 400px;
	}

	.collapsed .set.subset {
		background: none;
	}

	.collapsed .set .crdSubmenu.show, .collapsed .set.active .crdSubmenu.show {
		left: 50px;
		width: 350px;
		border-radius: 0 0 5px 0;
		border-top: none;
	}

	.collapsed .crdSubmenu {
		margin-top: 50px;
	}

	.displayMe {
		display: inline-block !important;
	}

	.collapsed .submenuTitle {
		position: absolute;
		left: 50px;
		top: 0;
		z-index: 5;
		width: 350px;
		margin: 0 !important;
		padding-left: 20px;
		font-weight: bold;
		font-size: 1.1rem;
		border: 1px solid var(--borderColor);
		background-color: var(--bgColor);
		border-left: 0;
		border-radius: 0 5px 0 0;
		height: 50px;
	}		

	.collapsed .addnew {
		position: absolute;
		z-index: 10;
		left: 350px;
		top: 0;
		width: 50px;
		height: 50px;
		padding: 0 20px 0 15px;
	}

	.collapsed .emptySubmenu {
		border-radius: 0 5px 5px 0;
		box-shadow: 2px 2px 5px rgb(0 0 0 / 10%);
		background-color: var(--bgColor);
	}

	/*.collapsed .crdSubmenu.show .item {
		max-width: 349px;
	}*/


	/*Darkmode Adjustments*/
	.darkmode .collapsed .set.backups .hideCollapsed:hover, .darkmode .collapsed .set.dbops .hideCollapsed:hover {
		background: #303030 !important;
	}

	.darkmode #side {
		background: var(--bgColor);
	}

	.darkmode .set.active, .darkmode .crdName:hover, .darkmode .crdSubmenu li:hover, .darkmode a.router-link-exact-active, .darkmode #ns-select li:hover, .darkmode.collapsed .submenuTitle:hover:before, .darkmode .hovered span {
		background-color: rgba(211, 211, 211, .05);
	}

	.darkmode.collapsed .submenuTitle:hover, .darkmode .hovBg {
		background: rgb(38, 38, 38);
	}

	.darkmode .conf, .darkmode .subset {
		background-color: rgba(211, 211, 211, .02);
	}

</style>