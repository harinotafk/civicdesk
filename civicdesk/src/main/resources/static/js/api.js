// CivicDesk API client: stores the login token and wraps fetch().
(function () {
  "use strict";

  const TOKEN_KEY = "civicdesk.token";
  const USER_KEY = "civicdesk.user";

  function safeGet(key) {
    try { return localStorage.getItem(key); } catch (e) { return null; }
  }
  function safeSet(key, value) {
    try { localStorage.setItem(key, value); } catch (e) { /* storage blocked: stay logged in for this page only */ }
  }
  function safeRemove(key) {
    try { localStorage.removeItem(key); } catch (e) { /* ignore */ }
  }

  class ApiError extends Error {
    constructor(status, body) {
      super((body && body.message) || ("Request failed (" + status + ")"));
      this.status = status;
      this.fieldErrors = (body && body.fieldErrors) || {};
    }
  }

  const session = {
    token: safeGet(TOKEN_KEY),
    user: (function () {
      try { return JSON.parse(safeGet(USER_KEY) || "null"); } catch (e) { return null; }
    })(),
  };

  function saveSession(auth) {
    session.token = auth.token;
    session.user = auth.user;
    safeSet(TOKEN_KEY, auth.token);
    safeSet(USER_KEY, JSON.stringify(auth.user));
  }

  function clearSession() {
    session.token = null;
    session.user = null;
    safeRemove(TOKEN_KEY);
    safeRemove(USER_KEY);
  }

  async function request(method, path, body) {
    const headers = { "Accept": "application/json" };
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (session.token) headers["Authorization"] = "Bearer " + session.token;

    let response;
    try {
      response = await fetch(path, {
        method: method,
        headers: headers,
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch (networkError) {
      throw new ApiError(0, { message: "Cannot reach the server. Is the app running?" });
    }

    let data = null;
    const text = await response.text();
    if (text) {
      try { data = JSON.parse(text); } catch (e) { data = null; }
    }

    if (response.status === 401 && session.token && !path.startsWith("/api/auth/login")) {
      // Token expired or account disabled: back to the login page.
      clearSession();
      window.location.href = "/index.html?expired=1";
      throw new ApiError(401, data);
    }
    if (!response.ok) {
      throw new ApiError(response.status, data);
    }
    return data;
  }

  function query(params) {
    const parts = [];
    Object.keys(params).forEach(function (k) {
      const v = params[k];
      if (v === undefined || v === null || v === "" || v === false) return;
      parts.push(encodeURIComponent(k) + "=" + encodeURIComponent(v));
    });
    return parts.length ? "?" + parts.join("&") : "";
  }

  window.Api = {
    ApiError: ApiError,
    session: session,
    saveSession: saveSession,
    clearSession: clearSession,
    query: query,

    // Auth
    login: (email, password) => request("POST", "/api/auth/login", { email: email, password: password }),
    register: (payload) => request("POST", "/api/auth/register", payload),
    me: () => request("GET", "/api/auth/me"),

    // Reference data
    zones: () => request("GET", "/api/zones"),
    wards: (zoneId) => request("GET", "/api/zones/" + encodeURIComponent(zoneId) + "/wards"),
    categories: () => request("GET", "/api/categories"),

    // Complaints
    createComplaint: (payload) => request("POST", "/api/complaints", payload),
    listComplaints: (params) => request("GET", "/api/complaints" + query(params || {})),
    getComplaint: (id) => request("GET", "/api/complaints/" + encodeURIComponent(id)),
    history: (id) => request("GET", "/api/complaints/" + encodeURIComponent(id) + "/history"),
    updateStatus: (id, payload) => request("PATCH", "/api/complaints/" + encodeURIComponent(id) + "/status", payload),

    // Officers and admin
    analytics: () => request("GET", "/api/analytics"),
    users: (page, size) => request("GET", "/api/admin/users" + query({ page: page, size: size })),
    createStaff: (payload) => request("POST", "/api/admin/users", payload),
    setActive: (id, active) => request("PATCH", "/api/admin/users/" + encodeURIComponent(id) + "/active", { active: active }),
    runSla: () => request("POST", "/api/admin/sla/run"),
    generateDemo: (count) => request("POST", "/api/admin/demo/complaints" + query({ count: count })),

    // Policies (read: any logged-in user, write: admin)
    policies: (params) => request("GET", "/api/policies" + query(params || {})),
    createPolicy: (payload) => request("POST", "/api/admin/policies", payload),
    updatePolicy: (id, payload) => request("PUT", "/api/admin/policies/" + encodeURIComponent(id), payload),
    setPolicyStatus: (id, status) => request("PATCH", "/api/admin/policies/" + encodeURIComponent(id) + "/status", { status: status }),
  };
})();
