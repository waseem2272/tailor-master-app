
function validateForm() {
    let isValid = true;

    // Validate if at least one product is selected
    const productCheckboxes = document.querySelectorAll('.product-checkbox');
    const isProductSelected = [...productCheckboxes].some(checkbox => checkbox.checked);

    if (!isProductSelected) {
        alert("Please select at least one product.");
        isValid = false;
        return isValid;
    }

    // Validate measurement fields for selected products
    document.querySelectorAll('.measurement-fields').forEach(measurementField => {
        // Retrieve the product ID dynamically from the data attribute
        const productId = measurementField.getAttribute('data-product-id');
        const checkbox = document.querySelector(`.product-checkbox[value="${productId}"]`);

        if (checkbox && checkbox.checked && measurementField.style.display !== 'none') {
            const inputs = measurementField.querySelectorAll('input');

            inputs.forEach(input => {
                // Remove previous error messages
                let errorSpan = input.nextElementSibling;
                if (errorSpan && errorSpan.classList.contains('error-message')) {
                    errorSpan.remove();
                }

                if (input.value.trim() === '') {
                    // Extract the field name (without prefixes)
                    const fieldName = input.name.split('.').pop();  // Get the last part of the name

                    // Create and display error message below the input field
                    const errorMessage = document.createElement('span');
                    errorMessage.classList.add('error-message');
                    errorMessage.style.color = 'red';
                    errorMessage.style.fontSize = '12px';
                    errorMessage.innerText = `* ${fieldName} is required.`;

                    input.after(errorMessage); // Insert error message after the input field
                    isValid = false;
                }
            });
        }
    });

    return isValid;
}