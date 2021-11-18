import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
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


        //System.out.println(path);
        Path filePath = getFilePath(path);
        if (Files.exists(filePath)) {
            // file exist
            String contentType = guessContentType(filePath);
            sendResponse(client, "200 OK", contentType, Files.readAllBytes(filePath));
        } else {
            // 404
            byte[] notFoundContent = "<h1>Not found :(</h1>".getBytes();
            // TODO This response isn't sending correctly
            sendResponse(client, "404 Not Found", "text/html", notFoundContent);
        }

    }

    
    private static void sendResponse(Socket client, String status, String contentType, byte[] content) throws IOException {
        OutputStream clientOutput = client.getOutputStream();
        //System.out.println("Status: " + status);
        // TODO either im not seeing this right in the console or im doing this wrong
        clientOutput.write(("HTTP/1.1 " + status + "\r\n\r\n").getBytes("UTF-8"));
        clientOutput.write(("ContentType: " + contentType + "\r\n").getBytes("UTF-8"));
        clientOutput.write("\r\n".getBytes("UTF-8"));
        clientOutput.write(content);
        clientOutput.write("\r\n\r\n".getBytes());
        clientOutput.flush();
        client.close();
    }

    private static Path getFilePath(String path) {
        if ("/".equals(path)) {
            path = "index.html";
        }

        return Paths.get("", path);
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