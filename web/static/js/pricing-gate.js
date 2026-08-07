// Email gate for the pricing calculator.
// Custom form → HubSpot Forms API (region-aware). Unlock rules:
//   - successful submission → localStorage unlock, 90 days
//   - HubSpot unreachable/timeout (ad-blockers) → sessionStorage fail-open,
//     retried next visit
//   - HubSpot 4xx → inline error (config problem; do NOT fail open)
(function () {
  var UNLOCK_KEY = 'sg-pricing-gate';
  var SESSION_KEY = 'sg-pricing-gate-session';
  var UNLOCK_MS = 90 * 24 * 60 * 60 * 1000;
  var SUBMIT_TIMEOUT_MS = 3000;
  var EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

  var gate = document.getElementById('pricing-gate');
  var content = document.getElementById('gated-content');
  if (!gate || !content) return;

  function reveal() {
    gate.hidden = true;
    content.hidden = false;
    var actions = document.querySelector('.hero-actions');
    if (actions) actions.hidden = false;
    // the commercial-licensing card takes the gate's spot beside the hero
    var aside = document.getElementById('heroAside');
    if (aside) aside.hidden = false;
    if (typeof window.syncUrl === 'function') {
      try { window.syncUrl(); } catch (e) {}
    }
  }

  function isUnlocked() {
    try {
      var raw = localStorage.getItem(UNLOCK_KEY);
      if (raw) {
        var data = JSON.parse(raw);
        if (data && data.ts && (Date.now() - data.ts) < UNLOCK_MS) return true;
        localStorage.removeItem(UNLOCK_KEY);
      }
      if (sessionStorage.getItem(SESSION_KEY)) return true;
    } catch (e) { /* storage unavailable → keep gate */ }
    return false;
  }

  function getCookie(name) {
    var m = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
    return m ? decodeURIComponent(m[1]) : null;
  }

  var portalId = gate.getAttribute('data-portal-id');
  var formId = gate.getAttribute('data-form-id');
  var region = gate.getAttribute('data-region') || 'na1';
  var apiHost = region === 'na1' ? 'https://api.hsforms.com' : 'https://api-' + region + '.hsforms.com';

  // shared estimate links (?e=<token>, or legacy plain params) preload the
  // shared configuration, but the gate still applies — every viewer is
  // captured before the pricing is revealed
  var params = new URLSearchParams(location.search);
  var isSharedLink = params.has('e') || params.has('cores') || params.has('view');

  // no portal configured → gate disabled entirely (safety valve)
  if (!portalId || isUnlocked()) {
    reveal();
    if (isSharedLink) content.scrollIntoView();
    return;
  }
  gate.hidden = false;
  if (isSharedLink) {
    var sharedTitle = gate.getAttribute('data-shared-title');
    var sharedLead = gate.getAttribute('data-shared-lead');
    var titleEl = gate.querySelector('h3');
    var leadEl = gate.querySelector('.gate-lead');
    if (titleEl && sharedTitle) titleEl.textContent = sharedTitle;
    if (leadEl && sharedLead) leadEl.textContent = sharedLead;
  }
  var heroActions = document.querySelector('.hero-actions');
  if (heroActions) heroActions.hidden = true;

  var fullNameInput = document.getElementById('gate-fullname');
  var emailInput = document.getElementById('gate-email');
  var form = document.getElementById('pricing-gate-form');
  var consentInput = document.getElementById('gate-consent');
  var errorEl = document.getElementById('gate-error');
  var submitBtn = document.getElementById('gate-submit');

  function showError(msg) {
    errorEl.textContent = msg;
    errorEl.hidden = false;
  }

  form.addEventListener('submit', function (ev) {
    ev.preventDefault();
    errorEl.hidden = true;

    var fullName = (fullNameInput.value || '').trim();
    var email = (emailInput.value || '').trim();
    if (!fullName) {
      showError('Please enter your full name.');
      fullNameInput.focus();
      return;
    }
    if (!EMAIL_RE.test(email)) {
      showError('Please enter a valid email address.');
      emailInput.focus();
      return;
    }
    if (!consentInput.checked) {
      showError('Please accept the privacy policy to continue.');
      return;
    }

    submitBtn.disabled = true;

    var consentText = document.getElementById('gate-consent-text').textContent.replace(/\s+/g, ' ').trim();
    var payload = {
      fields: [
        // the form's renamed "Full name" field still maps to firstname
        { objectTypeId: '0-1', name: 'firstname', value: fullName },
        { objectTypeId: '0-1', name: 'email', value: email }
      ],
      context: {
        pageUri: window.location.href,
        pageName: document.title
      },
      legalConsentOptions: {
        consent: {
          consentToProcess: true,
          text: consentText
        }
      }
    };
    var hutk = getCookie('hubspotutk');
    if (hutk) payload.context.hutk = hutk;

    var controller = new AbortController();
    var timer = setTimeout(function () { controller.abort(); }, SUBMIT_TIMEOUT_MS);

    fetch(apiHost + '/submissions/v3/integration/submit/' + portalId + '/' + formId, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      signal: controller.signal
    }).then(function (res) {
      clearTimeout(timer);
      if (res.ok) {
        try { localStorage.setItem(UNLOCK_KEY, JSON.stringify({ ts: Date.now() })); } catch (e) {}
        reveal();
        content.scrollIntoView({ behavior: 'smooth', block: 'start' });
      } else {
        // config/validation problem — surface it, do not fail open
        submitBtn.disabled = false;
        showError('Something went wrong submitting the form. Please try again or contact us.');
        res.text().then(function (t) { console.error('pricing-gate: HubSpot rejected submission', res.status, t); });
      }
    }).catch(function (err) {
      clearTimeout(timer);
      // network error / timeout / blocked → fail open for this session only
      console.warn('pricing-gate: HubSpot unreachable, failing open for the session', err);
      try { sessionStorage.setItem(SESSION_KEY, '1'); } catch (e) {}
      reveal();
      content.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  });
})();
