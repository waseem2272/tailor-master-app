document.addEventListener("DOMContentLoaded", function () {
    document.getElementById("profileForm").addEventListener("submit", function (event) {
        let isValid = true;

        // Function to Show Error Message
        function showError(input, message) {
            let errorDiv = input.nextElementSibling;
            if (!errorDiv || !errorDiv.classList.contains("text-danger")) {
                errorDiv = document.createElement("div");
                errorDiv.className = "text-danger small mt-1";
                input.parentNode.appendChild(errorDiv);
            }
            errorDiv.textContent = message;
        }

        // Function to Clear Error Message
        function clearError(input) {
            let errorDiv = input.nextElementSibling;
            if (errorDiv && errorDiv.classList.contains("text-danger")) {
                errorDiv.remove();
            }
        }

        // Validate Full Name
        let fullName = document.getElementById("fullName");
        if (fullName.value.trim().length < 3) {
            showError(fullName, "Full name must be at least 3 characters.");
            isValid = false;
        } else {
            clearError(fullName);
        }

        // Validate Phone Number 1
        let phone1 = document.getElementById("phone1");
        if (!/^03\d{9}$/.test(phone1.value.trim())) {
            showError(phone1, "Phone number must start with '03' and be 11 digits long.");
            isValid = false;
        } else {
            clearError(phone1);
        }

        // Validate Phone Number 2 (Optional, but must be valid if entered)
        let phone2 = document.getElementById("phone2");
        if (phone2.value.trim() !== "" && !/^03\d{9}$/.test(phone2.value.trim())) {
            showError(phone2, "Phone number must start with '03' and be 11 digits long.");
            isValid = false;
        } else {
            clearError(phone2);
        }


        // Validate Shop Name
        let shopName = document.getElementById("shopName");
        if (shopName.value.trim().length < 2) {
            showError(shopName, "Shop name must be at least 2 characters.");
            isValid = false;
        } else {
            clearError(shopName);
        }

        // Validate Proprietor Name
        let proprietorName = document.getElementById("proprietorName");
        if (proprietorName.value.trim().length < 3) {
            showError(proprietorName, "Proprietor name must be at least 3 characters.");
            isValid = false;
        } else {
            clearError(proprietorName);
        }

        // Validate Short Code
        let shortCode = document.getElementById("shortCode");
        if (shortCode.value.trim().length < 2 || shortCode.value.trim().length > 5) {
            showError(shortCode, "Short code must be 2-5 characters.");
            isValid = false;
        } else {
            clearError(shortCode);
        }

        // Validate Shop Address
        let shopAddress = document.getElementById("shopAddress");
        if (shopAddress.value.trim().length < 5) {
            showError(shopAddress, "Shop address must be at least 5 characters.");
            isValid = false;
        } else {
            clearError(shopAddress);
        }

        // If any validation fails, prevent form submission
        if (!isValid) {
            event.preventDefault();
        }
    });

});
