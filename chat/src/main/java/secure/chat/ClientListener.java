package secure.chat;

/**
 * Nakov Chat Server - (c) Svetlin Nakov, 2002
 *
 * ClientListener class is purposed to listen for client messages and
 * to forward them to ServerDispatcher.
 */
import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientListener extends Thread {
    private ServerDispatcher mServerDispatcher;
    private ClientInfo mClientInfo;
    private BufferedReader mIn;

    public ClientListener(ClientInfo aClientInfo, ServerDispatcher aServerDispatcher) throws IOException {
        mClientInfo = aClientInfo;
        mServerDispatcher = aServerDispatcher;
        Socket socket = aClientInfo.mSocket;
        mIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * Until interrupted, reads messages from the client socket, forwards them to
     * the server dispatcher's queue and notifies the server dispatcher.
     */
    public void run() {
        String message = null;
        String ciphertext = null;
        try {
            while (!isInterrupted()) {
                // decrypted message read from client socket
                try {
                    ciphertext = mIn.readLine();
                    message = mClientInfo.mEncrption.decrypt(ciphertext);
                    System.out.println(message);
                    if (message.contains("bulletin")) {
                        // decrypt vote & send it to DecryptVote which gets called by serverdispatcher
                        String nin;
                        String pin;
                        String voteC;
                        String voteaD;
                        String voteD;

                        System.out.println("NIN :");
                        nin = message.substring(11, 29);
                        System.out.println(nin);
                        System.out.println("PIN :");
                        pin = message.substring(30, 95);
                        System.out.println(pin); // length of PIN is 65
                        System.out.println("Vote chiffré :");
                        String pattern = "(\\s)(.*)";

                        // Create a Pattern object
                        Pattern r = Pattern.compile(pattern);

                        // Now create matcher object.
                        Matcher m = r.matcher(message.substring(95, message.length()));
                        if (m.find()) {
                            voteC = m.group(0);
                            voteaD = voteC.substring(1, voteC.length());
                            voteD = mClientInfo.mEncrption.decrypt(voteaD);
                            DecryptVote.setVoteD(voteD);
                        }
                    }
                    System.out.println("\t(Decrypted from cipher text: " + ciphertext + ")");
                } catch (ErrorException err) {
                    mClientInfo.mClientSender.interrupt();
                }

                if (message == null || message.equals("exit"))
                    break;

                // forward message to dispatch's queue
                mServerDispatcher.dispatchMessage(mClientInfo, message);
            }
        } catch (IOException ioex) {
            // Problem reading from socket (communication is broken)
            System.err.println(":fail COMMUNICATION WITH " + mClientInfo.mAlias + " IS BROKEN IS BROKENT!\n");
        }

        // Communication is broken. Interrupt both listener and sender threads
        System.out.println("***Bye " + mClientInfo.mAlias + " !");
        mClientInfo.mClientSender.interrupt();
        mServerDispatcher.deleteClient(mClientInfo);
    }
}
