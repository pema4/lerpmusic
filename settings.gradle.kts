rootProject.name = "lerpmusic"

include(
    ":btle:btle-domain",
    ":btle:btle-scrapper",
    ":btle:btle-server",
    ":btle:btle-live-receiver",
)

include(
    ":consensus:consensus-domain",
    ":consensus:consensus-server",
    ":consensus:consensus-live-device",
)

include(":portfolio")

include(":website")
