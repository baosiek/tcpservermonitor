package ca.baosiek.server;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.security.InvalidParameterException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPJMXServerExecutor {

    private final ExecutorService serversExecutor; // Executor service to manage servers threads
    private static Logger LOG = LoggerFactory.getLogger(TCPJMXServerExecutor.class);

    public TCPJMXServerExecutor(int threads) {

        serversExecutor = Executors.newFixedThreadPool(threads);
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

    /**
     * Initialize ExecuterService with the number of thread computed from
     * blocking coefficient.
     * @param args should contain -n for number of servers; -p for starting port; and
     *             -c for blocking coefficient
     */

    public static void main(String[] args) {

        int noServers = 0;
        int portSeqStart = 0;
        float blockingCoefficient = 0f;

        // Setting the options
        Options options = new Options();
        options.addOption("n", true, "number of servers");
        options.addOption("p", true, "starting port");
        options.addOption("c", true, "blocking coefficient");

        // Parsing command line
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);

           /*
            Checking and getting number of servers to be initialized.
             */
            if (cmd.hasOption('n')) {
                noServers = Integer.parseInt(cmd.getOptionValue('n'));
            } else {
                throw new InvalidParameterException("Number of servers wasn't specified");
            }

             /*
            Checking and getting the initial port.
             */
            if (cmd.hasOption('p')) {
                portSeqStart = Integer.parseInt(cmd.getOptionValue('p'));
            } else {
                throw new InvalidParameterException("Starting port wasn't specified");
            }

             /*
            Checking and getting the blocking coefficient. Number must be greater or equal to 0
            and smaller than 1.
             */
            if (cmd.hasOption('c')) {
                blockingCoefficient = Float.parseFloat(cmd.getOptionValue('c'));
                if (blockingCoefficient < 0 || blockingCoefficient >= 1){
                    throw new InvalidParameterException(
                            String.format("Blocking coefficient is a number in the interval [0,1)." +
                            " %.2f was informed.", blockingCoefficient));
                }
            } else {
                throw new InvalidParameterException("Blocking coefficient wasn't specified");
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }

        /*
        Calculating optimal number of threads based on Subramaniam (2011, p.31)
         */
        int cores = Runtime.getRuntime().availableProcessors();
        int threads = (int) (cores / (1 - blockingCoefficient));
        LOG.info(String.format("Setting number of threads to %d", threads));

        TCPJMXServerExecutor serversExecutor = new TCPJMXServerExecutor(threads);
        try {
            serversExecutor.startServers(noServers, portSeqStart);

        } catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException
                | NotCompliantMBeanException e) {

            e.printStackTrace();
        }

    }
}
