// Dashboard pieces: KPI tiles, charts (Chart.js) and summary tables.
(function () {
  "use strict";

  // Validated with the dataviz palette checker (contrast, chroma, colour-blind separation).
  const COLOR_ON_TIME = "#00918b";
  const COLOR_OVERDUE = "#c0391f";
  const INK_SOFT = "#4b5b6b";
  const GRID = "#e3e8e7";

  const charts = {};

  function fmtHours(h) {
    if (h === null || h === undefined) return "No data yet";
    if (h < 24) return h + " h";
    return (Math.round(h / 24 * 10) / 10) + " days";
  }

  function renderKpis(el, a) {
    const s = a.summary;
    const tiles = [
      { label: "Open complaints", value: s.open },
      { label: "Overdue right now", value: s.overdue, alert: s.overdue > 0 },
      { label: "Escalated and still open", value: s.escalatedOpen, alert: s.escalatedOpen > 0 },
      { label: "Resolved or closed", value: s.resolvedOrClosed },
      { label: "Average time to resolve", value: fmtHours(s.avgResolutionHours) },
      { label: "Resolved within deadline", value: s.resolvedOnTimePercent === null ? "No data yet" : s.resolvedOnTimePercent + "%" },
      { label: "Citizen rating", value: s.avgRating === null ? "No ratings yet" : s.avgRating + " / 5" },
    ];
    el.innerHTML = tiles.map(function (t) {
      return '<div class="kpi' + (t.alert ? " alert" : "") + '"><div class="value">' + UI.esc(t.value) +
        '</div><div class="label">' + UI.esc(t.label) + "</div></div>";
    }).join("");
  }

  function baseOptions(stacked, horizontal) {
    return {
      responsive: true,
      maintainAspectRatio: false,
      indexAxis: horizontal ? "y" : "x",
      interaction: { mode: "index", intersect: false },
      plugins: {
        legend: { display: stacked, position: "top", align: "start", labels: { color: INK_SOFT, boxWidth: 12, boxHeight: 12 } },
        tooltip: { backgroundColor: "#18283a", padding: 10 },
      },
      scales: {
        x: { stacked: stacked, grid: { display: !horizontal ? false : true, color: GRID }, ticks: { color: INK_SOFT, precision: 0 } },
        y: { stacked: stacked, grid: { display: horizontal ? false : true, color: GRID }, ticks: { color: INK_SOFT, precision: 0 }, beginAtZero: true },
      },
    };
  }

  function draw(id, canvas, config) {
    if (charts[id]) charts[id].destroy();
    charts[id] = new Chart(canvas, config);
  }

  function renderCharts(root, a) {
    if (typeof Chart === "undefined") {
      root.querySelectorAll(".chart-box").forEach(function (box) {
        box.innerHTML = '<p class="empty">Charts need an internet connection to load. The tables below have the same numbers.</p>';
      });
    } else {
      // Complaints filed per day, last 14 days (one series, no legend)
      draw("daily", root.querySelector("#chart-daily"), {
        type: "bar",
        data: {
          labels: a.last14Days.map(function (d) {
            return new Date(d.label + "T00:00:00").toLocaleDateString("en-IN", { day: "numeric", month: "short" });
          }),
          datasets: [{ label: "Complaints filed", data: a.last14Days.map(function (d) { return d.count; }),
            backgroundColor: COLOR_ON_TIME, borderRadius: 4, maxBarThickness: 22 }],
        },
        options: baseOptions(false, false),
      });

      // Open complaints per department, split into within-deadline and overdue
      draw("dept", root.querySelector("#chart-dept"), {
        type: "bar",
        data: {
          labels: a.byDepartment.map(function (d) { return d.label; }),
          datasets: [
            { label: "Open, within deadline", data: a.byDepartment.map(function (d) { return d.open - d.overdue; }),
              backgroundColor: COLOR_ON_TIME, borderRadius: 4, maxBarThickness: 22, borderColor: "#fff", borderWidth: { right: 2 } },
            { label: "Overdue", data: a.byDepartment.map(function (d) { return d.overdue; }),
              backgroundColor: COLOR_OVERDUE, borderRadius: 4, maxBarThickness: 22 },
          ],
        },
        options: baseOptions(true, true),
      });
    }

    function table(rows, firstHeader) {
      if (!rows.length) return '<p class="empty">No complaints yet.</p>';
      return '<div class="table-wrap"><table class="data"><thead><tr><th>' + firstHeader + '</th><th class="num">Total</th>' +
        '<th class="num">Open</th><th class="num">Overdue</th><th class="num">Avg. time to resolve</th></tr></thead><tbody>' +
        rows.map(function (r) {
          return "<tr><td>" + UI.esc(r.label) + '</td><td class="num">' + r.total + '</td><td class="num">' + r.open +
            '</td><td class="num">' + r.overdue + '</td><td class="num">' + UI.esc(fmtHours(r.avgResolutionHours)) + "</td></tr>";
        }).join("") + "</tbody></table></div>";
    }

    root.querySelector("#table-wards").innerHTML = table(a.topWards, "Ward");
    root.querySelector("#table-categories").innerHTML = table(a.byCategory, "Category");
    root.querySelector("#table-departments").innerHTML = table(a.byDepartment, "Department");
  }

  window.Dash = { renderKpis: renderKpis, renderCharts: renderCharts };
})();
