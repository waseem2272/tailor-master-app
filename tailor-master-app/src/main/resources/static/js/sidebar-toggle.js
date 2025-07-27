document.addEventListener("DOMContentLoaded", function () {

    const sidebar = document.getElementById('sidebar');
    const toggleBtn = document.getElementById('toggleSidebarBtn');
    const content = document.querySelector('.content');

    toggleBtn.addEventListener('click', function () {
        sidebar.classList.toggle('show');
        document.body.classList.toggle('sidebar-open');
    });

    document.addEventListener('click', function (e) {
        if (window.innerWidth <= 992 && sidebar.classList.contains('show')) {
            if (!sidebar.contains(e.target) && !toggleBtn.contains(e.target)) {
                sidebar.classList.remove('show');
                document.body.classList.remove('sidebar-open');
            }
        }
    });

    /*const toggleBtn = document.getElementById("toggleSidebarBtn");
    const sidebar = document.getElementById("sidebar");

    toggleBtn.addEventListener("click", function () {
        sidebar.classList.toggle("collapsed");
    });*/

});
