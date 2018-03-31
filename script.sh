#!/bin/bash
echo Hello World
javac -d bin/ -cp src src/initiator/Peer.java
javac -d bin/ -cp src src/client/TestApp.java
cd bin
#rmiregistry &
cd ..
gnome-terminal -x java -cp bin initiator.Peer 1 1 localhost:1099 226.0.0.1 8080 226.0.0.2 8081 226.0.0.0 8082 &
gnome-terminal -x java -cp bin initiator.Peer 1 2 localhost:1099 226.0.0.1 8080 226.0.0.2 8081 226.0.0.0 8082 &
gnome-terminal -x java -cp bin initiator.Peer 1 3 localhost:1099 226.0.0.1 8080 226.0.0.2 8081 226.0.0.0 8082 &
