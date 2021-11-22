var lunrIndex, pagesIndex;

function endsWith(str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
}

// Initialize lunrjs using our generated index file
function initLunr() {
    if (!endsWith(baseurl,"/")){
        baseurl = baseurl+'/'
    };

    // First retrieve the index file
    $.ajax({
        url: baseurl +"index.json",
    }).done(function(index) {
        pagesIndex = index;
        
        // Set up lunrjs by declaring the fields we use
        // Also provide their boost level for the ranking
        lunrIndex = lunr(function() {
            this.ref("uri");
            this.field('title', {
        boost: 15
            });
            this.field('tags', {
        boost: 10
            });
            this.field("content", {
        boost: 5
            });
            
            this.pipeline.remove(lunr.stemmer);
            this.searchPipeline.remove(lunr.stemmer);
            
            // Feed lunr with each file and let lunr actually index them
            pagesIndex.forEach(function(page) {
                this.add(page);

                // Set version selector URL
                if(page.uri == window.location.href) {

                    let currentVersion = baseurl.includes('latest') ? 'latest' : $('#sgVersion option:selected').text();
                    $('#sgVersion option:not(:selected)').each(function(index, alt) {

                        let altVersion = alt.text.replace(' (development)','');

                        if(baseurl.includes('localhost')) { // If testing locally
                            var altVersionIndex = baseurl+'index-'+altVersion+'.json';
                        } else if(baseurl.includes('stackgres.io')) { // If on Live site 
                            var altVersionIndex = baseurl.replace(currentVersion, altVersion) + '/index.json';
                        } 

                        $.ajax({
                            url: altVersionIndex,
                            dataType: 'text'
                        }).done(function(altVersionIndex){
                            altVersionIndex = JSON.parse(altVersionIndex.replace(/[\n\r\t]/g,""))
                            let vPage = altVersionIndex.find(p => (p.title == page.title))
                            
                            if(vPage !== undefined) {
                                $(alt).val(vPage.uri)
                            } else {
                                $(alt).val( baseurl.replace(currentVersion, altVersion) + '?not-found=1');
                            }
                        })
                    });
                }
                    
            }, this);
        })
    }).fail(function(jqxhr, textStatus, error) {
        var err = textStatus + ", " + error;
        console.error("Error getting Hugo index file:", err);
    });
}

/**
 * Trigger a search in lunr and transform the result
 *
 * @param  {String} query
 * @return {Array}  results
 */
function search(queryTerm) {
    // Find the item in our index corresponding to the lunr one to have more info
    return lunrIndex.search(queryTerm+"^100"+" "+queryTerm+"*^10"+" "+"*"+queryTerm+"^10"+" "+queryTerm+"~2^1").map(function(result) {
            return pagesIndex.filter(function(page) {
                return page.uri === result.ref;
            })[0];
        });
}

// Let's get started
initLunr();
$( document ).ready(function() {
    var searchList = new autoComplete({
        /* selector for the search box element */
        selector: $("#search-by").get(0),
        /* source is the callback to perform the search */
        source: function(term, response) {
            response(search(term));
        },
        /* renderItem displays individual search results */
        renderItem: function(item, term) {
            var numContextWords = 2;
            var text = item.content.match(
                "(?:\\s?(?:[\\w]+)\\s?){0,"+numContextWords+"}" +
                    term+"(?:\\s?(?:[\\w]+)\\s?){0,"+numContextWords+"}");
            item.context = text;
            return '<div class="autocomplete-suggestion" ' +
                'data-term="' + term + '" ' +
                'data-title="' + item.title + '" ' +
                'data-uri="'+ item.uri + '" ' +
                'data-context="' + item.context + '">' +
                '» ' + item.title +
                '<div class="context">' +
                (item.context || '') +'</div>' +
                '</div>';
        },
        /* onSelect callback fires when a search suggestion is chosen */
        onSelect: function(e, term, item) {
            location.href = item.getAttribute('data-uri');
        }
    });
});
