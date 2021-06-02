#! /usr/bin/bash

# Script for running a peer
# To be run in the root of the build tree

# Check number input arguments
argc=$#

if (( argc != 6 )) 
then
	echo "Usage: $0 <peer_id> <peer_address> <peer_port> <SSLPort> <main_peer_address> <main_peer_port>"
	exit 1
fi

# Assign input arguments to nicely named variables

id=$1
addr=$2
port=$3
ssl=$4
maddr=$5
mport=$6

# Execute the program
# Should not need to change anything but the class and its package, unless you use any jar file

# echo "java Peer ${id} ${addr} ${port} ${ssl} ${maddr} ${mport}"
java sdist1g21.Peer ${id} ${addr} ${port} ${ssl} ${maddr} ${mport}


