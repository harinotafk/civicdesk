// Simple tab switching: buttons with data-tab="x" show the section with id "tab-x".
(function () {
  "use strict";
  window.Tabs = {
    init: function (onShow) {
      const buttons = document.querySelectorAll('.tabs button[data-tab]');
      buttons.forEach(function (btn) {
        btn.addEventListener("click", function () {
          buttons.forEach(function (b) {
            const active = b === btn;
            b.setAttribute("aria-selected", String(active));
            const section = document.getElementById("tab-" + b.dataset.tab);
            if (section) section.classList.toggle("hidden", !active);
          });
          if (onShow) onShow(btn.dataset.tab);
        });
      });
    },
  };
})();
