package ca.baosiek.jmx;

/**
 * Interface to enable basic server operations like suspend, resume and stop.
 * It also enables getting this server name and port.
 */
public interface TCPJMXServerMXBean {

    public void suspend();
    public void resume();
    public void stop();
    public void start();
    public Integer getPort();
    public String getServerName();
}
