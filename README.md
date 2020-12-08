# TCP SERVER MONITORS

TCP Server monitored through JMX. This is a three tier application (server layer, monitor layer and client layer) that demonstrates the use of various Java's Concurrency (Multi-thread) API features.

A video tutorial of this app can be seen at https://vimeo.com/465979865

After cloning this repository go to it's root directory and build the application with maven.
Use the following commnads:

git clone https://github.com/baosiek/tcpservermonitor.git
cd tcpservermonitor
mvn clean install

You will find the .jar files at:
Server : your_home_directory/tcpserver/target/tcpserver-1.0-SNAPSHOT.jar
Monitor: your_home_directory/tcpmonitor/target/tcpmonitor-1.0-SNAPSHOT.jar
Client : your_home_directory/tcpclient/target/tcpclient-1.0-SNAPSHOT.jar

A bash scrip named start.sh starts everything that is necessary to playing with this application.
Before running it for the first time you may have to assign to it execute priviledge by typing:
sudo chmod u+x start.sh 
To initialize three servers and two clients (Vancouver and Toronto) execute the following:
**./start.sh -s 3 -p 8000 -b 0.5 -c "Vancouver Toronto"**, where:

- First argument **-s** is the number of servers to be started: 3 (in the example above)
- Second **-p** is the initial port where those above servers will start listening: 8000 (in the example above. Servers will listen to ports 8000, 8001, 8002)
- Third **-b** is the blocking coefficient to calculate the number of threads
- Fourth **-c** is the name of the clients separated with space
