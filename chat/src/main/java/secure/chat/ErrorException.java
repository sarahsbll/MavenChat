package secure.chat;

public class ErrorException extends Exception {
	public ErrorException(String command) {
		super(command);
	}
}
