$(document).ready(function(){
    if($('.postgresExtensions').length) {

        // Get postgres extensions file
        $.ajax({
            url: "https://extensions.stackgres.io/postgres/repository/index.json",
        }).done(function(extIndex) {
            let extensions = extIndex.extensions.sort((a,b) => (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0))
    
            let tableHtml = `
                <table>
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>PG 13</th>
                            <th>PG 12</th>
                            <th>PG 11</th>
                            <th>Description</th>
                        </tr>
                    </thead>
                    <tbody>`;
    
            extensions.forEach(ext => {

                tableHtml += `
                    <tr>
                        <td><a href="` + ext.url + `" target="_blank">` + ext.name + `</a></td>`;

                /* Postgres 13 */
                let pg13 = [];
                
                ext.versions.forEach(v => {
                    v.availableFor.forEach( pg => {
                        if( (pg.postgresVersion.split(".")[0] == "13") && !pg13.includes(v.version) ) {
                            pg13.push(v.version)
                        }
                    })
                })

                tableHtml += '<td>' + pg13.join(', ') + '</td>';

                /* Postgres 12 */
                let pg12 = [];
                
                ext.versions.forEach(v => {
                    v.availableFor.forEach( pg => {
                        if( (pg.postgresVersion.split(".")[0] == "12") && !pg12.includes(v.version) ) {
                            pg12.push(v.version)
                        }
                    })
                })

                tableHtml += '<td>' + pg12.join(', ') + '</td>';                
                        
                /* Postgres 11 */
                let pg11 = [];

                ext.versions.forEach(v => {
                    v.availableFor.forEach( pg => {
                        if( (pg.postgresVersion.split(".")[0] == "11") && !pg11.includes(v.version) ) {
                            pg11.push(v.version)
                        }
                    })
                })

                tableHtml += '<td>' + pg11.join(', ') + '</td>';

                tableHtml += `<td class="capitalFirstLetter">` + ext.description + `</td>
                </tr>`;


            });
            
            tableHtml += `
                    </tbody>
                </table>`;
        
            $('.postgresExtensions').html(tableHtml);
        }).fail(function(jqxhr, textStatus, error) {
            var err = textStatus + ", " + error;
            $('.postgresExtensions').html('<p class="warning">An error was found when trying to fetch the extensions list.<br/>Please refresh the page and try again.</p>');
            console.error("Error getting postgres extensions list: ", err);
        });
    
    }
})