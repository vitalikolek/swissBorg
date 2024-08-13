const trackOne = document.getElementById("track-one")

function getChildrenSumWidth({element}) {
    let i = 0
    for (const child of element.children) {
        i += child.scrollWidth
    }
    startAnimation(element, i / 2)
}

function startAnimation(element, value) {
    element.style.setProperty('--x', `-${value}px`)
}

setTimeout(getChildrenSumWidth, 100, {element: trackOne})