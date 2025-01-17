let askingForAction = false
const actionWrapper = document.getElementById("action-wrapper")
const actionButton = document.getElementById("action-button")

let askingForIntensity = false
const intensityWrapper = document.getElementById('intensity-wrapper')
const increaseIntensityButton = document.getElementById("increase-button")
const decreaseIntensityButton = document.getElementById("decrease-button")

function toggleIntensityWrapperVisibility(visible) {
    if (visible) {
        intensityWrapper.classList.remove('hidden')
    } else if (!intensityWrapper.classList.contains('hidden')) {
        intensityWrapper.classList.add('hidden')
    }
}

function toggleActionWrapperVisibility(visible) {
    if (visible) {
        actionWrapper.style.display = 'flex';
    } else {
        actionWrapper.style.display = 'none';
    }
}

// Вебсокеты

let socket
function actionPointerdownListener() {
    if (askingForAction) {
        const msg = { type: "Action" }
        socket.send(JSON.stringify(msg))
    }
}

function clickListener() {
}

function intensityButtonClickListener() {
    if (askingForIntensity) {
        let type
        if (this === increaseIntensityButton) {
            type = "IncreaseIntensity"
        } else if (this === decreaseIntensityButton) {
            type = "DecreaseIntensity"
        }

        socket.send(JSON.stringify({ type }))
    }
}

function openConnection() {
    let href = window.location.href
    let socketUrl = href
        .replace(/https/, 'wss')
        .replace(/http/, 'ws')
        .replace(/static\/consensus.html/, 'consensus/10') + '/listener'
    socket = new WebSocket(socketUrl)

    askingForAction = false
    actionButton.hidden = true
    askingForIntensity = false

    socket.addEventListener('open', () => {
        actionWrapper.addEventListener("pointerdown", actionPointerdownListener, false)
        actionWrapper.addEventListener("click", clickListener, false)
        increaseIntensityButton.addEventListener("click", intensityButtonClickListener, false)
        decreaseIntensityButton.addEventListener("click", intensityButtonClickListener, false)
    })

    socket.addEventListener('close', () => {
        askingForAction = false
        actionButton.hidden = true

        setTimeout(tryOpenConnection, 500)

        actionWrapper.removeEventListener("pointerdown", actionPointerdownListener, false)
        actionWrapper.removeEventListener("click", clickListener, false)
        increaseIntensityButton.removeEventListener("click", intensityButtonClickListener, false)
        decreaseIntensityButton.removeEventListener("click", intensityButtonClickListener, false)
    })

    socket.addEventListener('message', (event) => {
        const msg = JSON.parse(event.data)
        console.log('Message from server ', event.data)

        switch (msg.type) {
            case "AskForAction":
                askingForAction = true
                actionButton.hidden = false
                break

            case "Cancel":
                askingForAction = false
                actionButton.hidden = true
                break

            case "ReceiveIntensityUpdates":
                askingForIntensity = true
                toggleIntensityWrapperVisibility(true)
                toggleActionWrapperVisibility(false)
                break

            case "CancelIntensityUpdates":
                askingForIntensity = true
                toggleIntensityWrapperVisibility(false)
                toggleActionWrapperVisibility(true)
                break
        };
    })

    socket.addEventListener('error', () => {
        actionWrapper.removeEventListener("pointerdown", actionPointerdownListener, false)
        actionWrapper.removeEventListener("click", clickListener, false)
        increaseIntensityButton.removeEventListener("click", intensityButtonClickListener, false)
        decreaseIntensityButton.removeEventListener("click", intensityButtonClickListener, false)

        toggleIntensityWrapperVisibility(false)
        toggleActionWrapperVisibility(true)

        askingForAction = false
        actionButton.hidden = true
        askingForIntensity = false
    })
}

function tryOpenConnection() {
    try {
        openConnection()
    } catch (e) {
        console.error(e)
        if (!!socket) {
            socket.close()
        }
        askingForAction = false
        actionButton.hidden = true
        actionWrapper.removeEventListener("pointerdown", actionPointerdownListener, false)
        actionWrapper.removeEventListener("click", clickListener, false)
        setTimeout(tryOpenConnection, 500)
    }
}

tryOpenConnection()
