package ca.baosiek.controller;

import ba.baosiek.jmx.MonitorMXBean;
import ca.baosiek.model.ServiceModel;
import ca.baosiek.model.ServicesModel;
import ca.baosiek.model.TableModel;
import ca.baosiek.utils.DateConverter;
import ca.baosiek.view.CallerView;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

public class CallerController implements NotificationListener, TableModelListener {

    private CallerView view;
    private ServicesModel servicesModel;
    private final String name;
    private MBeanServerConnection mbsc;

    public CallerController(String name, MBeanServerConnection mbsc) {

        this.name = name;
        servicesModel = new ServicesModel();
        this.mbsc = mbsc;
    }

    public void setGraceTime(Duration duration) {

        servicesModel.setGraceTime(duration);
    }

    public Duration getGraceTime() {

        return servicesModel.getGraceTime();
    }

    public void setGraceTimeFlag(Boolean graceTimeFlag) {

        servicesModel.setGraceTimeFlag(graceTimeFlag);

        for (int row = 0; row < this.servicesModel.getTableModel().getRowCount(); row++) {

            // Can only set grace time for registered services
            if (this.servicesModel.getTableModel().getValueAt(row, 8).equals("UNREGISTERED")) {
                continue;
            }

            ObjectName serviceName;
            try {
                serviceName = new ObjectName((String) this.servicesModel.getTableModel().getValueAt(row, 9));
                MonitorMXBean mbeanProxy = JMX.newMBeanProxy(mbsc, serviceName, MonitorMXBean.class, true);

                this.view.log(String.format("GraceTime of %ds set to server %s\n",
                        this.servicesModel.getGraceTime().toMillis() / 1000,
                        (String) this.servicesModel.getTableModel().getValueAt(row, 9)));

                mbeanProxy.setGraceTimeDuration(this.servicesModel.getGraceTime().toMillis());
                mbeanProxy.setGraceTimeFlag(Boolean.valueOf(true), this.name);
            } catch (MalformedObjectNameException e) {

                e.printStackTrace();
            }

        }
    }

    public Boolean getGraceTimeFlag() {

        return servicesModel.getGraceTimeFlag();
    }

    public void setTableModel(TableModel tableModel) {

        servicesModel.setTableModel(tableModel);
    }

    public TableModel getTableModel() {

        return servicesModel.getTableModel();
    }

    public void register(Integer row)
            throws MalformedObjectNameException, InstanceNotFoundException, IOException, ParseException {

        ObjectName mbeanName = new ObjectName((String) this.servicesModel.getTableModel().getValueAt(row, 9));
        MonitorMXBean mbeanProxy = JMX.newMBeanProxy(mbsc, mbeanName, MonitorMXBean.class, true);
        mbsc.addNotificationListener(mbeanName, this, null, null);

        // Update status being rendered
        this.servicesModel.getTableModel().setValueAt(mbeanProxy.getStatus(), row, 8);

        // Update last modified being rendered
        this.servicesModel.getTableModel().setValueAt(DateConverter.localDateTime2String(LocalDateTime.now()), row, 6);

        // Set poling frequency
        this.setPollFrequency((Long)this.servicesModel.getTableModel().getValueAt(row, 4), row);
    }

    public void unRegister(Integer row)
            throws MalformedObjectNameException, InstanceNotFoundException, IOException, ListenerNotFoundException {

        ObjectName mbeanName = new ObjectName((String) this.servicesModel.getTableModel().getValueAt(row, 9));

        // Removes polling frequency from monitor
        MonitorMXBean mbeanProxy = JMX.newMBeanProxy(mbsc, mbeanName, MonitorMXBean.class, true);
        mbeanProxy.removePollFrequency(mbeanName.getCanonicalName());

        // Removes this caller from JMX notification list
        mbsc.removeNotificationListener(mbeanName, this, null, null);

        // Remove poling frequency
        this.removePollFrequency(row);
        this.servicesModel.getTableModel().setValueAt("UNREGISTERED", row, 8);
    }

    public void setOutage(Boolean value, int row) throws MalformedObjectNameException, ParseException {

        if (value) {

            // Does nothing for unregistered services
            if (this.servicesModel.getTableModel().getValueAt(row, 8).equals("UNREGISTERED")) {
                this.view.log(String.format("Attention! Cannot set OUTAGE on an UNREGISTERD service.\n"));
                return;
            }

            SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String start = (String) this.servicesModel.getTableModel().getValueAt(row, 2);
            String stop = (String) this.servicesModel.getTableModel().getValueAt(row, 3);
            ObjectName serviceName = new ObjectName((String) this.servicesModel.getTableModel().getValueAt(row, 9));
            MonitorMXBean mbeanProxy = JMX.newMBeanProxy(mbsc, serviceName, MonitorMXBean.class, true);
            mbeanProxy.setStartOutage(date.parse(start));
            mbeanProxy.setStopOutage(date.parse(stop), this.name);

            this.view.log(String.format("Outage is set to start at [%S] and end at [%s]\n", start, stop));
        }
    }

    public void setPollFrequency(Long frequency, int row)
            throws MalformedObjectNameException, InstanceNotFoundException, IOException {

        if (frequency <= 0) {
            this.view.log(String.format(
                    "Polling frequency is %d for registered service %s." +
                            "If there is no other caller registered you will not be seing status update\n",
                    frequency, this.servicesModel.getTableModel().getValueAt(row, 9)));
            return;
        }

        ObjectName mbeanName = new ObjectName((String) this.servicesModel.getTableModel().getValueAt(row, 9));
        MonitorMXBean mbeanProxy = JMX.newMBeanProxy(mbsc, mbeanName, MonitorMXBean.class, true);

        // Registration is required to set or update polling. If registered...
        if (((Boolean) this.servicesModel.getTableModel().getValueAt(row, 7))) {

            mbeanProxy.setPollFrequency(name, frequency);
        }
    }

    public void removePollFrequency(int row) throws MalformedObjectNameException {

        ObjectName mbeanName = new ObjectName((String) this.servicesModel.getTableModel().getValueAt(row, 9));
        MonitorMXBean mbeanProxy = JMX.newMBeanProxy(mbsc, mbeanName, MonitorMXBean.class, true);

        mbeanProxy.removePollFrequency(name);

    }

    @Override
    public void handleNotification(Notification notification, Object handback) {

        if (notification instanceof AttributeChangeNotification) {
            AttributeChangeNotification acn = (AttributeChangeNotification) notification;
            view.log(notification.getMessage() + "\n");

            if (acn.getAttributeName().contentEquals("Status")) {

                guiStatusChanged(acn.getNewValue(), acn.getOldValue(), notification.getSource().toString());
            }
        }
    }

    private void guiStatusChanged(Object newValue, Object oldValue, Object source) {

        String newStatus = (String) newValue;
        String oldStatus = (String) oldValue;
        String sourceName = (String) source;

        for (int i = 0; i < this.servicesModel.getTableModel().getRowCount(); i++) {
            if (((String) this.servicesModel.getTableModel().getValueAt(i, 9)).contentEquals(sourceName)) {
                this.servicesModel.getTableModel().setValueAt(newStatus, i, 8);
                this.servicesModel.getTableModel().setValueAt(DateConverter.localDateTime2String(LocalDateTime.now()),
                        i, 6);
                if (oldStatus.contentEquals("OUTAGE") && !newStatus.contentEquals("OUTAGE")) {
                    this.servicesModel.getTableModel().setValueAt(Boolean.valueOf(false), i, 5);
                }
                break;
            }
        }
    }

    @Override
    public void tableChanged(TableModelEvent event) {

        // Event row and column from JTable
        int row = event.getFirstRow();
        int column = event.getColumn();

        // Good practice should we have more than one JTable
        TableModel model = (TableModel) event.getSource();
        String columnName = model.getColumnName(column);
        Object data = model.getValueAt(row, column);

        switch (columnName) {

            case "Registered":
                if ((Boolean) data) {
                    try {
                        this.register(row);
                    } catch (MalformedObjectNameException | InstanceNotFoundException | IOException | ParseException e) {

                        e.printStackTrace();
                    }
                } else {
                    try {
                        this.unRegister(row);
                    } catch (MalformedObjectNameException | InstanceNotFoundException | ListenerNotFoundException
                            | IOException e) {

                        e.printStackTrace();
                    }
                }
                break;

            case "SetOutage":
                try {
                    setOutage((Boolean) data, row);
                } catch (MalformedObjectNameException | ParseException e) {

                    e.printStackTrace();
                }
                break;

            case "Poll Frequency":
                try {
                    this.setPollFrequency((Long) data, row);
                } catch (MalformedObjectNameException | InstanceNotFoundException | IOException e) {

                    e.printStackTrace();
                }

            default:

                System.out.printf("Uknown column: %s\n", columnName);
        }
        System.out.println(String.format("%s new Value is %s", columnName, data.toString()));
    }

    public void initializeView() {

        view = new CallerView(this.name, this);
        this.view.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.view.setSize(1400, 800);
        this.view.setVisible(true);
    }

    public static void main(String[] args) throws IOException, MalformedObjectNameException {

        // JMX url. Get it from command line. The string should be in <host:port> format
        // like localhost:9001
        JMXServiceURL url = new JMXServiceURL(String.format("service:jmx:rmi:///jndi/rmi://%s/jmxrmi", args[1]));

        // Connection Factory pattern
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);

        // JMX connection
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

        // Discover all TCPMonitors managed in JMX.
        // Query for searching them
        ObjectName queryMonitors = new ObjectName("MonitorManager:type=TCPMonitor,*");

        // Attributes to retrieve
        String[] attibutes = { "HostName", "PortNumber", "PollFrequency", "StartOutage", "StopOutage", "Status",
                "ServiceName" };

        // Execute query.
        Set<ObjectInstance> domains = mbsc.queryMBeans(queryMonitors, null);

        // Data structure to be encapsulated by TableModel
        Vector<ServiceModel> data = new Vector<>(domains.size());

        // Populate data with existing TCPMonitors queried from JMX
        data = domains.stream().map(obj -> {

            ServiceModel sm = new ServiceModel();
            try {

                return manipulate(obj.getObjectName(), mbsc, attibutes);
            } catch (InstanceNotFoundException | ReflectionException | IOException | ParseException e) {

                e.printStackTrace();
            }
            return sm;
        }).collect(Collectors.toCollection(Vector::new));

        // Create Table model with data
        TableModel tableModel = new TableModel(data);

        // Create controller
        CallerController controller = new CallerController(args[0], mbsc);

        // Set its model
        controller.setTableModel(tableModel);

        // Initialize its view
        controller.initializeView();
    }

    private static ServiceModel manipulate(ObjectName l, MBeanServerConnection mbsc, String[] attibutes)
            throws InstanceNotFoundException, ReflectionException, IOException, ParseException {

        // Attribute list as defined in String[] attributes.
        AttributeList attributeList = mbsc.getAttributes(l, attibutes);

        // ServicesModel in the end is a set of ServiceModel with additional types to be
        // applied to all services.
        ServiceModel serviceModel = new ServiceModel();

        for (Object attribute : attributeList) {

            String[] attributes = attribute.toString().split(" = ");

            SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);

            switch (attributes[0]) {

                case "HostName":
                    serviceModel.setHostname(attributes[1]);
                    break;

                case "PortNumber":
                    serviceModel.setPort(Integer.parseInt(attributes[1]));
                    break;

                case "PollFrequency":

                    // TCPMonitor polling is initialized with Long.MAX_VALUE
                    // Here this value is changed to zero to alert user that
                    // this attribute was not defined yet
                    if (Long.parseLong(attributes[1]) == Long.MAX_VALUE) {
                        serviceModel.setPoll(Long.valueOf(0));
                    } else {
                        serviceModel.setPoll(Long.parseLong(attributes[1]));
                    }
                    break;

                case "StartOutage":
                    Date start = formatter.parse(attributes[1]);
                    serviceModel.setStart(DateConverter.convertToLocalDateTime(start));
                    break;

                case "StopOutage":
                    Date stop = formatter.parse(attributes[1]);
                    serviceModel.setStop(DateConverter.convertToLocalDateTime(stop));
                    break;

                case "Status":
                    serviceModel.setStatus("UNREGISTERED");
                    break;

                case "ServiceName":
                    serviceModel.setServiceName(attributes[1]);
                    break;

                default:
                    System.out.printf("Attribute [%s] treated as default.\n", attributes[0]);
            }
        }

        // Default values
        serviceModel.setIsOutage(false);
        serviceModel.setIsRegistered(false);
        serviceModel.setLastUpdate(LocalDateTime.now());

        return serviceModel;
    }
}
