function formatAmount(amount) {
    if (amount === null || amount === undefined || amount === '') {
        return '0.00';
    }

    return Number(amount).toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

function formatNumber(value) {
    if (value === null || value === undefined || value === '') {
        return '0';
    }

    return Number(value).toLocaleString('en-US');
}

// Automatically format Thymeleaf values
document.addEventListener('DOMContentLoaded', function () {

    document.querySelectorAll('.format-amount').forEach(element => {
        element.textContent = formatAmount(element.textContent);
    });

    document.querySelectorAll('.format-number').forEach(element => {
        element.textContent = formatNumber(element.textContent);
    });

});