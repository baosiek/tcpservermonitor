# TCP SERVER MONITORS

TCP Server monitored through JMX. This is a three tier application (server layer, monitor layer and client layer) that demonstrates the use of various Java's Concurrency (Multi-thread) API features.

A video tutorial of this app can be seen at https://vimeo.com/465979865

After cloning this repository go to it's root directory and build the application with **mvn clean install"**.

You will find the .jar files at:
Server : your_home_directory/tcpserver/target/tcpserver-1.0-SNAPSHOT.jar
Monitor: your_home_directory/tcpmonitor/target/tcpmonitor-1.0-SNAPSHOT.jar
Client : your_home_directory/tcpclient/target/tcpclient-1.0-SNAPSHOT.jar

A bash scrip start.sh starts everything that is necessary to playing with the application.
To initialize three servers and two clients (Vancouver and Toronto) execute the following:
./start.sh -s 3 -p 8000 -b 0.5 -c "Vancouver Toronto"

First argument -s is the number of servers to start: 3 (in the example above)
Second is the initial port where those above servers will start listening: 8000 (in the example above. Servers will listen to ports 8000, 8001, 8002)
Third is the blocking coefficient to calculate the number of threads
Fourth the name of the clients separated with space
