(function () {
  try {
    var params = new URLSearchParams(window.location.search);
    if (params.get("theme") === "thesis") {
      document.body.classList.add("theme-thesis");
    }
  } catch (e) {
    // no-op for static mockup
  }
})();
