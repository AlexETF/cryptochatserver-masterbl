/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cryptochatserver.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ZM
 */
public class MainServer implements Runnable {
    
    private static UsersTable usersTable;
    private static ExecutorService executor;
    private static ScheduledExecutorService scheduledExecutor;

    static {
        usersTable = new UsersTable();
        executor = Executors.newCachedThreadPool();
        scheduledExecutor = Executors.newScheduledThreadPool(1);
    }

    private static void schedulePeriodicTasks() {
        //TO DO - Maybe add periodic behaviour here
    }

    private ServerSocket _serverSocket;
    private int _portNumber;
    private volatile boolean _running;
    private KeyPair _keyPair;
    
    public MainServer(int portNumber) {
        _portNumber = portNumber;
        _running = true;
    }
    
    @Override
    public void run() {
        try {
            //generate server private and pubilc key
            initialiseCryptoSettings();
            //open server socket listener
            _serverSocket = new ServerSocket(_portNumber);
            while (_running) {
                Socket clientSocket = _serverSocket.accept();
                ClientListener clientListener= new ClientListener(clientSocket, usersTable, executor, _keyPair);
                startClientHandelingThread(clientListener);
            }
        } catch (IOException ex) {
            System.err.println("E: IOException in MainServer, " + ex.getMessage());
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(MainServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchProviderException ex) {
            Logger.getLogger(MainServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startClientHandelingThread(ClientListener ch) {
        System.out.println("Novi korisnik startovan");
        executor.execute(ch);
    }

    private void initialiseCryptoSettings() throws NoSuchAlgorithmException, NoSuchProviderException {
       _keyPair = CryptoUtils.generateRSAKeyPair(2048);
       MainServer.schedulePeriodicTasks();
    }
    
}
