const iframe = document.getElementById("rs-buy-sell-iframe")
const iframeLoader = document.querySelector(".rs-buy-sell__loader")

iframe.addEventListener("load", () => {
    iframeLoader.classList.add("disable")
})