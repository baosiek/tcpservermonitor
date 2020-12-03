package ca.baosiek.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

public class TCPClient {

    private static Logger LOG = LoggerFactory.getLogger(TCPClient.class);

    public static void main(String[] args) {

        try (Socket s = new Socket("localhost", 8000)){
            OutputStream out = s.getOutputStream();
            PrintWriter writer = new PrintWriter(out);
            writer.println("Hello server!");
            writer.flush();


            InputStream input = s.getInputStream();
            InputStreamReader reader = new InputStreamReader(input);

            int character;
            StringBuilder data = new StringBuilder();

            while ((character = reader.read()) != -1) {
                data.append((char) character);
            }

            LOG.info(String.format("Server message is: %s", data));


        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
