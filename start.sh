#!/bin/bash

bold=$(tput bold)
normal=$(tput sgr0)

echo "---------------------------------------"
echo "      ${bold}Starting TCPServiceMonitors${normal}      "
echo "---------------------------------------"

init_params()
{
echo "Parameters are:"
echo -e "Number of servers..........:" $nservers 
echo -e "Initial port...............:" $port 
echo -e "Blocking coefficient.......:" $block 
echo -e "Client(s) name.............:" $cli

}

usage()
{
echo "Usage: [[-s <no. of servers>] [-p <initial port>][-b <blocking coefficient>][-c client name]]"
}

while [ "$1" != "" ]; do
    case $1 in
        -s | --servers )
        	shift
               nservers=$1
               ;;
        -p | --port )
        	shift
            	port=$1
               ;;
        -b | --blck )
        	shift
               block=$1
               ;;
        -c | --clients )
        	shift
        	cli=$1
        	;;         
        * )
          	usage
     		exit 1       	
        	        
    esac
    shift

done

# prints the initial parameters informed by the user
init_params

# checking for required jars
PWD=$(pwd) # current directory
SERVER_JAR=$PWD/tcpserver/target/tcpserver-1.0-SNAPSHOT.jar
MONITOR_JAR=$PWD/tcpmonitor/target/tcpmonitor-1.0-SNAPSHOT.jar
CLIENT_JAR=$PWD/tcpclient/target/tcpclient-1.0-SNAPSHOT.jar

SERVER_LOG=server.log
MONITOR_LOG=monitor.log
CLIENT_LOG=client.log
JCONSOLE_LOG=jconsole.log

if [ ! -f "$SERVER_JAR" ]; then
	echo "TCPServer jar file not found at:  $SERVER_JAR"
	exit
fi

if [ ! -f "$MONITOR_JAR" ]; then
	echo "TCPMonitor jar file not found at:  $MONITOR_JAR"
	exit
fi

if [ ! -f "$CLIENT_JAR" ]; then
	echo "TCPClient jar file not found at:  $CLIENT_JAR"
	exit
fi

if [[ -f "$SERVER_LOG" ]]; then
	rm -f $SERVER_LOG
fi

if [ -f "$MONITOR_LOG" ]; then
	rm -f $MONITOR_LOG
fi

if [ -f "$CLIENT_LOG" ]; then
	rm -f $CLIENT_LOG
fi

if [ -f "$JCONSOLE_LOG" ]; then
	rm -f $JCONSOLE_LOG
fi

nohup java -jar -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=9000 $SERVER_JAR -n $nservers -p $port -c $block > $SERVER_LOG 2>&1 &
SERVER_PID=$!

sleep 5

str=pong!
echo ''
echo "---------------------------------------"
echo "      ${bold}Checking if servers are UP${normal}      "
echo "---------------------------------------"

for i in $(seq $port "$(($port+$nservers-1))"); do 
	
	if [[ $(cat < /dev/tcp/127.0.0.1/$i) != "$str" ]]; then
		echo "Server at localhost:$i is DOWN"
		exit
	else
		echo "Server at localhost:$i is UP"
fi
done

nohup java -jar -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=9001 $MONITOR_JAR localhost:9000 > $MONITOR_LOG 2>&1 &
MONITOR_PID=$!

sleep 5

echo ''
echo "---------------------------------------"
echo "   ${bold}Checking i Monitors are LISTENING${normal}      "
echo "---------------------------------------"
netstat -atuln | grep LISTEN | grep :::9001 &> /dev/null
if [[ $? == 0 ]]; then
	echo "Monitors are LISTENING at localhost:9001"
else
	echo "Monitors are down. Exiting..."
	exit
fi

nohup jconsole > $JCONSOLE_LOG 2>&1 &


read -a array <<< $cli
for element in "${array[@]}"
do
    nohup java -jar $CLIENT_JAR $element localhost:9001 >> $CLIENT_LOG 2>&1 &
done

echo ''
echo "---------------------------------------"
echo "              ${bold}LOGS${normal}          "
echo "---------------------------------------"
echo ""
echo "Server log   : $PWD/$SERVER_LOG"
echo "Monitor log  : $PWD/$MONITOR_LOG"
echo "Client(s) log: $PWD/$CLIENT_LOG"
echo "JConsole log : $PWD/$JCONSOLE_LOG"
echo ""
echo ""
echo "---------------------------------------"
echo "         ${bold}PROCESSES PID's${normal}          "
echo "---------------------------------------"
echo ""
echo "Servers  : $SERVER_PID"
echo "Monitors : $MONITOR_PID"








