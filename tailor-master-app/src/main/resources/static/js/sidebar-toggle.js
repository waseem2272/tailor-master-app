document.addEventListener("DOMContentLoaded", function () {

    const sidebar = document.getElementById("sidebar");
    const toggleBtn = document.getElementById("toggleSidebarBtn");

    if (!sidebar || !toggleBtn) {
        return;
    }

    toggleBtn.addEventListener("click", function () {
        document.body.classList.toggle("sidebar-open");
    });

    document.addEventListener("click", function (e) {
        if (window.innerWidth <= 992 &&
            document.body.classList.contains("sidebar-open")) {

            if (!sidebar.contains(e.target) && !toggleBtn.contains(e.target)) {
                document.body.classList.remove("sidebar-open");
            }
        }
    });

});