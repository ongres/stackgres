var lunrIndex, pagesIndex, lunrIndexPromise;

function endsWith(str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
}

function normalizeBaseUrl(url) {
    return endsWith(url, "/") ? url : url + '/';
}

// Retrieve the pages index (index.json) of a documentation version.
// The index is parsed as text since older versions may contain unescaped control characters.
function fetchPagesIndex(versionBaseUrl) {
    return $.ajax({
        url: normalizeBaseUrl(versionBaseUrl) + "index.json",
        dataType: 'text'
    }).then(function(text) {
        return JSON.parse(text.replace(/[\n\r\t]/g, ""));
    });
}

// Initialize lunrjs using our generated index file.
// The index is built lazily the first time the search box is used: index.json contains the
// content of the whole site and indexing it on every page load is too expensive for
// low-end devices (and unnecessary for the vast majority of page views).
function initLunr() {
    if (lunrIndexPromise) {
        return lunrIndexPromise;
    }

    lunrIndexPromise = fetchPagesIndex(baseurl).then(function(index) {
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
            }, this);
        });

        return lunrIndex;
    }, function(jqxhr, textStatus, error) {
        var err = textStatus + ", " + error;
        console.error("Error getting Hugo index file:", err);
        lunrIndexPromise = null; // allow to retry on next search
    });

    return lunrIndexPromise;
}

/**
 * Trigger a search in lunr and transform the result
 *
 * @param  {String} query
 * @return {Array}  results
 */
function search(queryTerm) {
    if (!lunrIndex) {
        return [];
    }
    // Find the item in our index corresponding to the lunr one to have more info
    return lunrIndex.search(queryTerm+"^100"+" "+queryTerm+"*^10"+" "+"*"+queryTerm+"^10"+" "+queryTerm+"~2^1").map(function(result) {
            return pagesIndex.filter(function(page) {
                return page.uri === result.ref;
            })[0];
        });
}

/**
 * Find the page with the given title in another documentation version
 *
 * @param  {String} versionBaseUrl base URL of the documentation version
 * @param  {String} title          title of the page to look for
 * @return {Promise} resolved with the URI of the page, or undefined if not found
 */
function findPageInVersion(versionBaseUrl, title) {
    return fetchPagesIndex(versionBaseUrl).then(function(index) {
        var page = index.find(function(p) {
            return p.title == title;
        });
        return page !== undefined ? page.uri : undefined;
    });
}

// Let's get started
$( document ).ready(function() {
    var searchInput = $("#search-by");

    // Start loading the index as soon as the user shows the intention to search
    searchInput.one('focus', function() {
        initLunr();
    });

    var searchList = new autoComplete({
        /* selector for the search box element */
        selector: searchInput.get(0),
        /* source is the callback to perform the search */
        source: function(term, response) {
            initLunr().then(function() {
                response(search(term));
            });
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
