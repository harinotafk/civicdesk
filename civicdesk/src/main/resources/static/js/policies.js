// Policies: a read-only list for everyone (mountReader) and a manage screen for the Commissioner (mountAdmin).
(function () {
  "use strict";

  const STATUS_LABEL = { DRAFT: "Draft", PUBLISHED: "Published", ARCHIVED: "Archived" };

  function statusBadge(s) {
    return '<span class="badge ' + UI.esc(s) + '">' + UI.esc(STATUS_LABEL[s] || s) + "</span>";
  }

  function paragraphs(text) {
    return String(text || "").split(/\n{2,}/).map(function (p) {
      return "<p>" + UI.esc(p.trim()).replace(/\n/g, "<br>") + "</p>";
    }).join("");
  }

  function slaText(h) {
    if (!h) return "";
    return h >= 48 ? (h / 24) + " days" : h + " hours";
  }

  function dateText(d) {
    if (!d) return "";
    const date = new Date(d + "T00:00:00");
    return isNaN(date) ? "" : date.toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
  }

  // ---------- Reader (citizens, officers, admin) ----------

  function policyCardHtml(p) {
    const facts = [];
    if (p.categoryName) facts.push("Applies to: <strong>" + UI.esc(p.categoryName) + "</strong>");
    if (p.departmentName) facts.push("Department: <strong>" + UI.esc(p.departmentName) + "</strong>");
    if (p.slaHours) facts.push("Deadline: <strong>" + UI.esc(slaText(p.slaHours)) + "</strong>");
    if (p.effectiveFrom) facts.push("In effect from " + UI.esc(dateText(p.effectiveFrom)));
    facts.push("Version " + p.version + ", updated " + UI.esc(UI.formatDate(p.updatedAt)));
    return '<details class="policy" data-id="' + p.id + '">' +
      "<summary><span class=\"policy-title\">" + UI.esc(p.title) + "</span>" +
      '<span class="policy-tag">' + UI.esc(p.categoryName || "General") + "</span></summary>" +
      '<div class="policy-body"><p class="policy-lead">' + UI.esc(p.summary) + "</p>" + paragraphs(p.body) +
      '<p class="muted policy-facts">' + facts.join(" &middot; ") + "</p></div></details>";
  }

  function mountReader(el) {
    el.innerHTML =
      '<div class="toolbar">' +
        '<div><label class="visually-hidden" for="pr-q">Search policies</label>' +
          '<input id="pr-q" type="search" placeholder="Search policies" maxlength="100"></div>' +
        '<div><label class="visually-hidden" for="pr-cat">Filter by category</label>' +
          '<select id="pr-cat"><option value="">All categories</option><option value="general">General policies</option></select></div>' +
      "</div>" +
      '<p class="muted" id="pr-count" aria-live="polite"></p>' +
      '<div id="pr-list"></div>';

    const qEl = el.querySelector("#pr-q");
    const catEl = el.querySelector("#pr-cat");
    const countEl = el.querySelector("#pr-count");
    const listEl = el.querySelector("#pr-list");
    let all = [];

    function render() {
      const q = qEl.value.trim().toLowerCase();
      const cat = catEl.value;
      const shown = all.filter(function (p) {
        if (cat === "general" && p.categoryId !== null) return false;
        if (cat && cat !== "general" && String(p.categoryId) !== cat) return false;
        if (!q) return true;
        return (p.title + " " + p.summary + " " + p.body).toLowerCase().indexOf(q) !== -1;
      });
      countEl.textContent = shown.length === 1 ? "1 policy" : shown.length + " policies";
      listEl.innerHTML = shown.length
        ? shown.map(policyCardHtml).join("")
        : '<p class="empty">No policy matches. Clear the search or pick another category.</p>';
    }

    async function load() {
      try {
        const results = await Promise.all([Api.policies({}), Api.categories()]);
        all = results[0];
        const keep = catEl.value;
        catEl.innerHTML = '<option value="">All categories</option><option value="general">General policies</option>' +
          results[1].map(function (c) { return '<option value="' + c.id + '">' + UI.esc(c.name) + "</option>"; }).join("");
        catEl.value = keep;
        render();
      } catch (err) {
        listEl.innerHTML = '<p class="empty">' + UI.esc(err.message) + "</p>";
      }
    }

    qEl.addEventListener("input", render);
    catEl.addEventListener("change", render);
    const ready = load();

    return {
      ready: ready,
      reload: load,
      /** The first published policy for a complaint category, or null. */
      find: function (categoryId) {
        return all.find(function (p) { return p.categoryId === categoryId; }) || null;
      },
      /** Show the policy for a category and scroll to it. */
      focus: function (categoryId) {
        qEl.value = "";
        catEl.value = String(categoryId);
        render();
        const first = listEl.querySelector("details");
        if (first) {
          first.open = true;
          first.scrollIntoView({ behavior: "smooth", block: "center" });
        }
      },
    };
  }

  // ---------- Admin: create, edit, publish, archive ----------

  function mountAdmin(el) {
    el.innerHTML =
      '<div class="split">' +
        '<div class="panel">' +
          '<h2 id="pa-heading">Add a policy</h2>' +
          '<form id="policy-form" novalidate>' +
            '<label for="p-title">Title</label>' +
            '<input id="p-title" maxlength="200">' +
            '<div class="field-error" data-error-for="title"></div>' +

            '<div class="row">' +
              '<div><label for="p-category">Applies to</label>' +
                '<select id="p-category"><option value="">General (no category)</option></select>' +
                '<div class="field-error" data-error-for="categoryId"></div></div>' +
              '<div><label for="p-status">Status</label>' +
                '<select id="p-status"><option value="DRAFT">Draft (hidden)</option>' +
                '<option value="PUBLISHED">Published</option><option value="ARCHIVED">Archived</option></select>' +
                '<div class="field-error" data-error-for="status"></div></div>' +
            "</div>" +

            '<label for="p-date">In effect from <span class="hint">(optional)</span></label>' +
            '<input id="p-date" type="date">' +
            '<div class="field-error" data-error-for="effectiveFrom"></div>' +

            '<label for="p-summary">One-line summary <span class="hint">(up to 500 characters)</span></label>' +
            '<textarea id="p-summary" rows="2" maxlength="500"></textarea>' +
            '<div class="field-error" data-error-for="summary"></div>' +

            '<label for="p-body">Full policy text <span class="hint">(leave a blank line between paragraphs)</span></label>' +
            '<textarea id="p-body" rows="9" maxlength="8000"></textarea>' +
            '<div class="field-error" data-error-for="body"></div>' +

            '<div class="actions"><button type="submit" id="p-save">Save policy</button>' +
            '<button type="button" class="secondary hidden" id="p-cancel">Cancel editing</button></div>' +
            '<div class="form-message" role="alert"></div>' +
          "</form>" +
        "</div>" +
        '<div class="panel">' +
          '<div class="panel-head"><h2>All policies</h2>' +
          '<select id="pa-filter" style="width:auto" aria-label="Show policies">' +
            '<option value="">All</option><option value="PUBLISHED">Published</option>' +
            '<option value="DRAFT">Drafts</option><option value="ARCHIVED">Archived</option></select></div>' +
          '<div class="table-wrap"><table class="data"><thead><tr><th>Policy</th><th>Status</th><th></th></tr></thead>' +
          '<tbody id="pa-body"></tbody></table></div>' +
          '<p class="muted" style="margin-top:10px">Only published policies are visible to citizens and officers.</p>' +
        "</div>" +
      "</div>";

    const form = el.querySelector("#policy-form");
    const f = {
      title: form.querySelector("#p-title"), category: form.querySelector("#p-category"),
      status: form.querySelector("#p-status"), date: form.querySelector("#p-date"),
      summary: form.querySelector("#p-summary"), body: form.querySelector("#p-body"),
    };
    const heading = el.querySelector("#pa-heading");
    const saveBtn = form.querySelector("#p-save");
    const cancelBtn = form.querySelector("#p-cancel");
    const filterEl = el.querySelector("#pa-filter");
    const bodyEl = el.querySelector("#pa-body");
    let policies = [];
    let editingId = null;

    function resetForm() {
      editingId = null;
      form.reset();
      UI.showFormErrors(form, null);
      heading.textContent = "Add a policy";
      saveBtn.textContent = "Save policy";
      cancelBtn.classList.add("hidden");
    }

    function startEdit(p) {
      editingId = p.id;
      f.title.value = p.title;
      f.category.value = p.categoryId === null ? "" : String(p.categoryId);
      f.status.value = p.status;
      f.date.value = p.effectiveFrom || "";
      f.summary.value = p.summary;
      f.body.value = p.body;
      UI.showFormErrors(form, null);
      heading.textContent = "Edit policy (version " + p.version + ")";
      saveBtn.textContent = "Save changes";
      cancelBtn.classList.remove("hidden");
      form.scrollIntoView({ behavior: "smooth", block: "start" });
      f.title.focus();
    }

    function renderList() {
      const status = filterEl.value;
      const shown = policies.filter(function (p) { return !status || p.status === status; });
      if (!shown.length) {
        bodyEl.innerHTML = '<tr><td colspan="3" class="muted">No policies here yet.</td></tr>';
        return;
      }
      bodyEl.innerHTML = shown.map(function (p) {
        const buttons = ['<button type="button" class="secondary small" data-act="edit" data-id="' + p.id + '">Edit</button>'];
        if (p.status !== "PUBLISHED") buttons.push('<button type="button" class="small" data-act="PUBLISHED" data-id="' + p.id + '">Publish</button>');
        if (p.status === "PUBLISHED") buttons.push('<button type="button" class="secondary small" data-act="DRAFT" data-id="' + p.id + '">Unpublish</button>');
        if (p.status !== "ARCHIVED") buttons.push('<button type="button" class="secondary small" data-act="ARCHIVED" data-id="' + p.id + '">Archive</button>');
        return "<tr><td><strong>" + UI.esc(p.title) + '</strong><br><span class="muted">' + UI.esc(p.categoryName || "General") +
          " &middot; v" + p.version + " &middot; " + UI.esc(UI.formatDate(p.updatedAt)) + " by " + UI.esc(p.updatedBy) + "</span></td>" +
          "<td>" + statusBadge(p.status) + '</td><td class="policy-actions">' + buttons.join(" ") + "</td></tr>";
      }).join("");
    }

    async function load() {
      try {
        policies = await Api.policies({});
        renderList();
      } catch (err) {
        bodyEl.innerHTML = '<tr><td colspan="3">' + UI.esc(err.message) + "</td></tr>";
      }
    }

    bodyEl.addEventListener("click", async function (e) {
      const btn = e.target.closest("button[data-act]");
      if (!btn) return;
      const id = Number(btn.dataset.id);
      if (btn.dataset.act === "edit") {
        const p = policies.find(function (x) { return x.id === id; });
        if (p) startEdit(p);
        return;
      }
      btn.disabled = true;
      try {
        const updated = await Api.setPolicyStatus(id, btn.dataset.act);
        UI.toast("\u201c" + updated.title + "\u201d is now " + STATUS_LABEL[updated.status].toLowerCase());
        if (editingId === id) f.status.value = updated.status;
        load();
      } catch (err) {
        UI.toast(err.message, true);
        btn.disabled = false;
      }
    });

    form.addEventListener("submit", async function (e) {
      e.preventDefault();
      UI.showFormErrors(form, null);
      const payload = {
        title: f.title.value.trim(),
        summary: f.summary.value.trim(),
        body: f.body.value.trim(),
        categoryId: f.category.value ? Number(f.category.value) : null,
        effectiveFrom: f.date.value || null,
        status: f.status.value,
      };
      saveBtn.disabled = true;
      try {
        const saved = editingId === null ? await Api.createPolicy(payload) : await Api.updatePolicy(editingId, payload);
        UI.toast("Saved \u201c" + saved.title + "\u201d");
        resetForm();
        load();
      } catch (err) {
        UI.showFormErrors(form, err);
      } finally {
        saveBtn.disabled = false;
      }
    });

    cancelBtn.addEventListener("click", resetForm);
    filterEl.addEventListener("change", renderList);

    Api.categories().then(function (cats) {
      f.category.innerHTML = '<option value="">General (no category)</option>' + cats.map(function (c) {
        return '<option value="' + c.id + '">' + UI.esc(c.name) + "</option>";
      }).join("");
    }).catch(function (err) { UI.toast(err.message, true); });

    load();
    return { reload: load };
  }

  window.Policies = { mountReader: mountReader, mountAdmin: mountAdmin };
})();
