
function validateForm() {
    let isValid = true;

    // Validate Full Name
    const fullName = document.getElementById("fullName");
    if (fullName.value.trim() === "") {
        showError(fullName, "Full Name is required.");
        isValid = false;
    } else if (fullName.value.trim().length < 3) {
        showError(fullName, "Full Name must have at least 3 characters.");
        isValid = false;
    } else {
        removeError(fullName);
    }

    // Validate Phone Number (Must start with 03 and be exactly 11 digits)
    const phoneNumber = document.getElementById("phoneNumber");
    const phoneRegex = /^03\d{9}$/; // Starts with '03' and followed by 9 digits

    if (phoneNumber.value.trim() === "") {
        showError(phoneNumber, "Phone Number is required.");
        isValid = false;
    } else if (!phoneRegex.test(phoneNumber.value.trim())) {
        showError(phoneNumber, "Enter a valid 11-digit phone number starting with 03.");
        isValid = false;
    } else {
        removeError(phoneNumber);
    }

    // Validate if at least one product is selected
    const productCheckboxes = document.querySelectorAll('.product-checkbox');
    const isProductSelected = [...productCheckboxes].some(checkbox => checkbox.checked);
    const productErrorContainer = document.getElementById("productError");

    if (!isProductSelected) {
        productErrorContainer.innerText = "Please select at least one product.";
        productErrorContainer.style.color = "red";
        productErrorContainer.style.fontSize = "12px";
        isValid = false;
    } else {
        productErrorContainer.innerText = ""; // Clear error message if valid
    }

    // Validate measurement fields for selected products
    document.querySelectorAll('.measurement-fields').forEach(measurementField => {
        const productId = measurementField.getAttribute('data-product-id');
        const checkbox = document.querySelector(`.product-checkbox[value="${productId}"]`);

        if (checkbox && checkbox.checked && measurementField.style.display !== 'none') {
            const inputs = measurementField.querySelectorAll('input');

            inputs.forEach(input => {
                removeError(input); // Remove previous error messages

                if (input.value.trim() === '') {
                    showError(input, `${input.placeholder.split("Enter")[1]} is required.`);
                    isValid = false;
                }
            });
        }
    });

    return isValid;
}

// Function to show error message
function showError(input, message) {
    removeError(input); // Remove previous errors
    const errorMessage = document.createElement('span');
    errorMessage.classList.add('error-message');
    errorMessage.style.color = 'red';
    errorMessage.style.fontSize = '12px';
    errorMessage.innerText = `* ${message}`;
    input.after(errorMessage);
}

// Function to remove error message
function removeError(input) {
    let errorSpan = input.nextElementSibling;
    if (errorSpan && errorSpan.classList.contains('error-message')) {
        errorSpan.remove();
    }
}
