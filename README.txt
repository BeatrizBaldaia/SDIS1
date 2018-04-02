Deforma a começar o `rmiregistry`, abra um terminal na pasta bin e introduza o comando:

	rmiregistry

Para compilar o peer use o comando seguinte:

	javac -d bin/ -cp src src/initiator/Peer.java

Para compilar a TestApp use o seguinte comando:

	javac -d bin/ -cp src src/client/TestApp.java

Para iniciar o peer use o comando seguinte:

	java -cp bin initiator.Peer 1 1 localhost:1099 226.0.0.1 8080 226.0.0.2 8081 226.0.0.0 8082

Para iniciar a TestApp use o seguinte comando...

...para fazer backup de um ficheiro:

	java -cp bin client.TestApp localhost:1099:peer_1 BACKUP a.png 1

...para pedir de volta um ficheiro ao qual já executou backup:

	java -cp bin client.TestApp localhost:1099:peer_2 RESTORE a.png

...para apagar um ficheiro:

	java -cp bin client.TestApp localhost:1099:peer_2 DELETE a.png

...para ver o estado de um peer:

	java -cp bin client.TestApp localhost:1099:peer_2 STATE

...para alterar o espaço reservado de um peer:

	java -cp bin client.TestApp localhost:1099:peer_1 RECLAIM 0
