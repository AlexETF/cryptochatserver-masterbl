/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cryptochatserver;

import cryptochatserver.server.MainServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ZM
 */
public class CryptoChatServer {

    static BufferedReader sin = new BufferedReader(new InputStreamReader(System.in));
    static int portNumber = 8000;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Server port: ");
        try {
            portNumber = Integer.parseInt(sin.readLine());
        } catch (IOException ex) {
            System.out.println("Error while reading input, using default port 8000");
        } catch (NumberFormatException ex) {
            System.out.println("Error while reading input, using default port 8000");
        }
        //creates daemon thread for server and starts it
        //Thread server = new Thread(new MainServer(portNumber));
        //server.setDaemon(true);
        //server.start();
        //System.out.println("Server is running ...");
        //leaves console to the main thread
        //processInput();
        new MainServer(portNumber).run();
    }

    private static void processInput() {
        String command = "";
        try {
            do {
                System.out.print("Crypto server > ");
                command = sin.readLine();
            } while (!"shutdown server".equals(command));
        } catch (IOException ex) {
            Logger.getLogger(CryptoChatServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
