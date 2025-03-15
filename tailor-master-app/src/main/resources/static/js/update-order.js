document.addEventListener("DOMContentLoaded", function () {

    let selectedProductsContainer = document.getElementById("selectedProductsContainer");
    let selectedProductsHiddenContainer = document.getElementById("selectedProductsHiddenContainer");
    let advancePaymentInput = document.getElementById("advancePayment");
    let duePaymentInput = document.getElementById("duePayment");
    let totalProductAmountInput = document.getElementById("totalProductAmount");

    let updateOrderButton = document.getElementById("updateOrderButton");
    let confirmOrderButton = document.getElementById("confirmOrderButton");
    let orderSummaryBody = document.getElementById("orderSummaryBody");
    let modalTotalAmount = document.getElementById("modalTotalAmount");
    let modalAdvancePayment = document.getElementById("modalAdvancePayment");
    let modalDuePayment = document.getElementById("modalDuePayment");

    let modalCustomerName = document.getElementById("modalCustomerName");
    let modalCustomerContact = document.getElementById("modalCustomerContact");

    let orderDateInput = document.getElementById("orderDate");
    let deliveryDateInput = document.getElementById("deliveryDate");

    function setMinDates() {
        let today = new Date().toISOString().split("T")[0]; // Get today's date in yyyy-MM-dd format
        let orderDateValue = orderDateInput.value || today; // Default to today if empty

        orderDateInput.setAttribute("min", today);
        deliveryDateInput.setAttribute("min", orderDateValue);
    }

    function validateDates() {
        let orderDate = new Date(orderDateInput.value);
        let deliveryDate = new Date(deliveryDateInput.value);

        if (deliveryDate < orderDate) {
            showError(deliveryDateInput, "Delivery date cannot be before the order date.");
            deliveryDateInput.value = ""; // Reset invalid input
        } else {
            removeErrors(deliveryDateInput);
        }
    }

    orderDateInput.addEventListener("change", function () {
        deliveryDateInput.setAttribute("min", orderDateInput.value);
        validateDates();
    });

    deliveryDateInput.addEventListener("change", validateDates);

    setMinDates(); // Set min dates on page load

    let form = document.querySelector("form");

    function updateTotal() {
        let subtotal = 0;
        let productIndex = 0;

        document.querySelectorAll(".product-quantity").forEach(input => {
            let productId = input.dataset.productId;
            let quantity = parseInt(input.value.trim(), 10) || 0;

            let priceElement = document.getElementById(`hidden_price_${productId}`);
            let price = priceElement ? parseFloat(priceElement.value) || 0 : 0;

            let productSubtotal = quantity * price;
            subtotal += productSubtotal;

            let subtotalField = document.querySelector(`#product_card_${productId} .product-subtotal`);
            if (subtotalField) {
                subtotalField.innerText = productSubtotal.toFixed(2);
            }

            // ✅ Remove old hidden inputs before creating new ones
            removeProductCard(productId);

            // ✅ Correct indexing
            selectedProductsHiddenContainer.appendChild(createHiddenInput(`hidden_product_${productId}`, `orderProducts[${productIndex}].id`, productId));
            selectedProductsHiddenContainer.appendChild(createHiddenInput(`hidden_quantity_${productId}`, `orderProducts[${productIndex}].quantity`, quantity));
            selectedProductsHiddenContainer.appendChild(createHiddenInput(`hidden_price_${productId}`, `orderProducts[${productIndex}].price`, price));
            selectedProductsHiddenContainer.appendChild(createHiddenInput(`hidden_name_${productId}`, `orderProducts[${productIndex}].name`, document.querySelector(`#product_card_${productId} .card-header`).innerText));

            productIndex++;
        });

        totalProductAmountInput.value = subtotal.toFixed(2);
        let advancePayment = parseFloat(advancePaymentInput.value || 0);
        duePaymentInput.value = (subtotal - advancePayment).toFixed(2);
    }

    // ✅ Validate advance payment and recalculate due payment
    advancePaymentInput.addEventListener("input", function () {
        updateTotal();
    });

    function validateForm() {
        removeErrors();
        let isValid = true;

        if (document.querySelectorAll(".product-checkbox:checked").length === 0) {
            showError(selectedProductsContainer, "Please select at least one product.");
            isValid = false;
        }

        let totalCalculatedAmount = 0;

        document.querySelectorAll(".product-quantity").forEach(input => {
            let productId = input.dataset.productId;
            let quantity = parseInt(input.value.trim(), 10) || 0; // Ensure it's a valid number

            console.log(`Product ID: ${productId}, Quantity: ${quantity}`);

            let priceElement = document.getElementById(`hidden_price_${productId}`);
            let price = priceElement ? parseFloat(priceElement.value) || 0 : 0;

            if (quantity <= 0) {
                showError(input, "Quantity must be greater than 0.");
                isValid = false;
            }

            totalCalculatedAmount += (quantity * price);
        });

        let totalAmount = parseFloat(totalProductAmountInput.value || 0);
        if (totalAmount !== totalCalculatedAmount) {
            showError(totalProductAmountInput, "Total product amount does not match the calculated total.");
            isValid = false;
        }

        let advancePayment = parseFloat(advancePaymentInput.value || 0);
        let duePayment = parseFloat(duePaymentInput.value || 0);

        if (advancePayment < 0) {
            showError(advancePaymentInput, "Advance payment cannot be negative.");
            isValid = false;
        }

        if (advancePayment > totalCalculatedAmount) {
            showError(advancePaymentInput, "Advance payment cannot be greater than the total amount.");
            isValid = false;
        }

        let expectedDuePayment = totalCalculatedAmount - advancePayment;
        if (duePayment !== expectedDuePayment) {
            showError(duePaymentInput, "Due payment does not match the expected amount.");
            isValid = false;
        }

        updateTotal();
        return isValid;
    }

    function generateProductCard(productId, productName, productPrice, quantity) {
        let existingCard = document.getElementById(`product_card_${productId}`);

        if (existingCard) {
            let quantityInput = existingCard.querySelector(".product-quantity");
            quantityInput.value = quantity;

            // ✅ Remove hidden inputs before adding new ones
            removeProductCard(productId);

            selectedProductsHiddenContainer.appendChild(createHiddenInput(`hidden_quantity_${productId}`, `orderProducts[${productId}].quantity`, quantity));
            updateTotal();
            return;
        }

        removeProductCard(productId);

        let productCard = document.createElement("div");
        productCard.classList.add("col-md-6");
        productCard.id = `product_card_${productId}`;

        productCard.innerHTML = `
    <div class="card p-3 border-success shadow-sm">
        <div class="card-header bg-secondary text-white fw-bold">${productName}</div>
        <div class="card-body">
            <div class="row">
                <div class="col-6"><strong>Price:</strong> <span class="product-price">${productPrice}</span></div>
                <div class="col-6">
                    <strong>Quantity:</strong>
                    <input type="number" min="1" value="${quantity}" class="form-control product-quantity" data-product-id="${productId}">
                </div>
            </div>
            <div class="row mt-2">
                <div class="col-12"><strong>Subtotal:</strong> <span class="product-subtotal">${(productPrice * quantity).toFixed(2)}</span></div>
            </div>
        </div>
    </div>
    `;

        selectedProductsContainer.appendChild(productCard);

        selectedProductsHiddenContainer.appendChild(createHiddenInput(`hidden_product_${productId}`, `orderProducts[${productId}].id`, productId));
        selectedProductsHiddenContainer.appendChild(createHiddenInput(`hidden_quantity_${productId}`, `orderProducts[${productId}].quantity`, quantity));
        selectedProductsHiddenContainer.appendChild(createHiddenInput(`hidden_price_${productId}`, `orderProducts[${productId}].price`, productPrice));
        selectedProductsHiddenContainer.appendChild(createHiddenInput(`hidden_name_${productId}`, `orderProducts[${productId}].name`, productName));

        productCard.querySelector(".product-quantity").addEventListener("input", function () {
            updateTotal();
        });

        updateTotal();
    }

    function removeProductCard(productId) {
        document.getElementById(`hidden_product_${productId}`)?.remove();
        document.getElementById(`hidden_quantity_${productId}`)?.remove();
        document.getElementById(`hidden_price_${productId}`)?.remove();
        document.getElementById(`hidden_name_${productId}`)?.remove();
    }

    function createHiddenInput(id, name, value) {
        // Remove any existing input with the same name to avoid duplicates
        let existingInput = document.querySelector(`input[name="${name}"]`);
        if (existingInput) {
            existingInput.remove();
        }

        let input = document.createElement("input");
        input.type = "hidden";
        input.id = id;
        input.name = name; // Ensure correct name format
        input.value = value;
        return input;
    }


    function showError(element, message) {
        let errorDiv = document.createElement("div");
        errorDiv.className = "text-danger error-message";
        errorDiv.innerText = message;
        element.parentNode.appendChild(errorDiv);
    }

    function removeErrors() {
        document.querySelectorAll(".error-message").forEach(el => el.remove());
    }

    document.querySelectorAll(".product-checkbox").forEach((checkbox) => {
        let productId = checkbox.value;
        let productLabel = document.querySelector(`label[for="product_${productId}"]`);
        let productName = productLabel ? productLabel.innerText.trim() : "Unknown Product";
        let productPrice = parseFloat(checkbox.dataset.price || 0);
        let selectedQuantity = checkbox.dataset.quantity ? parseInt(checkbox.dataset.quantity) : 1;

        if (checkbox.checked) {
            generateProductCard(productId, productName, productPrice, selectedQuantity);
        }

        checkbox.addEventListener("change", function () {
            removeErrors();
            if (this.checked) {
                generateProductCard(productId, productName, productPrice, selectedQuantity);
            } else {
                let card = document.getElementById(`product_card_${productId}`);
                if (card) {
                    card.remove(); // ✅ Only remove the product card
                }
                removeProductCard(productId); // ✅ Remove associated hidden inputs
                updateTotal(); // ✅ Ensure the total updates correctly
            }
        });
    });


    updateOrderButton.addEventListener("click", function (event) {
        event.preventDefault();

        updateTotal(); // ✅ Ensure totals are updated before validation

        if (!validateForm()) return;

        modalCustomerName.innerText = document.getElementById("customerName").value || "-";
        modalCustomerContact.innerText = document.getElementById("customerPhone").value || "-";

        orderSummaryBody.innerHTML = "";

        document.querySelectorAll("#selectedProductsContainer .card").forEach((product) => {
            let productName = product.querySelector(".card-header").innerText;
            let productPrice = product.querySelector(".product-price").innerText;
            let quantity = product.querySelector(".product-quantity").value;
            let subtotal = product.querySelector(".product-subtotal").innerText;

            let row = `
            <tr>
                <td>${productName}</td>
                <td>${parseFloat(productPrice).toFixed(2)}</td>
                <td>${quantity}</td>
                <td>${parseFloat(subtotal).toFixed(2)}</td>
            </tr>
        `;
            orderSummaryBody.innerHTML += row;
        });

        modalTotalAmount.innerText = parseFloat(totalProductAmountInput.value || 0).toFixed(2);
        modalAdvancePayment.innerText = parseFloat(advancePaymentInput.value || 0).toFixed(2);
        modalDuePayment.innerText = parseFloat(duePaymentInput.value || 0).toFixed(2);

        let orderSummaryModal = new bootstrap.Modal(document.getElementById("orderSummaryModal"));
        orderSummaryModal.show();
    });


    confirmOrderButton.addEventListener("click", function () {
        let formData = new FormData(form);
        for (let pair of formData.entries()) {
            console.log(pair[0] + ": " + pair[1]); // ✅ Debug submitted values
        }

        form.submit();
    });
});