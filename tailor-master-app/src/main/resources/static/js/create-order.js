let orderItems = [];

document.addEventListener("DOMContentLoaded", function () {
    const orderDate = document.getElementById("orderDate");
    const deliveryDate = document.getElementById("deliveryDate");

    const today = new Date().toISOString().split("T")[0];

    orderDate.min = today;
    orderDate.max = today;

    deliveryDate.min = today;

    setupFabricControls();
    setupOrderProductType();
    setupInventoryStockValidation();
});

function showAddProductAlert(message, type = "danger") {
    const alertBox = document.getElementById("addProductAlert");

    if (!alertBox) return;

    alertBox.className = `alert alert-${type} mt-3`;
    alertBox.textContent = message;
    alertBox.classList.remove("d-none");

    setTimeout(() => {
        alertBox.classList.add("d-none");
        alertBox.textContent = "";
    }, 5000);
}

function hideAddProductAlert() {
    const alertBox = document.getElementById("addProductAlert");

    if (!alertBox) return;

    alertBox.classList.add("d-none");
    alertBox.textContent = "";
}

function setupFabricControls() {
    const fabricSource = document.getElementById("fabricSource");
    const shopFabricContainer = document.getElementById("shopFabricContainer");
    const fabricQuantityContainer = document.getElementById("fabricQuantityContainer");
    const inventoryItemSelect = document.getElementById("inventoryItemSelect");
    const fabricQuantity = document.getElementById("fabricQuantity");
    const fabricStockInfo = document.getElementById("fabricStockInfo");
    const fabricStockError = document.getElementById("fabricStockError");

    fabricSource.addEventListener("change", function () {
        const isShopFabric = this.value === "SHOP";

        shopFabricContainer.style.display = isShopFabric ? "block" : "none";
        fabricQuantityContainer.style.display = isShopFabric ? "block" : "none";

        inventoryItemSelect.value = "";
        fabricQuantity.value = "";
        fabricStockInfo.style.display = "none";
        fabricStockError.style.display = "none";
    });

    inventoryItemSelect.addEventListener("change", function () {

        const selectedOption =
            this.options[this.selectedIndex];

        if (!this.value) {
            fabricStockInfo.style.display = "none";
            fabricStockError.style.display = "none";
            return;
        }


        const physicalStock =
            parseFloat(selectedOption.dataset.stock) || 0;

        const reservedStock =
            parseFloat(selectedOption.dataset.reserved) || 0;

        const unit =
            selectedOption.dataset.unit || "METER";


        /*
         * Quantity of the same inventory item
         * already added in the current order.
         */
        const currentOrderQuantity =
            getCurrentOrderInventoryQuantity(this.value);


        /*
         * Real remaining available stock.
         *
         * Physical stock
         * - Existing pending reservations
         * - Current order usage
         */
        let availableStock =
            physicalStock -
            reservedStock -
            currentOrderQuantity;


        if (availableStock < 0) {
            availableStock = 0;
        }


        fabricStockInfo.innerHTML =
            "Physical Stock: " +
            physicalStock.toFixed(2) + " " + unit +

            " &nbsp; | &nbsp; " +

            "Reserved Stock: " +
            reservedStock.toFixed(2) + " " + unit +

            " &nbsp; | &nbsp; " +

            "Current Order: " +
            currentOrderQuantity.toFixed(2) + " " + unit +

            " &nbsp; | &nbsp; " +

            "Available Stock: " +
            availableStock.toFixed(2) + " " + unit;


        fabricStockInfo.style.display = "block";

        fabricQuantity.max = availableStock;

        fabricStockError.style.display = "none";
    });

    fabricQuantity.addEventListener("input", function () {

        const selectedOption =
            inventoryItemSelect.options[
                inventoryItemSelect.selectedIndex
                ];


        if (!selectedOption || !inventoryItemSelect.value) {
            return;
        }


        const physicalStock =
            parseFloat(selectedOption.dataset.stock) || 0;

        const reservedStock =
            parseFloat(selectedOption.dataset.reserved) || 0;


        /*
         * Already used by this current order.
         */
        const currentOrderQuantity =
            getCurrentOrderInventoryQuantity(
                inventoryItemSelect.value
            );


        /*
         * Remaining stock for this order.
         */
        const availableStock =
            Math.max(
                0,
                physicalStock -
                reservedStock -
                currentOrderQuantity
            );


        const quantity =
            parseFloat(this.value) || 0;


        if (quantity > availableStock) {

            fabricStockError.innerText =
                "Fabric quantity cannot be greater than available stock (" +
                availableStock.toFixed(2) +
                ").";

            fabricStockError.style.display = "block";

        } else {

            fabricStockError.innerText = "";

            fabricStockError.style.display = "none";
        }
    });
}

function setupOrderProductType() {
    const orderProductType =
        document.getElementById("orderProductType");

    const productSelect =
        document.getElementById("productSelect");

    const silaiSelect =
        document.getElementById("silaiSelect");

    const quantityInput =
        document.getElementById("quantity");

    const inventoryItemContainer =
        document.getElementById("inventoryItemContainer");

    const tailoringFabricFields =
        document.getElementById("tailoringFabricFields");

    const inventoryItemOrderSelect =
        document.getElementById("inventoryItemOrderSelect");

    const inventoryQuantityInput =
        document.getElementById("inventoryQuantity");

    const alterationRequired =
        document.getElementById("alterationRequired");

    const alterationFeeContainer =
        document.getElementById("alterationFeeContainer");

    const alterationFee =
        document.getElementById("alterationFee");

    const alterationFeeError =
        document.getElementById("alterationFeeError");

    const additionalNotesContainer =
        document.getElementById("additionalNotesContainer");

    const additionalNotesInput =
        document.getElementById("additionalNotes");

    alterationRequired.addEventListener("change", function () {
        const checked = this.checked;

        alterationFeeContainer.style.display =
            checked ? "block" : "none";

        alterationFeeError.style.display = "none";
        alterationFeeError.textContent = "";

        if (!checked) {
            alterationFee.value = "";
        }
    });

    function updateFields() {

        inventoryItemOrderSelect.value = "";
        inventoryQuantityInput.value = "";

        document.getElementById("inventoryStockInfo").style.display = "none";
        document.getElementById("inventoryStockError").style.display = "none";
        document.getElementById("inventoryStockInfo").textContent = "";
        document.getElementById("inventoryStockError").textContent = "";

        const isInventory =
            orderProductType.value === "INVENTORY";

        additionalNotesContainer.style.display =
            isInventory ? "" : "none";

        if (!isInventory) {
            additionalNotesInput.value = "";
        }

        productSelect.closest(".col-md-4").style.display =
            isInventory ? "none" : "";

        silaiSelect.closest(".col-md-3").style.display =
            isInventory ? "none" : "";

        quantityInput.closest(".col-md-2").style.display =
            isInventory ? "none" : "";

        tailoringFabricFields.style.display =
            isInventory ? "none" : "";

        inventoryItemContainer.style.display =
            isInventory ? "flex" : "none";

        if (!isInventory) {
            inventoryItemOrderSelect.value = "";
            inventoryQuantityInput.value = "";

            alterationRequired.checked = false;
            alterationFee.value = "";
            alterationFeeContainer.style.display = "none";
            alterationFeeError.style.display = "none";
        }
    }

    orderProductType.addEventListener(
        "change",
        updateFields
    );

    updateFields();
}

document.getElementById("productSelect").addEventListener("change", function () {
    const selectedId = this.value;
    const silaiSelect = document.getElementById("silaiSelect");

    silaiSelect.innerHTML =
        '<option value="" disabled selected>-- Silai Type --</option>';

    const product =
        productSilaiData.find(p => p.id == selectedId);

    if (product) {
        const opt1 = document.createElement("option");
        opt1.value = "single";
        opt1.text = "Single Silai - " +
            formatAmount(product.singleSilai);

        silaiSelect.appendChild(opt1);

        const opt2 = document.createElement("option");
        opt2.value = "double";
        opt2.text = "Double Silai - " +
            formatAmount(product.doubleSilai);

        silaiSelect.appendChild(opt2);
    }
});

function formatAmount(amount) {
    const value = parseFloat(amount);
    return isNaN(value) ? "0.00" : value.toFixed(2);
}

function getCurrentOrderInventoryQuantity(inventoryItemId) {
    let totalQuantity = 0;

    orderItems.forEach(function (item) {
        if (
            String(item.inventoryItemId) === String(inventoryItemId)
        ) {
            if (item.orderProductType === "INVENTORY") {
                totalQuantity += Number(item.qty || 0);
            } else if (
                item.orderProductType === "TAILORING" &&
                item.fabricSource === "SHOP"
            ) {
                totalQuantity += Number(item.fabricQuantity || 0);
            }
        }
    });

    return totalQuantity;
}

document.getElementById("addProductBtn").addEventListener("click", function () {

    hideAddProductAlert();

    const orderProductType = document.getElementById("orderProductType").value;
    const productSelect = document.getElementById("productSelect");
    const silaiSelect = document.getElementById("silaiSelect");
    const quantityInput = document.getElementById("quantity");
    const inventoryQuantityInput = document.getElementById("inventoryQuantity");
    const additionalNotesInput = document.getElementById("additionalNotes");
    const fabricSource = document.getElementById("fabricSource");
    const inventoryItemSelect = document.getElementById("inventoryItemSelect");
    const fabricQuantityInput = document.getElementById("fabricQuantity");
    const inventoryItemOrderSelect = document.getElementById("inventoryItemOrderSelect");
    const additionalNotes = additionalNotesInput.value.trim();

    if (orderProductType === "INVENTORY") {
        if (!inventoryItemOrderSelect.value) {
            showAddProductAlert("Please select an inventory item.");
            return;
        }

        const qty = parseFloat(inventoryQuantityInput.value);

        const alterationRequired =
            document.getElementById("alterationRequired").checked;

        const alterationFeeInput =
            document.getElementById("alterationFee");

        const alterationFeeError =
            document.getElementById("alterationFeeError");

        let alterationFee = 0;

        if (alterationRequired) {
            alterationFee =
                parseFloat(alterationFeeInput.value);

            if (!alterationFee || alterationFee <= 0) {
                alterationFeeError.textContent =
                    "Please enter a valid alteration fee.";

                alterationFeeError.style.display = "block";
                alterationFeeInput.focus();
                return;
            }

            alterationFeeError.textContent = "";
            alterationFeeError.style.display = "none";
        } else {
            alterationFee = 0;
        }

        if (!qty || qty <= 0) {
            showAddProductAlert("Please enter a valid inventory quantity.");
            return;
        }

        const selectedOption =
            inventoryItemOrderSelect.options[
                inventoryItemOrderSelect.selectedIndex
                ];

        const inventoryItemId = inventoryItemOrderSelect.value;
        const inventoryItemName =
            getInventoryItemDisplayName(selectedOption);

        const physicalStock =
            parseFloat(selectedOption.dataset.stock) || 0;

        const reservedStock =
            parseFloat(selectedOption.dataset.reserved) || 0;

        const currentOrderQuantity =
            getCurrentOrderInventoryQuantity(inventoryItemId);

        const availableStock =
            Math.max(
                0,
                physicalStock -
                reservedStock -
                currentOrderQuantity
            );

        if (qty > availableStock) {
            document.getElementById("inventoryStockError").textContent =
                "Quantity cannot be greater than available stock. " +
                "Available: " +
                availableStock.toFixed(2) +
                " " +
                (selectedOption.dataset.unit || "");

            document.getElementById("inventoryStockError").style.display =
                "block";

            return;
        }

        fetch(
            contextPath +
            `orders/inventory-preview?inventoryItemId=${encodeURIComponent(inventoryItemId)}&quantity=${encodeURIComponent(qty)}`
        )
            .then(async response => {
                const responseText = await response.text();

                console.log(
                    "Inventory preview status:",
                    response.status
                );

                console.log(
                    "Inventory preview response:",
                    responseText
                );

                if (!response.ok) {
                    throw new Error(
                        "HTTP " +
                        response.status +
                        ": " +
                        responseText
                    );
                }

                return JSON.parse(responseText);
            })
            .then(data => {
                console.log(
                    "Inventory preview data:",
                    data
                );

                if (data.error) {
                    const inventoryStockError =
                        document.getElementById("inventoryStockError");

                    inventoryStockError.textContent =
                        data.message || "Inventory price cannot be calculated for the selected Item. Please check available stock.";

                    inventoryStockError.style.display = "block";
                    return;
                }

                const inventoryAmount =
                    parseFloat(data.totalAmount) || 0;

                const itemAmount =
                    inventoryAmount + alterationFee;

                const item = {
                    orderProductType: "INVENTORY",
                    productId: null,
                    productName: inventoryItemName,
                    silaiType: null,
                    silai: "-",
                    silaiAmount: 0,
                    stitchingAmount: 0,
                    qty: qty,
                    amount: itemAmount,
                    fabricSource: "CUSTOMER",
                    inventoryItemId: inventoryItemId,
                    inventoryItemName: inventoryItemName,
                    fabricQuantity: null,
                    fabricSalePrice:
                        parseFloat(data.averageUnitPrice) || 0,
                    fabricAmount: 0,
                    inventoryUnitPrice:
                        parseFloat(data.averageUnitPrice) || 0,
                    inventoryAmount: inventoryAmount,
                    alterationRequired: alterationRequired,
                    alterationFee: alterationFee,
                    orderProductStatus:
                        alterationRequired
                            ? "PENDING_FOR_ALTERATION"
                            : "INSTANT_DELIVERED",
                    additionalNotes: additionalNotes
                };

                orderItems.push(item);
                renderTable();
                updateOrderDetails();

                inventoryItemOrderSelect.selectedIndex = 0;
                inventoryQuantityInput.value = "";
                additionalNotesInput.value = "";

                document.getElementById("alterationRequired").checked = false;
                document.getElementById("alterationFee").value = "";
                document.getElementById("alterationFeeContainer").style.display = "none";
                document.getElementById("alterationFeeError").style.display = "none";

                const inventoryStockInfo =
                    document.getElementById("inventoryStockInfo");

                const inventoryStockError =
                    document.getElementById("inventoryStockError");

                inventoryStockInfo.style.display = "none";
                inventoryStockInfo.innerText = "";

                inventoryStockError.style.display = "none";
                inventoryStockError.innerText = "";
            })
            .catch(error => {
                console.error("Inventory preview error:", error);

                const inventoryStockError =
                    document.getElementById("inventoryStockError");

                inventoryStockError.textContent =
                    "Inventory price cannot be calculated for the selected Item. Please check available stock.";

                inventoryStockError.style.display = "block";
            });

        return;
    }

    const qty = parseInt(quantityInput.value);

    if (!productSelect.value ||
        !silaiSelect.value ||
        !qty ||
        qty < 1) {
        showAddProductAlert(
            "Please select product, silai type and quantity."
        );
        return;
    }

    const selectedFabricSource = fabricSource.value;

    let inventoryItemId = null;
    let inventoryItemName = "-";
    let fabricQuantity = null;
    let fabricSalePrice = 0;
    let fabricAmount = 0;

    if (selectedFabricSource === "SHOP") {
        if (!inventoryItemSelect.value) {
            showAddProductAlert("Please select shop fabric.");
            return;
        }

        fabricQuantity =
            parseFloat(fabricQuantityInput.value);

        if (!fabricQuantity || fabricQuantity <= 0) {
            showAddProductAlert("Please enter fabric quantity.");
            return;
        }

        const selectedOption =
            inventoryItemSelect.options[
                inventoryItemSelect.selectedIndex
                ];

        const physicalStock =
            parseFloat(selectedOption.dataset.stock) || 0;

        const reservedStock =
            parseFloat(selectedOption.dataset.reserved) || 0;

        const currentOrderQuantity =
            getCurrentOrderInventoryQuantity(
                inventoryItemSelect.value
            );

        const availableStock =
            Math.max(
                0,
                physicalStock -
                reservedStock -
                currentOrderQuantity
            );

        if (fabricQuantity > availableStock) {
            showAddProductAlert(
                "Fabric quantity cannot be greater than available stock. " +
                "Available: " +
                availableStock.toFixed(2) +
                " M"
            );
            return;
        }

        inventoryItemId =
            inventoryItemSelect.value;

        inventoryItemName =
            getInventoryItemDisplayName(selectedOption);

        fetch(
            contextPath +
            `orders/inventory-preview?inventoryItemId=${encodeURIComponent(inventoryItemId)}&quantity=${encodeURIComponent(fabricQuantity)}`
        )
            .then(async response => {
                const responseText =
                    await response.text();

                console.log(
                    "Shop fabric preview status:",
                    response.status
                );

                console.log(
                    "Shop fabric preview response:",
                    responseText
                );

                if (!response.ok) {
                    throw new Error(
                        "HTTP " +
                        response.status +
                        ": " +
                        responseText
                    );
                }

                return JSON.parse(responseText);
            })
            .then(data => {
                console.log(
                    "Shop fabric preview data:",
                    data
                );

                if (data.error) {
                    const fabricStockError =
                        document.getElementById("fabricStockError");

                    fabricStockError.textContent =
                        data.message || "Shop fabric price cannot be calculated for the selected fabric. Please check available stock.";

                    fabricStockError.style.display = "block";
                    return;
                }

                fabricAmount =
                    parseFloat(data.totalAmount) || 0;

                fabricSalePrice =
                    parseFloat(data.averageUnitPrice) || 0;

                const product =
                    productSilaiData.find(
                        p =>
                            p.id ==
                            productSelect.value
                    );

                if (!product) {
                    showAddProductAlert(
                        "Product information not found."
                    );
                    return;
                }

                const silaiType =
                    silaiSelect.value;

                const silaiAmount =
                    silaiType === "single"
                        ? parseFloat(
                        product.singleSilai
                    ) || 0
                        : parseFloat(
                        product.doubleSilai
                    ) || 0;

                const stitchingAmount =
                    silaiAmount * qty;

                const itemAmount =
                    stitchingAmount +
                    fabricAmount;

                const item = {
                    orderProductType: "TAILORING",
                    productId:
                    productSelect.value,
                    productName:
                    productSelect.options[
                        productSelect.selectedIndex
                        ].text,
                    silaiType: silaiType,
                    silai:
                    silaiSelect.options[
                        silaiSelect.selectedIndex
                        ].text,
                    silaiAmount: silaiAmount,
                    stitchingAmount:
                    stitchingAmount,
                    qty: qty,
                    amount: itemAmount,
                    fabricSource:
                    selectedFabricSource,
                    inventoryItemId:
                    inventoryItemId,
                    inventoryItemName:
                    inventoryItemName,
                    fabricQuantity:
                    fabricQuantity,
                    fabricSalePrice:
                    fabricSalePrice,
                    fabricAmount:
                    fabricAmount,
                    additionalNotes: ""
                };

                orderItems.push(item);
                renderTable();
                updateOrderDetails();

                productSelect.selectedIndex = 0;

                silaiSelect.innerHTML =
                    '<option value="" disabled selected>' +
                    '-- Silai Type --' +
                    '</option>';

                quantityInput.value = "";
                additionalNotesInput.value = "";
                fabricSource.value = "CUSTOMER";
                inventoryItemSelect.value = "";
                fabricQuantityInput.value = "";

                document.getElementById(
                    "shopFabricContainer"
                ).style.display = "none";

                document.getElementById(
                    "fabricQuantityContainer"
                ).style.display = "none";

                document.getElementById(
                    "fabricStockInfo"
                ).style.display = "none";

                document.getElementById(
                    "fabricStockError"
                ).style.display = "none";
            })
            .catch(error => {
                console.error("Shop fabric preview error:", error);

                const fabricStockError =
                    document.getElementById("fabricStockError");

                fabricStockError.textContent =
                    "Shop fabric price cannot be calculated for the selected fabric. Please check available stock.";

                fabricStockError.style.display = "block";
            });

        return;
    }

    const product =
        productSilaiData.find(
            p => p.id == productSelect.value
        );

    if (!product) {
        showAddProductAlert("Product information not found.");
        return;
    }

    const silaiType = silaiSelect.value;

    const silaiAmount =
        silaiType === "single"
            ? parseFloat(product.singleSilai) || 0
            : parseFloat(product.doubleSilai) || 0;

    const stitchingAmount =
        silaiAmount * qty;

    const itemAmount =
        stitchingAmount + fabricAmount;

    const item = {
        orderProductType: "TAILORING",
        productId: productSelect.value,
        productName:
        productSelect.options[
            productSelect.selectedIndex
            ].text,
        silaiType: silaiType,
        silai:
        silaiSelect.options[
            silaiSelect.selectedIndex
            ].text,
        silaiAmount: silaiAmount,
        stitchingAmount: stitchingAmount,
        qty: qty,
        amount: itemAmount,
        fabricSource: selectedFabricSource,
        inventoryItemId: inventoryItemId,
        inventoryItemName: inventoryItemName,
        fabricQuantity: fabricQuantity,
        fabricSalePrice: fabricSalePrice,
        fabricAmount: fabricAmount,
        additionalNotes: ""
    };

    orderItems.push(item);
    renderTable();
    updateOrderDetails();

    productSelect.selectedIndex = 0;

    silaiSelect.innerHTML =
        '<option value="" disabled selected>' +
        '-- Silai Type --' +
        '</option>';

    quantityInput.value = "";
    additionalNotesInput.value = "";
    fabricSource.value = "CUSTOMER";
    inventoryItemSelect.value = "";
    fabricQuantityInput.value = "";

    document.getElementById(
        "shopFabricContainer"
    ).style.display = "none";

    document.getElementById(
        "fabricQuantityContainer"
    ).style.display = "none";

    document.getElementById(
        "fabricStockInfo"
    ).style.display = "none";

    document.getElementById(
        "fabricStockError"
    ).style.display = "none";
});

function renderTable() {
    const tbody = document.querySelector("#productTable tbody");
    tbody.innerHTML = "";

    orderItems.forEach((item, index) => {
        const isInventory = item.orderProductType === "INVENTORY";

        const fabricSourceText = isInventory
            ? "-"
            : item.fabricSource === "SHOP"
                ? "Shop Fabric"
                : "Customer Fabric";

        const shopFabricText = !isInventory && item.fabricSource === "SHOP"
            ? item.inventoryItemName
            : "-";

        const fabricQuantityText = !isInventory && item.fabricSource === "SHOP"
            ? item.fabricQuantity.toFixed(2) + " M"
            : "-";

        const fabricAmountText = !isInventory && item.fabricSource === "SHOP"
            ? item.fabricAmount.toFixed(2)
            : "0.00";

        const silaiAmountText = isInventory
            ? "0.00"
            : item.silaiAmount.toFixed(2);

        const unitPriceText = isInventory
            ? (item.inventoryUnitPrice || 0).toFixed(2)
            : "-";

        const subtotalText = item.amount.toFixed(2);

        tbody.innerHTML += `
            <tr>
                <td>
                    <span class="badge ${isInventory ? "bg-primary" : "bg-success"}">
                        ${isInventory ? "Inventory" : "Tailoring"}
                    </span>
                    <div class="mt-1">${item.productName}</div>
                </td>

                <td>${isInventory ? "-" : item.silai}</td>

                <td>${fabricSourceText}</td>

                <td>${shopFabricText}</td>

                <td>${fabricQuantityText}</td>

                <td>${fabricAmountText}</td>

                <td>${silaiAmountText}</td>

                <td>${unitPriceText}</td>

                <td>${item.qty}</td>

                <td>${subtotalText}</td>

                <td>
                    ${item.additionalNotes
            ? item.additionalNotes
            : "-"}
                </td>
                
                <td>
    ${isInventory
            ? item.alterationRequired
                ? "Yes - Rs. " + item.alterationFee.toFixed(2)
                : "No"
            : "-"}
</td>

                <td>
                    <button type="button"
                            class="btn btn-sm btn-danger"
                            onclick="deleteItem(${index})">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });

    refreshInventoryStockInfo();
}

function deleteItem(index) {
    orderItems.splice(index, 1);
    updateOrderDetails();
    renderTable();
}

function updateOrderDetails() {
    let totalAmount = 0;

    orderItems.forEach(item => {
        totalAmount += item.amount;
    });

    const totalInput =
        document.getElementById("totalProductAmount");

    const advanceInput =
        document.getElementById("advancePayment");

    const dueInput =
        document.getElementById("duePayment");

    const advanceError =
        document.getElementById("advancePaymentError");

    totalInput.value =
        totalAmount.toFixed(2);

    let advancePayment =
        parseFloat(advanceInput.value) || 0;

    if (advancePayment < 0) {
        advanceError.innerText =
            "Advance payment cannot be negative.";

        advanceError.style.display = "block";
        dueInput.value = totalAmount.toFixed(2);
        return;
    }

    if (advancePayment > totalAmount) {
        advanceError.innerText =
            "Advance payment cannot be greater than total product amount.";

        advanceError.style.display = "block";
        dueInput.value = "0.00";
        return;
    }

    advanceError.innerText = "";
    advanceError.style.display = "none";

    const duePayment =
        totalAmount - advancePayment;

    dueInput.value =
        duePayment.toFixed(2);
}

document.getElementById("advancePayment")
    .addEventListener("input", function () {

        if (this.value < 0) {
            this.value = 0;
        }

        updateOrderDetails();
    });

document.getElementById("createOrderButton")
    .addEventListener("click", function () {

        if (orderItems.length === 0) {
            showAddProductAlert("Please add at least one product.");
            return;
        }

        const orderDate =
            document.getElementById("orderDate").value;

        const deliveryDate =
            document.getElementById("deliveryDate").value;

        if (!orderDate || !deliveryDate) {
            showAddProductAlert("Please select order and delivery date.");
            return;
        }

        const totalAmount =
            parseFloat(
                document.getElementById("totalProductAmount").value
            ) || 0;

        const advancePayment =
            parseFloat(
                document.getElementById("advancePayment").value
            ) || 0;

        if (advancePayment < 0) {
            alert("Advance payment cannot be negative.");
            return;
        }

        if (advancePayment > totalAmount) {
            alert(
                "Advance payment cannot be greater than total product amount."
            );
            return;
        }

        const duePayment = totalAmount - advancePayment;

        document.getElementById("modalCustomerName").innerText =
            document.getElementById("customerName").innerText;

        document.getElementById("modalCustomerContact").innerText =
            document.getElementById("customerPhone").innerText;

        document.getElementById("modalCustomerBookNumber").innerText =
            document.getElementById("bookNumber").innerText;

        document.getElementById("modalOrderDate").innerText =
            orderDate;

        document.getElementById("modalDeliveryDate").innerText =
            deliveryDate;

        const summaryBody =
            document.getElementById("orderSummaryBody");

        summaryBody.innerHTML = "";

        orderItems.forEach(item => {
            const row = document.createElement("tr");

            if (item.orderProductType === "INVENTORY") {

                row.innerHTML = `
                <td>${item.inventoryItemName || "-"}</td>
                <td>
                    <span class="badge bg-success">Inventory</span>
                </td>
                <td>-</td>
                <td>-</td>
                <td>-</td>
                <td>-</td>
                <td>-</td>
                <td>${(item.inventoryUnitPrice || 0).toFixed(2)}</td>
                <td>${item.qty}</td>
                <td>${(item.amount || 0).toFixed(2)}</td>
                <td>
    ${item.alterationRequired
                    ? "Yes - Rs. " + (item.alterationFee || 0).toFixed(2)
                    : "No"}
</td>
                <td>${item.additionalNotes || "-"}</td>
            `;

            } else {

                row.innerHTML = `
                <td>${item.productName || "-"}</td>
                <td>
                    <span class="badge bg-primary">Tailoring</span>
                </td>
                <td>${item.silai || "-"}</td>
                <td>${item.fabricSource === "SHOP"
                    ? "Shop Fabric"
                    : "Customer Fabric"}</td>
                <td>${item.inventoryItemName || "-"}</td>
                <td>${item.fabricSource === "SHOP"
                    ? (item.fabricQuantity || 0).toFixed(2) + " M"
                    : "-"}</td>
                <td>${item.fabricSource === "SHOP"
                    ? (item.fabricAmount || 0).toFixed(2)
                    : "-"}</td>
                <td>${(item.silaiAmount || 0).toFixed(2)}</td>
                <td>${item.qty}</td>
                <td>${(item.amount || 0).toFixed(2)}</td>
                <td>-</td>
                <td>${item.additionalNotes || "-"}</td>
            `;
            }

            summaryBody.appendChild(row);
        });

        document.getElementById("modalTotalAmount").innerText =
            totalAmount.toFixed(2);

        document.getElementById("modalAdvancePayment").innerText =
            advancePayment.toFixed(2);

        document.getElementById("modalDuePayment").innerText =
            duePayment.toFixed(2);

        const modalElement =
            document.getElementById("orderSummaryModal");

        const modal =
            new bootstrap.Modal(modalElement);

        modal.show();
    });

document.getElementById("confirmOrderButton")
    .addEventListener("click", function () {

        const container =
            document.getElementById("orderProductsInputs");

        container.innerHTML = "";

        orderItems.forEach((item, index) => {

            addHiddenInput(
                container,
                `orderProducts[${index}].orderProductType`,
                item.orderProductType
            );

            addHiddenInput(
                container,
                `orderProducts[${index}].quantity`,
                item.qty
            );

            addHiddenInput(
                container,
                `orderProducts[${index}].amount`,
                item.amount
            );

            addHiddenInput(
                container,
                `orderProducts[${index}].additionalNotes`,
                item.additionalNotes
            );

            if (item.orderProductType === "INVENTORY") {

                addHiddenInput(
                    container,
                    `orderProducts[${index}].inventoryItemId`,
                    item.inventoryItemId
                );

                addHiddenInput(
                    container,
                    `orderProducts[${index}].alterationRequired`,
                    item.alterationRequired
                );

                addHiddenInput(
                    container,
                    `orderProducts[${index}].alterationFee`,
                    item.alterationFee
                );

                addHiddenInput(
                    container,
                    `orderProducts[${index}].orderProductStatus`,
                    item.orderProductStatus
                );

            } else {

                addHiddenInput(
                    container,
                    `orderProducts[${index}].productId`,
                    item.productId
                );

                addHiddenInput(
                    container,
                    `orderProducts[${index}].silaiType`,
                    item.silaiType
                );

                addHiddenInput(
                    container,
                    `orderProducts[${index}].silaiAmount`,
                    item.silaiAmount
                );

                addHiddenInput(
                    container,
                    `orderProducts[${index}].fabricSource`,
                    item.fabricSource
                );

                if (item.fabricSource === "SHOP") {

                    addHiddenInput(
                        container,
                        `orderProducts[${index}].inventoryItemId`,
                        item.inventoryItemId
                    );

                    addHiddenInput(
                        container,
                        `orderProducts[${index}].fabricQuantity`,
                        item.fabricQuantity
                    );
                }
            }
        });

        document.querySelector(".order-form").submit();
    });

function addHiddenInput(container, name, value) {
    const input =
        document.createElement("input");

    input.type = "hidden";
    input.name = name;

    input.value =
        value !== null &&
        value !== undefined
            ? value
            : "";

    container.appendChild(input);
}

function setupInventoryStockValidation() {
    const inventoryItemSelect =
        document.getElementById("inventoryItemOrderSelect");

    const inventoryQuantityInput =
        document.getElementById("inventoryQuantity");

    const inventoryStockInfo =
        document.getElementById("inventoryStockInfo");

    const inventoryStockError =
        document.getElementById("inventoryStockError");

    function validateInventoryQuantity() {
        inventoryStockError.style.display = "none";
        inventoryStockError.textContent = "";

        if (!inventoryItemSelect.value) {
            inventoryStockInfo.style.display = "none";
            return true;
        }

        const selectedOption =
            inventoryItemSelect.options[
                inventoryItemSelect.selectedIndex
                ];

        const physicalStock =
            parseFloat(selectedOption.dataset.stock) || 0;

        const reservedStock =
            parseFloat(selectedOption.dataset.reserved) || 0;

        const currentOrderQuantity =
            getCurrentOrderInventoryQuantity(
                inventoryItemSelect.value
            );

        const availableStock =
            Math.max(
                0,
                physicalStock -
                reservedStock -
                currentOrderQuantity
            );

        const unit =
            selectedOption.dataset.unit || "";

        inventoryStockInfo.textContent =
            "Physical Stock: " +
            physicalStock.toFixed(2) +
            " " + unit +
            " | Reserved Stock: " +
            reservedStock.toFixed(2) +
            " " + unit +
            " | Current Order: " +
            currentOrderQuantity.toFixed(2) +
            " " + unit +
            " | Available Stock: " +
            availableStock.toFixed(2) +
            " " + unit;

        inventoryStockInfo.style.display = "block";

        const quantity =
            parseFloat(inventoryQuantityInput.value);

        if (!quantity || quantity <= 0) {
            return true;
        }

        if (quantity > availableStock) {
            inventoryStockError.textContent =
                "Quantity cannot be greater than available stock. " +
                "Available: " +
                availableStock.toFixed(2) +
                " " +
                unit;

            inventoryStockError.style.display = "block";
            return false;
        }

        return true;
    }

    inventoryItemSelect.addEventListener(
        "change",
        validateInventoryQuantity
    );

    inventoryQuantityInput.addEventListener(
        "input",
        validateInventoryQuantity
    );
}

function refreshInventoryStockInfo() {
    const inventoryItemSelect =
        document.getElementById("inventoryItemOrderSelect");

    const inventoryQuantityInput =
        document.getElementById("inventoryQuantity");

    const inventoryStockInfo =
        document.getElementById("inventoryStockInfo");

    const inventoryStockError =
        document.getElementById("inventoryStockError");

    if (!inventoryItemSelect.value) {
        inventoryStockInfo.style.display = "none";
        inventoryStockError.style.display = "none";
        inventoryStockInfo.textContent = "";
        inventoryStockError.textContent = "";
        return;
    }

    const selectedOption =
        inventoryItemSelect.options[
            inventoryItemSelect.selectedIndex
            ];

    const physicalStock =
        parseFloat(selectedOption.dataset.stock) || 0;

    const reservedStock =
        parseFloat(selectedOption.dataset.reserved) || 0;

    const currentOrderQuantity =
        getCurrentOrderInventoryQuantity(
            inventoryItemSelect.value
        );

    const availableStock =
        Math.max(
            0,
            physicalStock -
            reservedStock -
            currentOrderQuantity
        );

    const unit =
        selectedOption.dataset.unit || "";

    inventoryStockInfo.textContent =
        "Physical Stock: " +
        physicalStock.toFixed(2) +
        " " + unit +
        " | Reserved Stock: " +
        reservedStock.toFixed(2) +
        " " + unit +
        " | Current Order: " +
        currentOrderQuantity.toFixed(2) +
        " " + unit +
        " | Available Stock: " +
        availableStock.toFixed(2) +
        " " + unit;

    inventoryStockInfo.style.display = "block";

    const quantity =
        parseFloat(inventoryQuantityInput.value);

    if (quantity > availableStock) {
        inventoryStockError.textContent =
            "Quantity cannot be greater than available stock. " +
            "Available: " +
            availableStock.toFixed(2) +
            " " + unit;

        inventoryStockError.style.display = "block";
    } else {
        inventoryStockError.textContent = "";
        inventoryStockError.style.display = "none";
    }
}

function getInventoryItemDisplayName(option) {
    const parts = [
        option.dataset.name,
        option.dataset.brand,
        option.dataset.category,
        option.dataset.color,
        option.dataset.design,
        option.dataset.size
    ].filter(value => value && value.trim());

    return parts.join(" | ");
}