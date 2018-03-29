# Project 1 -- Distributed Backup Service

In this project you will develop a distributed backup service for a local area network (LAN). The idea is to use the free disk space of the computers in a LAN for backing up files in other computers in the same LAN. The service is provided by servers in an environment that is assumed cooperative (rather than hostile). Nevertheless, each server retains control over its own disks and, if needed, may reclaim the space it made available for backing up other computers' files.

javac -d bin/ -cp src src/NonInitiator/Peer.java
java -cp bin NonInitiator.Peer 1 226.0.0.1 8080 226.0.0.2 8081

javac -d bin/ -cp src src/Initiator/Peer.java
java -cp bin Initiator.Peer 1 226.0.0.1 8080 226.0.0.2 8081 teste.txt 1

java -jar McastSnooper.jar 226.0.0.1:8080 226.0.0.2:8081 226.0.0.0:8082


Version 1.0 Normal
Version 1.1 Enhancement RESTORE
Version 1.2 Enhencement DELETE
