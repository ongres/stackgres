/**
 * Plugin Name : Accordion.JS
 * Version     : 2.1.0
 * Author      : ZeroWP Team
 * Author URL  : http://zerowp.com/
 * Plugin URL  : http://accordionjs.zerowp.com/
 * License     : MIT
 */
;(function ( $ ) {

	"use strict";

	$.fn.accordionjs = function( options ) {

		// Select all accordions that match a CSS selector
		if (this.length > 1){
			this.each(function() {
				$(this).accordionjs(options);
			});
			return this;
		}

		// Current accordion instance
		var accordion = this;

		// Setup utility functions
		var util = {

			/**
			 * Is integer
			 *
			 * Check if a value is a valid integer number
			 *
			 * @param {number} value
			 * @return {bool}
			 */
			isInteger:  function(value) {
				return typeof value === 'number' &&
					isFinite(value) &&
					Math.floor(value) === value;
			},

			//------------------------------------//--------------------------------------//

			/**
			 * Is array
			 *
			 * Check if a value is a valid array.
			 *
			 * @param {Array} arg
			 * @return {bool}
			 */
			isArray: function(arg) {
				return Object.prototype.toString.call(arg) === '[object Array]';
			},

			//------------------------------------//--------------------------------------//

			/**
			 * Is object
			 *
			 * Check if a value is a valid object.
			 *
			 * @param {Object} arg
			 * @return {bool}
			 */
			isObject: function isObject(arg) {
				return Object.prototype.toString.call(arg) === '[object Object]';
			},

			//------------------------------------//--------------------------------------//

			/**
			 * Sections is open
			 *
			 * Check if a section from current accordion is open.
			 *
			 * @param {Object}(jQuery) section
			 * @return {bool}
			 */
			sectionIsOpen: function( section ){
				return section.hasClass( 'acc_active' );
			},


			//------------------------------------//--------------------------------------//

			/**
			 * Get hash
			 *
			 * Get hash substring without # or false if the window does not have one.
			 *
			 * @return {string|bool(false)}
			 */
			getHash: function(){
				if(window.location.hash) {
					return window.location.hash.substring(1);
				}

				return false;
			},
		};

		/* Setup options
		---------------------*/
		var settings = $.extend({
			// Allow self close.
			closeAble   : false,

			// Close other sections.
			closeOther  : true,

			// Animation Speed.
			slideSpeed  : 150,

			// The section open on first init. A number from 1 to X or false.
			activeIndex : 1,

			// Callback when a section is open
			openSection: false, // function( section ){}

			// Callback before a section is open
			beforeOpenSection: false, // function( section ){}
		}, options );

		// Assign to accordion options data-* attributes if they exists
		$.each(settings, function( option ) {
			var data_attr = option.replace(/([A-Z])/g, '-$1').toLowerCase().toString(), //`optionsName` becomes `option-name`
			new_val       =  accordion.data( data_attr );

			if( new_val || false === new_val ){
				settings[ option ] = new_val;
			}
		});

		/*
		If the activeIndex is false then all sections are closed by default.
		If the closeOther is false then other section will not be closed when
		this is opened. That means, in both cases, sections should be able
		to be closed independently.
		*/
		if( settings.activeIndex === false || settings.closeOther === false ){
			settings.closeAble = true;
		}

		//------------------------------------//--------------------------------------//

		/**
		 * "Constructor"
		 *
		 * @return void
		 */
		var init = function() {
			accordion.create();
			accordion.openOnClick();

			$(window).on( 'load', function(){
				accordion.openOnHash();
			});

			$(window).on( 'hashchange', function(){
				accordion.openOnHash();
			});
		};

		//------------------------------------//--------------------------------------//

		/**
		 * Open section
		 *
		 * Open a single section.
		 *
		 * @param {Object}(jQuery) section The section to open
		 * @param {number} speed
		 * @return void
		 */
		this.openSection = function(section, speed){
			// Event before a section is opened
			$(document).trigger('accjs_before_open_section', [
				section,
			]);

			// Callback before a section is opened
			if( typeof settings.beforeOpenSection === "function" ){
				settings.beforeOpenSection.call(this, section);
			}

			// Setup the collapse speed
			speed = ( speed >= 0 ) ? speed : settings.slideSpeed;

			// Get the section content
			var section_content = section.children().eq(1); // .acc_content

			// Open the section
			section_content.slideDown( speed, function(){
				// Event when a section is opened
				$(document).trigger('accjs_open_section', [
					section,
				]);

				// Callback when a section is opened
				if( typeof settings.openSection === "function" ){
					settings.openSection.call(this, section);
				}
			} );

			// Make active
			section.addClass('acc_active');
		};

		//------------------------------------//--------------------------------------//

		/**
		 * Close section
		 *
		 * Close a single section.
		 *
		 * @param {Object}(jQuery) section The section to close
		 * @param {number} speed
		 * @return void
		 */
		this.closeSection = function(section, speed){
			// Event before a section is closed
			$(document).trigger('accjs_before_close_section', [
				section,
			]);

			// Callback before a section is closed
			if( typeof settings.beforeCloseSection === "function" ){
				settings.beforeCloseSection.call(this, section);
			}

			// Setup the collapse speed
			speed = ( speed >= 0 ) ? speed : settings.slideSpeed;

			// Get the section content
			var section_content = section.children().eq(1); // .acc_content

			// Open the section
			section_content.slideUp( speed, function(){
				// Event when a section is closed
				$(document).trigger('accjs_close_section', [
					section,
				]);

				// Callback when a section is closed
				if( typeof settings.closeSection === "function" ){
					settings.closeSection.call(this, section);
				}

			} );

			// Make inactive
			section.removeClass('acc_active');

		};

		//------------------------------------//--------------------------------------//

		/**
		 * Close other sections except this one
		 *
		 * @param {Object}(jQuery) section The section to exclude
		 * @param {number} speed
		 * @return void
		 */
		this.closeOtherSections = function(section, speed){
			var this_acc = section.closest('.accordionjs').children();
			$(this_acc).each(function() {
				accordion.closeSection( $(this).not(section), speed );
			});
		};

		//------------------------------------//--------------------------------------//

		/**
		 * Create the accordion
		 *
		 * Create the accordion structure. Add the necessary CSS classes and other stuff.
		 *
		 * @return void
		 */
		this.create = function() {

			//Add Main CSS Class
			accordion.addClass('accordionjs');

			// Get all current accordion sections
			var accordion_sections = accordion.children();

			//Add classes to accordion head and content for each section
			$.each( accordion_sections, function(index, elem){
				accordion.createSingleSection( $(elem) );
			});

			// //Active index
			if( util.isArray( settings.activeIndex ) ){
				var indexes = settings.activeIndex;
				for (var i = 0; i < indexes.length; i++) {
					accordion.openSection( accordion_sections.eq( indexes[i] - 1 ), 0 );
				}
			}
			else if( settings.activeIndex > 1 ){
				accordion.openSection( accordion_sections.eq( settings.activeIndex - 1 ), 0 );
			}
			else if( false !== settings.activeIndex ){
				accordion.openSection( accordion_sections.eq( 0 ), 0 );
			}

		};

		//------------------------------------//--------------------------------------//

		/**
		 * Create a single section
		 *
		 * Create the structure of a single section by adding the necessary CSS classes.
		 *
		 * @param {string} section The section to create. jQuery object.
		 * @return void
		 */
		this.createSingleSection = function( section ) {
			var childs = section.children();

			// Create sections if they were not created already
			section.addClass('acc_section');

			// Add the necessary CSS classes
			$(childs[0]).addClass('acc_head');
			$(childs[1]).addClass('acc_content');

			// Collapse section content.
			// Only if it does not have `.acc_active` CSS class set by default.
			if( ! section.hasClass('acc_active') ) {
				section.children('.acc_content').hide();
			}
		};

		//------------------------------------//--------------------------------------//

		/**
		 * Open on click
		 *
		 * Open a section when its header get a click.
		 *
		 * @return void
		 */
		this.openOnClick = function() {

			accordion.on('click', '.acc_head', function( event ){
				event.stopImmediatePropagation();

				var section = $(this).closest('.acc_section');
				if( util.sectionIsOpen( section ) ) {

					// If closeAble, then close this section but do not touch other.
					if( settings.closeAble ) {
						accordion.closeSection( section );
					}

					// If the accordion contains only one section, act like a toggle.
					else if( accordion.children().length === 1 ) {
						accordion.closeSection( section );
					}

				}

				// Section is closed
				else {
					// If closeOther, then close other sections when this is opened.
					if( settings.closeOther ) {
						accordion.closeOtherSections( section );
						accordion.openSection( section );
					}

					// Else open only this section and do not touch other sections.
					else {
						accordion.openSection( section );
					}
				}

			});

		};

		//------------------------------------//--------------------------------------//

		/**
		 * Open a section if a hash is present in URL and scroll to it.
		 *
		 * @return void
		 */
		this.openOnHash = function() {
			if( util.getHash() ) {
				var section = $( '#' + util.getHash() );
				if( section.hasClass('acc_section') ) {
					accordion.openSection( section );
					if( settings.closeOther ) {
						accordion.closeOtherSections( section );
					}
					$("html, body").animate({
						scrollTop: parseInt( section.offset().top ) - 50,
					}, 150);
				}
			}
		};

		//"Constructor" init
		init();
		return this;

	};

}( jQuery ));
