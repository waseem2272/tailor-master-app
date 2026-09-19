document.addEventListener("DOMContentLoaded", function () {
    let orderItems = [];
    let editingProductIndex = -1;

    const productSelect = document.getElementById("productSelect");
    const silaiSelect = document.getElementById("silaiSelect");
    const quantityInput = document.getElementById("quantity");
    const orderProductType = document.getElementById("orderProductType");
    const tailoringProductFields = document.getElementById("tailoringProductFields");
    const inventoryProductFields = document.getElementById("inventoryProductFields");
    const inventoryItemOrderSelect = document.getElementById("inventoryItemOrderSelect");
    const inventoryQuantityInput = document.getElementById("inventoryQuantity");
    const inventoryStockInfo = document.getElementById("inventoryStockInfo");
    const inventoryStockError = document.getElementById("inventoryStockError");
    const inventoryQuantityError = document.getElementById("inventoryQuantityError");
    const fabricSource = document.getElementById("fabricSource");
    const shopFabricContainer = document.getElementById("shopFabricContainer");
    const fabricQuantityContainer = document.getElementById("fabricQuantityContainer");
    const inventoryItemSelect = document.getElementById("inventoryItemSelect");
    const fabricQuantityInput = document.getElementById("fabricQuantity");
    const fabricStockInfo = document.getElementById("fabricStockInfo");
    const fabricStockError = document.getElementById("fabricStockError");
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
    const orderForm = document.getElementById("orderForm");
    const modalElement = document.getElementById("confirmOrderModal");
    const orderModal = new bootstrap.Modal(modalElement);

    const orderProductTypeError = document.getElementById("orderProductTypeError");
    const productError = document.getElementById("productError");
    const silaiError = document.getElementById("silaiError");
    const quantityError = document.getElementById("quantityError");
    const fabricSourceError = document.getElementById("fabricSourceError");
    const fabricQuantityError = document.getElementById("fabricQuantityError");
    const advancePaymentError = document.getElementById("advancePaymentError");

    function formatAmount(value) {
        const amount = Number(value || 0);
        return amount.toLocaleString("en-PK", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
    }

    function escapeHtml(value) {
        if (value === null || value === undefined) {
            return "";
        }

        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function clearErrors() {
        orderProductTypeError.textContent = "";
        productError.textContent = "";
        silaiError.textContent = "";
        quantityError.textContent = "";
        fabricSourceError.textContent = "";
        fabricStockError.textContent = "";
        fabricQuantityError.textContent = "";
        inventoryStockError.textContent = "";
        inventoryQuantityError.textContent = "";
        advancePaymentError.textContent = "";
    }

    function clearFabricErrors() {
        fabricStockError.textContent = "";
        fabricQuantityError.textContent = "";
    }

    function clearInventoryErrors() {
        inventoryStockError.textContent = "";
        inventoryQuantityError.textContent = "";
    }

    function getInventoryItemDisplayName(option) {
        if (!option || !option.value) {
            return "";
        }

        const parts = [
            option.dataset.name,
            option.dataset.brand,
            option.dataset.category,
            option.dataset.color,
            option.dataset.design,
            option.dataset.size
        ].filter(function (value) {
            return value && value.trim();
        });

        return parts.join(" | ");
    }

    function getInventoryUnit(option) {
        return option && option.dataset.unit
            ? option.dataset.unit
            : "";
    }

    function findInventoryOption(inventoryItemId) {
        if (!inventoryItemId) {
            return null;
        }

        const id = String(inventoryItemId);

        return Array.from(inventoryItemSelect.options).find(function (option) {
            return String(option.value) === id;
        }) || Array.from(inventoryItemOrderSelect.options).find(function (option) {
            return String(option.value) === id;
        }) || null;
    }

    function getCurrentOrderInventoryQuantity(inventoryItemId) {
        let total = 0;

        orderItems.forEach(function (item, index) {
            if (index === editingProductIndex) {
                return;
            }

            if (item.orderProductType === "INVENTORY" &&
                String(item.inventoryItemId) === String(inventoryItemId)) {
                total += Number(item.quantity || 0);
            }

            if (item.orderProductType === "TAILORING" &&
                item.fabricSource === "SHOP" &&
                String(item.inventoryItemId) === String(inventoryItemId)) {
                total += Number(item.fabricQuantity || 0);
            }
        });

        return total;
    }

    function getCurrentOrderFabricQuantity(inventoryItemId) {
        let total = 0;

        orderItems.forEach(function (item, index) {
            if (index === editingProductIndex) {
                return;
            }

            if (item.orderProductType === "TAILORING" &&
                item.fabricSource === "SHOP" &&
                String(item.inventoryItemId) === String(inventoryItemId)) {
                total += Number(item.fabricQuantity || 0);
            }
        });

        return total;
    }

    function getAvailableStock(selectElement, inventoryItemId, isFabric) {
        const option = Array.from(selectElement.options).find(function (item) {
            return String(item.value) === String(inventoryItemId);
        });

        if (!option) {
            return 0;
        }

        const physicalStock = Number(option.dataset.stock || 0);
        const reservedStock = Number(option.dataset.reserved || 0);

        const currentOrderQuantity = isFabric
            ? getCurrentOrderFabricQuantity(inventoryItemId)
            : getCurrentOrderInventoryQuantity(inventoryItemId);

        return Math.max(
            0,
            physicalStock - reservedStock - currentOrderQuantity
        );
    }

    function updateInventoryStockInfo() {
        clearInventoryErrors();

        const option =
            inventoryItemOrderSelect.options[
                inventoryItemOrderSelect.selectedIndex
                ];

        if (!option || !option.value) {
            inventoryStockInfo.textContent = "";
            return;
        }

        const physicalStock = Number(option.dataset.stock || 0);
        const reservedStock = Number(option.dataset.reserved || 0);
        const currentOrderQuantity =
            getCurrentOrderInventoryQuantity(option.value);
        const availableStock =
            getAvailableStock(
                inventoryItemOrderSelect,
                option.value,
                false
            );
        const unit = getInventoryUnit(option);

        inventoryStockInfo.textContent =
            "Physical Stock: " + formatAmount(physicalStock) + " " + unit +
            " | Reserved Stock: " + formatAmount(reservedStock) + " " + unit +
            " | Current Order: " + formatAmount(currentOrderQuantity) + " " + unit +
            " | Available Stock: " + formatAmount(availableStock) + " " + unit;
    }

    function updateFabricStockInfo() {
        clearFabricErrors();

        const option =
            inventoryItemSelect.options[
                inventoryItemSelect.selectedIndex
                ];

        if (!option || !option.value) {
            fabricStockInfo.textContent = "";
            return;
        }

        const physicalStock = Number(option.dataset.stock || 0);
        const reservedStock = Number(option.dataset.reserved || 0);
        const currentOrderQuantity =
            getCurrentOrderFabricQuantity(option.value);
        const availableStock =
            getAvailableStock(
                inventoryItemSelect,
                option.value,
                true
            );
        const unit = getInventoryUnit(option);

        fabricStockInfo.textContent =
            "Physical Stock: " + formatAmount(physicalStock) + " " + unit +
            " | Reserved Stock: " + formatAmount(reservedStock) + " " + unit +
            " | Current Order: " + formatAmount(currentOrderQuantity) + " " + unit +
            " | Available Stock: " + formatAmount(availableStock) + " " + unit;
    }

    function populateSilaiOptions(selectedSilaiType) {
        selectedSilaiType = selectedSilaiType || "";

        silaiSelect.innerHTML =
            '<option value="">-- Silai Type --</option>';

        const selectedProduct =
            productSelect.options[productSelect.selectedIndex];

        if (!selectedProduct || !selectedProduct.value) {
            return;
        }

        const singleSilai = selectedProduct.dataset.singleSilai;
        const doubleSilai = selectedProduct.dataset.doubleSilai;

        if (singleSilai !== undefined &&
            singleSilai !== null &&
            singleSilai !== "") {

            const option = document.createElement("option");
            option.value = "single";
            option.textContent =
                "Single Silai - Rs. " + formatAmount(singleSilai);
            option.dataset.amount = singleSilai;
            silaiSelect.appendChild(option);
        }

        if (doubleSilai !== undefined &&
            doubleSilai !== null &&
            doubleSilai !== "") {

            const option = document.createElement("option");
            option.value = "double";
            option.textContent =
                "Double Silai - Rs. " + formatAmount(doubleSilai);
            option.dataset.amount = doubleSilai;
            silaiSelect.appendChild(option);
        }

        if (selectedSilaiType) {
            silaiSelect.value = selectedSilaiType.toLowerCase();
        }
    }

    function calculateSilaiAmount() {
        const selectedOption =
            silaiSelect.options[silaiSelect.selectedIndex];

        if (!selectedOption || !selectedOption.value) {
            return 0;
        }

        return Number(selectedOption.dataset.amount || 0);
    }

    async function fetchInventoryPreview(inventoryItemId, quantity) {
        const basePath =
            typeof contextPath !== "undefined" && contextPath
                ? contextPath
                : "/";

        const separator =
            basePath.endsWith("/") ? "" : "/";

        const url =
            basePath +
            separator +
            "orders/inventory-preview?inventoryItemId=" +
            encodeURIComponent(inventoryItemId) +
            "&quantity=" +
            encodeURIComponent(quantity);

        const response = await fetch(url, {
            method: "GET",
            headers: {
                "Accept": "application/json"
            }
        });

        if (!response.ok) {
            throw new Error("Unable to calculate inventory amount.");
        }

        return response.json();
    }

    async function calculateInventoryAmount(inventoryItemId, quantity) {
        if (!inventoryItemId || quantity <= 0) {
            return 0;
        }

        const result =
            await fetchInventoryPreview(
                inventoryItemId,
                quantity
            );

        if (result === null || result === undefined) {
            throw new Error("Invalid inventory preview.");
        }

        if (typeof result === "number") {
            return Number(result);
        }

        if (result.amount !== undefined &&
            result.amount !== null) {
            return Number(result.amount);
        }

        if (result.totalAmount !== undefined &&
            result.totalAmount !== null) {
            return Number(result.totalAmount);
        }

        throw new Error("Inventory amount was not returned.");
    }

    function validateInventoryProduct() {
        clearInventoryErrors();

        let valid = true;

        const inventoryItemId =
            inventoryItemOrderSelect.value;

        const quantity =
            Number(inventoryQuantityInput.value || 0);

        if (!inventoryItemId) {
            inventoryStockError.textContent =
                "Inventory item is required.";
            valid = false;
        }

        if (!Number.isInteger(quantity) || quantity < 1) {
            inventoryQuantityError.textContent =
                "Quantity must be a whole number of at least 1.";
            valid = false;
        }

        if (inventoryItemId &&
            Number.isInteger(quantity) &&
            quantity > 0) {

            const selectedOption =
                inventoryItemOrderSelect.options[
                    inventoryItemOrderSelect.selectedIndex
                    ];

            const availableStock =
                getAvailableStock(
                    inventoryItemOrderSelect,
                    inventoryItemId,
                    false
                );

            if (quantity > availableStock) {
                inventoryStockError.textContent =
                    "Insufficient stock. Available stock: " +
                    formatAmount(availableStock) +
                    " " +
                    getInventoryUnit(selectedOption);

                valid = false;
            }
        }

        return valid;
    }

    function validateTailoringProduct() {
        clearErrors();

        let valid = true;

        const productId =
            productSelect.value;

        const silaiType =
            silaiSelect.value;

        const quantity =
            Number(quantityInput.value || 0);

        if (!productId) {
            productError.textContent =
                "Product is required.";
            valid = false;
        }

        if (!silaiType) {
            silaiError.textContent =
                "Silai type is required.";
            valid = false;
        }

        if (!Number.isInteger(quantity) || quantity < 1) {
            quantityError.textContent =
                "Quantity must be a whole number of at least 1.";
            valid = false;
        }

        if (!fabricSource.value) {
            fabricSourceError.textContent =
                "Fabric source is required.";
            valid = false;
        }

        if (fabricSource.value === "SHOP") {
            if (!inventoryItemSelect.value) {
                fabricStockError.textContent =
                    "Shop fabric is required.";
                valid = false;
            }

            const fabricQuantity =
                Number(fabricQuantityInput.value || 0);

            if (!fabricQuantity || fabricQuantity <= 0) {
                fabricQuantityError.textContent =
                    "Fabric quantity must be greater than zero.";
                valid = false;
            }

            if (inventoryItemSelect.value &&
                fabricQuantity > 0) {

                const selectedOption =
                    inventoryItemSelect.options[
                        inventoryItemSelect.selectedIndex
                        ];

                const availableStock =
                    getAvailableStock(
                        inventoryItemSelect,
                        inventoryItemSelect.value,
                        true
                    );

                if (fabricQuantity > availableStock) {
                    fabricStockError.textContent =
                        "Insufficient stock. Available stock: " +
                        formatAmount(availableStock) +
                        " " +
                        getInventoryUnit(selectedOption);

                    valid = false;
                }
            }
        }

        return valid;
    }

    function createInventoryProductObject(
        inventoryAmount,
        inventoryUnitPrice,
        existingId
    ) {
        const selectedOption =
            inventoryItemOrderSelect.options[
                inventoryItemOrderSelect.selectedIndex
                ];

        const inventoryItemId =
            Number(inventoryItemOrderSelect.value);

        const quantity =
            Number(inventoryQuantityInput.value);

        const inventoryItemName =
            getInventoryItemDisplayName(selectedOption);

        return {
            id: existingId || null,
            orderProductType: "INVENTORY",
            productId: null,
            productName: inventoryItemName,
            silaiType: "",
            silaiAmount: 0,
            stitchingAmount: 0,
            quantity,
            amount: inventoryAmount,
            subtotal: inventoryAmount,
            fabricSource: "CUSTOMER",
            inventoryItemId,
            inventoryItemName,
            fabricQuantity: null,
            fabricUnit: getInventoryUnit(selectedOption),
            fabricSalePrice: 0,
            fabricAmount: 0,
            inventoryUnitPrice,
            inventoryAmount,
            additionalNotes: additionalNotes.value.trim()
        };
    }

    function createTailoringProductObject(existingId) {
        const selectedProductOption =
            productSelect.options[
                productSelect.selectedIndex
                ];

        const productId =
            Number(productSelect.value);

        const productName =
            selectedProductOption.textContent.trim();

        const quantity =
            Number(quantityInput.value);

        const silaiType =
            silaiSelect.value;

        const silaiAmount =
            calculateSilaiAmount();

        const fabricSourceValue =
            fabricSource.value;

        let inventoryItemId = null;
        let inventoryItemName = "";
        let fabricQuantity = null;
        let fabricUnit = "";
        let fabricSalePrice = 0;
        let fabricAmount = 0;

        if (fabricSourceValue === "SHOP") {
            const selectedInventoryOption =
                inventoryItemSelect.options[
                    inventoryItemSelect.selectedIndex
                    ];

            inventoryItemId =
                Number(inventoryItemSelect.value);

            inventoryItemName =
                getInventoryItemDisplayName(
                    selectedInventoryOption
                );

            fabricQuantity =
                Number(fabricQuantityInput.value);

            fabricUnit =
                selectedInventoryOption.dataset.unit || "";

            fabricSalePrice =
                Number(
                    selectedInventoryOption.dataset.salePrice || 0
                );

            fabricAmount =
                fabricQuantity * fabricSalePrice;
        }

        const stitchingAmount =
            silaiAmount * quantity;

        const subtotal =
            stitchingAmount + fabricAmount;

        return {
            id: existingId || null,
            orderProductType: "TAILORING",
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
            inventoryUnitPrice: 0,
            inventoryAmount: 0,
            additionalNotes: additionalNotes.value.trim()
        };
    }

    async function addOrUpdateProduct() {
        clearErrors();

        const type =
            orderProductType.value;

        if (!type) {
            orderProductTypeError.textContent =
                "Product type is required.";
            return;
        }

        const existingId =
            editingProductIndex !== -1
                ? orderItems[editingProductIndex].id
                : null;

        if (type === "INVENTORY") {
            const valid =
                validateInventoryProduct();

            if (!valid) {
                return;
            }

            const inventoryItemId =
                Number(inventoryItemOrderSelect.value);

            const quantity =
                Number(inventoryQuantityInput.value);

            let inventoryAmount;

            try {
                inventoryAmount =
                    await calculateInventoryAmount(
                        inventoryItemId,
                        quantity
                    );
            } catch (error) {
                inventoryStockError.textContent =
                    "Inventory price cannot be calculated for the selected item. Please check available stock.";
                return;
            }

            const inventoryUnitPrice =
                quantity > 0
                    ? inventoryAmount / quantity
                    : 0;

            const product =
                createInventoryProductObject(
                    inventoryAmount,
                    inventoryUnitPrice,
                    existingId
                );

            if (editingProductIndex === -1) {
                orderItems.push(product);
            } else {
                orderItems[editingProductIndex] =
                    product;
            }

            renderSelectedProducts();
            resetProductForm();
            return;
        }

        const valid =
            validateTailoringProduct();

        if (!valid) {
            return;
        }

        const product =
            createTailoringProductObject(existingId);

        if (editingProductIndex === -1) {
            orderItems.push(product);
        } else {
            orderItems[editingProductIndex] =
                product;
        }

        renderSelectedProducts();
        resetProductForm();
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
            const row =
                document.createElement("tr");

            const isInventory =
                item.orderProductType === "INVENTORY";

            const typeBadge =
                isInventory
                    ? '<span class="badge bg-primary"><i class="bi bi-box-seam me-1"></i>Inventory</span>'
                    : '<span class="badge bg-success"><i class="bi bi-scissors me-1"></i>Tailoring</span>';

            const productText =
                isInventory
                    ? item.inventoryItemName
                    : item.productName;

            const silaiText =
                isInventory
                    ? "-"
                    : escapeHtml(item.silaiType) +
                    '<br><small class="text-muted">Rs. ' +
                    formatAmount(item.silaiAmount) +
                    "</small>";

            const fabricSourceText =
                isInventory
                    ? "-"
                    : item.fabricSource === "SHOP"
                        ? "Shop Fabric"
                        : "Customer Fabric";

            const shopFabricText =
                !isInventory &&
                item.fabricSource === "SHOP"
                    ? escapeHtml(item.inventoryItemName)
                    : "-";

            const fabricUnit =
                item.fabricUnit || "M";

            const fabricQuantityText =
                !isInventory &&
                item.fabricSource === "SHOP"
                    ? formatAmount(item.fabricQuantity) +
                    " " +
                    escapeHtml(fabricUnit)
                    : "-";

            const fabricAmountText =
                !isInventory &&
                item.fabricSource === "SHOP"
                    ? "Rs. " +
                    formatAmount(item.fabricAmount)
                    : "0.00";

            const stitchingAmountText =
                isInventory
                    ? "0.00"
                    : "Rs. " +
                    formatAmount(item.stitchingAmount);

            const notes =
                item.additionalNotes
                    ? escapeHtml(item.additionalNotes)
                    : "-";

            row.innerHTML = `
            <td>${typeBadge}</td> <td> <div class="fw-semibold"> ${escapeHtml(productText)} </div> </td> <td>${silaiText}</td> <td>${fabricSourceText}</td> <td>${shopFabricText}</td> <td>${fabricQuantityText}</td> <td>${fabricAmountText}</td> <td>${stitchingAmountText}</td> <td>${item.quantity}</td> <td> <strong>Rs. ${formatAmount(item.subtotal)}</strong> </td> <td>${notes}</td> <td class="text-nowrap"> <button type="button" class="btn btn-sm btn-outline-primary edit-product-btn" data-index="${index}" title="Edit Product"> <i class="bi bi-pencil-square"></i> </button> <button type="button" class="btn btn-sm btn-outline-danger delete-product-btn" data-index="${index}" title="Delete Product"> <i class="bi bi-trash"></i> </button> </td> `;

            productTableBody.appendChild(row);
        });

        createHiddenFields();
        calculateOrderTotals();
    }

    function editProduct(index) {
        if (index < 0 ||
            index >= orderItems.length) {
            return;
        }

        const item =
            orderItems[index];

        editingProductIndex =
            index;

        orderProductType.value =
            item.orderProductType || "TAILORING";

        toggleProductType();

        if (item.orderProductType === "INVENTORY") {
            inventoryItemOrderSelect.value =
                item.inventoryItemId
                    ? String(item.inventoryItemId)
                    : "";

            inventoryQuantityInput.value =
                item.quantity || 1;

            updateInventoryStockInfo();
        } else {
            productSelect.value =
                item.productId
                    ? String(item.productId)
                    : "";

            populateSilaiOptions(
                item.silaiType
            );

            quantityInput.value =
                item.quantity || 1;

            fabricSource.value =
                item.fabricSource || "CUSTOMER";

            toggleShopFabric();

            if (item.fabricSource === "SHOP") {
                inventoryItemSelect.value =
                    item.inventoryItemId
                        ? String(item.inventoryItemId)
                        : "";

                fabricQuantityInput.value =
                    item.fabricQuantity !== null &&
                    item.fabricQuantity !== undefined
                        ? item.fabricQuantity
                        : "";

                updateFabricStockInfo();
            }
        }

        additionalNotes.value =
            item.additionalNotes || "";

        addProductBtn.innerHTML =
            '<i class="bi bi-check-circle me-1"></i> Update Product';

        cancelEditBtn.classList.remove("d-none");

        productSectionTitle.innerHTML =
            '<i class="bi bi-pencil-square me-2"></i> Edit Product';

        const productCard =
            productSectionTitle.closest(".card");

        if (productCard) {
            productCard.scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
        }
    }

    function deleteProduct(index) {
        if (index < 0 ||
            index >= orderItems.length) {
            return;
        }

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

        orderProductType.value =
            "TAILORING";

        productSelect.value =
            "";

        silaiSelect.innerHTML =
            '<option value="">-- Silai Type --</option>';

        quantityInput.value =
            "1";

        inventoryItemOrderSelect.value =
            "";

        inventoryQuantityInput.value =
            "1";

        inventoryStockInfo.textContent =
            "";

        fabricSource.value =
            "CUSTOMER";

        inventoryItemSelect.value =
            "";

        fabricQuantityInput.value =
            "";

        fabricStockInfo.textContent =
            "";

        additionalNotes.value =
            "";

        toggleProductType();
        toggleShopFabric();

        clearErrors();

        addProductBtn.innerHTML =
            '<i class="bi bi-plus-circle me-1"></i> Add Product';

        cancelEditBtn.classList.add("d-none");

        productSectionTitle.innerHTML =
            '<i class="bi bi-box-seam me-2"></i> Add Product to Order';
    }

    function createHiddenFields() {
        orderProductsInputs.innerHTML = "";

        orderItems.forEach(function (item, index) {
            if (item.id !== null && item.id !== undefined && item.id !== "") {
                addHiddenField(`orderProducts[${index}].id`, item.id);
            }
            addHiddenField(`orderProducts[${index}].orderProductType`, item.orderProductType);
            addHiddenField(`orderProducts[${index}].quantity`, item.quantity);
            addHiddenField(`orderProducts[${index}].amount`, item.amount);
            addHiddenField(`orderProducts[${index}].additionalNotes`, item.additionalNotes || "");
            if (item.orderProductType === "INVENTORY") {
                addHiddenField(`orderProducts[${index}].productId`, "");
                addHiddenField(`orderProducts[${index}].silaiType`, "");
                addHiddenField(`orderProducts[${index}].silaiAmount`, "");
                addHiddenField(`orderProducts[${index}].fabricSource`, "CUSTOMER");
                addHiddenField(`orderProducts[${index}].inventoryItemId`, item.inventoryItemId);
                addHiddenField(`orderProducts[${index}].fabricQuantity`, "");
            } else {
                addHiddenField(`orderProducts[${index}].productId`, item.productId);
                addHiddenField(`orderProducts[${index}].silaiType`, item.silaiType);
                addHiddenField(`orderProducts[${index}].silaiAmount`, item.silaiAmount);
                addHiddenField(`orderProducts[${index}].fabricSource`, item.fabricSource);
                addHiddenField(`orderProducts[${index}].inventoryItemId`, item.fabricSource === "SHOP" ? item.inventoryItemId : "");
                addHiddenField(`orderProducts[${index}].fabricQuantity`, item.fabricSource === "SHOP" ? item.fabricQuantity : "");
            }
        });
    }

    function addHiddenField(name, value) {
        const input =
            document.createElement("input");

        input.type =
            "hidden";

        input.name =
            name;

        input.value =
            value !== null &&
            value !== undefined
                ? value
                : "";

        orderProductsInputs.appendChild(input);
    }

    function calculateOrderTotals() {
        let total = 0;

        orderItems.forEach(function (item) {
            if (item.orderProductType === "INVENTORY") {
                item.amount =
                    Number(item.inventoryAmount || item.amount || 0);

                item.subtotal =
                    item.amount;

                total +=
                    item.amount;

                return;
            }

            const stitchingAmount =
                Number(item.silaiAmount || 0) *
                Number(item.quantity || 0);

            const fabricAmount =
                Number(item.fabricAmount || 0);

            item.stitchingAmount =
                stitchingAmount;

            item.amount =
                stitchingAmount +
                fabricAmount;

            item.subtotal =
                item.amount;

            total +=
                item.amount;
        });

        totalProductAmount.value =
            total.toFixed(2);

        const advance =
            Number(advancePayment.value || 0);

        const due =
            Math.max(
                total - advance,
                0
            );

        duePayment.value =
            due.toFixed(2);
    }

    function validatePayment() {
        advancePaymentError.textContent =
            "";

        const total =
            Number(totalProductAmount.value || 0);

        const advance =
            Number(advancePayment.value || 0);

        if (advance < 0) {
            advancePaymentError.textContent =
                "Advance payment cannot be negative.";

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
            alert(
                "Delivery date cannot be before order date."
            );
            return false;
        }

        return true;
    }

    function validateBeforeSubmit() {
        if (editingProductIndex !== -1) {
            alert(
                "Please update the product or cancel editing before submitting the order."
            );
            return false;
        }

        if (orderItems.length === 0) {
            alert(
                "At least one product must be added."
            );
            return false;
        }

        calculateOrderTotals();

        if (!validatePayment()) {
            return false;
        }

        if (!validateDates()) {
            return false;
        }

        createHiddenFields();

        return true;
    }

    function buildOrderSummary() {
        let html = "";

        orderItems.forEach(function (item) {
            const isInventory =
                item.orderProductType === "INVENTORY";

            const typeText =
                isInventory
                    ? '<span class="badge bg-primary"><i class="bi bi-box-seam me-1"></i>Inventory</span>'
                    : '<span class="badge bg-success"><i class="bi bi-scissors me-1"></i>Tailoring</span>';

            const productText =
                isInventory
                    ? escapeHtml(item.inventoryItemName)
                    : escapeHtml(item.productName);

            const silaiText =
                isInventory
                    ? "-"
                    : escapeHtml(item.silaiType) +
                    '<br><small class="text-muted">Rs. ' +
                    formatAmount(item.silaiAmount) +
                    "</small>";

            const fabricSourceText =
                isInventory
                    ? "-"
                    : item.fabricSource === "SHOP"
                        ? "Shop Fabric"
                        : "Customer Fabric";

            const shopFabricText =
                !isInventory &&
                item.fabricSource === "SHOP"
                    ? escapeHtml(item.inventoryItemName)
                    : "-";

            const fabricUnit =
                item.fabricUnit || "M";

            const fabricQuantityText =
                !isInventory &&
                item.fabricSource === "SHOP"
                    ? formatAmount(item.fabricQuantity) +
                    " " +
                    escapeHtml(fabricUnit)
                    : "-";

            const fabricAmountText =
                !isInventory &&
                item.fabricSource === "SHOP"
                    ? "Rs. " +
                    formatAmount(item.fabricAmount)
                    : "0.00";

            const stitchingAmountText =
                isInventory
                    ? "0.00"
                    : "Rs. " +
                    formatAmount(item.stitchingAmount);

            const notes =
                item.additionalNotes
                    ? escapeHtml(item.additionalNotes)
                    : "-";

            html += `
            <tr> <td>${typeText}</td> <td> <div class="fw-semibold"> ${productText} </div> </td> <td>${silaiText}</td> <td>${fabricSourceText}</td> <td>${shopFabricText}</td> <td>${fabricQuantityText}</td> <td>${fabricAmountText}</td> <td>${stitchingAmountText}</td> <td>${item.quantity}</td> <td> <strong>Rs. ${formatAmount(item.subtotal)}</strong> </td> <td>${notes}</td> </tr> `;
        });

        document.getElementById(
            "orderSummary"
        ).innerHTML = html;

        document.getElementById(
            "orderInformation"
        ).innerHTML = `

<div class="row"> <div class="col-md-6"> <p class="mb-0"> <strong>Order Date:</strong> ${escapeHtml(orderDate.value)} </p> </div> <div class="col-md-6"> <p class="mb-0"> <strong>Delivery Date:</strong> ${escapeHtml(deliveryDate.value)} </p> </div> </div> `;

        document.getElementById(
            "modalTotalAmount"
        ).textContent =
            formatAmount(totalProductAmount.value);

        document.getElementById(
            "modalAdvancePayment"
        ).textContent =
            formatAmount(advancePayment.value);

        document.getElementById(
            "modalDuePayment"
        ).textContent =
            formatAmount(duePayment.value);
    }

    function toggleShopFabric() {
        clearFabricErrors();

        const isShopFabric =
            fabricSource.value === "SHOP";

        shopFabricContainer.style.display =
            isShopFabric
                ? "block"
                : "none";

        fabricQuantityContainer.style.display =
            isShopFabric
                ? "block"
                : "none";

        if (!isShopFabric) {
            inventoryItemSelect.value =
                "";

            fabricQuantityInput.value =
                "";

            fabricStockInfo.textContent =
                "";
        }
    }

    function toggleProductType() {
        clearErrors();

        const type =
            orderProductType.value;

        const isInventory =
            type === "INVENTORY";

        tailoringProductFields.style.display =
            isInventory
                ? "none"
                : "block";

        inventoryProductFields.style.display =
            isInventory
                ? "block"
                : "none";

        if (isInventory) {
            fabricSource.value =
                "CUSTOMER";

            toggleShopFabric();
        } else {
            inventoryItemOrderSelect.value =
                "";

            inventoryQuantityInput.value =
                "1";

            inventoryStockInfo.textContent =
                "";

            clearInventoryErrors();
        }
    }

    function loadExistingProducts() {
        const existingProducts =
            document.querySelectorAll(
                "#existingOrderProductsData .existing-product"
            );

        if (!existingProducts.length) {
            renderSelectedProducts();
            return;
        }

        existingProducts.forEach(function (element) {
            const id =
                element.dataset.id
                    ? Number(element.dataset.id)
                    : null;

            const orderProductType =
                (
                    element.dataset.orderProductType ||
                    "TAILORING"
                ).toUpperCase();

            const productId =
                element.dataset.productId
                    ? Number(element.dataset.productId)
                    : null;

            const productName =
                element.dataset.productName || "";

            const quantity =
                Number(element.dataset.quantity || 1);

            const storedAmount =
                element.dataset.amount !== undefined &&
                element.dataset.amount !== ""
                    ? Number(element.dataset.amount)
                    : null;

            const silaiType =
                (
                    element.dataset.silaiType ||
                    ""
                ).toLowerCase();

            const silaiAmount =
                Number(
                    element.dataset.silaiAmount || 0
                );

            const fabricSourceValue =
                (
                    element.dataset.fabricSource ||
                    "CUSTOMER"
                ).toUpperCase();

            const inventoryItemId =
                element.dataset.inventoryItemId
                    ? Number(element.dataset.inventoryItemId)
                    : null;

            const fabricQuantity =
                element.dataset.fabricQuantity !== undefined &&
                element.dataset.fabricQuantity !== ""
                    ? Number(element.dataset.fabricQuantity)
                    : null;

            const notes =
                element.dataset.additionalNotes || "";

            const inventoryOption =
                findInventoryOption(
                    inventoryItemId
                );

            const inventoryItemName =
                inventoryOption
                    ? getInventoryItemDisplayName(
                        inventoryOption
                    )
                    : "";

            const fabricUnit =
                inventoryOption
                    ? getInventoryUnit(
                        inventoryOption
                    )
                    : "";

            let stitchingAmount =
                0;

            let fabricAmount =
                0;

            if (orderProductType === "TAILORING") {
                stitchingAmount =
                    silaiAmount * quantity;

                if (fabricSourceValue === "SHOP") {
                    if (storedAmount !== null) {
                        fabricAmount =
                            Math.max(
                                0,
                                storedAmount - stitchingAmount
                            );
                    } else if (inventoryOption &&
                        fabricQuantity !== null) {
                        fabricAmount =
                            fabricQuantity *
                            Number(
                                inventoryOption.dataset.salePrice || 0
                            );
                    }
                }

                const subtotal =
                    storedAmount !== null
                        ? storedAmount
                        : stitchingAmount + fabricAmount;

                const fabricSalePrice =
                    fabricQuantity &&
                    fabricQuantity > 0
                        ? fabricAmount / fabricQuantity
                        : 0;

                orderItems.push({
                    id,
                    orderProductType: "TAILORING",
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
                    inventoryUnitPrice: 0,
                    inventoryAmount: 0,
                    additionalNotes: notes
                });

                return;
            }

            const inventoryAmount =
                storedAmount !== null
                    ? storedAmount
                    : quantity *
                    Number(
                        inventoryOption
                            ? inventoryOption.dataset.salePrice || 0
                            : 0
                    );

            const inventoryUnitPrice =
                quantity > 0
                    ? inventoryAmount / quantity
                    : 0;

            orderItems.push({
                id,
                orderProductType: "INVENTORY",
                productId: null,
                productName: inventoryItemName,
                silaiType: "",
                silaiAmount: 0,
                stitchingAmount: 0,
                quantity,
                amount: inventoryAmount,
                subtotal: inventoryAmount,
                fabricSource: "CUSTOMER",
                inventoryItemId,
                inventoryItemName,
                fabricQuantity: null,
                fabricUnit,
                fabricSalePrice: 0,
                fabricAmount: 0,
                inventoryUnitPrice,
                inventoryAmount,
                additionalNotes: notes
            });
        });

        renderSelectedProducts();
    }

    productSelect.addEventListener(
        "change",
        function () {
            populateSilaiOptions();
            silaiSelect.value = "";
        }
    );

    orderProductType.addEventListener(
        "change",
        function () {
            toggleProductType();
        }
    );

    fabricSource.addEventListener(
        "change",
        function () {
            toggleShopFabric();
        }
    );

    inventoryItemSelect.addEventListener(
        "change",
        function () {
            updateFabricStockInfo();
        }
    );

    inventoryItemOrderSelect.addEventListener(
        "change",
        function () {
            updateInventoryStockInfo();
        }
    );

    fabricQuantityInput.addEventListener(
        "input",
        function () {
            clearFabricErrors();

            if (!inventoryItemSelect.value) {
                return;
            }

            const quantity =
                Number(
                    fabricQuantityInput.value || 0
                );

            if (quantity <= 0) {
                return;
            }

            const selectedOption =
                inventoryItemSelect.options[
                    inventoryItemSelect.selectedIndex
                    ];

            const availableStock =
                getAvailableStock(
                    inventoryItemSelect,
                    inventoryItemSelect.value,
                    true
                );

            if (quantity > availableStock) {
                fabricQuantityError.textContent =
                    "Fabric quantity cannot exceed available stock of " +
                    formatAmount(availableStock) +
                    " " +
                    getInventoryUnit(selectedOption);
            }
        }
    );

    inventoryQuantityInput.addEventListener(
        "input",
        function () {
            clearInventoryErrors();

            const inventoryItemId =
                inventoryItemOrderSelect.value;

            const quantity =
                Number(
                    inventoryQuantityInput.value || 0
                );

            if (!inventoryItemId ||
                quantity <= 0) {
                return;
            }

            const selectedOption =
                inventoryItemOrderSelect.options[
                    inventoryItemOrderSelect.selectedIndex
                    ];

            const availableStock =
                getAvailableStock(
                    inventoryItemOrderSelect,
                    inventoryItemId,
                    false
                );

            if (quantity > availableStock) {
                inventoryStockError.textContent =
                    "Quantity cannot exceed available stock of " +
                    formatAmount(availableStock) +
                    " " +
                    getInventoryUnit(selectedOption);
            }
        }
    );

    advancePayment.addEventListener(
        "input",
        function () {
            calculateOrderTotals();
            validatePayment();
        }
    );

    addProductBtn.addEventListener(
        "click",
        async function () {
            await addOrUpdateProduct();
        }
    );

    cancelEditBtn.addEventListener(
        "click",
        function () {
            resetProductForm();
        }
    );

    productTableBody.addEventListener(
        "click",
        function (event) {
            const editButton =
                event.target.closest(
                    ".edit-product-btn"
                );

            if (editButton) {
                editProduct(
                    Number(
                        editButton.dataset.index
                    )
                );
                return;
            }

            const deleteButton =
                event.target.closest(
                    ".delete-product-btn"
                );

            if (deleteButton) {
                deleteProduct(
                    Number(
                        deleteButton.dataset.index
                    )
                );
            }
        }
    );

    updateOrderButton.addEventListener(
        "click",
        function () {
            if (!validateBeforeSubmit()) {
                return;
            }

            buildOrderSummary();
            orderModal.show();
        }
    );

    confirmOrderButton.addEventListener(
        "click",
        function () {
            if (!validateBeforeSubmit()) {
                return;
            }

            createHiddenFields();

            confirmOrderButton.disabled =
                true;

            orderForm.submit();
        }
    );

    toggleProductType();
    toggleShopFabric();
    loadExistingProducts();
    calculateOrderTotals();

});