package ba.baosiek.observable;

import ba.baosiek.jmx.MonitorMXBean;
import ba.baosiek.utils.DateConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class TCPMonitor extends NotificationBroadcasterSupport
        implements Runnable, MonitorMXBean, ITCPMonitorScheduler {

    private static Logger log = LoggerFactory.getLogger(TCPMonitor.class);
    private AtomicReference<String> hostName;
    private AtomicInteger portNumber;
    private AtomicReference<LocalDateTime> startOutage;
    private AtomicReference<LocalDateTime> stopOutage;
    private AtomicLong polling;
    private AtomicReference<String> status;
    private AtomicReference<String> serviceName;
    private AtomicLong graceTimeDuration;
    private AtomicBoolean graceTimeFlag;
    private Boolean isGraceTime;
    private Boolean isNormalSchedule;
    private ScheduledExecutorService ses;
    private ScheduledFuture<?> futureTask;
    private Map<String, Long> pollingFrequencies;
    private int notificationCounter;

    public TCPMonitor(ScheduledExecutorService ses) {

        hostName = new AtomicReference<>("localhost");
        portNumber = new AtomicInteger(8080);
        startOutage = new AtomicReference<>(LocalDateTime.now());
        stopOutage = new AtomicReference<>(LocalDateTime.now());
        polling = new AtomicLong(Long.MAX_VALUE);
        status = new AtomicReference<>("UNK");
        serviceName = new AtomicReference<>("");
        graceTimeDuration = new AtomicLong(0);
        graceTimeFlag = new AtomicBoolean(Boolean.valueOf(false));
        isGraceTime = false;
        isNormalSchedule = true;
        notificationCounter = 0;

        this.ses = ses;
        pollingFrequencies = new ConcurrentHashMap<>();
        pollingFrequencies.put("Default", Long.MAX_VALUE);
    }

    @Override
    public void setHostName(String name) {

        this.hostName.set(name);
    }

    @Override
    public String getHostName() {

        return this.hostName.get();
    }

    @Override
    public void setPortNumber(Integer port) {

        this.portNumber.set(port);
    }

    @Override
    public Integer getPortNumber() {

        return this.portNumber.get();
    }

    @Override
    public void setStartOutage(Date start) {

        this.startOutage.set(DateConverter.convertToLocalDateTime(start));
    }

    @Override
    public Date getStartOutage() {

        return DateConverter.convertToDate(this.startOutage.get());
    }

    @Override
    public void setStopOutage(Date stop, String callerName) {

        this.stopOutage.set(DateConverter.convertToLocalDateTime(stop));
        String message = String.format("Outage was set by caller %s to Start at: %s and End at: %s", callerName,
                this.getStartOutage(), this.getStopOutage());

        Notification notification = new AttributeChangeNotification(this, notificationCounter++,
                System.currentTimeMillis(), message, "Outage", "String", null, null);

        // Send notification to listeners
        super.sendNotification(notification);
    }

    @Override
    public Date getStopOutage() {

        return DateConverter.convertToDate(this.stopOutage.get());
    }

    @Override
    public void setPollFrequency(String name, Long polling) {

        pollingFrequencies.put(name, polling);
        Long minimum = pollingFrequencies.entrySet().stream().sorted(Map.Entry.comparingByValue()).findFirst().get()
                .getValue();

        if (minimum < 1) {
            this.polling.set(Long.valueOf(1));
        } else {
            this.polling.set(minimum);
        }

        this.reschedule(this.polling.get());
    }

    @Override
    public Long getPollFrequency() {

        return this.polling.get();
    }

    @Override
    public void removePollFrequency(String name) {

        if (pollingFrequencies.containsKey(name)) {

            pollingFrequencies.remove(name);

            // Rechecks for minimum frequency
            Long minimum = pollingFrequencies.entrySet().stream().sorted(Map.Entry.comparingByValue()).findFirst().get()
                    .getValue();

            if (minimum < 1) {
                this.polling.set(Long.valueOf(1));
            } else {
                this.polling.set(minimum);
            }

            this.reschedule(this.polling.get());
        }
    }

    @Override
    public void setStatus(String status) {

        // If status hasn't changed there is no need do update it,
        // least to notify listeners.
        if (this.getStatus().contentEquals(status)) {
            return;
        }

        // Temporarily stores old status to send it via notification
        String oldStatus = this.getStatus();

        // Update state to its new state
        this.status.set(status);
        String message = String.format("Status for service changed from [%s] to [%s]", oldStatus, this.getStatus());

        Notification notification = new AttributeChangeNotification(this, notificationCounter++,
                System.currentTimeMillis(), message, "Status", "String", oldStatus, this.getStatus());

        // Send notification to listeners
        super.sendNotification(notification);
    }

    @Override
    public String getStatus() {

        return this.status.get();
    }

    @Override
    public void setServiceName(String serviceName) {

        this.serviceName.set(serviceName);
    }

    @Override
    public String getServiceName() {

        return this.serviceName.get();
    }

    @Override
    public Long getGraceTimeDuration() {

        return graceTimeDuration.get();
    }

    @Override
    public void setGraceTimeDuration(Long graceTimeDuration) {

        this.graceTimeDuration.set(graceTimeDuration);
    }

    @Override
    public Boolean getGraceTimeFlag() {

        return this.graceTimeFlag.get();
    }

    @Override
    public void setGraceTimeFlag(Boolean value, String callerName) {

        Boolean tmpFlag = this.graceTimeFlag.get();
        this.graceTimeFlag.set(value);
        this.isGraceTime = value;
        this.reschedule(this.getPollFrequency());

        String message = String.format("Grace time was set by caller %s with duration of %s s for service at %s%s\n",
                callerName, this.graceTimeDuration.doubleValue() / 1000, this.hostName, this.portNumber);
        Notification notification = new AttributeChangeNotification(this, notificationCounter++,
                System.currentTimeMillis(), message, "GraceTime", "String", tmpFlag,
                String.valueOf(this.getGraceTimeFlag()));

        // Send notification to listeners
        super.sendNotification(notification);
    }

    @Override
    public void run() {

        LocalDateTime now = LocalDateTime.now();

        // Checking outage.
        if (now.isBefore(DateConverter.convertToLocalDateTime(this.getStartOutage()))
                || now.isAfter(DateConverter.convertToLocalDateTime(this.getStopOutage()))) {

            try {
                Socket clientSocket = new Socket(this.getHostName(), this.getPortNumber());

                if (clientSocket.isConnected()) {
                    // If previous condition was within grace time with extra checks,
                    // here the server is pinging and polling should return to
                    // normal.
                    if (!isNormalSchedule) {
                        this.display(String.format("Service at address [%s:%d] is RETURNING to [%d] seconds at [%s]",
                                this.getHostName(), this.getPortNumber(), this.getPollFrequency(),
                                LocalDateTime.now()));

                        this.reschedule(this.getPollFrequency());
                        isNormalSchedule = true;
                    }

                    // resets grace time flag to its original condition
                    isGraceTime = this.graceTimeFlag.get();

                    // Server is pinging
                    this.setStatus("UP");
                    this.display(String.format("Service at address [%s:%d] is [%s] at [%s]", this.getHostName(),
                            this.getPortNumber(), this.getStatus(), LocalDateTime.now()));
                }

                // Avoiding resource leaking
                clientSocket.close();

            } catch (Exception ex) {

                // Server is not pinging. Checks if grace time is on
                if (isGraceTime) {

                    // waits for grace time before notifying or not all callers
                    try {
                        Thread.sleep(this.graceTimeDuration.get());
                    } catch (InterruptedException e) {

                        e.printStackTrace();
                    } finally {

                        // Grace time should be executed only once before the server
                        // returns to respond to ping.
                        isGraceTime = false;

                        if (this.graceTimeDuration.get() / 1000 < this.getPollFrequency()) {

                            this.display(String.format(
                                    "Service at address [%s:%d] is being reschedule to [%d] seconds at [%s]",
                                    this.getHostName(), this.getPortNumber(), Long.valueOf(1), LocalDateTime.now()));
                            this.reschedule(Long.valueOf(1));
                            isNormalSchedule = false;
                        }
                    }
                    return;
                }

                // Notify callers
                this.setStatus("NOT_UP");
                this.display(String.format("Service at address [%s:%d] is [%s] at [%s]", this.getHostName(),
                        this.getPortNumber(), this.getStatus(), LocalDateTime.now()));
            }
        } else {

            // Notify callers.
            display(String.format("Service at address [%s:%d] is within outage interval", this.getHostName(),
                    this.getPortNumber()));
            this.setStatus("OUTAGE");
        }
    }

    public void display(String message) {

        log.info(message);
    }

    @Override
    public void schedule(long interval) {

        futureTask = ses.scheduleAtFixedRate(this, 0, interval, TimeUnit.SECONDS);
    }

    @Override
    public void reschedule(long interval) {

        if (interval > 0) {

            if (futureTask != null) {
                futureTask.cancel(true);
            }

            if (futureTask.isCancelled()) {
                display("Future Schedule is cancelled");
            } else {
                display("Future Schedule is NOT cancelled");
            }
        }

        futureTask = ses.scheduleAtFixedRate(this, 1, interval, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {

    }

    public static void main(String[] args) throws IOException, MalformedObjectNameException {

        // To discover registered servers..
        // 1) Define its URL
        JMXServiceURL url = new JMXServiceURL(String.format("service:jmx:rmi:///jndi/rmi://%s/jmxrmi", args[0]));
        System.out.printf("service:jmx:rmi:///jndi/rmi://%s/jmxrmi\n", args[0]);
        // 2) With the URL create connection to JMX server
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);

        // 3) Connect to the server
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

        // Query that returns all registers ServerManagers
        ObjectName queryServer = new ObjectName("ServerManager:type=TCPJMXServer,id=*");

        // Query attributes
        String[] attibutes = { "Port", "ServiceName" }; // required attributes to start monitors

        // Set with all registered servers
        Set<ObjectInstance> registeredServers = mbsc.queryMBeans(queryServer, null);

        // Map with monitor entries to be registered in JMX server
        ConcurrentMap<TCPMonitor, ObjectName> monitorMap = new ConcurrentHashMap<>();

        // Initialize ExecutorService
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(monitorMap.size());

        // Populates monitorMap
        registeredServers.stream().map(objectInstance -> {

            try {
                return createServerEntry(objectInstance, mbsc, attibutes, ses);
            } catch (MalformedObjectNameException | InstanceNotFoundException | ReflectionException | IOException e) {

                e.printStackTrace();
            }

            return null;

        }).forEach(entry -> monitorMap.put(entry.getKey(), entry.getValue()));

        // Initialize JMX server
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        // Register monitor in JMX server and initialize its execution in
        // ScheduledExecutorService
        monitorMap.forEach((k, v) -> {
            try {
                initializeMonitor(k, v, mBeanServer);
            } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException
                    | InterruptedException e) {

                e.printStackTrace();
            }
        });
    }

    // Helper function to create jmx server entries
    private static Map.Entry<TCPMonitor, ObjectName> createServerEntry(ObjectInstance obj, MBeanServerConnection mbsc,
                                                                       String[] attibutes, ScheduledExecutorService ses)
            throws MalformedObjectNameException, InstanceNotFoundException, ReflectionException, IOException {

        AttributeList attributeList = mbsc.getAttributes(obj.getObjectName(), attibutes);

        TCPMonitor monitor = new TCPMonitor(ses);

        for (Object item : attributeList) {

            String[] parameters = item.toString().split(" = ");

            switch (parameters[0]) {

                case "Port":

                    monitor.setPortNumber(Integer.valueOf(parameters[1]));
                    break;

                default:

                    System.out.println("Unknown parameter");
            }
        }

        // Default value
        monitor.setHostName("localhost");

        // Create ObjectName
        ObjectName monitorName = new ObjectName(
                String.format("MonitorManager:type=TCPMonitor,id=mAt%d", monitor.getPortNumber()));

        // Set object name in the model
        monitor.setServiceName(String.format("MonitorManager:type=TCPMonitor,id=mAt%d", monitor.getPortNumber()));

        return Map.entry(monitor, monitorName);
    }

    // Helper function to register monitor at jmx server and starts monitor
    // execution
    private static void initializeMonitor(TCPMonitor k, ObjectName v, MBeanServer mBeanServer)
            throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException,
            InterruptedException {

        mBeanServer.registerMBean(k, v);
        k.schedule(k.getPollFrequency());
    }
}
