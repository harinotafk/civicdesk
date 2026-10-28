// Citizen page: file a complaint, track my complaints, confirm or reopen resolved ones.
(function () {
  "use strict";

  const user = UI.requireRole(["CITIZEN"]);
  if (!user) return;

  const form = document.getElementById("complaint-form");
  const categorySel = document.getElementById("category");
  const zoneSel = document.getElementById("zone");
  const wardSel = document.getElementById("ward");
  const slaHint = document.getElementById("sla-hint");
  const mapNote = document.getElementById("map-note");
  const mineList = document.getElementById("mine");
  const minePager = document.getElementById("mine-pager");
  const mineFilter = document.getElementById("mine-filter");

  let categories = [];
  let pin = null;      // { lat, lng }
  let marker = null;
  let map = null;
  let currentPage = 0;

  // Rough bounds accepted by the server
  const BOUNDS = { minLat: 12.8, maxLat: 13.3, minLng: 80.0, maxLng: 80.4 };

  // ---------- Reference data ----------

  async function loadReference() {
    try {
      const results = await Promise.all([Api.categories(), Api.zones()]);
      categories = results[0];
      categorySel.innerHTML = '<option value="">Choose a category</option>' + categories.map(function (c) {
        return '<option value="' + c.id + '">' + UI.esc(c.name) + "</option>";
      }).join("");
      zoneSel.innerHTML = '<option value="">Choose a zone</option>' + results[1].map(function (z) {
        return '<option value="' + z.id + '">' + z.id + ". " + UI.esc(z.name) + "</option>";
      }).join("");
    } catch (err) {
      UI.toast(err.message, true);
    }
  }

  categorySel.addEventListener("change", function () {
    const c = categories.find(function (x) { return String(x.id) === categorySel.value; });
    slaHint.textContent = c
      ? "Goes to " + c.departmentName + ". They have " + (c.slaHours >= 48 ? (c.slaHours / 24) + " days" : c.slaHours + " hours") + " to fix it."
      : "";
    showPolicyHint(c);
  });

  // Link the category to its policy, so people know what to expect before they file.
  const policyHint = document.getElementById("policy-hint");
  const policyReader = Policies.mountReader(document.getElementById("policies-reader"));

  function showPolicyHint(c) {
    policyHint.classList.add("hidden");
    if (!c) return;
    policyReader.ready.then(function () {
      const p = policyReader.find(c.id);
      if (!p) return;
      policyHint.innerHTML = 'Policy: <a href="#policies">' + UI.esc(p.title) + "</a>";
      policyHint.classList.remove("hidden");
      policyHint.querySelector("a").addEventListener("click", function (e) {
        e.preventDefault();
        policyReader.focus(c.id);
      });
    });
  }

  zoneSel.addEventListener("change", async function () {
    wardSel.disabled = true;
    if (!zoneSel.value) {
      wardSel.innerHTML = '<option value="">Choose a zone first</option>';
      return;
    }
    wardSel.innerHTML = '<option value="">Loading...</option>';
    try {
      const wards = await Api.wards(zoneSel.value);
      wardSel.innerHTML = '<option value="">Choose a ward</option>' + wards.map(function (w) {
        return '<option value="' + w.id + '">Ward ' + w.wardNumber + (w.name !== "Ward " + w.wardNumber ? ", " + UI.esc(w.name) : "") + "</option>";
      }).join("");
      wardSel.disabled = false;
    } catch (err) {
      UI.toast(err.message, true);
    }
  });

  // ---------- Map ----------

  function setPin(lat, lng) {
    if (lat < BOUNDS.minLat || lat > BOUNDS.maxLat || lng < BOUNDS.minLng || lng > BOUNDS.maxLng) {
      UI.toast("That spot is outside Chennai. Choose a place within the city.", true);
      return;
    }
    pin = { lat: Math.round(lat * 100000) / 100000, lng: Math.round(lng * 100000) / 100000 };
    if (map) {
      if (marker) marker.setLatLng([pin.lat, pin.lng]);
      else marker = L.marker([pin.lat, pin.lng]).addTo(map);
    }
    mapNote.textContent = "Pin at " + pin.lat + ", " + pin.lng + ".";
  }

  function clearPin() {
    pin = null;
    if (marker && map) { map.removeLayer(marker); marker = null; }
    mapNote.textContent = "No spot chosen. A pin helps the field team and spots duplicate reports.";
  }

  if (typeof L !== "undefined") {
    map = L.map("map").setView([13.05, 80.24], 11);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: "&copy; OpenStreetMap contributors",
    }).addTo(map);
    map.on("click", function (e) { setPin(e.latlng.lat, e.latlng.lng); });
  } else {
    document.getElementById("map").innerHTML = '<p class="empty">The map could not load (no internet?). You can still submit without a pin.</p>';
  }

  document.getElementById("use-location").addEventListener("click", function () {
    if (!navigator.geolocation) {
      UI.toast("This browser cannot share your location.", true);
      return;
    }
    navigator.geolocation.getCurrentPosition(function (pos) {
      setPin(pos.coords.latitude, pos.coords.longitude);
      if (map && pin) map.setView([pin.lat, pin.lng], 16);
    }, function () {
      UI.toast("Location permission was denied. Tap the map instead.", true);
    });
  });
  document.getElementById("clear-pin").addEventListener("click", clearPin);

  // ---------- Submit ----------

  form.addEventListener("submit", async function (e) {
    e.preventDefault();
    UI.showFormErrors(form, null);

    const payload = {
      categoryId: categorySel.value ? Number(categorySel.value) : null,
      wardId: wardSel.value ? Number(wardSel.value) : null,
      description: document.getElementById("description").value.trim(),
      landmark: document.getElementById("landmark").value.trim() || null,
      latitude: pin ? pin.lat : null,
      longitude: pin ? pin.lng : null,
    };

    const button = form.querySelector("button[type=submit]");
    button.disabled = true;
    try {
      const result = await Api.createComplaint(payload);
      if (result.duplicate) {
        UI.showFormOk(form, "This was already reported nearby as complaint #" + result.id +
          ". Your report was added to it, and you can track it in your list.");
      } else {
        UI.showFormOk(form, "Complaint #" + result.id + " submitted. " +
          (result.assignedOfficerName ? "Assigned to " + result.assignedOfficerName + "." : "It will be assigned to an officer."));
      }
      document.getElementById("description").value = "";
      document.getElementById("landmark").value = "";
      clearPin();
      loadMine(0);
    } catch (err) {
      UI.showFormErrors(form, err);
    } finally {
      button.disabled = false;
    }
  });

  // ---------- My complaints ----------

  async function loadMine(page) {
    currentPage = page;
    try {
      const data = await Api.listComplaints({
        status: mineFilter.value,
        page: page,
        size: 10,
        sort: "createdAt,desc",
      });
      UI.renderComplaintList(mineList, minePager, data,
        function (id) { UI.openComplaint(id, function () { loadMine(currentPage); }); },
        loadMine,
        mineFilter.value ? "No complaints with this status." : "You have not reported anything yet. Use the form to report a problem.");
    } catch (err) {
      mineList.innerHTML = '<li class="empty">' + UI.esc(err.message) + "</li>";
    }
  }

  mineFilter.addEventListener("change", function () { loadMine(0); });

  loadReference();
  loadMine(0);
})();
