export PORT=80
export SSL_PORT=443
export KEYSTORE_PASSWORD=$(cat .tokens/keystore-password)

SCREEN_NAME=lerpmusic-site-app
screen -S $SCREEN_NAME -X quit >/dev/null
screen -dmS $SCREEN_NAME java -Xmx256m -jar lerpmusic-site.jar
