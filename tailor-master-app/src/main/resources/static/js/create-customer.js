
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
