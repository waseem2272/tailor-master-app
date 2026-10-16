
(function () {
    const THEME_KEY = "tailor-master-theme";
    const root = document.documentElement;

    function applyTheme(theme) {
        const isDark = theme === "dark";

        root.setAttribute("data-theme", isDark ? "dark" : "light");

        const icon = document.getElementById("themeToggleIcon");
        const button = document.getElementById("themeToggleBtn");

        if (icon) {
            icon.className = isDark
                ? "bi bi-sun-fill"
                : "bi bi-moon-stars-fill";
        }

        if (button) {
            button.setAttribute(
                "aria-label",
                isDark ? "Switch to light mode" : "Switch to dark mode"
            );

            button.setAttribute(
                "title",
                isDark ? "Light Mode" : "Dark Mode"
            );
        }
    }

    const savedTheme = localStorage.getItem(THEME_KEY);
    applyTheme(savedTheme === "dark" ? "dark" : "light");

    document.addEventListener("DOMContentLoaded", function () {
        const button = document.getElementById("themeToggleBtn");

        if (!button) {
            return;
        }

        button.addEventListener("click", function () {
            const currentTheme =
                root.getAttribute("data-theme") || "light";

            const newTheme =
                currentTheme === "dark" ? "light" : "dark";

            localStorage.setItem(THEME_KEY, newTheme);
            applyTheme(newTheme);
        });
    });
})();
