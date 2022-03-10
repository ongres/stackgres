<template>
	<div id="babelfish-compass" v-if="loggedIn && isReady && !notFound">
        <header>
            <ul class="breadcrumbs">
                <li class="namespace">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                    <router-link :to="'/' + $route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
                </li>
                <li>
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 19 19"><path fill="#36A8FF" d="M13.7 19H8.8c-.5 0-.9-.4-1-.9 0-.3 0-.7.1-1 0-.1.1-.2.1-.2.2-.3.4-.7.4-1.1 0-.3-.1-.6-.3-.8-.2-.2-.5-.3-.8-.3-.3 0-.6.1-.9.3-.2.2-.3.4-.3.7.1.4.2.7.4 1.1.1.1.1.2.1.3.1.3.1.6.1.9 0 .5-.5 1-1 1H1c-.6 0-1-.4-1-1V5.3c0-.6.4-1 1-1h3.5c-.1-.3-.2-.6-.3-1v-.1c0-.8.3-1.6.9-2.2.6-.7 1.4-1 2.2-1 .9 0 1.7.3 2.3.9.6.6.9 1.4.9 2.3v.1c-.1.3-.1.7-.3 1h3.5c.6 0 1 .4 1 1v3.5c.3-.1.6-.2 1-.3H16c.8 0 1.6.4 2.2 1s.9 1.4.8 2.2c0 .8-.4 1.6-1 2.2-.6.6-1.4.9-2.2.8h-.1c-.3-.1-.6-.1-1-.3V18c0 .6-.4 1-1 1zm-3.5-2h2.5v-3.9c0-.5.4-1 1-1 .3 0 .6 0 .9.1.1 0 .2.1.3.1.3.2.7.4 1.1.4.3 0 .5-.1.7-.3.2-.2.3-.5.4-.8 0-.3-.1-.6-.3-.8-.2-.2-.5-.3-.8-.4-.3.1-.7.2-1.1.4-.1.1-.2.1-.2.1-.3.1-.6.1-1 .1-.5 0-.9-.5-.9-1V6.2h-4c-.5 0-1-.4-1-1 0-.3 0-.6.1-.9 0 0 .1-.1.1-.2.3-.3.4-.7.5-1.1 0-.3-.1-.5-.3-.7-.2-.2-.5-.3-.8-.3-.4 0-.7.1-.9.3-.2.2-.3.5-.3.7.1.4.2.7.4 1.1.1.1.1.2.1.2.1.3.1.7.1 1 0 .5-.5.9-1 .9H2V17h2.5c-.1-.3-.2-.6-.3-1v-.1c0-.8.3-1.6.9-2.2.6-.6 1.4-.9 2.3-.9.8 0 1.6.3 2.2.9.6.6.9 1.4.9 2.3v.1l-.3.9z"/></svg>
                    Applications
                </li>
                <li>
                    Babelfish Compass
                </li>
            </ul>

            <div class="actions">
                <a class="documentation" href="https://github.com/babelfish-for-postgresql/babelfish_compass/" target="_blank" title="Babelfish Compass Documentation">Babelfish Compass Documentation</a>
            </div>
        </header>
        
        <form
            id="babelfishCompass"
            class="form"
            :class="processingFile && 'loading'"
            @submit.prevent>

            <div class="header step">
                <h2>Babelfish Compass</h2>
                <p>Babelfish Compass is a compatibility assessment tool for Babelfish for PostgreSQL. With Babelfish Compass, users can quickly analyze T-SQL DDL/SQL scripts for compatibility with Babelfish.</p>
                <p>The assessment report output file provides a detailed summary of the supported and unsupported SQL features in Babelfish for the analyzed SQL Server script(s).</p>
            </div>

           
                <label for="reportName">
                    Report Name
                    <span class="req">*</span>
                </label>
                <input 
                    v-model="reportName"
                    class="reportName"
                    data-field="reportName"
                    required
                >
                <span class="helpTooltip" data-tooltip="The name used by the Babelfish Compass tool to generate the report"></span>
                
                
                <div class="repeater">
                    <div class="header">
                        <h3>
                            SQL files
                            <span class="req">*</span>
                            <span class="helpTooltip" data-tooltip="One or more SQL/DDL scripts to be analyzed"></span>
                        </h3>
                    </div>
                    <fieldset v-if="sqlFilesLength.length">
                        <template v-for="(item, index) in sqlFilesLength">
                            <div class="header">
                                File #{{ index+1 }}
                                <a class="addRow" @click="sqlFilesLength.splice(index, 1)">Delete</a>
                            </div>
                            <input 
                                type="file" 
                                name="sqlFiles"
                                :id="item"
                                :ref="item"
                                @change="uploadFile" 
                            >
                        </template>
                    </fieldset>
                    <div class="fieldsetFooter" :class="!sqlFilesLength.length && 'topBorder'">
                        <a class="addRow" @click="addNewFile()">Add File</a>
                    </div>
                </div>
            <br/>
            <hr/>
            <button
                class="btn"
                @click="sendFiles()"
                :disabled="processingFile"
                >
                SEND
            </button>
        </form>

        <div 
            v-if="Object.keys(result).length"
            class="compassReport"
            >
            <div class="header">
                <h2>Report Results</h2>
            </div>
            <br/>
            <p class="warningText">
                <strong>Notice:</strong> <br/>
                This report contains an assessment based on the resources you scanned with the
                Babelfish Compass tool. The information contained in this report, including whether
                or not a feature is 'supported' or 'not supported', is made available 'as is',
                and may be incomplete, incorrect, and subject to interpretation.
                You should not base decisions on the information in this report without independently
                validating it against the actual SQL/DDL code on which this report is based.
            </p>
           
            <table class="setup clusterConfig">
                <tbody>
                    <tr>
                        <td class="label">
                            Report Name
                        </td>
                        <td class="value" colspan="2">
                            {{ reportName }}
                        </td>
                    </tr>
                    <tr v-for="(value, key, index) in result.setup">
                        <td 
                            v-if="!index"
                            class="label" 
                            :rowspan="Object.keys(result.setup).length">
                            Report Setup
                        </td>
                        <td class="label">
                            {{ key }}
                        </td>
                        <td class="value">
                            {{ value }}
                        </td>
                    </tr>
                    <tr v-for="(value, key, index) in result.metrics">
                        <td 
                            v-if="!index"
                            class="label" 
                            :rowspan="Object.keys(result.metrics).length">
                            Report Metrics
                        </td>
                        <td class="label">
                            {{ key }}
                        </td>
                        <td class="value">
                            {{ value }}
                        </td>
                    </tr>
                    <template v-for="(fileName, index) in result.files">
                        <tr>
                            <td 
                                v-if="!index"
                                class="label" 
                                :rowspan="Object.keys(result.files).length">
                                Files Processed
                            </td>
                            <td class="value" colspan="2">
                                {{ fileName }}
                            </td>
                        </tr>
                    </template>
                </tbody>
            </table>
           
            <pre 
                id="toc"
                class="monoFont"
                v-html="result.report"
            ></pre>
        </div>
	</div>
</template>

<script>
    import axios from 'axios'
	import { mixin } from '../mixins/mixin'

    export default {
        name: 'BabelfishCompass',

		mixins: [mixin],

		data: function() {

			return {
                reportName: '',
				sqlFiles: [],
                sqlFilesLength: ['file-0'],
                result: {},
                processingFile: false
			}
		},

        methods: {
            
            addNewFile() {
                this.sqlFilesLength.push('file-' + (this.sqlFilesLength.length + 1) );
                setTimeout(() => {
                    document.getElementById('file-' + this.sqlFilesLength.length).click();
                }, 100);
            },

            uploadFile(e) {
                this.sqlFiles.push(e.target.files[0]);
            },
            
            clearFile(index) {
                this.$refs['file-' + index][0].value = null;
                this.sqlFiles.splice(index, 1);
            },

            sendFiles() {
                let vc = this;

                vc.processingFile = true;
                vc.result = {};

                const formData = new FormData();
                formData.append('reportName', vc.reportName)
                vc.sqlFiles.forEach((file) => {
                    formData.append('sqlFiles', file)
                })

                axios
                .post(
                    '/stackgres/applications/com.ongres/babelfish-compass',
                    formData
                )
                .then( function(response){
                    let parser = new DOMParser();
                    let htmlDoc = parser.parseFromString(response.data.report, 'text/html');
                    vc.result = vc.parseCompassReport(htmlDoc.getElementsByTagName('pre')[0].innerHTML);
                    vc.cleanUp();
                }).catch(function(err) {
                    vc.notify('<p>There was an error when trying to process your file. Please try again!</p>', 'error');
                    console.log(err);
                    vc.cleanUp();
                });
            },

            parseCompassReport(content) {
                let result = {};
               
                // Truncate first part
                content = content.substring(
                    content.indexOf('BabelfishFeatures.cfg file'), 
                    content.length
                );
                
                // Get compass setup
                result['setup'] = {};
                let setup = content.substring(
                    0, 
                    content.indexOf('================================================================================')
                ).split('\n');

                for(let i = 0; i < setup.length; i+=2) {
                    let prop = setup[i].split(' : ');

                    if(prop[0].length && !prop[1].includes('/home/babelfish/')) {
                        result.setup[prop[0]] = prop[1];
                    }
                }

                // Get report
                result['report'] = content.substring(
                    content.indexOf('--------------------------------------------------------------------------------'),
                    content.indexOf('--- Run Metrics')
                );

                // Get metrics
                result['metrics'] = {};
                let metrics = content.substring(
                    content.indexOf('--- Run Metrics ----------------------------------------------------------------') + 80,
                    content.lastIndexOf('================================================================================')
                ).split('\n');

                for(let i = 0; i < metrics.length; i+=2) {
                    let prop = metrics[i].split(' : ');
                    
                    if(prop[0].length && !prop[1].includes('/home/babelfish/')) {
                        result.metrics[prop[0]] = prop[1];
                    }
                }
                
                result['files'] = [];
                this.sqlFiles.forEach( (file) => {
                    result['files'].push(file.name)
                });
                
                return result
            },

            cleanUp() {
                const vc = this;

                vc.sqlFiles.splice(0, vc.sqlFiles.length);
                vc.sqlFilesLength.splice(0, vc.sqlFilesLength.length);
                vc.name = '';
                vc.processingFile = false;
            }
        }
	}
</script>

<style scoped>
    #babelfishCompass {
        width: calc(35% - 40px);
        margin-right: 40px;
    }

    #babelfishCompass.loading > *{
        opacity: .4;
    }

    #babelfishCompass.loading:after {
        display: block;
        content: " ";
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: url('/assets/img/loader.gif') center center no-repeat;
        background-size: 40px;
    }


    #babelfishCompass h2 {
        margin-bottom: 10px;
    }

    input.reportName + .helpTooltip {
        transform: translate(20px, -50px);
    }

    input[type="file"] {
        padding: 7px 10px;
        margin-bottom: 20px;
        margin-top: -5px;
    }

    input[type="file"]:last-of-type {
        margin-bottom: 0;
    }

    .repeater > .header {
        margin-bottom: 10px;
    }

    .compassReport {
        display: inline-block;
        width: 65%;
        padding: 40px;
        border-left: 1px solid var(--borderColor);
        margin-top: -15px;
        overflow-y: auto;
        overflow-x: hidden;
        height: calc(100vh - 220px);
        scroll-behavior: smooth;
    }

    .compassReport pre {
        width: 100%;
        overflow-x: auto;
        border: 1px solid var(--borderColor);
        padding: 20px;
        margin-top: 30px;
        border-radius: 5px;
    }
</style>

<style>

    #toc [target="_blank"] {
        pointer-events: none;
    }

</style>