package ca.baosiek.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPJMXServerExecutor {

    private final ExecutorService serversExecutor; // Executor service to manage servers threads
    private static Logger LOG = LoggerFactory.getLogger(TCPJMXServerExecutor.class);

    public TCPJMXServerExecutor() {

        serversExecutor = Executors.newFixedThreadPool(10);
    }

    public void startServers(int numServers, int startPort) throws MalformedObjectNameException,
            InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {

        LOG.info(String.format("Executor is starting [%d] servers starting at port number [%d] at %s", numServers,
                startPort, LocalDateTime.now()));

        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        for (int i = 0; i < numServers; i++) {

            TCPJMXServer server = new TCPJMXServer("Server_" + i, startPort++);
            ObjectName serverName = new ObjectName("ServerManager:type=TCPJMXServer,id=Server_" + i);
            mBeanServer.registerMBean(server, serverName);
            serversExecutor.execute(server);
        }
    }

    public static void main(String[] args) {

        int noServers = Integer.parseInt(args[0]);
        int portSeqStart = Integer.parseInt(args[1]);
        TCPJMXServerExecutor serversExecutor = new TCPJMXServerExecutor();
        try {
            serversExecutor.startServers(noServers, portSeqStart);

        } catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException
                | NotCompliantMBeanException e) {

            e.printStackTrace();
        }

    }
}
