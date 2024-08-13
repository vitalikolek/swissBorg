new Swiper('.instantly__items', {
    slidesPerView: "auto",
    slidesPerGroup: 1.2,
    spaceBetween: 26,

    navigation: {
        nextEl: '.instantly__arrow-next',
        prevEl: '.instantly__arrow-prev',
    },

    breakpoints: {
        100: {
            slidesPerView: 1,
            slidesPerGroup: 1,
        },
        501: {
            slidesPerView: "auto",
            slidesPerGroup: 1.2,
        },
    }
})