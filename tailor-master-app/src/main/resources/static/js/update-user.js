document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("profileForm");
    const modalElement = document.getElementById("confirmProfileModal");
    const confirmButton = document.getElementById("confirmProfileUpdate");
    const logoInput = document.getElementById("logo");
    const messages = window.profileMessages || {};
    const profileModal = bootstrap.Modal.getOrCreateInstance(modalElement);
    let confirmed = false;

    const fields = [
        { id: "fullName", message: messages.fullName, min: 3 },
        { id: "phone1", message: messages.phone, phone: true, required: true },
        { id: "phone2", message: messages.phone, phone: true, required: false },
        { id: "shopName", message: messages.shopName, min: 2 },
        { id: "proprietorName", message: messages.proprietorName, min: 3 },
        { id: "shopAddress", message: messages.shopAddress, min: 5 }
    ];

    function validateField(input, config) {
        const value = input.value.trim();
        let error = "";

        if (config.required && !value) {
            error = config.message;
        } else if (!value && !config.required) {
            error = "";
        } else if (config.phone && !/^03\d{9}$/.test(value)) {
            error = config.message;
        } else if (config.min && value.length < config.min) {
            error = config.message;
        }

        input.setCustomValidity(error);
        return input.checkValidity();
    }

    function validateLogo() {
        if (!logoInput || !logoInput.files.length) {
            if (logoInput) logoInput.setCustomValidity("");
            return true;
        }

        const file = logoInput.files[0];
        const allowedTypes = ["image/png", "image/jpeg"];
        const valid = allowedTypes.includes(file.type);

        logoInput.setCustomValidity(valid ? "" : messages.logo);
        return valid;
    }

    fields.forEach(function (config) {
        const input = document.getElementById(config.id);
        if (!input) return;

        input.addEventListener("input", function () {
            validateField(input, config);
        });

        input.addEventListener("blur", function () {
            validateField(input, config);
        });
    });

    if (logoInput) {
        logoInput.addEventListener("change", validateLogo);
    }

    form.addEventListener("submit", function (event) {
        if (confirmed) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();

        let isValid = true;

        fields.forEach(function (config) {
            const input = document.getElementById(config.id);
            if (input && !validateField(input, config)) {
                isValid = false;
            }
        });

        if (!validateLogo()) {
            isValid = false;
        }

        form.classList.add("was-validated");

        if (!isValid || !form.checkValidity()) {
            form.reportValidity();

            const firstInvalid = form.querySelector(":invalid");
            if (firstInvalid) {
                firstInvalid.focus();
            }
            return;
        }

        profileModal.show();
    });

    confirmButton.addEventListener("click", function () {
        confirmed = true;
        confirmButton.disabled = true;
        form.requestSubmit();
    });

    modalElement.addEventListener("hidden.bs.modal", function () {
        if (!confirmed) {
            confirmButton.disabled = false;
        }
    });
});