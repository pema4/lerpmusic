let socket;
let askingForAction;

let wrapper = document.getElementById("consensus-wrapper");
let text = document.getElementById("consensus-text");

function pointerdownListener() {
    if (askingForAction) {
        const msg = {type: "Action"};
        socket.send(JSON.stringify(msg));
    }
}

function clickListener() {
}

function openConnection() {
    let href = window.location.href;
    let socketUrl = href
        .replace(/https/, 'wss')
        .replace(/http/, 'ws')
        .replace(/static\/consensus.html/, 'consensus/10') + '/listener'
    socket = new WebSocket(socketUrl);

    askingForAction = false;
    text.hidden = true;

    socket.addEventListener('open', () => {
        wrapper.addEventListener("pointerdown", pointerdownListener, false);
        wrapper.addEventListener("click", clickListener, false);
    });

    socket.addEventListener('close', () => {
        askingForAction = false;
        text.hidden = true;

        setTimeout(tryOpenConnection, 500);

        wrapper.removeEventListener("pointerdown", pointerdownListener, false);
        wrapper.removeEventListener("click", clickListener, false);
    });

    socket.addEventListener('message', (event) => {
        const msg = JSON.parse(event.data);
        console.log('Message from server ', event.data);

        switch (msg.type) {
            case "AskForAction":
                askingForAction = true;
                text.hidden = false;
                break;

            case "Cancel":
                askingForAction = false;
                text.hidden = true;
                break;
        }
    });

    socket.addEventListener('error', () => {
        wrapper.removeEventListener("pointerdown", pointerdownListener, false);
        wrapper.removeEventListener("click", clickListener, false);
        askingForAction = false;
        text.hidden = true;
    });
}

function tryOpenConnection() {
    try {
        openConnection();
    } catch (e) {
        console.error(e);
        if (!!socket) {
            socket.close()
        }
        askingForAction = false;
        text.hidden = true;
        wrapper.removeEventListener("pointerdown", pointerdownListener, false);
        wrapper.removeEventListener("click", clickListener, false);
        setTimeout(tryOpenConnection, 500);
    }
}

tryOpenConnection()
