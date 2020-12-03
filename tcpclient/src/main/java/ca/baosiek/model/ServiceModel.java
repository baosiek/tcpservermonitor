package ca.baosiek.model;

import java.time.LocalDateTime;

public class ServiceModel {

    private String hostname;
    private Integer port;
    private LocalDateTime start;
    private LocalDateTime stop;
    private Long poll;
    private Boolean isOutage;
    private LocalDateTime lastUpdate;
    private Boolean isRegistered;
    private String status;
    private String serviceName;

    public ServiceModel() {

        this.status = "UNREGISTERED";
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getStop() {
        return stop;
    }

    public void setStop(LocalDateTime stop) {
        this.stop = stop;
    }

    public Long getPoll() {
        return poll;
    }

    public void setPoll(Long poll) {
        this.poll = poll;
    }

    public Boolean getIsOutage() {
        return isOutage;
    }

    public void setIsOutage(Boolean isOutage) {
        this.isOutage = isOutage;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Boolean getIsRegistered() {
        return isRegistered;
    }

    public void setIsRegistered(Boolean isRegistered) {
        this.isRegistered = isRegistered;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String toString() {
        return "ServiceModel [hostname=" + hostname + ", port=" + port + ", start=" + start + ", stop=" + stop
                + ", poll=" + poll + ", isOutage=" + isOutage + ", lastUpdate="
                + lastUpdate + ", isRegistered=" + isRegistered + ", status=" + status + ", serviceName=" + serviceName
                + "]";
    }
}
