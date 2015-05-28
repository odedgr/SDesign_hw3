package il.ac.technion.cs.sd.msg;

import java.util.function.Consumer;

public class ClientConnection<Message> {
	public ClientConnection(String serverAddress, String clientAddress, Consumer<Message> consumer) {
		throw new UnsupportedOperationException("Not Implemented");
	}
	
	public void send(Message message) {
		throw new UnsupportedOperationException("Not Implemented");
	}
	
	public void kill() {
		throw new UnsupportedOperationException("Not Implemented");
	}
}
