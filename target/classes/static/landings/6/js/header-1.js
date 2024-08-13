const navBtn = document.querySelector('.header__btn-mobile')
const navMenu = document.querySelector('.header__wrapper')
const body = document.querySelector('body')

navBtn.addEventListener('click', () => {
    navBtn.classList.toggle('header__mobile-open')
    navMenu.classList.toggle('header__wrapper-active')
    body.classList.toggle('lock')
})

body.addEventListener("click", (event) => {
    if (!event.target.classList.contains('header__btn-mobile')
        && !event.target.classList.contains('header__container')
        && !event.target.classList.contains('header__right')) {
        navBtn.classList.remove('header__mobile-open')
        navMenu.classList.remove('header__wrapper-active')
        body.classList.remove('lock')
    }
})