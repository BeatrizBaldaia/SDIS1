# Projeto 1 - Serviço distribuido de Backup

Neste projeto foi desenvolvido um serviço de backup distribuído para uma rede local (LAN) usando-se o espaço livre dos computadores que dela fazem parte. O serviço é fornecido por servidores num ambiente que é considerado cooperativo. No entanto, cada servidor retem o controlo sobre os seus próprios discos e, se necessário, pode recuperar o espaço disponibilizado para fazer o backup de ficheiros de outros computadores.

### Correr o programa

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

MCASTSNOOPER:

	java -jar McastSnooper.jar 226.0.0.1:8080 226.0.0.2:8081 226.0.0.0:8082

Alterar permissões:

	chmod +x script.sh

Se a porta já estiver a ser usada:

	pidof rmiregistry
	kill -kill <PID>


