import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class HttpServer {

    public static void main( String[] args ) throws Exception {
        if (args.length == 1){
            int port = Integer.parseInt(args[0]);
            System.out.println("Started HTTP Server on port " + port);
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    try (Socket client = serverSocket.accept()) {
                        new Thread(() -> new ConnectionHandler(client)).start();
                    }
                }
            }
        } else {
            System.out.println("Invalid usage. Use java HttpServer (port)");
        }

    }

}