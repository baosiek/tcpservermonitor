package ca.baosiek.server;

import ca.baosiek.jmx.TCPJMXServerMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TCPJMXServer implements Runnable, TCPJMXServerMXBean {

    private final AtomicInteger port; // port
    private final String serverName; // service name that facilitates service identification
    private static Logger LOG = LoggerFactory.getLogger(TCPJMXServer.class);
    private boolean isSuspended;
    private boolean isStopped;
    private ServerSocket socket;

    /**
     * Returns a TCPJMXServer object that can then be monitored and managed via JMX.
     *
     * @param   port  an absolute URL giving the base location of the image
     * @param   serverName the location of the image, relative to the url argument
     * @see     TCPJMXServer
     */

    public TCPJMXServer(String serverName, int port) {

        this.port = new AtomicInteger(port);
        this.serverName = serverName;
        this.isSuspended = false;
        this.isStopped = false;
    }

    @Override
    public void suspend() {

        if (this.isStopped) {
            LOG.info("Server is stopped and cannot be suspended");
            return;
        }

        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.isSuspended = true;

    }

    @Override
    public synchronized void resume() {

        this.isSuspended = false;
        notify();

    }

    @Override
    public synchronized void stop() {

        try {
            this.socket.close();
        } catch (IOException e) {

            e.printStackTrace();
        }

        if (this.isSuspended) {
            this.resume();
        }

        this.isStopped = true;
        notify();

    }

    @Override
    public void start() {

        if(this.isStopped == true){
            this.isStopped = false;
        } else {
            LOG.info("Server is running and cannot be started again.");
            return;
        }
        this.run();
    }

    @Override
    public Integer getPort() {
        return this.port.get();
    }

    @Override
    public String getServerName() {
        return this.serverName;
    }

    /**
     * When an object implementing interface {@code Runnable} is used
     * to create a thread, starting the thread causes the object's
     * {@code run} method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method {@code run} is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {

        LOG.info(String.format("Server %s started running at port: %d at %s", this.serverName, this.port.get(),
                LocalDateTime.now()));

        while (!this.isStopped) {

            try {
                socket = new ServerSocket(this.getPort());
            } catch (IOException e1) {

                e1.printStackTrace();
            }

            try {

                if (!this.isSuspended) {

                    if (socket != null) {
                        new TCPJMXClientHandler(socket.accept()).start();
                    }
                }

                socket.close();

                while(this.isSuspended) {

                    LOG.info(String.format("Server [%s] suspended at [%s]", this.serverName, LocalDateTime.now()));
                    synchronized(this) {
                        try {
                            wait();
                        } catch (InterruptedException e) {

                            e.printStackTrace();
                        }
                    }

                    LOG.info(String.format("Server [%s] resumed at [%s]", this.serverName, LocalDateTime.now()));
                }
                
            } catch (IOException e) {

                if (e.getLocalizedMessage().equals("Socket closed")) continue;
				LOG.error(e.getLocalizedMessage());
            }
        }
        LOG.info(String.format("Server [%s] stopped at [%s]", this.serverName, LocalDateTime.now()));
    }

    /**
     *  This handler in different threads enables handling of multiple requests
      */

    private class TCPJMXClientHandler extends Thread {

        private Socket clientSocket;

        public TCPJMXClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        /**
         * Just handling the ping, by replying with the string "pong!".
         */
        public void run() {

            try {
                LOG.info(String.format("New client socket accepted at channel %s at %s", this.clientSocket.getInetAddress(),
                        LocalDateTime.now()));

                OutputStream out = clientSocket.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
                writer.append("pong!");
                writer.flush();

                clientSocket.close();

            } catch (IOException e) {

                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws MalformedObjectNameException,
            NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {

        ExecutorService serversExecutor = Executors.newFixedThreadPool(4);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        TCPJMXServer server = new TCPJMXServer("Server_" + 0, 8000);
        ObjectName serverName = new ObjectName("ServerManager:type=TCPJMXServer,id=Server_" + 0);
        mBeanServer.registerMBean(server, serverName);
        serversExecutor.execute(server);
    }
}
