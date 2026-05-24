document.addEventListener('DOMContentLoaded', function() {

    // Dashboard charts (static demo data; replace with real values).

    // Monthly sales trend.
    const salesCtx = document.getElementById('salesChart');
    if (salesCtx) {
        new Chart(salesCtx, {
            type: 'line',
            data: {
                labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
                datasets: [{
                    label: 'Sales',
                    data: [42, 51, 48, 62, 55, 59],
                    borderColor: '#3b82f6',
                    backgroundColor: 'rgba(59, 130, 246, 0.1)',
                    borderWidth: 2,
                    pointBackgroundColor: '#ffffff',
                    pointBorderColor: '#3b82f6',
                    pointRadius: 4,
                    fill: false,
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: { 
                        beginAtZero: true,
                        grid: { borderDash: [5, 5] }
                    },
                    x: {
                        grid: { display: false }
                    }
                }
            }
        });
    }

    // Monthly revenue trend.
    const revenueCtx = document.getElementById('revenueChart');
    if (revenueCtx) {
        new Chart(revenueCtx, {
            type: 'line',
            data: {
                labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
                datasets: [{
                    label: 'Revenue',
                    data: [120000, 150000, 135000, 180000, 165000, 175000],
                    borderColor: '#10b981',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    borderWidth: 2,
                    pointBackgroundColor: '#ffffff',
                    pointBorderColor: '#10b981',
                    pointRadius: 4,
                    fill: false,
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: { 
                        beginAtZero: true,
                        grid: { borderDash: [5, 5] },
                        ticks: {
                            callback: function(value) {
                                return value;
                            }
                        }
                    },
                    x: {
                        grid: { display: false }
                    }
                }
            }
        });
    }

    // Reports charts.

    // Monthly revenue vs profit.
    const profitRevenueCtx = document.getElementById('profitRevenueChart');
    if (profitRevenueCtx) {
        new Chart(profitRevenueCtx, {
            type: 'bar',
            data: {
                labels: ['Feb 2026'],
                datasets: [
                    {
                        label: 'Revenue',
                        data: [170333],
                        backgroundColor: '#3b82f6',
                        barPercentage: 0.4,
                        categoryPercentage: 0.8
                    },
                    {
                        label: 'Profit',
                        data: [68183],
                        backgroundColor: '#10b981',
                        barPercentage: 0.4,
                        categoryPercentage: 0.8
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            usePointStyle: true,
                            boxWidth: 8
                        }
                    }
                },
                scales: {
                    y: { 
                        beginAtZero: true,
                        grid: { borderDash: [5, 5] }
                    },
                    x: {
                        grid: { display: false }
                    }
                }
            }
        });
    }

    // Sales by buyer type.
    const buyerTypeCtx = document.getElementById('buyerTypeChart');
    if (buyerTypeCtx) {
        new Chart(buyerTypeCtx, {
            type: 'pie',
            data: {
                labels: ['Auction', 'Export', 'Regular Customer'],
                datasets: [{
                    data: [0, 33.3, 66.7],
                    backgroundColor: ['#f97316', '#10b981', '#3b82f6'],
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            usePointStyle: true,
                            boxWidth: 8
                        }
                    }
                }
            }
        });
    }

});
