$(document).ready(function(){
    if($('.postgresExtensions').length) {

        // Get postgres extensions file
        $.ajax({
            url: "https://extensions.stackgres.io/postgres/repository/v2/index.json",
        }).done(function(extIndex) {
            let extensions = extIndex.extensions
                .sort((a,b) => (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0))
            let postgresVersions = extensions
                .flatMap(e => e.versions)
                .flatMap(v => v.availableFor)
                .reduce((pgs,af) => {
                  if (pgs.find(pg => pg == af.postgresVersion.replace(/^([^.]+)(\.[^.]+)?$/, "$1")) == null) {
                    pgs.push(af.postgresVersion.replace(/^([^.]+)(\.[^.]+)?$/, "$1"))
                  }
                  return pgs
                }, [])
                .sort()
            let tableHtml = `
                <table>
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Versions</th>
                            <th>Description</th>
                        </tr>
                    </thead>
                    <tbody>`;
    
            extensions.forEach(ext => {

                tableHtml += `
                    <tr>
                        <td><a href="` + ext.url + `" target="_blank">` + ext.name + `</a></td>`;
                
                tableHtml += '<td>' + postgresVersions
                  .reduce((tds, pg) => {
                    versions = ext.versions
                      .reduce((vs, v) => {
                        if (v.availableFor.find(af => af.postgresVersion.split(".")[0] == pg)
                            && vs.find(vv => vv == v.version) == null) {
                          vs.push(v.version)
                        }                   
                        return vs         
                      }, [])
                      .map(v => {
                        return {
                          v: v,
                          sv: v.split(".").concat(Array(16).fill("")).slice(0,16).reduce((sv, v) => sv + v.padStart(8, "0"), "")
                        }
                      })
                      .sort(v => v.sv)
                      .map(v => v.v)
                    if (!versions.length) {
                      return ""
                    }
                    return tds + '<b>PG ' + pg + '</b>: ' + versions.join(", ") + '</br>'
                  }, "") + '</td>'

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