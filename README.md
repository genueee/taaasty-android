taaasty-android
===============

Android-приложение по тейсти



#### Отпечатки ключей


**Для facebook**

`keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore | openssl sha1 -binary | openssl base64`

* release: rAbzTHUr/O6ffvqdxHP0y6QUK9w=
* beta: AqYtfAjH1vMMTnrmYBuU1MjAYMs=
* debug: l1DCWqo8n8qDaF/bhUSelWhBoA8=

**Для вконтакта, ютуба и прочих**

`keytool -exportcert -alias androiddebugkey -keystore path-to-debug-or-production-keystore -list -v `

* release: AC:06:F3:4C:75:2B:FC:EE:9F:7E:FA:9D:C4:73:F4:CB:A4:14:2B:DC
* beta: 02:A6:2D:7C:08:C7:D6:F3:0C:4E:7A:E6:60:1B:94:D4:C8:C0:60:CB
* debug: 97:50:C2:5A:AA:3C:9F:CA:83:68:5F:DB:85:44:9E:95:68:41:A0:0F


