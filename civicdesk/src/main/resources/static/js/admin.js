// Commissioner (admin) dashboard: city-wide numbers, all complaints, staff accounts, SLA engine.
(function () {
  "use strict";

  const user = UI.requireRole(["ADMIN"]);
  if (!user) return;

  const overview = document.getElementById("tab-overview");
  let latest = null;
  let board = null;
  let usersPage = 0;

  // ---------- Overview ----------

  async function loadOverview() {
    try {
      latest = await Api.analytics();
      Dash.renderKpis(document.getElementById("kpis"), latest);
      if (!overview.classList.contains("hidden")) Dash.renderCharts(overview, latest);
    } catch (err) {
      UI.toast(err.message, true);
    }
  }

  // ---------- Users ----------

  const ROLE_SHORT = { CITIZEN: "Citizen", WARD_OFFICER: "Ward Officer", ZONAL_OFFICER: "Zonal Officer", ADMIN: "Admin" };

  async function loadUsers(page) {
    usersPage = page;
    const body = document.getElementById("users-body");
    try {
      const data = await Api.users(page, 25);
      body.innerHTML = data.content.map(function (u) {
        let area = "";
        if (u.role === "WARD_OFFICER" && u.wardNumber) area = "Ward " + u.wardNumber + ", " + (u.zoneName || "");
        if (u.role === "ZONAL_OFFICER" && u.zoneName) area = u.zoneName + " zone";
        const self = u.id === user.id;
        return "<tr><td><strong>" + UI.esc(u.name) + "</strong><br><span class=\"muted\">" + UI.esc(u.email) + "</span></td>" +
          "<td>" + UI.esc(ROLE_SHORT[u.role] || u.role) + "</td><td>" + UI.esc(area) + "</td>" +
          "<td>" + (u.active ? "Active" : "Deactivated") + "</td>" +
          "<td>" + (self ? "" : '<button type="button" class="secondary small" data-user="' + u.id + '" data-active="' + (!u.active) + '">' +
            (u.active ? "Deactivate" : "Reactivate") + "</button>") + "</td></tr>";
      }).join("");
      body.querySelectorAll("button[data-user]").forEach(function (btn) {
        btn.addEventListener("click", async function () {
          btn.disabled = true;
          try {
            const updated = await Api.setActive(Number(btn.dataset.user), btn.dataset.active === "true");
            UI.toast(updated.name + (updated.active ? " reactivated" : " deactivated"));
            loadUsers(usersPage);
          } catch (err) {
            UI.toast(err.message, true);
            btn.disabled = false;
          }
        });
      });

      const pager = document.getElementById("users-pager");
      if (data.totalPages <= 1) {
        pager.innerHTML = '<span class="muted">' + data.totalElements + " accounts</span>";
      } else {
        pager.innerHTML = '<button class="secondary small" data-p="prev"' + (data.page === 0 ? " disabled" : "") + ">Previous</button>" +
          '<span class="muted">Page ' + (data.page + 1) + " of " + data.totalPages + "</span>" +
          '<button class="secondary small" data-p="next"' + (data.last ? " disabled" : "") + ">Next</button>";
        pager.querySelector('[data-p="prev"]').addEventListener("click", function () { loadUsers(data.page - 1); });
        pager.querySelector('[data-p="next"]').addEventListener("click", function () { loadUsers(data.page + 1); });
      }
    } catch (err) {
      body.innerHTML = '<tr><td colspan="5">' + UI.esc(err.message) + "</td></tr>";
    }
  }

  const staffForm = document.getElementById("staff-form");
  const roleSel = document.getElementById("s-role");
  const zoneSel = document.getElementById("s-zone");
  const wardSel = document.getElementById("s-ward");

  function updateAreaFields() {
    const role = roleSel.value;
    document.getElementById("s-area").classList.toggle("hidden", role === "ADMIN");
    document.getElementById("s-ward-box").classList.toggle("hidden", role !== "WARD_OFFICER");
  }
  roleSel.addEventListener("change", updateAreaFields);

  zoneSel.addEventListener("change", async function () {
    wardSel.disabled = true;
    wardSel.innerHTML = '<option value="">' + (zoneSel.value ? "Loading..." : "Choose a zone first") + "</option>";
    if (!zoneSel.value) return;
    try {
      const wards = await Api.wards(zoneSel.value);
      wardSel.innerHTML = '<option value="">Choose a ward</option>' + wards.map(function (w) {
        return '<option value="' + w.id + '">Ward ' + w.wardNumber + "</option>";
      }).join("");
      wardSel.disabled = false;
    } catch (err) {
      UI.toast(err.message, true);
    }
  });

  staffForm.addEventListener("submit", async function (e) {
    e.preventDefault();
    UI.showFormErrors(staffForm, null);
    const role = roleSel.value;
    const payload = {
      role: role,
      name: document.getElementById("s-name").value.trim(),
      email: document.getElementById("s-email").value.trim(),
      phone: document.getElementById("s-phone").value.trim(),
      password: document.getElementById("s-password").value,
      zoneId: role === "ZONAL_OFFICER" && zoneSel.value ? Number(zoneSel.value) : null,
      wardId: role === "WARD_OFFICER" && wardSel.value ? Number(wardSel.value) : null,
    };
    const button = staffForm.querySelector("button[type=submit]");
    button.disabled = true;
    try {
      const created = await Api.createStaff(payload);
      UI.showFormOk(staffForm, "Created " + created.name + ". They can log in with " + created.email + " and the temporary password.");
      ["s-name", "s-email", "s-phone", "s-password"].forEach(function (id) { document.getElementById(id).value = ""; });
      loadUsers(0);
      if (board) board.reload();
      loadOverview();
    } catch (err) {
      UI.showFormErrors(staffForm, err);
    } finally {
      button.disabled = false;
    }
  });

  Api.zones().then(function (zones) {
    zoneSel.innerHTML += zones.map(function (z) {
      return '<option value="' + z.id + '">' + z.id + ". " + UI.esc(z.name) + "</option>";
    }).join("");
  }).catch(function (err) { UI.toast(err.message, true); });

  // ---------- Tools ----------

  document.getElementById("run-sla").addEventListener("click", async function (e) {
    const btn = e.currentTarget;
    btn.disabled = true;
    try {
      const r = await Api.runSla();
      document.getElementById("sla-result").textContent = r.message + ".";
      loadOverview();
      if (board) board.reload();
    } catch (err) {
      UI.toast(err.message, true);
    } finally {
      btn.disabled = false;
    }
  });

  document.getElementById("run-demo").addEventListener("click", async function (e) {
    const btn = e.currentTarget;
    const count = Number(document.getElementById("demo-count").value) || 5;
    btn.disabled = true;
    try {
      const r = await Api.generateDemo(count);
      document.getElementById("demo-result").textContent = r.message + ".";
      loadOverview();
      if (board) board.reload();
    } catch (err) {
      UI.toast(err.message, true);
    } finally {
      btn.disabled = false;
    }
  });

  // ---------- Tabs ----------

  let usersLoaded = false;
  let policiesMounted = false;
  Tabs.init(function (tab) {
    if (tab === "overview" && latest) Dash.renderCharts(overview, latest);
    if (tab === "work" && !board) board = Board.mount(document.getElementById("board"), user, loadOverview);
    if (tab === "policies" && !policiesMounted) { policiesMounted = true; Policies.mountAdmin(document.getElementById("policies-admin")); }
    if (tab === "users" && !usersLoaded) { usersLoaded = true; loadUsers(0); }
  });

  document.getElementById("refresh").addEventListener("click", function () {
    loadOverview();
    if (board) board.reload();
    if (usersLoaded) loadUsers(usersPage);
  });

  updateAreaFields();
  loadOverview();
})();
