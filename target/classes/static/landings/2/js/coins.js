const swiper = new Swiper('.coins__slider', {
    loop: true,
    spaceBetween: 11,
    slidesPerView: "auto",

    navigation: {
        nextEl: '.coins__slider-next',
        prevEl: '.coins__slider-prev',
    },

    autoplay: {
        delay: 5000,
    },
});