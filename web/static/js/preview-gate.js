// Preview-only access gate. Client-side: deters casual browsing and crawlers,
// does NOT cryptographically protect content. Loaded only when the site is
// built with HUGO_PARAMS_previewgate=true (see .github/workflows/website-preview.yml).
(function () {
  var KEY = 'sg-preview-auth';
  var HASH = 6954021903221; // djb2 of the preview password
  function djb2(s) {
    var h = 5381;
    for (var i = 0; i < s.length; i++) h = h * 33 + s.charCodeAt(i);
    return h;
  }
  try {
    if (sessionStorage.getItem(KEY) === String(HASH)) return;
  } catch (e) { /* storage unavailable → always prompt */ }
  for (var tries = 0; tries < 3; tries++) {
    var p = window.prompt('This is a preview site. Enter the preview password:');
    if (p !== null && djb2(p) === HASH) {
      try { sessionStorage.setItem(KEY, String(HASH)); } catch (e) {}
      return;
    }
  }
  document.documentElement.innerHTML =
    '<head><title>Preview</title></head><body style="font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0">' +
    '<div style="text-align:center"><h1>Preview locked</h1><p>Reload the page to try again.</p></div></body>';
  window.stop && window.stop();
})();
