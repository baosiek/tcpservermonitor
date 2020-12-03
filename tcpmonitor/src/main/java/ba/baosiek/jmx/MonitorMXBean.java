package ba.baosiek.jmx;

import java.util.Date;

public interface MonitorMXBean {

    void setHostName(String name);
    String getHostName();
    void setPortNumber(Integer port);
    Integer getPortNumber();
    void setStartOutage(Date start);
    Date getStartOutage();
    void setStopOutage(Date stop, String callerName);
    Date getStopOutage();
    void setPollFrequency(String name, Long polling);
    Long getPollFrequency();
    void removePollFrequency(String name);
    void setStatus(String status);
    String getStatus();
    void setServiceName(String serviceName);
    String getServiceName();
    Long getGraceTimeDuration();
    void setGraceTimeDuration(Long graceTimeDuration);
    Boolean getGraceTimeFlag();
    void setGraceTimeFlag(Boolean value, String callerName);
}
