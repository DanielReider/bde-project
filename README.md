# bde-project
[![Build Status](https://travis-ci.org/dr830029/bde-project.svg?branch=master)](https://travis-ci.org/dr830029/bde-project)

#Getting started

##FLume Einrichtung

Um Flume nutzen zu können, muss die .jar Datei in das lib-Verzeichnis von Flume kopiert werden. Dadurch wird es möglich die benötigte IrcSource zu nutzen.  

```
$ sudo cp twitchChatPull/target/twitchChatPull-0.0.1-jar-with-dependencies.jar /usr/lib/flume-ng/lib/
```

Die flume.properties Datei dient als Template für alle Chats. Beim ausführen der .jar Datei werden die aktuellen Channel eingelesen, die zuvor über den MapReduce Job abgerufen wurden. Für jeden Channel wird auf Grundlage des Templates eine neue .config-Datei erstellt, die als Konfiguration für den Flume-Agent dient. 
