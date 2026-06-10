document.querySelector('.theme-toggle-button').addEventListener('click', function () {
  var current = document.documentElement.getAttribute('data-theme') || 'dark';
  var next = current === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  try { localStorage.setItem('yoja-theme', next); } catch (e) {}
});
