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
                        آنے والی ترسیلیں (${count})
                    </div>
                `;

                if (count > 0) {

                    const container = document.createElement('div');
                    container.className = 'max-height-scroll';

                    notifications.forEach(notification => {

                        const listItem = document.createElement('a');
                        const isOverdue = notification.isOverdue === true;

                        // اگر تیاری کی تاریخ گزر چکی ہو
                        listItem.className =
                            'dropdown-item notification-item d-flex align-items-start px-3 py-3 gap-3 notification-wrap ' +
                            (isOverdue
                                ? 'border border-danger rounded bg-light-subtle'
                                : '');

                        listItem.href =
                            `${contextPath}orders/details/${notification.orderId_pk}`;

                        let badgeClass = 'bg-light text-dark';

                        const status =
                            (notification.status || '').toLowerCase();

                        let statusText =
                            notification.status || 'نامعلوم';

                        if (status === 'pending') {
                            badgeClass = 'bg-secondary text-light';
                            statusText = 'زیرِ التوا';
                        } else if (status === 'in_progress') {
                            badgeClass = 'bg-primary text-light';
                            statusText = 'تیاری جاری ہے';
                        } else if (status === 'ready_for_pickup') {
                            badgeClass = 'bg-warning text-dark';
                            statusText = 'پک اپ کے لیے تیار';
                        } else if (status === 'completed') {
                            badgeClass = 'bg-success text-light';
                            statusText = 'مکمل';
                        } else if (status === 'delivered') {
                            badgeClass = 'bg-success text-light';
                            statusText = 'حوالہ کر دیا گیا';
                        } else if (status === 'cancelled') {
                            badgeClass = 'bg-danger text-light';
                            statusText = 'منسوخ';
                        }

                        // اگر تاریخ گزر چکی ہو
                        const overdueTag = isOverdue
                            ? `<span class="badge bg-danger text-light me-2">تاریخ گزر چکی</span>`
                            : '';

                        const date = new Date(notification.deliveryDate);

                        const deliveryDate =
                            String(date.getDate()).padStart(2, '0') + '-' +
                            String(date.getMonth() + 1).padStart(2, '0') + '-' +
                            date.getFullYear();

                        listItem.innerHTML = `
                            <div class="pt-1 flex-shrink-0">
                                <i class="bi bi-exclamation-circle-fill 
                                    ${isOverdue ? 'text-danger' : 'text-warning'} fs-5">
                                </i>
                            </div>

                            <div class="flex-grow-1 d-flex flex-column gap-1">

                                <div class="fw-semibold text-break">
                                    آرڈر نمبر: #${notification.orderId || 'نامعلوم'}
                                    ${overdueTag}
                                </div>

                                <div class="text-muted small text-break">
                                    گاہک: ${notification.customerName || 'نامعلوم'}
                                </div>

                                <div class="d-flex justify-content-between align-items-center small mt-1 flex-wrap">

                                    <span class="badge rounded-pill ${badgeClass}">
                                        ${statusText}
                                    </span>

                                    <span class="text-muted ms-3 text-end">
                                        تیاری کی تاریخ: ${deliveryDate}
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
                            <i class="bi bi-check-circle-fill text-success me-2"></i>
                            فی الحال کوئی آنے والی ترسیل نہیں ہے۔
                        </div>
                    `;
                }
            })
            .catch(error => {

                console.error('Error fetching notifications:', error);

                notificationListDiv.innerHTML = `
                    <div class="dropdown-header text-white bg-success rounded-top px-3 py-2 fw-semibold">
                        آنے والی ترسیلیں
                    </div>

                    <div class="dropdown-item text-center py-3 text-danger small">
                        <i class="bi bi-exclamation-circle-fill me-2"></i>
                        اطلاعات حاصل کرنے میں مسئلہ پیش آیا۔
                    </div>
                `;
            });
    }

    fetchNotifications();
});