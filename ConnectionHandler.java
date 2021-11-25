import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ConnectionHandler extends Thread {

    private Socket clientSocket;

    public ConnectionHandler(Socket socket){
        this.clientSocket = socket;
        run();
    }

    private static void handleClient(Socket client) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));

        StringBuilder requestBuilder = new StringBuilder();
        String line;
        while (!(line = br.readLine()).isBlank()) {
            System.out.println(line);
            requestBuilder.append(line + "\r\n");
        }

        String request = requestBuilder.toString();
        String[] requestsLines = request.split("\r\n");
        String[] requestLine = requestsLines[0].split(" ");
        String method = requestLine[0];
        String path = requestLine[1];
        String version = requestLine[2];
        String host = requestsLines[1].split(" ")[1];

        List<String> headers = new ArrayList<>();
        for (int h = 2; h < requestsLines.length; h++) {
            String header = requestsLines[h];
            headers.add(header);
        }

        /*String accessLog = String.format("Client %s, method %s, path %s, version %s, host %s, headers %s",
                client.toString(), method, path, version, host, headers.toString());
        System.out.println(accessLog);*/

        if (method.equalsIgnoreCase("head")){
            // TODO
            return;
        }


        if (method.equalsIgnoreCase("options") || method.equalsIgnoreCase("post")
                || method.equalsIgnoreCase("put") || method.equalsIgnoreCase("delete") ||
                method.equalsIgnoreCase("trace") || method.equalsIgnoreCase("connect")){
            // 500
            byte[] notImplementedContent = "<h1>Method Not Implemented</h1>".getBytes();
            sendResponse(client, "501 Not Implemented", "text/html", notImplementedContent);
            return; // finished
        }

        Path filePath;
        if (!isPathValid(path)){
            // 400
            byte[] badRequestContent = "<h1>Bad Request :(</h1>".getBytes();
            sendResponse(client, "400 Bad Request", "text/html", badRequestContent);
        } else {
            filePath = getFilePath(path);
            if (Files.exists(filePath)) {
                // file exist
                String contentType = guessContentType(filePath); // TODO CHECK IF THIS WORKS FOR ALL FOUR CASES OR IF I NEED TO HARDCODE
                sendResponse(client, "200 OK", contentType, Files.readAllBytes(filePath));
            } else {
                // 404
                byte[] notFoundContent = "<h1>Not found :(</h1>".getBytes();
                sendResponse(client, "404 Not Found", "text/html", notFoundContent);
            }
        }
    }

    public static boolean isPathValid(String path) {
        try {
            Paths.get(path);

        } catch (InvalidPathException ex) {
            return false;
        }

        return true;
    }

    
    private static void sendResponse(Socket client, String status, String contentType, byte[] content) throws IOException {
        OutputStream clientOutput = client.getOutputStream();
        // Status Line
        clientOutput.write(("HTTP/1.1 " + status + "\r\n").getBytes());
        // Header Line
        clientOutput.write(("Server: " + "HttpServer/1.0\r\n").getBytes());
        clientOutput.write(("Content-Length: " + content.length + "\r\n").getBytes());
        clientOutput.write(("Content-Type: " + contentType + "\r\n").getBytes());
        clientOutput.write("\r\n".getBytes());
        // Content
        clientOutput.write(content);
        // End of Content
        clientOutput.write("\r\n\r\n".getBytes());
        clientOutput.flush();
        client.close();
    }

    private static Path getFilePath(String path) {
        if ("/".equals(path)) {
            path = "index.html";
        }
        return Paths.get("public_html", path);

    }

    private static String guessContentType(Path filePath) throws IOException {
        return Files.probeContentType(filePath);
    }

    @Override
    public void run(){
        try {
            handleClient(this.clientSocket);
        } catch (IOException e){
            System.out.println("Lmao error"); // TODO
        }

    }

}