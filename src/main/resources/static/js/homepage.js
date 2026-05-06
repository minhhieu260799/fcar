(function () {
    var heroCarousel = document.getElementById('homeHeroCarousel');
    if (!heroCarousel || typeof bootstrap === 'undefined' || !bootstrap.Carousel) {
        return;
    }

    var prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    var carousel = bootstrap.Carousel.getOrCreateInstance(heroCarousel, {
        interval: prefersReducedMotion ? false : 5800,
        pause: false,
        ride: prefersReducedMotion ? false : 'carousel',
        touch: true,
        wrap: true
    });

    if (!prefersReducedMotion) {
        heroCarousel.addEventListener('mouseenter', function () {
            carousel.pause();
        });
        heroCarousel.addEventListener('mouseleave', function () {
            carousel.cycle();
        });
    }

    heroCarousel.addEventListener('keydown', function (event) {
        if (event.key === 'ArrowLeft') {
            event.preventDefault();
            carousel.prev();
        } else if (event.key === 'ArrowRight') {
            event.preventDefault();
            carousel.next();
        }
    });
})();
