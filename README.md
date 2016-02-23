# bde-project
[![Build Status](https://travis-ci.org/dr830029/bde-project.svg?branch=master)](https://travis-ci.org/dr830029/bde-project)


# Project Information

## Data Ingest

#### Twitch Meta Daten
* Die Meta Daten werden über ein MR-Job in das HDFS geschrieben.
* Hierfür ist ein Oozie Workflow implementiert, welcher den MR-Job alle 10 Minuten ausführt.

#### Twitch IRC Chat Daten
* Es werden alle Twitch Chat Daten mit Flume kontinuierlich abgegriffen und im HDFS persistiert.
* Die Chat Nachrichten werden über die Alchemy API bezogen auf ihr Sentiment analysiert.
* Es wird nach dem Twitch Meta Daten Job geprüft ob ggf. neue Chats zur verfügung stehen. Für diese wird ein neuer Flume Agent instanziiert.

#### Wetter Data
* Die Wetter Daten werden über ein MR Job geladen und über die HBase API an HBAse übergeben.
* Wetter Daten werden für für den ML Algorithmus zur Vorhersage der Zuschauerzahlen benötigt.

## Staging Storage
#### Komprimierung
* Alle im HDFS liegenden Ingest Daten werden nach der Verarbeitung mittels bzip2 komprimiert
* bzip2 wurde verwendet, um die Skalierungsfähigkeit von Hadoop in allen Bereichen zu nutzen.
* Es können mehrere Jobs für eine Datei verwendet werden, um diese im Cluster zu verarbeiten.
* Es muss keine externe Bibliothek eingebunden werden.

#### HDFS Struktur
* Alle Daten die im Zusammenhang zum Projekt stehen werden in einer einheitlichen Datenstruktur gespeichert.
* Bsp.:
```
/data/twitch/streammetadata/input
/data/twitch/streammetadata/processing
/data/twitch/streammetadata/completed
```

## Processing
Die gesammelten Daten werden sowohl mit PIG als auch mit Spark ML aggregiert und aufgearbeitet:
#### Transformation
* Alle im HDFS vorliegenden Daten müssen Aggregiert und Aufgearbeitet werden um Informationen zu Streamern und Channels zu erhalten.
* Die vorliegenden Daten werden mit PIG Aggregiert und Aufgearbeitet.
* Die Skripte werden über Workflows & Coordinators automatisiert ausgeführt.

#### Machine Learning
* Es wird die Spark MLlib aufgrund der schnelligkeit und skalierbarkeit verwendet.
* Um die verwendeten Daten für den Algorithmus zu verwenden, werden die Daten zunächst über ein Pig Skript Aggregiert und in das HDFS geschrieben
* Über Spark wird eine Data Pipeline erstellt, welche die Daten einliest und in das für den Algorithmus benötigte Format überführt
* Wir haben uns für ein Naive Bayes Algorithmus aufgrund der guten Spark implementierung und schnellen berechenbarkeit entschieden.
* Für die Prognose wird ein Modell exportiert, welches über den Wildfly abgerufen und ausgeführt werden kann.
* Es wird die Anzahl der Zuschauer prognostiziert

## Access
Die Daten können über ein Web Frontend abgerufen werden: [10.60.64.45:1234](http://10.60.64.45:1234) (Nur über das VPN erreichbar; siehe Präsentation)

## Production
* Es wurde ein CI-Workflow implementiert, wobei jedes Team Mitglied Local an seinem Modul/Aufgabenbereich gearbeitet hat und ein gemeinsames Git Repo verwendet wurde.
* Es wurde Maven als Build-Management-Tool verwendet
* Travis-CI wurde als CI-Server verwendet. Sobald ein Build in den Master-Branch des Projektes gepushed wird, wird automatisch ein neues Bild auf dem CI-Server generiert und getestet. 

##Automation
* Alle Processes sprich Pig Skripte, Flume-Agents, Map-Reduce-Jobs, und Spark-Jobs werden mit Hilfe von Oozie-Workflows und Oozie-Coordinators gemanaged und Ausgeführt.
* Die über das Web-Interface getriggerten Aktionen werden in real-time ausgeführt.

#Getting started

## Setup Apache
Aufrufen der Config:
```
sudo nano /etc/httpd/conf/httpd.conf
```
anpassen des Ports:
```
Listen 80
```
ändern in:
```
Listen 1234
```
einfügen in die Config:
```
ProxyPass /TwitchAnalyticsBackend http://localhost:1235/TwitchAnalyticsWeb
ProxyPassReverse /TwitchAnalyticsBackend http://localhost:1235/TwitchAnalyticsWeb
```
Dann den Service starten:
```
sudo service httpd start
```
im nächsten Schritt muss das Frontend kopiert werden:
```
sudo cp -r TwitchAnalyticsFrontend/ /var/www/html/
```

## Setup Wildfly
für Wildfly wechseln wie in das Homeverzeichnis zurück und laden Wildfly:
```
cd ..
wget "http://download.jboss.org/wildfly/8.2.0.Final/wildfly-8.2.0.Final.zip"
unzip wildfly-8.2.0.Final.zip
mv wildfly-8.2.0.Final WildFly
sudo nano WildFly/standalone/configuration/standalone-full.xml
```
in der Config ändern wir den HTTP.Port:
```
<socket-binding name="http" port="${jboss.http.port:8080}"/>
```
in
```
<socket-binding name="http" port="${jboss.http.port:1235}"/>
```
im Anschluss speichern und Wildfly als Service einrichten:

```
sudo cp WildFly/bin/init.d/wildfly.conf /etc/default/
sudo nano /etc/default/wildfly.conf
```
Dort folgende Configs einstellen und speichern:
```
JBOSS_HOME="/home/cloudera/WildFly"
JBOSS_USER=wildfly
JBOSS_CONFIG=standalone-full.xml
JBOSS_CONSOLE_LOG="/var/log/wildfly/console.log"
```

Service Startskript hinzufügen, Ordner für Logs hinzufügen und Berechtigungen setzten:
```
sudo cp WildFly/bin/init.d/wildfly-init-redhat.sh /etc/init.d/wildfly
sudo mkdir -p /var/log/wildfly
sudo adduser wildfly
sudo chown -R wildfly:wildfly /home/cloudera/WildFly/
sudo chown -R wildfly:wildfly /var/log/wildfly/
sudo chkconfig --add wildfly
sudo service wildfly start
```
Server ist gestartet. Der Service kann deployed werden:
```
sudo cp bde-project/TwitchAnalyticsWeb.war WildFly/standalone/deployments/
```
Der Wildfly ist nun eingerichtet. (Machine Learning Model wurde noch nicht generiert und keine Daten im HBase, daher noch nicht funktionsfähig)

## Setup Pipeline

Zunächst einmal muss das Github Projekt geklont und kompiliert werden.

```
$ git clone https://github.com/dr830029/bde-project.git
$ cd bde-project/
$ mvn clean package
```


Im Anschluss müssen die JAR-Files in das Hadoop-Cluster hochgeladen werden, Bsp. für weatherPull:
```
hadoop fs -put /weatherPull/target/weatherPull-0.0.1-jar-with-dependencies.jar /lib
```

### Flume Einrichtung

Um Flume nutzen zu können, muss die .jar Datei in das lib-Verzeichnis von Flume kopiert werden. Dadurch wird es möglich die benötigte IrcSource zu nutzen. Hierzu muss zunächst in das bde-project navigiert werden und anschließend die .jar Datei in das lib/ Verzeichnis von flume-ng kopiert werden.

```
$ cd bde-project/
$ sudo cp twitchChatPull/target/twitchChatPull-0.0.1-jar-with-dependencies.jar /usr/lib/flume-ng/lib/
```

Die flume.properties Datei dient als Template für alle Chats. Beim ausführen der .jar Datei werden die aktuellen Channel eingelesen, die zuvor über den MapReduce Job abgerufen wurden. Für jeden Channel wird auf Grundlage des Templates eine neue .config-Datei erstellt, die als Konfiguration für den Flume-Agent dient. 
