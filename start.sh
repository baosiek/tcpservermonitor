#!/bin/bash

# To make the output text more legible
bold=$(tput bold)
normal=$(tput sgr0)

# HEADER PARAMETERS
ECHO_SIZE=60
# Build LINE which is a sequence $ECHO_SIZE long of '-'
LINE=''
for i in $(seq 1 $ECHO_SIZE); do 
     	LINE=${LINE}"-"
done

# Helper method to center title within HEADER
print_header()
{
	printf "\n%s\n" $LINE
	NUM="$((($ECHO_SIZE+${#1})/2))" 
	printf "$bold%*s\n$normal" $NUM "$1"
	printf "%s\n" $LINE
}

# Assumes Java is not installed if JAVA_HOME is empty
if [[ -z $JAVA_HOME ]]; then
	echo "JAVA_HOME is not set. Exiting..."
	exit
fi

# Even finding JAVA_HOME this script further checks for the presence of java and jconsole
if ! command -v $JAVA_HOME/bin/java &> /dev/null; then
    echo "Java could not be found @ $JAVA_HOME/bin/java. Exiting..."
    exit
fi

if ! command -v $JAVA_HOME/bin/jconsole &> /dev/null; then
    echo "JConsole could not be found @ $JAVA_HOME/bin/jconsole. Exiting..."
    exit
fi

# This script makes use of netstat and nohup. Check if they are installed.
if ! command -v /usr/bin/nohup &> /dev/null; then
    echo "Command nohup could not be found @ /bin/usr. Exiting..."
    exit
fi

if ! command -v /usr/bin/netstat &> /dev/null; then
    echo "Command netstat could not be found @ /bin/usr. Exiting..."
    exit
fi

# helper method to print initial parameters
init_params()
{
echo "Parameters are:"
echo -e "Number of servers..........:" $nservers 
echo -e "Initial port...............:" $port 
echo -e "Blocking coefficient.......:" $block 
echo -e "Client(s) name.............:" $cli
}

# In case a wrong parameter is typed.
usage()
{
echo -e "Usage: [-h] [--help] -s <no. of servers> -p <initial port> -b <blocking coefficient> -c <\"client_name1 client_name2 ...\">"
}

# Checks if help is required
if [[ "$1" == "-h" ]] || [[ "$1" == "--help" ]] ; then
	usage
	exit
fi

# Checking number of arguments passed
if [ "$#" -ne 8 ]; then
    echo "Illegal number of parameters"
    usage
    exit
fi

# Checks and stores arguments if they are correct
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

print_header "Starting TCPServiceMonitors Application"

# prints the initial input parameters
init_params

# checking for required jars. Exits if any is not found.
# IS_HOME is the root directory of this project where this script is expected to be.
IS_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
SERVER_JAR=$IS_HOME/tcpserver/target/tcpserver-1.0-SNAPSHOT.jar
MONITOR_JAR=$IS_HOME/tcpmonitor/target/tcpmonitor-1.0-SNAPSHOT.jar
CLIENT_JAR=$IS_HOME/tcpclient/target/tcpclient-1.0-SNAPSHOT.jar


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


# Cleaning logs from previous runs
SERVER_LOG=server.log
MONITOR_LOG=monitor.log
CLIENT_LOG=client.log
JCONSOLE_LOG=jconsole.log


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

# Starts the servers and stores the pid
nohup $JAVA_HOME/bin/java -jar -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=9000 $SERVER_JAR -n $nservers -p $port -c $block > $SERVER_LOG 2>&1 &
	
SERVER_PID=$!

# Wait for them to start
sleep 5

# Checking if all servers are responding. Exists if not.
# RESPONSE contais server response
RESPONSE=UP

print_header "Checking if servers are UP"

for i in $(seq $port "$(($port+$nservers-1))"); do 
	
	if [[ $(cat < /dev/tcp/127.0.0.1/$i) != "$RESPONSE" ]]; then
		echo "Server at localhost:$i is DOWN"
		exit
	else
		echo "Server at localhost:$i is UP"
fi
done

# Starting monitors and stores the pid
nohup $JAVA_HOME/bin/java -jar -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=9001 $MONITOR_JAR localhost:9000 > $MONITOR_LOG 2>&1 &
MONITOR_PID=$!

# Waits for them to start
sleep 5


# Proxy for checking if JMXMonitors are listening ate 9001.
print_header "Checking if Monitors are LISTENING$"

NET_STATUS=$(netstat -atuln | grep LISTEN | grep :::9001)

if [[ -n $NET_STATUS ]]; then
	echo "Monitors are LISTENING at localhost:9001"
else
	echo "Monitors are down. Exiting..."
	# For convenience here this script kills server process
	KILL=$(ps aux | grep $SERVER_PID)
	
	if [[ -n $KILL ]]; then
		kill -9 $SERVER_PID		
	fi
		
	exit
fi

# Starts JConsole
nohup $JAVA_HOME/bin/jconsole > $JCONSOLE_LOG 2>&1 &
JCONSOLE_PID=$!

# Starts all clients
read -a array <<< $cli
pids=()
for element in "${array[@]}"
do
    nohup $JAVA_HOME/bin/java -jar $CLIENT_JAR $element localhost:9001 >> $CLIENT_LOG 2>&1 &
    pids+=($!)
done

# Prints informations that may be required
# JConsole and Clients are run in maximized windows and thus
# can be easily tracked within it or via JConsole itself.

print_header "LOGS"

echo "Server log   : $PWD/$SERVER_LOG"
echo "Monitor log  : $PWD/$MONITOR_LOG"
echo "Client(s) log: $PWD/$CLIENT_LOG"
echo "JConsole log : $PWD/$JCONSOLE_LOG"
echo ""

print_header "PROCESSES PID's"

echo "Servers  : $SERVER_PID"
echo "Monitors : $MONITOR_PID"
echo "JConsole : $JCONSOLE_PID"

# Prints footer with clients PIDs
printf "\n$bold%-20s%20s\n$normal" "Client Name" "PID"
echo "----------------------------------------"
for i in $(seq 0 "$((${#array[@]}-1))"); do 
	printf "%-20.20s%20d\n" ${array[i]} ${pids[i]}
done








