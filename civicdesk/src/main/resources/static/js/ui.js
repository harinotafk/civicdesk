// Shared UI helpers: formatting, the SLA clock, complaint list and detail drawer.
(function () {
  "use strict";

  const STATUS_LABELS = {
    SUBMITTED: "Submitted",
    ASSIGNED: "Assigned",
    IN_PROGRESS: "In progress",
    RESOLVED: "Resolved",
    CLOSED: "Closed",
    REOPENED: "Reopened",
  };

  const ACTION_LABELS = {
    CREATED: "Complaint filed",
    ASSIGNED: "Assigned",
    STATUS_CHANGED: "Status changed",
    ESCALATED: "Escalated",
    DUPLICATE_REPORTED: "Reported again by another citizen",
    FEEDBACK: "Citizen feedback",
  };

  const ROLE_LABELS = {
    CITIZEN: "Citizen",
    WARD_OFFICER: "Ward Officer",
    ZONAL_OFFICER: "Zonal Officer",
    ADMIN: "Commissioner (Admin)",
  };

  function esc(value) {
    if (value === null || value === undefined) return "";
    return String(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function parseDate(s) {
    return s ? new Date(s) : null;
  }

  function formatDate(s) {
    const d = parseDate(s);
    if (!d || isNaN(d)) return "";
    return d.toLocaleString("en-IN", { day: "numeric", month: "short", hour: "numeric", minute: "2-digit" });
  }

  function duration(ms) {
    const mins = Math.max(0, Math.round(ms / 60000));
    if (mins < 60) return mins + " min";
    const hours = Math.floor(mins / 60);
    if (hours < 24) return hours + "h " + (mins % 60) + "m";
    const days = Math.floor(hours / 24);
    return days + "d " + (hours % 24) + "h";
  }

  function badge(status) {
    return '<span class="badge ' + esc(status) + '">' + esc(STATUS_LABELS[status] || status) + "</span>";
  }

  function levelBadge(c) {
    if (!c.escalationLevel) return "";
    return '<span class="badge level-' + Math.min(c.escalationLevel, 2) + '">Escalated to ' + esc(c.escalationLabel) + "</span>";
  }

  /** The SLA clock: how much of the deadline window has been used. */
  function slaHtml(c) {
    const created = parseDate(c.createdAt);
    const due = parseDate(c.dueAt);
    const now = new Date();

    if (c.status === "RESOLVED" || c.status === "CLOSED") {
      const end = parseDate(c.resolvedAt) || parseDate(c.closedAt) || now;
      const verb = c.resolvedAt ? "Resolved in " : "Closed after ";
      return '<div class="sla done"><div class="sla-track"><div class="sla-fill" style="width:100%"></div></div>' +
        '<div class="sla-text"><strong>' + verb + duration(end - created) + "</strong><span>" +
        (c.resolvedAt && due && parseDate(c.resolvedAt) <= due ? "Within deadline" : "") + "</span></div></div>";
    }

    const total = Math.max(1, due - created);
    const used = now - created;
    const ratio = Math.min(1, Math.max(0, used / total));
    let cls = "";
    let text;
    if (now > due) {
      cls = "over";
      text = "Overdue by " + duration(now - due);
    } else {
      if (ratio >= 0.75) cls = "warn";
      text = "Due in " + duration(due - now);
    }
    return '<div class="sla ' + cls + '" title="Deadline ' + esc(formatDate(c.dueAt)) + '">' +
      '<div class="sla-track"><div class="sla-fill" style="width:' + (cls === "over" ? 100 : Math.round(ratio * 100)) + '%"></div></div>' +
      '<div class="sla-text"><strong>' + text + "</strong><span>Deadline " + esc(formatDate(c.dueAt)) + "</span></div></div>";
  }

  function complaintItemHtml(c) {
    const support = c.supportCount > 1 ? ' <span class="support">' + c.supportCount + " reports</span>" : "";
    return '<li class="complaint' + (c.overdue ? " is-over" : "") + '" tabindex="0" data-id="' + c.id + '">' +
      '<div class="top">' + badge(c.status) + levelBadge(c) +
      '<span class="title">' + esc(c.categoryName) + "</span>" +
      '<span class="id">#' + c.id + "</span>" + support + "</div>" +
      '<div class="where">Ward ' + esc(c.wardNumber) + ", " + esc(c.zoneName) +
      (c.landmark ? ", " + esc(c.landmark) : "") + ". " + esc(c.departmentName) + "</div>" +
      '<div class="desc">' + esc(c.description) + "</div>" +
      slaHtml(c) + "</li>";
  }

  /** Renders a page of complaints; clicking one calls onOpen(id). */
  function renderComplaintList(listEl, pagerEl, page, onOpen, onPage, emptyText) {
    if (!page.content.length) {
      listEl.innerHTML = '<li class="empty">' + esc(emptyText || "Nothing here yet.") + "</li>";
    } else {
      listEl.innerHTML = page.content.map(complaintItemHtml).join("");
    }
    listEl.querySelectorAll(".complaint").forEach(function (li) {
      const open = function () { onOpen(Number(li.dataset.id)); };
      li.addEventListener("click", open);
      li.addEventListener("keydown", function (e) {
        if (e.key === "Enter" || e.key === " ") { e.preventDefault(); open(); }
      });
    });

    if (!pagerEl) return;
    if (page.totalPages <= 1) {
      pagerEl.innerHTML = '<span class="muted">' + page.totalElements + " complaint" + (page.totalElements === 1 ? "" : "s") + "</span>";
      return;
    }
    pagerEl.innerHTML =
      '<button class="secondary small" data-p="prev"' + (page.page === 0 ? " disabled" : "") + ">Previous</button>" +
      '<span class="muted">Page ' + (page.page + 1) + " of " + page.totalPages + ", " + page.totalElements + " complaints</span>" +
      '<button class="secondary small" data-p="next"' + (page.last ? " disabled" : "") + ">Next</button>";
    pagerEl.querySelector('[data-p="prev"]').addEventListener("click", function () { onPage(page.page - 1); });
    pagerEl.querySelector('[data-p="next"]').addEventListener("click", function () { onPage(page.page + 1); });
  }

  // ---------- Toast ----------

  let toastTimer = null;
  function toast(message, isError) {
    let el = document.getElementById("toast");
    if (!el) {
      el = document.createElement("div");
      el.id = "toast";
      el.className = "toast";
      el.setAttribute("role", "status");
      el.setAttribute("aria-live", "polite");
      document.body.appendChild(el);
    }
    el.textContent = message;
    el.classList.toggle("error", !!isError);
    el.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.classList.remove("show"); }, 3500);
  }

  // ---------- Forms ----------

  function showFormErrors(form, err) {
    form.querySelectorAll("[data-error-for]").forEach(function (el) { el.textContent = ""; });
    const box = form.querySelector(".form-message");
    if (!err) {
      if (box) { box.className = "form-message"; box.textContent = ""; }
      return;
    }
    const fields = err.fieldErrors || {};
    Object.keys(fields).forEach(function (name) {
      const el = form.querySelector('[data-error-for="' + name + '"]');
      if (el) el.textContent = fields[name];
    });
    if (box) {
      box.textContent = err.message || "Something went wrong";
      box.className = "form-message show error";
    }
  }

  function showFormOk(form, message) {
    const box = form.querySelector(".form-message");
    if (box) {
      box.textContent = message;
      box.className = "form-message show ok";
    }
  }

  // ---------- Session / page guard ----------

  function homeFor(role) {
    if (role === "CITIZEN") return "/citizen.html";
    if (role === "ADMIN") return "/admin.html";
    return "/officer.html";
  }

  /** Redirects away unless the user is logged in with one of the given roles. */
  function requireRole(roles) {
    const user = Api.session.user;
    if (!Api.session.token || !user) {
      window.location.replace("/index.html");
      return null;
    }
    if (roles.indexOf(user.role) === -1) {
      window.location.replace(homeFor(user.role));
      return null;
    }
    const who = document.getElementById("who");
    if (who) {
      let area = ROLE_LABELS[user.role] || user.role;
      if (user.role === "WARD_OFFICER" && user.wardNumber) area += ", Ward " + user.wardNumber;
      if (user.role === "ZONAL_OFFICER" && user.zoneName) area += ", " + user.zoneName + " zone";
      who.innerHTML = "<strong>" + esc(user.name) + "</strong><span>" + esc(area) + "</span>";
    }
    const logout = document.getElementById("logout");
    if (logout) {
      logout.addEventListener("click", function () {
        Api.clearSession();
        window.location.href = "/index.html";
      });
    }
    return user;
  }

  // ---------- Complaint drawer ----------

  let drawerEl = null;
  let backdropEl = null;
  let lastFocus = null;

  function ensureDrawer() {
    if (drawerEl) return;
    backdropEl = document.createElement("div");
    backdropEl.className = "drawer-backdrop";
    drawerEl = document.createElement("aside");
    drawerEl.className = "drawer";
    drawerEl.setAttribute("role", "dialog");
    drawerEl.setAttribute("aria-modal", "true");
    drawerEl.setAttribute("aria-label", "Complaint details");
    document.body.appendChild(backdropEl);
    document.body.appendChild(drawerEl);
    backdropEl.addEventListener("click", closeDrawer);
    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape" && drawerEl.classList.contains("open")) closeDrawer();
    });
  }

  function closeDrawer() {
    if (!drawerEl) return;
    drawerEl.classList.remove("open");
    backdropEl.classList.remove("open");
    if (lastFocus) lastFocus.focus();
  }

  /** Which status buttons this user may press for this complaint. */
  function availableActions(c, user) {
    const s = c.status;
    const out = [];
    if (user.role === "CITIZEN") {
      if (c.citizenId === user.id && s === "RESOLVED") {
        out.push({ to: "CLOSED", label: "Confirm fixed and close", citizenClose: true });
        out.push({ to: "REOPENED", label: "Not fixed, reopen", noteRequired: true, secondary: true });
      }
      return out;
    }
    if (s === "SUBMITTED") out.push({ to: "ASSIGNED", label: "Assign to ward officer", secondary: true });
    if (s === "SUBMITTED" || s === "ASSIGNED" || s === "REOPENED") out.push({ to: "IN_PROGRESS", label: "Start work" });
    if (s === "ASSIGNED" || s === "IN_PROGRESS" || s === "REOPENED") out.push({ to: "RESOLVED", label: "Mark resolved", noteRequired: true });
    if (user.role === "ADMIN") {
      if (s === "RESOLVED") out.push({ to: "CLOSED", label: "Close", secondary: true });
      else if (s !== "CLOSED") out.push({ to: "CLOSED", label: "Close without resolving", noteRequired: true, danger: true });
    }
    return out;
  }

  function actionBoxHtml(c, actions) {
    if (!actions.length) return "";
    const citizenClose = actions.some(function (a) { return a.citizenClose; });
    let html = '<form class="action-box" id="action-form" novalidate><h3>Update this complaint</h3>';
    if (citizenClose) {
      html += '<fieldset class="stars" style="border:0;padding:0;margin:0"><legend class="visually-hidden">Rating</legend>';
      for (let i = 1; i <= 5; i++) {
        html += '<label><input type="radio" name="rating" value="' + i + '"' + (i === 5 ? " checked" : "") + "> " + i + "</label>";
      }
      html += "</fieldset><p class=\"muted\" style=\"margin:4px 0 0\">Rate how well it was fixed, 5 is best.</p>";
      html += '<label for="feedback">Feedback <span class="hint">(optional)</span></label>' +
        '<textarea id="feedback" name="feedback" maxlength="500"></textarea><div class="field-error" data-error-for="feedback"></div>';
    }
    html += '<label for="note">Note <span class="hint">(required to resolve, reopen or close without resolving)</span></label>' +
      '<textarea id="note" name="note" maxlength="500"></textarea><div class="field-error" data-error-for="note"></div>' +
      '<div class="actions">';
    actions.forEach(function (a) {
      html += '<button type="button" data-to="' + a.to + '" class="' + (a.secondary ? "secondary" : a.danger ? "danger" : "") + '">' + esc(a.label) + "</button>";
    });
    html += '</div><div class="form-message" role="alert"></div></form>';
    return html;
  }

  async function openComplaint(id, onChanged) {
    ensureDrawer();
    lastFocus = document.activeElement;
    drawerEl.innerHTML = '<button class="secondary small drawer-close" type="button">Close</button><p class="muted">Loading complaint #' + esc(id) + "...</p>";
    drawerEl.querySelector(".drawer-close").addEventListener("click", closeDrawer);
    drawerEl.classList.add("open");
    backdropEl.classList.add("open");

    let c, history;
    try {
      const results = await Promise.all([Api.getComplaint(id), Api.history(id)]);
      c = results[0];
      history = results[1];
    } catch (err) {
      drawerEl.innerHTML = '<button class="secondary small drawer-close" type="button">Close</button><p class="form-message show error">' + esc(err.message) + "</p>";
      drawerEl.querySelector(".drawer-close").addEventListener("click", closeDrawer);
      return;
    }

    const user = Api.session.user;
    const actions = availableActions(c, user);
    const mapLink = (c.latitude != null && c.longitude != null)
      ? '<a href="https://www.openstreetmap.org/?mlat=' + c.latitude + "&mlon=" + c.longitude + '#map=18/' + c.latitude + "/" + c.longitude + '" target="_blank" rel="noopener">Open in map</a>'
      : "Not given";

    drawerEl.innerHTML =
      '<button class="secondary small drawer-close" type="button">Close</button>' +
      "<h2>" + esc(c.categoryName) + " #" + c.id + "</h2>" +
      '<div class="top" style="display:flex;gap:8px;flex-wrap:wrap">' + badge(c.status) + levelBadge(c) +
      (c.supportCount > 1 ? '<span class="badge">' + c.supportCount + " citizens reported this</span>" : "") + "</div>" +
      slaHtml(c) +
      "<p style=\"margin-top:14px\">" + esc(c.description) + "</p>" +
      '<dl class="facts">' +
      "<dt>Location</dt><dd>Ward " + esc(c.wardNumber) + ", " + esc(c.zoneName) + " zone" + (c.landmark ? "<br>" + esc(c.landmark) : "") + "</dd>" +
      "<dt>Map</dt><dd>" + mapLink + "</dd>" +
      "<dt>Department</dt><dd>" + esc(c.departmentName) + "</dd>" +
      "<dt>Filed by</dt><dd>" + esc(c.citizenName) + ", " + esc(formatDate(c.createdAt)) + "</dd>" +
      "<dt>Officer</dt><dd>" + (c.assignedOfficerName ? esc(c.assignedOfficerName) : "Not assigned yet") + "</dd>" +
      "<dt>Responsible</dt><dd>" + esc(c.escalationLabel) + "</dd>" +
      (c.resolutionNote ? "<dt>Resolution</dt><dd>" + esc(c.resolutionNote) + "</dd>" : "") +
      (c.rating ? "<dt>Rating</dt><dd>" + esc(c.rating) + " out of 5" + (c.feedback ? ", \"" + esc(c.feedback) + "\"" : "") + "</dd>" : "") +
      "</dl>" +
      actionBoxHtml(c, actions) +
      "<h3>History</h3>" +
      '<ol class="timeline">' + history.map(function (h) {
        const change = (h.fromStatus && h.toStatus && h.fromStatus !== h.toStatus)
          ? " to " + (STATUS_LABELS[h.toStatus] || h.toStatus) : "";
        return '<li class="' + esc(h.action) + '"><div class="what">' + esc(ACTION_LABELS[h.action] || h.action) + esc(change) + "</div>" +
          (h.note ? "<div>" + esc(h.note) + "</div>" : "") +
          '<div class="when">' + esc(h.actorLabel) + ", " + esc(formatDate(h.createdAt)) + "</div></li>";
      }).join("") + "</ol>";

    drawerEl.querySelector(".drawer-close").addEventListener("click", closeDrawer);
    drawerEl.querySelector(".drawer-close").focus();

    const form = drawerEl.querySelector("#action-form");
    if (!form) return;
    form.querySelectorAll("button[data-to]").forEach(function (btn) {
      btn.addEventListener("click", async function () {
        const action = actions.find(function (a) { return a.to === btn.dataset.to && a.label === btn.textContent; });
        const note = form.note.value.trim();
        showFormErrors(form, null);
        if (action && action.noteRequired && !note) {
          showFormErrors(form, { message: "Please add a note for this step.", fieldErrors: { note: "A note is required here" } });
          form.note.focus();
          return;
        }
        const payload = { status: btn.dataset.to, note: note || null };
        if (action && action.citizenClose) {
          const checked = form.querySelector('input[name="rating"]:checked');
          payload.rating = checked ? Number(checked.value) : null;
          payload.feedback = form.feedback.value.trim() || null;
        }
        form.querySelectorAll("button").forEach(function (b) { b.disabled = true; });
        try {
          const updated = await Api.updateStatus(c.id, payload);
          toast("Complaint #" + updated.id + " is now " + (STATUS_LABELS[updated.status] || updated.status).toLowerCase());
          if (onChanged) onChanged();
          openComplaint(c.id, onChanged);
        } catch (err) {
          showFormErrors(form, err);
          form.querySelectorAll("button").forEach(function (b) { b.disabled = false; });
        }
      });
    });
  }

  window.UI = {
    STATUS_LABELS: STATUS_LABELS,
    ROLE_LABELS: ROLE_LABELS,
    esc: esc,
    formatDate: formatDate,
    duration: duration,
    badge: badge,
    slaHtml: slaHtml,
    renderComplaintList: renderComplaintList,
    toast: toast,
    showFormErrors: showFormErrors,
    showFormOk: showFormOk,
    homeFor: homeFor,
    requireRole: requireRole,
    openComplaint: openComplaint,
    closeDrawer: closeDrawer,
  };
})();
