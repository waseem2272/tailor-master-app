document.addEventListener('DOMContentLoaded', function () {
    const notificationCountSpan = document.getElementById('notificationCount');
    const notificationListDiv = document.getElementById('notificationList');

    function fetchNotifications() {
        fetch(contextPath + 'dashboard/notifications/upcoming-deliveries')
            .then(response => response.json())
            .then(notifications => {
                const count = notifications.length;
                notificationCountSpan.textContent = count;

                // Header
                notificationListDiv.innerHTML = `
                    <div class="dropdown-header text-white bg-success rounded-top px-3 py-2 fw-semibold">
                        Upcoming Deliveries (${count})
                    </div>
                `;

                if (count > 0) {
                    const container = document.createElement('div');
                    container.className = 'max-height-scroll';

                    notifications.forEach(notification => {
                        const listItem = document.createElement('a');
                        const isOverdue = notification.isOverdue === true;

                        // Use a warning or danger border if overdue
                        listItem.className = 'dropdown-item notification-item d-flex align-items-start px-3 py-3 gap-3 notification-wrap ' +
                            (isOverdue ? 'border border-danger rounded bg-light-subtle' : '');

                        listItem.href = `${contextPath}orders/details/${notification.orderId_pk}`;

                        let badgeClass = 'bg-light text-dark';
                        const status = (notification.status || '').toLowerCase();

                        if (status === 'pending') badgeClass = 'bg-secondary text-light';
                        else if (status === 'in_progress') badgeClass = 'bg-primary text-light';

                        // Optional: add an overdue badge or icon
                        const overdueTag = isOverdue
                            ? `<span class="badge bg-danger text-light ms-2">Overdue</span>`
                            : '';

                        listItem.innerHTML = `
        <div class="pt-1 flex-shrink-0">
            <i class="bi bi-exclamation-circle-fill ${isOverdue ? 'text-danger' : 'text-warning'} fs-5"></i>
        </div>
        <div class="flex-grow-1 d-flex flex-column gap-1">
            <div class="fw-semibold text-break">Order #${notification.orderId || 'N/A'} ${overdueTag}</div>
            <div class="text-muted small text-break">Customer: ${notification.customerName || 'Unknown'}</div>
            <div class="d-flex justify-content-between align-items-center small mt-1 flex-wrap">
                <span class="badge rounded-pill ${badgeClass}">${notification.status || 'N/A'}</span>
                <span class="text-muted ms-3 text-end">
                    Deliver by: ${new Date(notification.deliveryDate).toLocaleString('en-PK', {
                            year: 'numeric', month: 'short', day: 'numeric'
                        })}
                </span>
            </div>
        </div>
    `;

                        container.appendChild(listItem);
                    });


                    notificationListDiv.appendChild(container);
                } else {
                    notificationListDiv.innerHTML += `
                        <div class="dropdown-item text-center py-3 text-muted small">
                            <i class="bi bi-check-circle-fill text-success me-2"></i> No upcoming deliveries
                        </div>
                    `;
                }
            })
            .catch(error => {
                console.error('Error fetching notifications:', error);
                notificationListDiv.innerHTML = `
                    <div class="dropdown-header text-white bg-success rounded-top px-3 py-2 fw-semibold">
                        Upcoming Deliveries
                    </div>
                    <div class="dropdown-item text-center py-3 text-danger small">
                        <i class="bi bi-exclamation-circle-fill me-2"></i> Error loading notifications
                    </div>
                `;
            });
    }

    fetchNotifications();
});
