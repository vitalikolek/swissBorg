const swiper = new Swiper('.slider__box', {
    slidesPerView: "auto",
    slidesPerGroup: 1,
    spaceBetween: 10,
    pagination: {
        el: '.slider__pagination',
        clickable: true
    },
    navigation: {
        nextEl: '.slider-button-next',
        prevEl: '.slider-button-prev',
        clickable: true
    },
});

