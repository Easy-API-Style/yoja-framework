/* ---- nav email button: copy address + show "Copied!" in the tooltip ---- */
(function () {
  var btn = document.querySelector('.nav-email-button');
  if (!btn) return;

  function fallbackCopy(text, done) {
    var ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.top = '-9999px';
    ta.setAttribute('readonly', '');
    document.body.appendChild(ta);
    ta.select();
    try { document.execCommand('copy'); done(); } catch (e) {}
    document.body.removeChild(ta);
  }

  var resetTimer;
  function showCopied() {
    btn.setAttribute('data-tooltip', 'Copied ' + btn.dataset.email);
    btn.classList.add('is-copied');
    clearTimeout(resetTimer);
    resetTimer = setTimeout(function () {
      btn.classList.remove('is-copied');
      btn.setAttribute('data-tooltip', 'Copy email');
    }, 1600);
  }

  btn.addEventListener('click', function () {
    var email = btn.dataset.email;
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(email).then(showCopied, function () { fallbackCopy(email, showCopied); });
    } else {
      fallbackCopy(email, showCopied);
    }
  });
})();

document.querySelector('.theme-toggle-button').addEventListener('click', function () {
  var current = document.documentElement.getAttribute('data-theme') || 'dark';
  var next = current === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  try { localStorage.setItem('yoja-theme', next); } catch (e) {}
});
