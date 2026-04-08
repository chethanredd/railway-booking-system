// RailWay Pro Main Script
document.addEventListener("DOMContentLoaded", () => {
  // auto-dismiss alerts after ~5 seconds if desired (optional)
  const alerts = document.querySelectorAll('.alert');
  alerts.forEach(alert => {
    if (!alert.classList.contains('alert-warn') && !alert.classList.contains('alert-error')) {
      setTimeout(() => {
        alert.style.transition = 'opacity 0.5s ease';
        alert.style.opacity = '0';
        setTimeout(() => alert.remove(), 500);
      }, 5000);
    }
  });

  // Calculate total fare dynamically on booking page if elements are present
  const totalFareEl = document.getElementById('totalFare');
  if (totalFareEl) {
    const searchParams = new URLSearchParams(window.location.search);
    const passCount = parseInt(searchParams.get('passengers') || 1);
    const travelClass = searchParams.get('travelClass');

    // Super simple stub computation logic just for frontend illustration since backend
    // already computes correct DB total.
    // We update this via Thymeleaf's data attribute or fallback.
    // In our booking.html we could inject this, but for now we look for elements.
    const trainDistance = 1000;
    const baseFare = 500; // Mock calculation display 
    if(totalFareEl.innerText === "—"){
      setTimeout(() => {
        // Typically handled by server side calculation 
        // We will leave the backend to render the true fare.
      }, 100);
    }
  }
});
