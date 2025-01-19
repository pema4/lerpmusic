// Le socket
let socket

function increaseIntensity() {
    socket.send(JSON.stringify({ type: "IncreaseIntensity" }))
}

function decreaseIntensity() {
    socket.send(JSON.stringify({ type: "DecreaseIntensity" }))
}

let previousIntensityButtonsState = 'hidden'
const intensityButtonsContainer = document.getElementById('intensity-buttons-container')
const increaseIntensityButton = document.getElementById("increase-intensity-button")
const decreaseIntensityButton = document.getElementById("decrease-intensity-button")
function setIntensityButtonsState(state /* disabled | active */) {
    if (state == previousIntensityButtonsState) return

    switch (state) {
        case 'disabled':
            intensityButtonsContainer.classList.add('disabled')
            increaseIntensityButton.removeEventListener("click", increaseIntensity, false)
            decreaseIntensityButton.removeEventListener("click", decreaseIntensity, false)
            break

        case 'active':
            intensityButtonsContainer.classList.remove('disabled')
            increaseIntensityButton.addEventListener("click", increaseIntensity, false)
            decreaseIntensityButton.addEventListener("click", decreaseIntensity, false)
            break
    }

    previousIntensityButtonsState = state
}

function openConnection() {
    let href = window.location.href
    let socketUrl = href
        .replace(/https/, 'wss')
        .replace(/http/, 'ws')
        .replace(/static\/consensus.html/, 'consensus/10') + '/listener'
    socket = new WebSocket(socketUrl)

    socket.addEventListener('open', () => {})
    socket.addEventListener('error', () => setIntensityButtonsState('disabled'))
    socket.addEventListener('close', () => {
        setIntensityButtonsState('disabled')
        setTimeout(mainLoop, 500)
    })
    socket.addEventListener('message', (event) => {
        let msg = JSON.parse(event.data)
        switch (msg.type) {
            // Отсылаются миди-девайсом, сейчас не работают
            case "AskForAction":
                break

            // Отсылаются миди-девайсом, сейчас не работают
            case "Cancel":
                break

            case "ReceiveIntensityUpdates":
                setIntensityButtonsState('active')
                break

            case "CancelIntensityUpdates":
                setIntensityButtonsState('disabled')
                break
        };
    })
}

function mainLoop() {
    try {
        openConnection()
    } catch (e) {
        console.error(e)
        if (!!socket) {
            socket.close()
        }
        setIntensityButtonsState('disabled')
        setTimeout(mainLoop, 500)
    }
}

mainLoop()
