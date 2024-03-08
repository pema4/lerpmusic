let href = window.location.href;
let imageUrl = href.replace(/\/qr/, '/qr-image.png')

let img = document.getElementById('qr-img')
img.src = imageUrl;
