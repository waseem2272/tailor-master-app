document.addEventListener("DOMContentLoaded", function () {
    const sidebar = document.getElementById('sidebar');
    const toggleBtn = document.getElementById('toggleSidebarBtn');

    toggleBtn.addEventListener('click', function () {
        sidebar.classList.toggle('show');
    });

    document.addEventListener('click', function (e) {
        if (window.innerWidth <= 992 && sidebar.classList.contains('show')) {
            if (!sidebar.contains(e.target) && !toggleBtn.contains(e.target)) {
                sidebar.classList.remove('show');
            }
        }
    });
});
