const mobileBtn = document.querySelector('.header__mobile-btn')
const mobileNav = document.querySelector('.header__nav')
const headerBtnBox = document.querySelector('.header__btn-box')
const headerBgMobile = document.querySelector('.header__bg-mobile')
const body = document.querySelector('.body')


mobileBtn.addEventListener('click', () => {
    mobileNav.classList.toggle('header__nav-active')
    headerBtnBox.classList.toggle('header__btn-box-active')
    headerBgMobile.classList.toggle('header__bg-mobile-active')
    body.classList.toggle('lock')
    mobileBtn.classList.toggle('header__mobile-btn-active')
})

headerBgMobile.addEventListener('click', () => {
    mobileNav.classList.remove('header__nav-active')
    headerBtnBox.classList.remove('header__btn-box-active')
    headerBgMobile.classList.remove('header__bg-mobile-active')
    body.classList.remove('lock')
    body.classList.remove('lock')
    mobileBtn.classList.remove('header__mobile-btn-active')
})