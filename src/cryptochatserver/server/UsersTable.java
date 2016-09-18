package cryptochatserver.server;

import cryptochatserver.message.Message;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UsersTable {

    /*
     * 	 private field
     */
    private Map<String, User> _usersTable;
    //These fields needed to customize ConcurrentHashMap
    private int _concurrencyLevel;
    private float _loadFactor;
    private int _initialCapacity;

    public UsersTable() {
        _concurrencyLevel = 2;
        _loadFactor = 0.90f;
        _initialCapacity = 50;
        _usersTable = new ConcurrentHashMap<String, User>(_concurrencyLevel, _loadFactor, _initialCapacity);
    }

    public UsersTable(int concurrencyLevel, float loadFactor, int initialCapacity) {
        _concurrencyLevel = concurrencyLevel;
        _loadFactor = loadFactor;
        _initialCapacity = initialCapacity;
        _usersTable = new ConcurrentHashMap<String, User>(_concurrencyLevel, _loadFactor, _initialCapacity);
    }

    public boolean checkIfUserExists(String name) {
        return _usersTable.containsKey(name);
    }

    public User getUserByName(String name) {
        return _usersTable.get(name);
    }

    public boolean addUser(User user) {
        if (checkIfUserExists(user.getUsername())) {
            return false;
        }
        System.out.println("Added user " + user.getUsername());
        _usersTable.put(user.getUsername(), user);
        System.out.println(_usersTable);
        return true;
    }

    public boolean removeUser(User user) {
        if (!checkIfUserExists(user.getUsername())) {
            return false;
        }
        _usersTable.remove(user.getUsername());
        System.out.println("Removed user " + user.getUsername());
        System.out.println(_usersTable);
        return true;
    }

    public synchronized void addMessageToAllUsers(Message msg) {
        for (User u : this._usersTable.values()) {
            u.addMessage(msg);
        }
    }

    public synchronized void addMessageToAllUsersExcept(Message msg, User user) {
        for (User u : this._usersTable.values()) {
            if (!user.getUsername().equals(u.getUsername())) {
                u.addMessage(msg);
            }
        }
    }
    
    public Collection<User> getAllUsers(){
        return _usersTable.values();
    }
}
