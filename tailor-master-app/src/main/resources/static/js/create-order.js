// $(document).ready(function () {
//     // Product selection handling
//     $('#products').on('change', function () {
//         const productId = $(this).val();
//
//         // Reset all dependent fields
//         resetFields();
//
//         if (productId) {
//             // Fetch product details via AJAX
//             $.ajax({
//                 url: contextPath + 'products/' + productId,
//                 type: 'GET',
//                 success: function (response) {
//                     const price = response.price; // Assuming 'price' is a property in the response
//                     $('#price').val(price); // Set the product price
//                     $('#quantity').val(1); // Reset quantity to 1
//                     calculateTotal(); // Calculate the total payment
//                     $('#productDetails').show(); // Show product details if applicable
//                 },
//                 error: function (jqXHR, textStatus, errorThrown) {
//                     console.error("Error fetching product details:", textStatus, errorThrown);
//                     // Handle error appropriately
//                 }
//             });
//         }
//     });
//
//     // Order Date and Delivery Date Management
//     const orderDateInput = document.getElementById("orderDate");
//     const deliveryDateInput = document.getElementById("deliveryDate");
//
//     if (orderDateInput && deliveryDateInput) {
//         const today = new Date().toISOString().split("T")[0];
//         orderDateInput.min = today;
//         deliveryDateInput.min = today;
//
//         orderDateInput.addEventListener("change", function () {
//             const selectedOrderDate = orderDateInput.value;
//             deliveryDateInput.min = selectedOrderDate;
//
//             if (deliveryDateInput.value < selectedOrderDate) {
//                 deliveryDateInput.value = selectedOrderDate;
//             }
//         });
//     }
//
//     // Quantity and Total Payment Calculation
//     $('#quantity, #price').on('input', calculateTotal);
//
//     // Extra Charges Description Handling
//     $('#extraCharges').on('input', toggleExtraChargesDescription);
//     toggleExtraChargesDescription(); // Initialize extra charges description state
// });

// Reset dependent fields
function resetFields() {
    $('#price').val(''); // Clear price
    $('#quantity').val(1); // Reset quantity to default 1
    $('#totalPayment').val(''); // Clear total payment
    $('#extraCharges').val(''); // Clear extra charges
    $('#extraChargesDescription').val('').prop('readonly', true); // Clear and disable extra charges description
    $('#productDetails').hide(); // Hide product details section if applicable
}

// Calculate total payment
function calculateTotal() {
    const quantity = parseInt($('#quantity').val());
    const price = parseFloat($('#price').val());

    if (!isNaN(quantity) && !isNaN(price)) {
        const totalPayment = quantity * price;
        $('#totalPayment').val(totalPayment.toFixed(2));
    } else {
        $('#totalPayment').val('');
    }
}

// Toggle extra charges description field
function toggleExtraChargesDescription() {
    const extraCharges = parseFloat($('#extraCharges').val()) || 0;
    if (extraCharges > 0) {
        $('#extraChargesDescription').prop('readonly', false);
    } else {
        $('#extraChargesDescription').val('').prop('readonly', true);
    }
}

$(document).ready(function () {
    $('#products').on('change', function () {
        var selectedProductId = $('#products').val();
        if (selectedProductId) {
            // Make an AJAX call to fetch product details based on selectedProductId
            $.ajax({
                url: contextPath + 'products/' + selectedProductId, // Replace with your actual product details endpoint
                type: 'GET',
                dataType: 'json',
                success: function (productData) {
                    // Generate the dynamic HTML for the selected product
                    var productHtml = `
                        <div class="product-card">
                            <span class="remove-product" onclick="removeProduct(this)">✖ Remove</span>
                            
                            <div class="form-group mt-3">
                                <label for="productName">Product Name</label>
                                <h3 class="product-name" th:text="${productData.name}"></h3>
                            </div>
                            <div class="form-group mt-3">
                                <label for="productPrice">Price</label>
                                <input type="text" id="productPrice" value="${productData.price}" class="form-control" readonly>
                            </div>
                            <div class="form-group mt-3">
                                <label for="quantity">Quantity</label>
                                <input type="number" id="quantity" name="quantity" class="form-control" min="1" required value="1">
                            </div>
                        </div>`;

                    $('#productsContainer').append(productHtml);
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    console.error('Error fetching product details:', textStatus, errorThrown);
                    // Handle errors appropriately, e.g., display an error message to the user
                }
            });
        }
    });

});

function removeProduct(element) {
    $(element).closest('.product-card').remove();
}