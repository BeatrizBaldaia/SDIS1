#!/bin/bash

echo "Compiling...!"
javac -d bin/ -cp src src/initiator/Peer.java
javac -d bin/ -cp src src/client/TestApp.java

cd bin
echo "Creating RMI registry!"
#rmiregistry &
cd ..

echo "Creating peers!"
for (( c=1; c<=5; c++ ))
do  
	gnome-terminal -x java -cp bin initiator.Peer 1 $c localhost:1099 226.0.0.1 8080 226.0.0.2 8081 226.0.0.0 8082
done
