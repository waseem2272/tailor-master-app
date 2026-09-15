document.addEventListener("DOMContentLoaded", function () {
    let orderItems = [];
    let editingProductIndex = -1;

    const productSelect = document.getElementById("productSelect");
    const silaiSelect = document.getElementById("silaiSelect");
    const quantityInput = document.getElementById("quantity");
    const fabricSource = document.getElementById("fabricSource");
    const shopFabricContainer = document.getElementById("shopFabricContainer");
    const fabricQuantityContainer = document.getElementById("fabricQuantityContainer");
    const inventoryItemSelect = document.getElementById("inventoryItemSelect");
    const fabricQuantityInput = document.getElementById("fabricQuantity");
    const fabricStockInfo = document.getElementById("fabricStockInfo");
    const additionalNotes = document.getElementById("additionalNotes");
    const addProductBtn = document.getElementById("addProductBtn");
    const cancelEditBtn = document.getElementById("cancelEditBtn");
    const productTableBody = document.getElementById("productTableBody");
    const noProductsMessage = document.getElementById("noProductsMessage");
    const orderProductsInputs = document.getElementById("orderProductsInputs");
    const totalProductAmount = document.getElementById("totalProductAmount");
    const advancePayment = document.getElementById("advancePayment");
    const duePayment = document.getElementById("duePayment");
    const orderDate = document.getElementById("orderDate");
    const deliveryDate = document.getElementById("deliveryDate");
    const updateOrderButton = document.getElementById("updateOrderButton");
    const confirmOrderButton = document.getElementById("confirmOrderButton");
    const productSectionTitle = document.getElementById("productSectionTitle");
    const productError = document.getElementById("productError");
    const silaiError = document.getElementById("silaiError");
    const quantityError = document.getElementById("quantityError");
    const fabricSourceError = document.getElementById("fabricSourceError");
    const fabricStockError = document.getElementById("fabricStockError");
    const fabricQuantityError = document.getElementById("fabricQuantityError");
    const advancePaymentError = document.getElementById("advancePaymentError");
    // const orderEditContext = document.getElementById("orderEditContext");
    // const orderStatus = orderEditContext?.dataset.orderStatus || "PENDING";
    const modalElement = document.getElementById("confirmOrderModal");
    const orderModal = new bootstrap.Modal(modalElement);

    function formatAmount(value) {
        const amount = Number(value || 0);
        return amount.toLocaleString("en-PK", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
    }

    function escapeHtml(value) {
        if (value === null || value === undefined) return "";
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function clearErrors() {
        productError.textContent = "";
        silaiError.textContent = "";
        quantityError.textContent = "";
        fabricSourceError.textContent = "";
        fabricStockError.textContent = "";
        fabricQuantityError.textContent = "";
        advancePaymentError.textContent = "";
    }

    function clearFabricErrors() {
        fabricStockError.textContent = "";
        fabricQuantityError.textContent = "";
    }

    function populateSilaiOptions(selectedSilaiType = "") {
        silaiSelect.innerHTML = '<option value="">-- Silai Type --</option>';

        const selectedProduct = productSelect.options[productSelect.selectedIndex];
        if (!selectedProduct || !selectedProduct.value) return;

        const singleSilai = selectedProduct.dataset.singleSilai;
        const doubleSilai = selectedProduct.dataset.doubleSilai;

        if (singleSilai !== undefined && singleSilai !== null && singleSilai !== "") {
            const option = document.createElement("option");
            option.value = "single";
            option.textContent = "Single Silai - Rs. " + formatAmount(singleSilai);
            option.dataset.amount = singleSilai;
            silaiSelect.appendChild(option);
        }

        if (doubleSilai !== undefined && doubleSilai !== null && doubleSilai !== "") {
            const option = document.createElement("option");
            option.value = "double";
            option.textContent = "Double Silai - Rs. " + formatAmount(doubleSilai);
            option.dataset.amount = doubleSilai;
            silaiSelect.appendChild(option);
        }

        if (selectedSilaiType) {
            silaiSelect.value = selectedSilaiType.toLowerCase();
        }
    }

    function calculateSilaiAmount() {
        const selectedOption = silaiSelect.options[silaiSelect.selectedIndex];
        if (!selectedOption || !selectedOption.value) return 0;
        return Number(selectedOption.dataset.amount || 0);
    }

    function getSelectedInventorySalePrice() {
        const selectedOption = inventoryItemSelect.options[inventoryItemSelect.selectedIndex];
        if (!selectedOption || !selectedOption.value) return 0;
        return Number(selectedOption.dataset.salePrice || 0);
    }

    function getCurrentOrderFabricQuantities(inventoryItemId) {
        let total = 0;
        let other = 0;

        orderItems.forEach(function (item, index) {
            if (item.fabricSource !== "SHOP" ||
                String(item.inventoryItemId) !== String(inventoryItemId)) {
                return;
            }

            const quantity = Number(item.fabricQuantity || 0);
            total += quantity;

            if (index !== editingProductIndex) {
                other += quantity;
            }
        });

        return {total, other};
    }

    function getAvailableStock(inventoryItemId) {
        const selectedOption = Array.from(inventoryItemSelect.options)
            .find(option => String(option.value) === String(inventoryItemId));

        if (!selectedOption) return 0;

        const physicalStock = Number(selectedOption.dataset.stock || 0);
        const reservedStock = Number(selectedOption.dataset.reserved || 0);
        const currentOrderQuantities = getCurrentOrderFabricQuantities(inventoryItemId);

        let availableStock = physicalStock - reservedStock;

        // if (orderStatus === "IN_PROGRESS") {
        //     availableStock += currentOrderQuantities.total;
        // }

        availableStock -= currentOrderQuantities.other;

        return Math.max(0, availableStock);
    }

    function updateFabricStockInfo() {
        clearFabricErrors();

        const selectedOption = inventoryItemSelect.options[inventoryItemSelect.selectedIndex];

        if (!selectedOption || !selectedOption.value) {
            fabricStockInfo.textContent = "";
            return;
        }

        const physicalStock = Number(selectedOption.dataset.stock || 0);
        const reservedStock = Number(selectedOption.dataset.reserved || 0);
        const unit = selectedOption.dataset.unit || "";
        const currentOrderQuantities = getCurrentOrderFabricQuantities(selectedOption.value);
        const availableStock = getAvailableStock(selectedOption.value);

        fabricStockInfo.textContent =
            "Physical Stock: " + formatAmount(physicalStock) + " " + unit +
            " | Reserved Stock: " + formatAmount(reservedStock) + " " + unit +
            " | Current Order: " + formatAmount(currentOrderQuantities.other) + " " + unit +
            " | Available Stock: " + formatAmount(availableStock) + " " + unit;
    }

    function validateProduct() {
        clearErrors();

        let valid = true;
        const productId = productSelect.value;
        const silaiType = silaiSelect.value;
        const quantity = Number(quantityInput.value || 0);

        if (!productId) {
            productError.textContent = "Product is required.";
            valid = false;
        }

        if (!silaiType) {
            silaiError.textContent = "Silai type is required.";
            valid = false;
        }

        if (!Number.isInteger(quantity) || quantity < 1) {
            quantityError.textContent = "Quantity must be a whole number of at least 1.";
            valid = false;
        }

        if (!fabricSource.value) {
            fabricSourceError.textContent = "Fabric source is required.";
            valid = false;
        }

        if (fabricSource.value === "SHOP") {
            if (!inventoryItemSelect.value) {
                fabricStockError.textContent = "Shop fabric is required.";
                valid = false;
            }

            const fabricQuantity = Number(fabricQuantityInput.value || 0);

            if (!fabricQuantity || fabricQuantity <= 0) {
                fabricQuantityError.textContent = "Fabric quantity must be greater than zero.";
                valid = false;
            }

            if (inventoryItemSelect.value && fabricQuantity > 0) {
                const selectedOption = inventoryItemSelect.options[inventoryItemSelect.selectedIndex];
                const availableStock = getAvailableStock(inventoryItemSelect.value);

                if (fabricQuantity > availableStock) {
                    fabricStockError.textContent =
                        "Insufficient stock. Available stock: " +
                        formatAmount(availableStock) + " " +
                        (selectedOption.dataset.unit || "");
                    valid = false;
                }
            }

            const salePrice = getSelectedInventorySalePrice();

            if (salePrice < 0) {
                fabricStockError.textContent = "Invalid fabric sale price.";
                valid = false;
            }
        }

        return valid;
    }

    function createProductObject() {
        const selectedProductOption = productSelect.options[productSelect.selectedIndex];
        const productId = Number(productSelect.value);
        const productName = selectedProductOption.textContent.trim();
        const quantity = Number(quantityInput.value);
        const silaiType = silaiSelect.value;
        const silaiAmount = calculateSilaiAmount();
        const fabricSourceValue = fabricSource.value;

        let inventoryItemId = null;
        let inventoryItemName = "";
        let fabricQuantity = null;
        let fabricUnit = "";
        let fabricSalePrice = 0;
        let fabricAmount = 0;

        if (fabricSourceValue === "SHOP") {
            const selectedInventoryOption = inventoryItemSelect.options[inventoryItemSelect.selectedIndex];

            inventoryItemId = Number(inventoryItemSelect.value);
            inventoryItemName = selectedInventoryOption.textContent.split(" - Stock:")[0].trim();
            fabricQuantity = Number(fabricQuantityInput.value);
            fabricUnit = selectedInventoryOption.dataset.unit || "";
            fabricSalePrice = getSelectedInventorySalePrice();
            fabricAmount = fabricQuantity * fabricSalePrice;
        }

        const stitchingAmount = silaiAmount * quantity;
        const subtotal = stitchingAmount + fabricAmount;

        return {
            productId,
            productName,
            silaiType,
            silaiAmount,
            stitchingAmount,
            quantity,
            amount: subtotal,
            subtotal,
            fabricSource: fabricSourceValue,
            inventoryItemId,
            inventoryItemName,
            fabricQuantity,
            fabricUnit,
            fabricSalePrice,
            fabricAmount,
            additionalNotes: additionalNotes.value.trim()
        };
    }

    function addOrUpdateProduct() {
        if (!validateProduct()) return;

        const product = createProductObject();

        if (editingProductIndex === -1) {
            orderItems.push(product);
        } else {
            orderItems[editingProductIndex] = product;
        }

        renderSelectedProducts();
        resetProductForm();
        calculateOrderTotals();
    }

    function renderSelectedProducts() {
        productTableBody.innerHTML = "";

        if (orderItems.length === 0) {
            noProductsMessage.style.display = "block";
            createHiddenFields();
            calculateOrderTotals();
            return;
        }

        noProductsMessage.style.display = "none";

        orderItems.forEach(function (item, index) {
            const row = document.createElement("tr");
            const fabricSourceText = item.fabricSource === "SHOP" ? "Shop Fabric" : "Customer Fabric";
            const shopFabricText = item.fabricSource === "SHOP" ? escapeHtml(item.inventoryItemName) : "-";
            const fabricUnit = item.fabricUnit || "M";
            const fabricQuantityText = item.fabricSource === "SHOP"
                ? formatAmount(item.fabricQuantity) + " " + escapeHtml(fabricUnit)
                : "-";
            const fabricAmountText = item.fabricSource === "SHOP"
                ? "Rs. " + formatAmount(item.fabricAmount)
                : "0.00";
            const notes = item.additionalNotes ? escapeHtml(item.additionalNotes) : "-";

            row.innerHTML = `
                <td>${escapeHtml(item.productName)}</td>
                <td>${escapeHtml(item.silaiType)}<br><small class="text-muted">Rs. ${formatAmount(item.silaiAmount)}</small></td>
                <td>${fabricSourceText}</td>
                <td>${shopFabricText}</td>
                <td>${fabricQuantityText}</td>
                <td>${fabricAmountText}</td>
                <td>Rs. ${formatAmount(item.stitchingAmount)}</td>
                <td>${item.quantity}</td>
                <td><strong>Rs. ${formatAmount(item.subtotal)}</strong></td>
                <td>${notes}</td>
                <td class="text-nowrap">
                    <button type="button" class="btn btn-sm btn-outline-primary edit-product-btn"
                            data-index="${index}" title="Edit Product">
                        <i class="bi bi-pencil-square"></i>
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-danger delete-product-btn"
                            data-index="${index}" title="Delete Product">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            `;

            productTableBody.appendChild(row);
        });

        createHiddenFields();
        calculateOrderTotals();
    }

    function editProduct(index) {
        if (index < 0 || index >= orderItems.length) return;

        const item = orderItems[index];
        editingProductIndex = index;

        productSelect.value = String(item.productId);
        populateSilaiOptions(item.silaiType);
        quantityInput.value = item.quantity;
        fabricSource.value = item.fabricSource || "CUSTOMER";

        toggleShopFabric();

        if (item.fabricSource === "SHOP") {
            inventoryItemSelect.value = item.inventoryItemId ? String(item.inventoryItemId) : "";
            fabricQuantityInput.value =
                item.fabricQuantity !== null && item.fabricQuantity !== undefined
                    ? item.fabricQuantity
                    : "";
            updateFabricStockInfo();
        } else {
            inventoryItemSelect.value = "";
            fabricQuantityInput.value = "";
        }

        additionalNotes.value = item.additionalNotes || "";

        addProductBtn.innerHTML = '<i class="bi bi-check-circle me-1"></i> Update Product';
        cancelEditBtn.classList.remove("d-none");
        productSectionTitle.innerHTML = '<i class="bi bi-pencil-square me-2"></i> Edit Product';

        const productCard = productSectionTitle.closest(".card");

        if (productCard) {
            productCard.scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
        }
    }

    function deleteProduct(index) {
        if (index < 0 || index >= orderItems.length) return;

        if (editingProductIndex === index) {
            resetProductForm();
        } else if (editingProductIndex > index) {
            editingProductIndex--;
        }

        orderItems.splice(index, 1);
        renderSelectedProducts();
    }

    function resetProductForm() {
        editingProductIndex = -1;
        productSelect.value = "";
        silaiSelect.innerHTML = '<option value="">-- Silai Type --</option>';
        quantityInput.value = "1";
        fabricSource.value = "CUSTOMER";
        inventoryItemSelect.value = "";
        fabricQuantityInput.value = "";
        additionalNotes.value = "";

        toggleShopFabric();
        clearErrors();

        addProductBtn.innerHTML = '<i class="bi bi-plus-circle me-1"></i> Add Product';
        cancelEditBtn.classList.add("d-none");
        productSectionTitle.innerHTML = "Add Product to Order";
    }

    function createHiddenFields() {
        orderProductsInputs.innerHTML = "";

        orderItems.forEach(function (item, index) {
            addHiddenField(`orderProducts[${index}].productId`, item.productId);
            addHiddenField(`orderProducts[${index}].quantity`, item.quantity);
            addHiddenField(`orderProducts[${index}].silaiType`, item.silaiType);
            addHiddenField(`orderProducts[${index}].silaiAmount`, item.silaiAmount);
            addHiddenField(`orderProducts[${index}].amount`, item.amount);
            addHiddenField(`orderProducts[${index}].fabricSource`, item.fabricSource);
            addHiddenField(
                `orderProducts[${index}].inventoryItemId`,
                item.fabricSource === "SHOP" ? item.inventoryItemId : ""
            );
            addHiddenField(
                `orderProducts[${index}].fabricQuantity`,
                item.fabricSource === "SHOP" ? item.fabricQuantity : ""
            );
            addHiddenField(
                `orderProducts[${index}].additionalNotes`,
                item.additionalNotes || ""
            );
        });
    }

    function addHiddenField(name, value) {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = name;
        input.value = value !== null && value !== undefined ? value : "";
        orderProductsInputs.appendChild(input);
    }

    function calculateOrderTotals() {
        let total = 0;

        orderItems.forEach(function (item) {
            const stitchingAmount =
                Number(item.silaiAmount || 0) * Number(item.quantity || 0);
            const fabricAmount = Number(item.fabricAmount || 0);

            item.stitchingAmount = stitchingAmount;
            item.amount = stitchingAmount + fabricAmount;
            item.subtotal = item.amount;

            total += item.amount;
        });

        totalProductAmount.value = total.toFixed(2);

        const advance = Number(advancePayment.value || 0);
        const due = Math.max(total - advance, 0);

        duePayment.value = due.toFixed(2);
    }

    function validatePayment() {
        advancePaymentError.textContent = "";

        const total = Number(totalProductAmount.value || 0);
        const advance = Number(advancePayment.value || 0);

        if (advance < 0) {
            advancePaymentError.textContent = "Advance payment cannot be negative.";
            return false;
        }

        if (advance > total) {
            advancePaymentError.textContent =
                "Advance payment cannot be greater than total amount.";
            return false;
        }

        return true;
    }

    function validateDates() {
        if (!orderDate.value) {
            alert("Order date is required.");
            return false;
        }

        if (!deliveryDate.value) {
            alert("Delivery date is required.");
            return false;
        }

        if (deliveryDate.value < orderDate.value) {
            alert("Delivery date cannot be before order date.");
            return false;
        }

        return true;
    }

    function validateBeforeSubmit() {
        if (editingProductIndex !== -1) {
            alert("Please update the product or cancel editing before submitting the order.");
            return false;
        }

        if (orderItems.length === 0) {
            alert("At least one product must be added.");
            return false;
        }

        calculateOrderTotals();

        if (!validatePayment()) return false;
        if (!validateDates()) return false;

        createHiddenFields();
        return true;
    }

    function buildOrderSummary() {
        let html = "";

        orderItems.forEach(function (item) {
            const fabricSourceText =
                item.fabricSource === "SHOP" ? "Shop Fabric" : "Customer Fabric";
            const shopFabricText =
                item.fabricSource === "SHOP" ? escapeHtml(item.inventoryItemName) : "-";
            const fabricUnit = item.fabricUnit || "M";
            const fabricQuantityText =
                item.fabricSource === "SHOP"
                    ? formatAmount(item.fabricQuantity) + " " + escapeHtml(fabricUnit)
                    : "-";
            const fabricAmountText =
                item.fabricSource === "SHOP"
                    ? "Rs. " + formatAmount(item.fabricAmount)
                    : "0.00";
            const notes = item.additionalNotes
                ? escapeHtml(item.additionalNotes)
                : "-";

            html += `
                <tr>
                    <td>${escapeHtml(item.productName)}</td>
                    <td>${escapeHtml(item.silaiType)}<br><small class="text-muted">Rs. ${formatAmount(item.silaiAmount)}</small></td>
                    <td>${fabricSourceText}</td>
                    <td>${shopFabricText}</td>
                    <td>${fabricQuantityText}</td>
                    <td>${fabricAmountText}</td>
                    <td>Rs. ${formatAmount(item.stitchingAmount)}</td>
                    <td>${item.quantity}</td>
                    <td><strong>Rs. ${formatAmount(item.subtotal)}</strong></td>
                    <td>${notes}</td>
                </tr>
            `;
        });

        document.getElementById("orderSummary").innerHTML = html;

        document.getElementById("orderInformation").innerHTML = `
            <div class="row">
                <div class="col-md-6">
                    <p class="mb-0">
                        <strong>Order Date:</strong> ${escapeHtml(orderDate.value)}
                    </p>
                </div>
                <div class="col-md-6">
                    <p class="mb-0">
                        <strong>Delivery Date:</strong> ${escapeHtml(deliveryDate.value)}
                    </p>
                </div>
            </div>
        `;

        document.getElementById("modalTotalAmount").textContent =
            formatAmount(totalProductAmount.value);
        document.getElementById("modalAdvancePayment").textContent =
            formatAmount(advancePayment.value);
        document.getElementById("modalDuePayment").textContent =
            formatAmount(duePayment.value);
    }

    function toggleShopFabric() {
        clearFabricErrors();

        const isShopFabric = fabricSource.value === "SHOP";

        shopFabricContainer.style.display = isShopFabric ? "block" : "none";
        fabricQuantityContainer.style.display = isShopFabric ? "block" : "none";

        if (!isShopFabric) {
            inventoryItemSelect.value = "";
            fabricQuantityInput.value = "";
            fabricStockInfo.textContent = "";
        }
    }

    productSelect.addEventListener("change", function () {
        populateSilaiOptions();
        silaiSelect.value = "";
    });

    fabricSource.addEventListener("change", function () {
        toggleShopFabric();
    });

    inventoryItemSelect.addEventListener("change", function () {
        updateFabricStockInfo();
    });

    fabricQuantityInput.addEventListener("input", function () {
        clearFabricErrors();

        if (inventoryItemSelect.value && Number(fabricQuantityInput.value || 0) > 0) {
            const selectedOption =
                inventoryItemSelect.options[inventoryItemSelect.selectedIndex];
            const availableStock = getAvailableStock(inventoryItemSelect.value);
            const quantity = Number(fabricQuantityInput.value);

            if (quantity > availableStock) {
                fabricQuantityError.textContent =
                    "Fabric quantity cannot exceed available stock of " +
                    formatAmount(availableStock) + " " +
                    (selectedOption.dataset.unit || "");
            }
        }
    });

    advancePayment.addEventListener("input", function () {
        calculateOrderTotals();
        validatePayment();
    });

    addProductBtn.addEventListener("click", function () {
        addOrUpdateProduct();
    });

    cancelEditBtn.addEventListener("click", function () {
        resetProductForm();
    });

    productTableBody.addEventListener("click", function (event) {
        const editButton = event.target.closest(".edit-product-btn");

        if (editButton) {
            editProduct(Number(editButton.dataset.index));
            return;
        }

        const deleteButton = event.target.closest(".delete-product-btn");

        if (deleteButton) {
            deleteProduct(Number(deleteButton.dataset.index));
        }
    });

    updateOrderButton.addEventListener("click", function () {
        if (!validateBeforeSubmit()) return;

        buildOrderSummary();
        orderModal.show();
    });

    confirmOrderButton.addEventListener("click", function () {
        if (!validateBeforeSubmit()) return;

        createHiddenFields();
        confirmOrderButton.disabled = true;
        document.getElementById("orderForm").submit();
    });

    function loadExistingProducts() {
        const existingProducts =
            document.querySelectorAll("#existingOrderProductsData .existing-product");

        if (!existingProducts.length) {
            renderSelectedProducts();
            return;
        }

        existingProducts.forEach(function (element) {
            const productId = Number(element.dataset.productId);
            const productName = element.dataset.productName || "";
            const quantity = Number(element.dataset.quantity || 1);
            const silaiType = (element.dataset.silaiType || "").toLowerCase();
            const silaiAmount = Number(element.dataset.silaiAmount || 0);
            const fabricSourceValue =
                (element.dataset.fabricSource || "CUSTOMER").toUpperCase();
            const inventoryItemId =
                element.dataset.inventoryItemId
                    ? Number(element.dataset.inventoryItemId)
                    : null;
            const fabricQuantity =
                element.dataset.fabricQuantity
                    ? Number(element.dataset.fabricQuantity)
                    : null;
            const notes = element.dataset.additionalNotes || "";

            let inventoryItemName = "";
            let fabricUnit = "";
            let fabricSalePrice = 0;
            let fabricAmount = 0;

            if (fabricSourceValue === "SHOP" && inventoryItemId) {
                const inventoryOption =
                    inventoryItemSelect.querySelector(
                        `option[value="${inventoryItemId}"]`
                    );

                if (inventoryOption) {
                    inventoryItemName =
                        inventoryOption.textContent.split(" - Stock:")[0].trim();
                    fabricUnit = inventoryOption.dataset.unit || "";
                    fabricSalePrice =
                        Number(inventoryOption.dataset.salePrice || 0);
                    fabricAmount =
                        Number(fabricQuantity || 0) * fabricSalePrice;
                }
            }

            const stitchingAmount = silaiAmount * quantity;
            const subtotal = stitchingAmount + fabricAmount;

            orderItems.push({
                productId,
                productName,
                silaiType,
                silaiAmount,
                stitchingAmount,
                quantity,
                amount: subtotal,
                subtotal,
                fabricSource: fabricSourceValue,
                inventoryItemId,
                inventoryItemName,
                fabricQuantity,
                fabricUnit,
                fabricSalePrice,
                fabricAmount,
                additionalNotes: notes
            });
        });

        renderSelectedProducts();
    }

    toggleShopFabric();
    loadExistingProducts();
    calculateOrderTotals();
});