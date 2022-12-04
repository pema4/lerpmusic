# BTLE

## scrapper

Запускается на raspberry pi/ноутбуке, слушает анонсы Bluetooth LE устройств поблизости

### Формат сообщений

Отправляет сообщения серверу по веб-сокету `wss://lerpmusic.ru/btle/{sessionId}/scrapper`:

* `{"type": "Announcement", "id": "…", "rssi": …}`

## receiver

Live MIDI-эффект c поддержкой MPE. Имеет следующие настройки:

* `server-host`
* `bucket-start`
* `bucket-offset` (от 1 до 16)
* `note-start` (от 1 до 128)
* `note-step` (от 1 до 128)
* `rssi-min` (от -150 до 0)
* `rssi-max` (от -150 до 0, должно быть больше `rssi-min`)
* `pressure-smoothing` (от 0с до 5с - сглаживание изменения pressure)
* `legato` (on/off) - если настройка выключена, при получении любого сообщения PeripheralFound отправляются
  последовательно сообщения Note On и Note Off.

### Формат сообщений

Получает от сервера сообщения по веб-сокету `wss://lerpmusic.ru/btle/{sessionId}/receiver`:

* `{"type": "FoundPeripheral", "bucket": "…", "rssi": …}`
* `{"type": "LostPeripheral", "bucket": "…"}`

### Принцип работы

Каждому найденному устройству сооветствует нота, она вычисляется по
формуле `note-start + (bucket - bucket-start) * note-step`.

Девайс работает с MPE - значения rssi конвертируются в pressure по
формуле `128 * clamp(0, 1, (rssi - rssi-min) / (rssi-max - rssi-min))`

Сообщение NoteOn с нужным pressure отправляется при получении первого сообщения `PeripheralFound`, NoteOff оправляется
при получении `PeripheralLost`.

Возможно, в MIDI-эффекте нужно будет реализовать сглаживание pressure.

## server

Прослойка, через которую общаются один `scrapper` и несколько `receiver`

### Принцип работы

* каждому найденному устройству присваивается бакет - случайное число из
  диапазона `[start₁; start₁ + offset₁) ∪ … ∪ [startₙ; startₙ + offsetₙ)`
* устройство забывается, если в течение 10 секунд оно не отправляло анонсы
* при получении анонса `receiverᵢ` оповещается, если бакет устройства попадает в диапазон `[startᵢ; startᵢ + offsetᵢ)`
