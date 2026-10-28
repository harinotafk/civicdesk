// Landing page: log in, register, demo account shortcuts.
(function () {
  "use strict";

  // Already logged in? Go straight to the right dashboard.
  if (Api.session.token && Api.session.user) {
    window.location.replace(UI.homeFor(Api.session.user.role));
    return;
  }

  if (new URLSearchParams(window.location.search).get("expired")) {
    document.getElementById("expired").classList.add("show");
  }

  const tabLogin = document.getElementById("tab-login");
  const tabRegister = document.getElementById("tab-register");
  const loginForm = document.getElementById("login-form");
  const registerForm = document.getElementById("register-form");

  function showTab(which) {
    const login = which === "login";
    tabLogin.setAttribute("aria-selected", String(login));
    tabRegister.setAttribute("aria-selected", String(!login));
    loginForm.classList.toggle("hidden", !login);
    registerForm.classList.toggle("hidden", login);
  }
  tabLogin.addEventListener("click", function () { showTab("login"); });
  tabRegister.addEventListener("click", function () { showTab("register"); });

  function goHome(auth) {
    Api.saveSession(auth);
    window.location.href = UI.homeFor(auth.user.role);
  }

  loginForm.addEventListener("submit", async function (e) {
    e.preventDefault();
    UI.showFormErrors(loginForm, null);
    const button = loginForm.querySelector("button[type=submit]");
    button.disabled = true;
    try {
      const auth = await Api.login(document.getElementById("login-email").value.trim(),
        document.getElementById("login-password").value);
      goHome(auth);
    } catch (err) {
      UI.showFormErrors(loginForm, err);
      button.disabled = false;
    }
  });

  registerForm.addEventListener("submit", async function (e) {
    e.preventDefault();
    UI.showFormErrors(registerForm, null);
    const button = registerForm.querySelector("button[type=submit]");
    button.disabled = true;
    try {
      const auth = await Api.register({
        // (form.name would be the form's own name attribute, so read inputs by id)
        name: document.getElementById("reg-name").value.trim(),
        email: document.getElementById("reg-email").value.trim(),
        phone: document.getElementById("reg-phone").value.trim(),
        password: document.getElementById("reg-password").value,
      });
      goHome(auth);
    } catch (err) {
      UI.showFormErrors(registerForm, err);
      button.disabled = false;
    }
  });

  document.querySelectorAll("[data-demo]").forEach(function (btn) {
    btn.addEventListener("click", function () {
      showTab("login");
      document.getElementById("login-email").value = btn.dataset.demo;
      document.getElementById("login-password").value = "Password@123";
      loginForm.querySelector("button[type=submit]").focus();
    });
  });
})();
