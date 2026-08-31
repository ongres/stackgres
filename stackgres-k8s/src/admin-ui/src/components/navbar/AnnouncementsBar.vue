<template>
	<div id="announcements" ref="bar" v-if="visibleAnnouncements.length">
		<div
			class="announcement"
			:class="currentAnnouncement.severity"
			:role="currentAnnouncement.severity === 'info' ? 'status' : 'alert'"
			@mouseenter="stopAutoplay()"
			@mouseleave="startAutoplay"
		>
			<div class="announcement-content">
				<transition :name="'slide-' + direction" mode="out-in">
					<div class="announcement-slide" :key="currentAnnouncement.id">
						<span class="announcement-icon" aria-hidden="true" v-if="icons[currentAnnouncement.severity]" v-html="icons[currentAnnouncement.severity]"></span>
						<vue-markdown class="announcement-message" :source="currentAnnouncement.message" :breaks="false"></vue-markdown>
					</div>
				</transition>
			</div>

			<div class="announcement-controls">
				<template v-if="visibleAnnouncements.length > 1">
					<button class="announcement-nav plain" aria-label="Previous announcement" @click="move(-1)">‹</button>
					<span class="announcement-position">{{ currentIndex + 1 }}/{{ visibleAnnouncements.length }}</span>
					<button class="announcement-nav plain" aria-label="Next announcement" @click="move(1)">›</button>
				</template>
				<button
					class="announcement-dismiss plain"
					:disabled="currentAnnouncement.dismissible === false"
					aria-label="Dismiss announcement"
					:title="currentAnnouncement.dismissible === false ? 'This announcement cannot be dismissed' : 'Dismiss'"
					@click="dismiss(currentAnnouncement.id)"
				>
					✕
				</button>
			</div>
		</div>
	</div>
</template>

<script>
	import store from '../../store'
	import VueMarkdown from 'vue-markdown'

	export default {
		name: 'AnnouncementsBar',

		components: {
			VueMarkdown
		},

		data: function() {
			return {
				index: 0,
				autoplayId: null,
				autoplayStopped: false,
				direction: 'next',
				icons: {
					warning: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><path d="M29 29H11c-.4 0-.7-.2-.9-.5-.2-.3-.2-.7 0-1l9-16c.3-.5.9-.6 1.4-.3.1.1.2.2.3.3l9 16c.2.3.2.7 0 1-.3.3-.6.5-1 .5zM12.8 26.9h14.5L20 14.1 12.8 26.9z"/><path d="M19.9 17.1h.2c.6 0 1 .4 1 .9v3.5c0 .5-.4.9-.9.9h-.2c-.5 0-.9-.4-.9-.9V18c-.1-.5.3-.9.8-.9z"/><path d="M20 23.4c.6 0 1 .4 1 1s-.4 1-1 1-1-.4-1-1 .4-1 1-1z"/></svg>',
					error: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><path d="M20 8a12 12 0 1012 12A12 12 0 0020 8zm0 21.9A9.9 9.9 0 1129.9 20 9.9 9.9 0 0120 29.9z"/><path d="M20 12.5a1 1 0 00-1 1v8a1 1 0 002 0v-8a1 1 0 00-1-1zM20 24.5a1.2 1.2 0 101.2 1.2 1.2 1.2 0 00-1.2-1.2z"/></svg>'
				}
			}
		},

		computed: {

			visibleAnnouncements() {
				const severityRank = { error: 0, warning: 1, info: 2 };
				const now = Date.now();

				return store.state.announcements
					.filter((announcement) => {
						if (announcement.validUntil && (now > new Date(announcement.validUntil).getTime())) {
							return false;
						}

						const dismissedAt = store.state.announcementDismissals[announcement.id];
						if (dismissedAt) {
							return Boolean(announcement.reappearAfter) && (now > (dismissedAt + (announcement.reappearAfter * 1000)));
						}

						return true;
					})
					.sort((a, b) => (
						(severityRank.hasOwnProperty(a.severity) ? severityRank[a.severity] : 3) -
						(severityRank.hasOwnProperty(b.severity) ? severityRank[b.severity] : 3)
					));
			},

			currentIndex() {
				return Math.min(this.index, this.visibleAnnouncements.length - 1);
			},

			currentAnnouncement() {
				return this.visibleAnnouncements[this.currentIndex];
			}

		},

		methods: {

			move(step) {
				const total = this.visibleAnnouncements.length;
				this.direction = step > 0 ? 'next' : 'prev';
				this.index = (this.currentIndex + step + total) % total;
				this.stopAutoplay(true);
			},

			dismiss(id) {
				if (this.currentAnnouncement.dismissible === false) {
					return;
				}
				this.direction = 'next';
				store.commit('dismissAnnouncement', id);
				this.stopAutoplay(true);
			},

			startAutoplay() {
				if (!this.autoplayStopped && !this.autoplayId && (this.visibleAnnouncements.length > 1)) {
					this.autoplayId = setInterval(() => {
						const total = this.visibleAnnouncements.length;
						this.direction = 'next';
						this.index = (this.currentIndex + 1 + total) % total;
					}, 4000);
				}
			},

			/* pass true when the user interacted: autoplay stops for good */
			stopAutoplay(forGood) {
				clearInterval(this.autoplayId);
				this.autoplayId = null;
				if (forGood === true) {
					this.autoplayStopped = true;
				}
			},

			/* The console chrome (#nav, #side) is position:fixed, so the bar
			   cannot push it down from the normal flow: instead the bar is
			   fixed above it and publishes its height as --announcementsHeight,
			   which the layout offsets consume (see the global style block). */
			publishHeight() {
				const height = this.$refs.bar ? this.$refs.bar.offsetHeight : 0;
				document.documentElement.style.setProperty('--announcementsHeight', height + 'px');
			}

		},

		watch: {

			visibleAnnouncements: {
				handler() {
					this.$nextTick(this.publishHeight);
				},
				immediate: true
			},

			currentIndex() {
				this.$nextTick(this.publishHeight);
			}

		},

		mounted() {
			window.addEventListener('resize', this.publishHeight);
			this.startAutoplay();
		},

		destroyed() {
			this.stopAutoplay();
			window.removeEventListener('resize', this.publishHeight);
			document.documentElement.style.setProperty('--announcementsHeight', '0px');
		}
	}
</script>

<style scoped>
	#announcements {
		position: fixed;
		top: 0;
		left: 0;
		width: 100vw;
		z-index: 30;
	}

	/* Same visual language as the in-view notices (form .warning /
	   .warningText): translucent --blue tint + blue border, so it adapts to
	   the light and dark chromes alike — severity reads from the icon color. */
	.announcement {
		position: relative;
		display: flex;
		align-items: center;
		/* fixed height: slides differ in intrinsic height (code pills, bold)
		   and the out-in transition empties the bar for a moment — without
		   this the chrome below would jump on every slide change */
		height: 38px;
		padding: 0 130px;
		/* the tint layered over an opaque base: content scrolls beneath the
		   fixed bar and must not show through */
		background: linear-gradient(rgba(0, 173, 181, .08), rgba(0, 173, 181, .08)), #fff;
		border-bottom: 1px solid var(--blue);
	}

	.announcement-content {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
	}

	.announcement-slide {
		display: flex;
		align-items: center;
		gap: 9px;
		white-space: nowrap;
	}

	.announcement-icon {
		display: inline-flex;
		width: 22px;
		height: 22px;
		flex: none;
	}

	.announcement-controls {
		position: absolute;
		right: 15px;
		top: 50%;
		transform: translateY(-50%);
		display: flex;
		align-items: center;
		gap: 4px;
	}

	.announcement-position {
		font-size: 11px;
		opacity: .55;
	}

	.announcement-nav,
	.announcement-dismiss {
		cursor: pointer;
		font-size: 15px;
		line-height: 1;
		padding: 0 6px;
		color: inherit;
		opacity: .6;
		background: none;
		border: none;
	}

	.announcement-nav:hover,
	.announcement-dismiss:hover {
		opacity: 1;
	}

	.announcement-dismiss:disabled {
		opacity: .35;
		cursor: not-allowed;
	}

	.slide-next-enter-active, .slide-next-leave-active,
	.slide-prev-enter-active, .slide-prev-leave-active {
		transition: transform .25s ease, opacity .25s ease;
	}

	.slide-next-enter, .slide-prev-leave-to {
		transform: translateX(60px);
		opacity: 0;
	}

	.slide-next-leave-to, .slide-prev-enter {
		transform: translateX(-60px);
		opacity: 0;
	}
</style>

<style>
	/* Shift the fixed console chrome down by the announcements bar height.
	   --announcementsHeight is set by AnnouncementsBar (0 when hidden). */
	#app #nav {
		top: var(--announcementsHeight, 0px);
	}

	#app #side {
		top: calc(50px + var(--announcementsHeight, 0px));
		height: calc(100vh - 50px - var(--announcementsHeight, 0px));
	}

	#app #main {
		padding-top: calc(75px + var(--announcementsHeight, 0px));
	}

	/* dropdowns anchored to the nav are position:fixed with hardcoded tops */
	#app #notifications .tooltip,
	#app #delete .tooltip,
	#app #help .tooltip {
		top: calc(60px + var(--announcementsHeight, 0px));
	}

	#announcements .announcement-icon svg {
		width: 100%;
	}

	#announcements .announcement.warning .announcement-icon svg {
		fill: #F2A90C; /* amber; no warning token in the console palette */
	}

	#announcements .announcement.error .announcement-icon svg {
		fill: #DB3A2F; /* legible red on the tinted bar (--red is too harsh) */
	}

	.darkmode #announcements .announcement {
		background: linear-gradient(rgba(0, 173, 181, .1), rgba(0, 173, 181, .1)), #171717;
	}

	#announcements .announcement-message p {
		display: inline;
		margin: 0;
	}

	#announcements .announcement-message code {
		font-family: 'Courier New', monospace;
		font-size: .9em;
		background: rgba(2, 14, 20, .07);
		border-radius: 4px;
		padding: 1px 6px;
	}

	.darkmode #announcements .announcement-message code {
		background: rgba(255, 255, 255, .12);
	}

	#announcements .announcement-message a {
		color: var(--blue);
		font-weight: 600;
	}

	#announcements .announcement-message a:hover {
		text-decoration: underline;
	}
</style>
