
(function () {
    const localeCookie = document.cookie
        .split("; ")
        .find(row => row.startsWith("TAILOR_MASTER_LOCALE="));

    const language = localeCookie
        ? decodeURIComponent(localeCookie.split("=").slice(1).join("="))
        : "en";

    const isUrdu = language === "ur";

    document.documentElement.lang = language;
    document.documentElement.dir = isUrdu ? "rtl" : "ltr";
})();