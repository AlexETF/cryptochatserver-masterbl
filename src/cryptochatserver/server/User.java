package cryptochatserver.server;

import cryptochatserver.message.Message;
import java.security.PublicKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;

public class User {

    /*
     * 	private fields
     */
    private String _username;
    private PublicKey _publicKey;
    private Session _session;
    
    private Queue<Message> _messages;

    public User(String username, PublicKey publicKey, Session session) {
        super();
        _username = username;
        _publicKey = publicKey;
        _session = session;
        _messages = new ConcurrentLinkedQueue<Message>();
    }

    public String getUsername() {
        return _username;
    }

    public void setUsername(String username) {
        _username = username;
    }

    public PublicKey getPublicKey() {
        return _publicKey;
    }

    public void setPublicKey(PublicKey _publicKey) {
        this._publicKey = _publicKey;
    }

    public Session getSession() {
        return _session;
    }

    public void setSession(Session _session) {
        this._session = _session;
    }
    
    public synchronized boolean addMessage(Message message) {
        if (message == null) {
            return false;
        }
        boolean result = _messages.offer(message);
        System.out.println("message is added " + result);
        if (result && _messages.size() == 1) {
            System.out.println("Entered into notify section");
            this.notify();
        }
        return result;
    }

    public synchronized Message pollMessage() throws InterruptedException {
        return _messages.poll();
    }

    public synchronized Message pollMessage(boolean wait) throws InterruptedException {
        if (_messages.size() == 0 && wait) {
            this.wait();
        }
        return _messages.poll();
    }

    public synchronized Message peekMessage(boolean wait) throws InterruptedException {
        if (_messages.size() == 0 && wait) {
            this.wait();
        }
        return _messages.peek();
    }

}
