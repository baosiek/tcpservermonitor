# TCP SERVER MONITORS

TCP Server monitored through JMX. This is a three tier application (server layer, monitor layer and client layer) that demonstrates the use of various Java's Concurrency (Multi-thread) API features.

A video tutorial of this app can be seen at https://vimeo.com/465979865

After cloning this repository go to it's root directory and build the application with <bold>"mvn clean install"</bold>

Download tcpserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar from: https://github.com/baosiek/tcpserver/raw/master/target/tcpserver-1.0.0-jar-with-dependencies.jar

Or go to target directory and download from there

To run copy/past the bellow command:

java -jar -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=9000 tcpserver-1.0.0-jar-with-dependencies.jar 3 8000

Above there are 2 arguments that are passed to the application:

First is the number of servers to start: 3 (in the example above)
Second is the initial port where those above servers will start listening: 8000 (in the example above. Servers will listen to ports 8000, 8001, 8002)
