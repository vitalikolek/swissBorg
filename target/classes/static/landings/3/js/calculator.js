const buttonsContainer = document.querySelector(".calculator__select-buttons")
const buttons = document.querySelectorAll(".calculator__select-button")
const inputCalculator = document.querySelector(".calculator__number")
const inputRange = document.querySelector(".calculator__range")


function changeBgInputRange(value) {
    inputRange.style.background = 'linear-gradient(to right, #6FCF97 0%, #6FCF97 ' + value++ + '%, rgba(255, 255, 255, 0.13) ' + value++ + '%, rgba(255, 255, 255, 0.13)'
}

function changeValueInputRange() {
    let valuePercent = (inputRange.value-inputRange.min)/(inputRange.max-inputRange.min)*100
    changeBgInputRange(valuePercent)
}

changeBgInputRange(0)

inputCalculator.oninput = function () {
    inputRange.value = inputCalculator.value
    changeValueInputRange()

    if (inputCalculator.value === "") {
        inputRange.value = 0
        changeValueInputRange()
    }

    if (inputCalculator.value > 10000) {
        inputCalculator.value = 10000
        changeValueInputRange()
    }
};

inputRange.oninput = function () {
    inputCalculator.value = inputRange.value
    changeValueInputRange()
}

function removeAllActiveClass(arr) {
    arr.forEach(function (item) {
        item.classList.remove("calculator__select-button-active")
    })
}

buttonsContainer.addEventListener("click", (event) => {
    if (event.target.classList.contains("calculator__select-button")) {
        removeAllActiveClass(buttons)
        event.target.classList.add("calculator__select-button-active")
    }
})