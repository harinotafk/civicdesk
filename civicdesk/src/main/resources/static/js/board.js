// The complaints board used by officers and the admin: filters, sorting, list, detail drawer.
(function () {
  "use strict";

  const ALL_STATUSES = "SUBMITTED,ASSIGNED,IN_PROGRESS,RESOLVED,CLOSED,REOPENED";

  function mount(root, user, onDataChanged) {
    const showZone = user.role === "ADMIN";
    const showWard = user.role === "ADMIN" || user.role === "ZONAL_OFFICER";

    root.innerHTML =
      '<div class="filters">' +
        '<div><label for="f-status">Status</label><select id="f-status">' +
          '<option value="">Open (needs action)</option>' +
          '<option value="' + ALL_STATUSES + '">All</option>' +
          Object.keys(UI.STATUS_LABELS).map(function (s) {
            return '<option value="' + s + '">' + UI.STATUS_LABELS[s] + "</option>";
          }).join("") +
        "</select></div>" +
        (showZone ? '<div><label for="f-zone">Zone</label><select id="f-zone"><option value="">All zones</option></select></div>' : "") +
        (showWard ? '<div><label for="f-ward">Ward</label><select id="f-ward"' + (showZone ? " disabled" : "") + '><option value="">All wards</option></select></div>' : "") +
        '<div><label for="f-category">Category</label><select id="f-category"><option value="">All categories</option></select></div>' +
        '<div><label for="f-sort">Sort by</label><select id="f-sort">' +
          '<option value="dueAt,asc">Deadline, most urgent first</option>' +
          '<option value="createdAt,desc">Newest first</option>' +
          '<option value="supportCount,desc">Most reports</option>' +
          '<option value="escalationLevel,desc">Most escalated</option>' +
        "</select></div>" +
        '<div><label class="check"><input type="checkbox" id="f-overdue"> Overdue only</label>' +
        '<label class="check"><input type="checkbox" id="f-escalated"> Escalated only</label></div>' +
      "</div>" +
      '<ul class="complaints" id="board-list"></ul>' +
      '<div class="pager" id="board-pager"></div>';

    const $ = function (id) { return root.querySelector("#" + id); };
    const listEl = $("board-list");
    const pagerEl = $("board-pager");
    let currentPage = 0;

    async function load(page) {
      currentPage = page;
      const params = {
        status: $("f-status").value,
        categoryId: $("f-category").value,
        overdue: $("f-overdue").checked,
        escalated: $("f-escalated").checked,
        sort: $("f-sort").value,
        page: page,
        size: 15,
      };
      if (showWard) params.wardId = $("f-ward").value;
      listEl.innerHTML = '<li class="empty">Loading...</li>';
      try {
        const data = await Api.listComplaints(params);
        UI.renderComplaintList(listEl, pagerEl, data,
          function (id) {
            UI.openComplaint(id, function () {
              load(currentPage);
              if (onDataChanged) onDataChanged();
            });
          },
          load,
          "No complaints match these filters.");
      } catch (err) {
        listEl.innerHTML = '<li class="empty">' + UI.esc(err.message) + "</li>";
        pagerEl.innerHTML = "";
      }
    }

    async function fillWards(zoneId) {
      const wardSel = $("f-ward");
      wardSel.innerHTML = '<option value="">All wards</option>';
      wardSel.disabled = !zoneId;
      if (!zoneId) return;
      try {
        const wards = await Api.wards(zoneId);
        wardSel.innerHTML += wards.map(function (w) {
          return '<option value="' + w.id + '">Ward ' + w.wardNumber + "</option>";
        }).join("");
      } catch (err) {
        UI.toast(err.message, true);
      }
    }

    // Reference data for the filters
    Api.categories().then(function (cats) {
      $("f-category").innerHTML += cats.map(function (c) {
        return '<option value="' + c.id + '">' + UI.esc(c.name) + "</option>";
      }).join("");
    }).catch(function (err) { UI.toast(err.message, true); });

    if (showZone) {
      Api.zones().then(function (zones) {
        $("f-zone").innerHTML += zones.map(function (z) {
          return '<option value="' + z.id + '">' + z.id + ". " + UI.esc(z.name) + "</option>";
        }).join("");
      }).catch(function (err) { UI.toast(err.message, true); });
      $("f-zone").addEventListener("change", function () {
        fillWards($("f-zone").value).then(function () { load(0); });
      });
    } else if (showWard && user.zoneId) {
      fillWards(user.zoneId);
    }

    ["f-status", "f-category", "f-sort", "f-overdue", "f-escalated"].concat(showWard ? ["f-ward"] : [])
      .forEach(function (id) { $(id).addEventListener("change", function () { load(0); }); });

    load(0);
    return { reload: function () { load(currentPage); } };
  }

  window.Board = { mount: mount };
})();
