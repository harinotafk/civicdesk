// Officer desk: complaints in my ward (ward officer) or zone (zonal officer), plus numbers.
(function () {
  "use strict";

  const user = UI.requireRole(["WARD_OFFICER", "ZONAL_OFFICER"]);
  if (!user) return;

  const overview = document.getElementById("tab-overview");
  let latest = null;

  async function loadNumbers() {
    try {
      latest = await Api.analytics();
      document.getElementById("scope-title").textContent = latest.scope;
      const s = latest.summary;
      document.getElementById("kpis-mini").innerHTML =
        kpi("Open complaints", s.open, false) +
        kpi("Overdue right now", s.overdue, s.overdue > 0) +
        kpi("Escalated and still open", s.escalatedOpen, s.escalatedOpen > 0);
      Dash.renderKpis(document.getElementById("kpis"), latest);
      if (!overview.classList.contains("hidden")) Dash.renderCharts(overview, latest);
    } catch (err) {
      UI.toast(err.message, true);
    }
  }

  function kpi(label, value, alert) {
    return '<div class="kpi' + (alert ? " alert" : "") + '"><div class="value">' + UI.esc(value) +
      '</div><div class="label">' + UI.esc(label) + "</div></div>";
  }

  const board = Board.mount(document.getElementById("board"), user, loadNumbers);

  let policiesMounted = false;
  Tabs.init(function (tab) {
    if (tab === "policies" && !policiesMounted) { policiesMounted = true; Policies.mountReader(document.getElementById("policies-reader")); }
    if (tab === "overview" && latest) Dash.renderCharts(overview, latest);
  });

  document.getElementById("refresh").addEventListener("click", function () {
    board.reload();
    loadNumbers();
  });

  loadNumbers();
})();
