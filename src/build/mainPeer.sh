#! /usr/bin/bash

# Script for running the main peer
# To be run in the root of the build tree

# Check number input arguments
argc=$#

if (( argc != 2 )) 
then
	echo "Usage: $0 <main_peer_address> <main_peer_port>"
	exit 1
fi

# Assign input arguments to nicely named variables

addr=$1
port=$2

# echo "java MainPeer ${addr} ${port}"
java sdist1g21.MainPeer ${addr} ${port}


