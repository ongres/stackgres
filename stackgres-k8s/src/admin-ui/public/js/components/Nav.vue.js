var Nav = Vue.component("sg-nav", {
	template: `
		<aside id="nav" class="disabled">
			<div id="logo">
				<svg xmlns="http://www.w3.org/2000/svg" width="29.997" height="25.348"><path d="M0 14.125l6.78 7.769h16.438l6.779-7.769-6.779-7.769H6.78z" fill="#42a8c8"/><path fill="#426d88" d="M6.78 21.894h16.443v3.455H6.78z"/><path d="M6.78 25.348L0 17.574v-3.45l6.78 7.77z" fill="#428bb4"/><path d="M23.218 25.348l6.779-7.769v-3.454l-6.779 7.769z" fill="#16657c"/><g><path d="M28.213 12.882c0-2.882-5.92-5.219-13.215-5.219s-13.21 2.336-13.21 5.219 5.915 5.219 13.21 5.219 13.215-2.337 13.215-5.219z" fill="#39b54a"/><path d="M28.213 12.882c0 2.882-5.92 5.219-13.215 5.219s-13.21-2.336-13.21-5.219v2.873c.91 2.533 6.525 5.219 13.21 5.219s12.3-2.687 13.215-5.219v-2.873z" fill="#009245"/></g><g><path d="M.678 8.302l14.323 8.3 14.323-8.3-14.323-8.3z" fill="#f2c63f"/><path d="M.678 8.302v3.235l14.323 8.3v-3.235z" fill="#f2b136"/><path d="M29.324 8.302L15 16.602v3.235l14.324-8.3z" fill="#f2a130"/></g><g><path d="M3.846 10.368l22.307-3.242-5.657-6.5z" fill="#ff7124"/><path d="M3.846 10.368l22.307-3.242v2.928L3.846 13.295z" fill="#d93d1b"/></g></svg> <h1>STACK<strong>GRES</strong></h1>
			</div>

			<!--<router-link :to="'/'" title="" id="collapse" class="nav-item">
				<svg xmlns="http://www.w3.org/2000/svg" width="43.657" height="37.19" viewBox="0 0 43.657 37.19"><path d="M43.258 20.444l-2.371-2.724 1.412-.819a.1.1 0 0 0 .049-.1v-4.6c.025-.049 0-.074-.049-.1a.075.075 0 0 0-.1 0l-6.047-3.487-6.4-7.36a.115.115 0 0 0-.148-.025l-3.108 1.814-4.593-2.65a.112.112 0 0 0-.123 0L1.359 12.129a.024.024 0 0 0-.025.025l-.025.025v4.65a.128.128 0 0 0 .049.1l1.394.806L.4 20.444c0 .025-.025.049-.025.074v4.379a.091.091 0 0 0 .025.074l9.644 11.588.025.025h.049l23.373.049h.074a.024.024 0 0 0 .025-.025l9.644-11.047a.091.091 0 0 0 .025-.074l.025-4.97a.09.09 0 0 0-.026-.073zm-.246.074l-2.591 2.966a4.038 4.038 0 0 0 .279-.6v-4.088c.049-.025.049-.049.049-.049a3.117 3.117 0 0 0-.129-.871l.059-.034zM3.155 22.806v-2.854c1.2 2.976 6.783 5.376 14.006 6.112l4.618 2.671c.025 0 .049.025.074.025s.049 0 .049-.025l4.606-2.672c7.211-.737 12.772-3.132 13.994-6.086v2.829c-1.279 3.518-9.078 7.278-18.673 7.278s-17.37-3.76-18.674-7.278zm13.488 2.959c-7.774-.881-13.488-3.689-13.488-7.018a2.94 2.94 0 0 1 .095-.727zM6.083 15.278l31.467-4.576v3.912h.025l-31.492 4.28zm31.192-.336l-15.422 8.942-9.61-5.549zm3.127 3.06a2.936 2.936 0 0 1 .1.745c0 3.329-5.7 6.138-13.481 7.018zm1.723-1.3L21.976 28.416v-4.332l20.15-11.686zm-.172-4.478l-4.157 2.412.024-4.085a.02.02 0 0 0-.006-.016.15.15 0 0 0-.019-.034l-1.187-1.363zM29.628 1.475l7.824 9-13.581 1.973-17.3 2.509zM21.853.614l4.424 2.556-20.045 11.7-4.233-2.446-.344-.2zM1.531 12.448l4.445 2.57-.066.038-.025.025-.025.025a.024.024 0 0 1-.025.025v3.936a.185.185 0 0 0 .049.1.09.09 0 0 0 .074.025h.025l5.838-.793 9.887 5.711v4.33L1.531 16.779zm1.43 5.4l.071.041a3.112 3.112 0 0 0-.123.853v4.142a4.035 4.035 0 0 0 .2.442l-.172-.2-2.288-2.608zm7.058 18.264l-9.4-11.293v-3.981l5.339 6.077 4.06 4.625zm23.4.3l-23.127-.049v-4.724l11.662.025 11.465.025zm.049-4.97l-11.59-.025-11.686-.025-5.074-5.778c3.245 2.578 9.466 4.721 16.686 4.721 7.377 0 13.724-2.237 16.9-4.89zm9.57-6.028l-9.4 10.776v-4.6l9.4-10.776z" stroke-linecap="round" stroke-width=".75"/></svg>
			</router-link>
			
			<router-link :to="'/overview/'+currentNamespace" title="Overview" class="view nav-item">
				<svg xmlns="http://www.w3.org/2000/svg" width="28" height="16.333" viewBox="0 0 28 16.333"><path data-name="Trazado 2234" d="M17.5 8.167a3.5 3.5 0 1 1-6.934-.678 2.181 2.181 0 0 0 2.806-2.766A3.694 3.694 0 0 1 14 4.667a3.5 3.5 0 0 1 3.5 3.5zM14.017 0C5.187 0 0 7.643 0 7.643s5.641 8.69 14.017 8.69c9.022 0 13.983-8.69 13.983-8.69S22.994 0 14.017 0zM14 14a5.833 5.833 0 1 1 5.833-5.833A5.833 5.833 0 0 1 14 14z"/></svg>
			</router-link>-->

			<!-- <div class="top">
				<router-link :to="( typeof currentCluster.name === 'undefined' ) ? '/' : '/information/'+currentNamespace+'/'+currentCluster.name" title="Information" class="info nav-item">
					<svg xmlns="http://www.w3.org/2000/svg" width="22" height="20.167" viewBox="0 0 22 20.167"><path data-name="Trazado 2236" d="M11 0C5.19 0 0 3.874 0 9.173a8.082 8.082 0 0 0 1.876 5.156c.05 1.678-.938 4.085-1.827 5.837a26.4 26.4 0 0 0 7.313-2.325C15.829 19.9 22 14.721 22 9.173 22 3.845 16.774 0 11 0zm.917 13.75h-1.834v-5.5h1.833zM11 6.646A1.146 1.146 0 1 1 12.146 5.5 1.146 1.146 0 0 1 11 6.646z"/></svg>
				</router-link>

				<router-link :to="( typeof currentCluster.name === 'undefined' ) ? '/' : '/status/'+currentNamespace+'/'+currentCluster.name" title="Status" class="status nav-item">
					<svg xmlns="http://www.w3.org/2000/svg" width="24" height="20" viewBox="0 0 24 20"><path data-name="Trazado 2238" d="M18.905 12c-2.029 2.4-4.862 5-7.905 8C5.107 14.2 0 9.866 0 5.629 0-.525 8.114-1.958 11 2.953c2.865-4.875 11-3.5 11 2.676A6.323 6.323 0 0 1 21.5 8h-6.275a.7.7 0 0 0-.61.358l-.813 1.45-2.27-4.437a.7.7 0 0 0-1.2-.081L8.454 8H7.227a2 2 0 1 0 0 2h1.956a.7.7 0 0 0 .573-.3l.989-1.406L13 12.856a.7.7 0 0 0 1.227.052L15.987 10H24v2z" fill-rule="evenodd"/></svg>
				</router-link>

				<router-link id="backup-btn" :to="( typeof currentCluster.name === 'undefined' ) ? '/' : '/backups/'+currentNamespace+'/'+currentCluster.name" title="Status" class="status nav-item" style="display:none;">
					<svg xmlns="http://www.w3.org/2000/svg" width="20" height="17.143"><path d="M13.333 8.571a1.9 1.9 0 10-1.9 1.9 1.91 1.91 0 001.9-1.9zM11.429 0a8.572 8.572 0 00-8.571 8.571H0l3.81 3.81 3.81-3.81H4.762A6.671 6.671 0 117.562 14L6.21 15.371A8.572 8.572 0 1011.429 0z"/></svg>
				</router-link>

				<router-link id="grafana-btn" :to="( typeof currentCluster.name === 'undefined' ) ? '/grafana' : '/grafana/'+currentCluster.name" title="Grafana" class="grafana nav-item" style="display:none;">
					<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"><g transform="translate(-215 -1005)"><g transform="translate(50)"><circle cx="10" cy="10" r="10" transform="translate(165 1005)"/></g><path d="M5.591,18.366,6.957,9.445l1.394,6.422a.63.63,0,0,0,1.223.1L10.838,12.5l.726,1.583a.836.836,0,0,0,.631.427h1.747V13.153H12.611L11.4,10.419a.626.626,0,0,0-1.186.022L9.142,13.376,7.491,5.537A.632.632,0,0,0,6.87,5a.641.641,0,0,0-.636.578l-1.37,9.057-1.4-5.577a.63.63,0,0,0-1.232-.043L1.19,13.153H0V14.49H1.695a.621.621,0,0,0,.549-.439l.591-2.046,1.5,6.418A.633.633,0,0,0,5.591,18.366Z" transform="translate(218.029 1003.029)" fill="#fff"/></g></svg>
				</router-link>

				<div class="tooltip">Please select a cluster.</div>
			</div> -->

			<div class="right">
				<!-- <div id="help" class="hasTooltip">
					<a href="javascript:void(0)" title="Need some help?">
					<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M9.9 12.89a1.175 1.175 0 00-.839.331 1.111 1.111 0 00-.345.817 1.093 1.093 0 00.362.871 1.208 1.208 0 00.8.3H9.9a1.153 1.153 0 00.8-.3 1.084 1.084 0 00.356-.852 1.13 1.13 0 00-.334-.828 1.2 1.2 0 00-.822-.339zm3.042-6.543a2.841 2.841 0 00-1.177-1 4.1 4.1 0 00-1.709-.36 3.972 3.972 0 00-1.836.43 3.2 3.2 0 00-1.209 1.1 2.509 2.509 0 00-.424 1.335v.012a.979.979 0 00.308.682 1.016 1.016 0 00.733.317h.022a1.038 1.038 0 001.023-.87 2.562 2.562 0 01.5-.942 1.161 1.161 0 01.891-.295 1.188 1.188 0 01.824.293.916.916 0 01.3.7.763.763 0 01-.1.378 1.593 1.593 0 01-.267.353c-.117.118-.31.294-.574.525a10.19 10.19 0 00-.754.718 2.495 2.495 0 00-.654 1.741 1.065 1.065 0 00.29.81 1 1 0 00.692.275h.061a.918.918 0 00.606-.228.883.883 0 00.306-.6c.042-.2.074-.34.1-.42a1.1 1.1 0 01.082-.2 1.172 1.172 0 01.166-.233 4.073 4.073 0 01.328-.329c.52-.46.887-.8 1.091-1a3.074 3.074 0 00.56-.754 2.181 2.181 0 00.244-1.043 2.48 2.48 0 00-.423-1.395z"/><path class="a" d="M10 0a10 10 0 1010 10A10 10 0 0010 0zm0 18.015A8.015 8.015 0 1118.015 10 8.023 8.023 0 0110 18.015z"/></svg>
					</a>

					<div class="tooltip">
						<span>NEED SOME HELP?</span>
						<p class="message">
							Contact us at<br/>
							<a href="mailto:mail@stackgres.io">mail@stackgres.io</a>
						</p>
					</div>
				</div> -->

				<div id="darkmode" :class="(theme == 'dark') ? 'active' : ''">
					<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><g transform="translate(-90 -152)"><rect width="2" height="2" rx="1" transform="translate(103 152) rotate(90)"/><rect width="2" height="2" rx="1" transform="translate(103 174) rotate(90)"/><rect width="2" height="2" rx="1" transform="translate(114 165) rotate(180)"/><rect width="2" height="2" rx="1" transform="translate(92 165) rotate(180)"/><rect width="2" height="2" rx="1" transform="translate(111.778 155.636) rotate(135)"/><rect width="2" height="2" rx="1" transform="translate(95.05 172.364) rotate(135)"/><rect width="2" height="2" rx="1" transform="translate(93.636 154.222) rotate(45)"/><rect width="2" height="2" rx="1" transform="translate(110.364 170.95) rotate(45)"/><path d="M102,156a8,8,0,1,0,8,8A8,8,0,0,0,102,156Zm-5.336,8A5.343,5.343,0,0,1,102,158.664v10.672A5.343,5.343,0,0,1,96.664,164Z" transform="translate(0 0)"/></g></svg>
				</div>

				<div id="notifications" class="hasTooltip">
					<a href="javascript:void(0)" title="Notifications">
						<span class="loader"></span>
						<span class="count zero">0</span>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M11.877 16.75a.918.918 0 00-.225-.026 1.013 1.013 0 00-.984.783.812.812 0 01-.158.35.6.6 0 01-.467.179h-.1a.579.579 0 01-.469-.156.881.881 0 01-.174-.38 1.008 1.008 0 00-1.988.213.95.95 0 00.022.2A2.518 2.518 0 009.81 20h.155c.055 0 .105.006.148.006a2.529 2.529 0 002.523-2.07.955.955 0 00-.13-.744 1 1 0 00-.629-.442zm6.108-2.332a6.6 6.6 0 00-.479-.527l-.01-.01c-.757-.77-1.696-1.723-1.696-5.181 0-5.52-2.942-6.791-4.207-7.08l-.019-.005h-.01v-.071a1.563 1.563 0 00-3.126 0v.066h-.006l-.019.005C7.148 1.907 4.2 3.18 4.2 8.7c0 3.45-.935 4.405-1.687 5.173a6.06 6.06 0 00-.5.545 1.283 1.283 0 00-.257.968 1.307 1.307 0 00.521.87 1.365 1.365 0 00.81.264h13.852a1.352 1.352 0 00.941-.409 1.291 1.291 0 00.105-1.689zm-2.572.137H4.568a6.173 6.173 0 00.9-1.475 10.979 10.979 0 00.728-4.394 6.858 6.858 0 01.977-3.973 2.783 2.783 0 011.679-1.174 2.63 2.63 0 001.179-.612 2.153 2.153 0 001.09.609A2.768 2.768 0 0112.8 4.7a6.877 6.877 0 01.983 3.986 10.966 10.966 0 00.737 4.409 6.18 6.18 0 00.893 1.46z"/></svg>
					</a>

					<div class="tooltip">
						<span>Notifications</span>
						<p class="zero message">There are no new notifications.</p>
					</div>
				</div>

				<div id="reload">
					<svg xmlns="http://www.w3.org/2000/svg" width="20.001" height="20" viewBox="0 0 20.001 20"><g transform="translate(0 0)"><path d="M1.053,11.154A1.062,1.062,0,0,1,0,10.089,9.989,9.989,0,0,1,16.677,2.567l.484-.484a.486.486,0,0,1,.2-.121.541.541,0,0,1,.663.343l1.318,3.748a.522.522,0,0,1,.007.327.5.5,0,0,1-.627.323L18.7,6.7l-3.743-1.32a.531.531,0,0,1-.206-.13.52.52,0,0,1-.016-.733l.464-.465A7.9,7.9,0,0,0,2.092,10.1a1.04,1.04,0,0,1-1.039,1.057Z"/><path d="M18.947,8.844A1.063,1.063,0,0,1,20,9.91,9.989,9.989,0,0,1,3.323,17.434l-.484.484a.476.476,0,0,1-.2.121.541.541,0,0,1-.663-.343L.659,13.948a.522.522,0,0,1-.007-.327.5.5,0,0,1,.627-.323l.022.008,3.743,1.32a.531.531,0,0,1,.206.13.52.52,0,0,1,.016.733l-.464.465A7.9,7.9,0,0,0,17.908,9.9a1.04,1.04,0,0,1,1.039-1.057Z"/></g></svg>
				</div>

				<div id="logout" v-if="loggedIn">
					<a @click="logout()">Logout <svg xmlns="http://www.w3.org/2000/svg" width="10.546" height="10.5" viewBox="0 0 10.546 10.5"><g transform="translate(-30 -181.75)"><path d="M33.92,192h-2.1a1.538,1.538,0,0,1-1.571-1.5v-7a1.538,1.538,0,0,1,1.571-1.5h2.1a.5.5,0,1,1,0,1h-2.1a.515.515,0,0,0-.527.5v7a.515.515,0,0,0,.527.5h2.1a.5.5,0,1,1,0,1Z" fill="#00adb5" stroke="#00adb5" stroke-width="0.5"/><path d="M42.157,192.074l1.965-1.965a.525.525,0,0,0,0-.741L42.157,187.4a.524.524,0,0,0-.741.74l1.072,1.071h-3.7a.524.524,0,1,0,0,1.048h3.7l-1.072,1.071a.524.524,0,0,0,.741.74Z" transform="translate(-4.026 -2.739)" fill="#00adb5" stroke="#00adb5" stroke-width="0.5"/></g></svg></a>
				</div>

				<div id="delete" class="hasTooltip">
					<div class="tooltip">
						<h3>This action is permanent</h3>
						<p>
							Are you sure you want to delete the {{ deleteItem.kind }} <strong>{{ deleteItem.name }}</strong>?<br/>
							Keep in mind that this action will permanently delete your {{ deleteItem.kind }}.
						</p>
						<p>
							Please type the exact name of the {{ deleteItem.kind }} to confirm.
						</p>
						<form @submit="confirmDelete(confirmDeleteName)">
							<input v-model="confirmDeleteName" :placeholder="deleteItem.kind+' name'">
							<span class="warning" style="display:none">The {{ deleteItem.kind }} name does not match the name of the element requested to be deleted.</span>
							<a @click="confirmDelete(confirmDeleteName)">DELETE ITEM</a> <a @click="cancelDelete()">CANCEL</a>
						</form>
						
					</div>
				</div>

				<div id="signup">
					<form id="login" class="form noSubmit">
						<div class="header">
							<h2>Welcome to StackGres!</h2>
						</div>
						<p>To continue, please Log in.</p>
						
						<label for="loginUser">
							Username <span class="req">*</span>
						</label>
						<input v-model="loginUser" placeholder="username">

						<label for="loginPassword">
							Password <span class="req">*</span>
						</label>
						<input v-model="loginPassword" placeholder="password" :type="loginPasswordType">

						<a @click="showPassword()" id="showPassword">
							<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
						</a>

						<span class="warning" style="display:none">
							Wrong username or password. Please try again!
						</span>
						
						<hr/>
						<button @click="login">Login</button>
					</form>
				</div>

				<div id="clone" style="display:none">
					<form class="form noSubmit">
						<div class="header">
							<h2>Clone {{ clone.kind }}</h2>
						</div>
						<label for="cloneNamespace">Namespace <span class="req">*</span></label>
						<select @change="setCloneNamespace" id="cloneNamespace">
							<option v-for="namespace in namespaces">{{ namespace }}</option>
						</select>

						<label for="cloneName">Name <span class="req">*</span></label>
						<input @keyup="setCloneName" id="cloneName">

						<span class="warning" v-if="nameColission">
							There's already a <strong>{{ clone.kind }}</strong> with the same name on the specified namespace. Please specify a different name or choose another namespace
						</span>

						<span class="warning" v-if="clone.kind == 'SGCluster'">
							This action will create a new cluster with the same configuration as the source cluster. Please note that the cluster will be created as soon as this configuration is copied and no source data is copied whatsoever.
						</span>

						<button @click="cloneCRD" :disabled="nameColission">CLONE</button> <a class="btn border" @click="cancelClone">CANCEL</a>
					</form>
				</div>

				<!--<div id="settings">
					<a href="javascript:void(0)" title="Settings" class="nav-item">
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M13.193 10A3.193 3.193 0 1010 13.2a3.2 3.2 0 003.193-3.2zm-1.809 0A1.384 1.384 0 1110 8.614 1.386 1.386 0 0111.384 10z"/><path class="a" d="M16.961 12.835a.443.443 0 01.44-.246 2.6 2.6 0 000-5.2h-.136a.4.4 0 01-.318-.157.988.988 0 00-.055-.164.427.427 0 01.122-.486A2.6 2.6 0 1013.3 2.937a.414.414 0 01-.287.116.4.4 0 01-.292-.12.455.455 0 01-.123-.357 2.591 2.591 0 00-.762-1.84 2.659 2.659 0 00-3.675 0 2.6 2.6 0 00-.76 1.84v.137a.406.406 0 01-.158.318 1.078 1.078 0 00-.163.055.41.41 0 01-.465-.1l-.076-.077a2.5 2.5 0 00-1.853-.729 2.576 2.576 0 00-1.822.8 2.632 2.632 0 00.1 3.71.434.434 0 01.058.5.423.423 0 01-.422.265 2.6 2.6 0 000 5.2h.133a.41.41 0 01.285.117.43.43 0 01-.035.629l-.079.079v.005A2.61 2.61 0 003 17.135a2.479 2.479 0 001.853.728 2.614 2.614 0 001.847-.827.429.429 0 01.5-.057.419.419 0 01.264.42 2.6 2.6 0 105.2 0v-.132a.414.414 0 01.116-.284.421.421 0 01.3-.126.356.356 0 01.278.113l.1.1a2.731 2.731 0 001.852.728 2.6 2.6 0 002.55-2.65 2.611 2.611 0 00-.825-1.857.4.4 0 01-.081-.444zm-6.2 4.422v.143a.691.691 0 01-.69.691.718.718 0 01-.692-.788 2.289 2.289 0 00-1.457-2.095 2.274 2.274 0 00-.919-.2 2.427 2.427 0 00-1.7.728.7.7 0 01-.5.213.652.652 0 01-.482-.194.676.676 0 01-.208-.477.749.749 0 01.217-.53l.064-.064a2.323 2.323 0 00-1.654-3.938H2.6a.692.692 0 01-.489-1.18.755.755 0 01.587-.2A2.286 2.286 0 004.788 7.9a2.306 2.306 0 00-.467-2.556l-.069-.069a.693.693 0 01.478-1.191.655.655 0 01.5.213l.069.071a2.257 2.257 0 002.334.536.92.92 0 00.27-.071 2.312 2.312 0 001.4-2.121v-.134a.687.687 0 01.2-.489.705.705 0 01.977 0 .751.751 0 01.2.571 2.3 2.3 0 00.705 1.64 2.331 2.331 0 001.649.665 2.369 2.369 0 001.652-.713.691.691 0 011.181.488.753.753 0 01-.259.547 2.253 2.253 0 00-.538 2.334.932.932 0 00.072.274 2.313 2.313 0 002.119 1.4h.139a.691.691 0 01.69.692.717.717 0 01-.768.691 2.312 2.312 0 00-2.113 1.395 2.345 2.345 0 00.533 2.619.693.693 0 01-.45 1.192.749.749 0 01-.506-.19l-.1-.1a2.4 2.4 0 00-1.653-.654 2.325 2.325 0 00-2.283 2.312zM5.5 4.177z"/></svg>
					</a>
				</div>-->
			</div>
		</aside>`,

	data: function() {
		return {
			loginUser: '',
			loginPassword: '',
			loginPasswordType: 'password',
			confirmDeleteName: '',
			nameColission: false,
		}
	},
	
	computed: {

		namespaces () {
			return store.state.namespaces
		},

		currentNamespace () {
			return store.state.currentNamespace;
		},

		currentCluster () {
			return store.state.currentCluster;
		},

		currentPods () {
			return store.state.currentPods;
		},

		deleteItem () {
			return store.state.deleteItem;
		},

		theme () {
			return store.state.theme
		},

		loggedIn () {
			if (typeof store.state.loginToken !== 'undefined')
				return store.state.loginToken.length > 0
			else
				return false
		},

		clone () {
			return store.state.cloneCRD
		},

		/* confirmDeleteName() {
			return store.state.confirmDeleteName
		}*/
	},

	methods: {

		login: function() {

			/* let token = btoa(this.loginUser+':'+this.loginPassword);

			axios
			.get(apiURL+'namespace',{
				headers: {
					'Authorization': 'Basic '+token
				}
			})
			.then( function(response){
				store.commit('setLoginToken', token);
				$('#signup').fadeOut();
				document.cookie = "sgToken="+token;
				vm.fetchAPI();
			}
			).catch(function(err) {
				$('#login .warning').fadeIn();
				//checkAuthError(err);
			}); */

			axios
			.post(apiURL+'auth/login',{
				username: this.loginUser,
				password: this.loginPassword	
			})
			.then( function(response){
				console.log(response);
				store.commit('setLoginToken', response.data.access_token);
				$('#signup').fadeOut();
				document.cookie = "sgToken="+response.data.access_token;
				vm.fetchAPI();
			}
			).catch(function(err) {
				$('#login .warning').fadeIn();
				//checkAuthError(err);
			});



		},

		logout: function() {
			document.cookie = 'sgToken=';
			store.commit('setLoginToken');
			router.push('/');
			//store.replaceState({})
			$('#signup').addClass('login').fadeIn();
		},

		showPassword: function() {
			
			if(this.loginPasswordType == 'text') {
				this.loginPasswordType = 'password';
				$('#showPassword').removeClass('active');
			} else {
				this.loginPasswordType = 'text';
				$('#showPassword').addClass('active');
			}
		},

		setCloneName: function() {

			var nameColission = false;

			//console.log($('#cloneName').val())
			store.commit('setCloneName', $('#cloneName').val());
			
			store.state.clusters.forEach(function(item, index){
				if( (item.name == $('#cloneName').val()) && (item.data.metadata.namespace == $('#cloneNamespace').val() ) )
					nameColission = true
			})

			this.nameColission = nameColission;
		},

		setCloneNamespace: function() {
			store.commit('setCloneNamespace', $('#cloneNamespace').val());
		},

		cancelClone: function() {
			$('#clone').hide();
			store.commit('setCloneCRD', {});
		},

		cloneCRD: function() {
			//console.log($('#cloneName').val() + ' / '+ store.state.cloneCRD.data.metadata.name)
			
			if(store.state.cloneCRD.kind == 'SGPoolingConfig')
				var endpoint = 'sgpoolconfig'
			else if (store.state.cloneCRD.kind == 'SGPostgresConfig')
				var endpoint = 'sgpgconfig'
			else
				var endpoint = store.state.cloneCRD.kind.toLowerCase()

			const res = axios
			.post(
				apiURL+endpoint, 
				store.state.cloneCRD.data 
			)
			.then(function (response) {
				//console.log("GOOD");
				notify(store.state.cloneCRD.kind+' <strong>"'+store.state.cloneCRD.data.metadata.name+'"</strong> cloned successfully', 'message', store.state.cloneCRD.kind.toLowerCase());

				vm.fetchAPI(endpoint);
				$('#clone').fadeOut().removeClass('show');
				$('#cloneName, #cloneNamespace').val('');
				//router.push('/cluster/status/'+store.state.cloneCRD.data.metadata.namespace+'/'+store.state.cloneCRD.data.metadata.name);
				
			})
			.catch(function (error) {
				console.log(error.response);
				notify(error.response.data,'error',endpoint);
				$('#clone').fadeOut().removeClass('show');
			});
		},
		flushToken: function() {
			document.cookie = 'sgToken=';
			store.commit('setLoginToken','401 Authentication Error');
			console.log('Flushed');
		}


	},
	mounted: function() {
		/* if( store.state.cloneCRD.data.length ) {
			//console.log('clone creado');
			this.cloneNamespace = store.state.cloneCRD.data.metadata.namespace;
			this.cloneName = store.state.cloneCRD.data.metadata.name;
		} */
	}
})