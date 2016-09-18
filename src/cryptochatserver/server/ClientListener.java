package cryptochatserver.server;

import cryptochatserver.message.AckMessage;
import cryptochatserver.message.CreateSessionMessage;
import cryptochatserver.message.CreateTextMessage;
import cryptochatserver.message.ErrorMessage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import cryptochatserver.message.Message;
import cryptochatserver.message.MessageFactory;
import cryptochatserver.message.PublicKeyExchangeMessage;
import cryptochatserver.message.SymmetricEncryptionDataMessage;
import cryptochatserver.message.UserConnectedMessage;
import cryptochatserver.message.UserDisconnectedMessage;
import cryptochatserver.server.UsersTable;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.bouncycastle.util.encoders.Base64;

public class ClientListener implements Runnable {

    private User _user;
    private UsersTable _table;

    private Socket _socket;
    private ClientSender _sender;
    private boolean _running;
    DataOutputStream outStream;
    DataInputStream inStream;

    private KeyPair _serverKeyPair;

    private ExecutorService _executor;

    ClientListener(Socket socket, UsersTable table, ExecutorService executor, KeyPair serverKeyPair) {
        _socket = socket;
        _table = table;
        _running = true;
        _executor = executor;
        _serverKeyPair = serverKeyPair;
    }

    @Override
    public void run() {
        try {
            inStream = new DataInputStream(_socket.getInputStream());
            outStream = new DataOutputStream(_socket.getOutputStream());
            byte[] buffer = new byte[2048];

            byte[] clientKey = Base64.decode(inStream.readUTF());
            //byte[] clientKey = Arrays.copyOfRange(buffer, 0, numOfBytes);

            PublicKeyExchangeMessage message = new PublicKeyExchangeMessage(clientKey);
            //create message that contains server public key
            PublicKeyExchangeMessage answer = new PublicKeyExchangeMessage(_serverKeyPair.getPublic().getEncoded());
            //prepare message data, calculate hash and crc32

            //encrypt and send server pub key
            byte[] encrypted = Base64.encode(_serverKeyPair.getPublic().getEncoded());
            outStream.writeUTF(new String(encrypted));
            System.out.println("Sent: " + encrypted.length);

            //read client 
            
            encrypted = Base64.decode(inStream.readUTF());
            //encrypted = Arrays.copyOfRange(buffer, 0, numOfBytes);
            byte[] decrypted = CryptoUtils.decryptRSA(encrypted, _serverKeyPair.getPrivate());

            SymmetricEncryptionDataMessage clientSession = (SymmetricEncryptionDataMessage) MessageFactory.getMessage(decrypted, null);
            Session session = new Session(clientSession.getAlgorythm(), clientSession.getHashAlgorythm(), clientSession.getKey(), clientSession.getIvVector());

            boolean result = _table.checkIfUserExists(clientSession.getUsername());
            if (result) {
                ErrorMessage errorMessage = new ErrorMessage("User with name " + clientSession.getUsername() + " already exists");
                encrypted = Base64.encode(CryptoUtils.encryptData(Message.getFullMessage(errorMessage, session), session));
                outStream.writeUTF(new String(encrypted));
                return;
            }
            System.out.println("Dodao je novog korisnika " + clientSession.getUsername() + "u tabelu");

            _user = new User(clientSession.getUsername(),
                    CryptoUtils.getPublicKeyFromByteArray(message.getKey(), CryptoUtils.RSA_ALGORYTHM), session);

            //signal the user that everything is OK - send ACK message
            encrypted = Base64.encode(CryptoUtils.encryptData(Message.getFullMessage(new AckMessage(), session), session));
            outStream.writeUTF(new String(encrypted));

            //notify other users that new user has connected
            _table.addMessageToAllUsers(new UserConnectedMessage(clientSession.getUsername(),
                    message.getKey()));
            //add new user to the table of server users
            _table.addUser(_user);

            _sender = new ClientSender(_user, _socket.getOutputStream(), this);
            _executor.execute(_sender);

            sendAllUsersInfo();

            receive();

            _socket.close();
            inStream.close();
            System.out.println("Client closed the connection !");
            if (_user != null) {
                _table.removeUser(_user);
                _table.addMessageToAllUsers(new UserDisconnectedMessage(_user.getUsername()));
                stop();
            }
        } catch (IOException ex) {
            stop();
            System.out.println("IO Exception block reached");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidAlgorithmParameterException ex) {
            Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (_user != null) {
                _table.removeUser(_user);
                _table.addMessageToAllUsers(new UserDisconnectedMessage(_user.getUsername()));
                stop();
            }
            System.out.println("Finally block reached");
        }
        System.out.println("Listener ended");
    }

    private void receive() throws IOException {
        DataInputStream stream = new DataInputStream(_socket.getInputStream());
        byte[] buffer = new byte[4096];
        int numberOfBytes = 0;
        while (_running) {
            try {
                System.out.println("Entered loop");
                String data = stream.readUTF();
                readMessage(Base64.decode(data));
            }catch (IOException ex) {
                break;
            } 
            catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
        System.out.println("Exited loop");
    }

    public void stop() {
        _running = false;
        _sender.stop();
    }

    private void readMessage(byte[] parts) throws Exception {
        byte[] decrypted = CryptoUtils.decryptData(parts, _user.getSession());
        Message message = MessageFactory.getMessage(decrypted, _user.getSession());
        switch (decrypted[0]) {
            case Message.CREATE_SESSION:
                CreateSessionMessage sessionMessage = (CreateSessionMessage) message;
                System.out.println("Dobio sam poruku za kreiranje sesije");
                User user = _table.getUserByName(sessionMessage.getUsername());
                if(user == null){
                    System.out.println("Failed to find user " + sessionMessage.getUsername());
                    return;
                }
                user.addMessage(message);
                break;
            case Message.CREATE_TEXT_MESSAGE:
                CreateTextMessage createTextMessage = (CreateTextMessage)message;
                System.out.println("Dobio sam poruku za kreiranje poruke");
                User recipient = _table.getUserByName(createTextMessage.getUserRecipient());
                if(recipient == null){
                    System.out.println("Failed to find user " + createTextMessage.getUserRecipient());
                    return;
                }
                recipient.addMessage(createTextMessage);
                break;
            case Message.USER_CONNECTED:

                break;
            case Message.USER_DISCONNECTED:
                _running = false;
                break;
            default:
                break;
        }
    }

    private void sendAllUsersInfo() {
        synchronized (_table) {
            Collection<User> users = _table.getAllUsers();
            for (User user : users) {
                if (!user.getUsername().equals(_user.getUsername())) {
                    _user.addMessage(new UserConnectedMessage(user.getUsername(), user.getPublicKey().getEncoded()));
                }
            }
        }
    }

    protected boolean removeUser(User user) {
        synchronized (_table) {
            boolean result = _table.removeUser(user);
            _table.addMessageToAllUsers(new UserDisconnectedMessage(_user.getUsername()));
            return result;
        }
    }

}

class ClientSender implements Runnable {

    private boolean _running;
    private OutputStream _outStream;
    Thread myThread;
    private User _user;
    private ClientListener _listener;

    public ClientSender(User user, OutputStream out, ClientListener listener) {
        _user = user;
        _outStream = out;
        _running = true;
        _listener = listener;
    }

    @Override
    public void run() {
        try {
            DataOutputStream stream = new DataOutputStream(_outStream);
            myThread = Thread.currentThread();
            System.out.println("Krenuo je listener ");
            System.out.println(_user.getUsername());
            System.out.println(_user.getSession().getSymmetricAlgorythm());
            System.out.println(_user.getSession().getHashAlgorythm());
            System.out.println(_user.getSession().getKey());
            System.out.println(_user.getSession().getIvVector());
            while (_running && !myThread.interrupted()) {
                Message message = _user.peekMessage(true);

                byte[] data = Message.getFullMessage(message, _user.getSession());
                byte[] encrypted = Base64.encode(CryptoUtils.encryptData(data, _user.getSession()));
                stream.writeUTF(new String(encrypted));
                _user.pollMessage();
                System.out.println("Sent " + message.toString() + " to user " + _user.getUsername());
            }
            _outStream.close();
        } catch (IOException ex) {
            System.out.println("Sender thread closed ! IO");
        } catch (InterruptedException e) {
            System.out.println("Sender thread closed ! Interrupted ");
        } catch (Exception ex) {
            Logger.getLogger(ClientSender.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Called remove user from sender: " + _listener.removeUser(_user));
        System.out.println("Sender of user " + _user.getUsername() + " is closed");
    }

    public void stop() {
        _running = false;
        while (!myThread.isInterrupted()) {
            myThread.interrupt();
            System.out.println("Upao sam stop listener petlju u petlju");
        }
        System.out.println("Izasao iz stop listener petlje");
    }

}
