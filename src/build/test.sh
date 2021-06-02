#! /usr/bin/bash

# Script for running the test app
# To be run at the root of the compiled tree

# Check number input arguments
argc=$#

if (( argc < 3 )) 
then
	echo "Usage: $0 <peer_address> <peer_port> BACKUP|RESTORE|DELETE|RECLAIM|STATE|MAINSTATE [<opnd_1> [<optnd_2]]"
	exit 1
fi

# Assign input arguments to nicely named variables

addr=$1
port=$2
oper=$3

# Validate remaining arguments 

case $oper in
BACKUP)
	if(( argc != 5 )) 
	then
		echo "Usage: $0 <peer_address> <peer_port> BACKUP <filename> <rep degree>"
		exit 1
	fi
	opernd_1=$4
	rep_deg=$5
	;;
RESTORE)
	if(( argc != 4 ))
	then
		echo "Usage: $0 <peer_address> <peer_port> RESTORE <filename>"
	fi
	opernd_1=$4
	rep_deg=""
	;;
DELETE)
	if(( argc != 4 ))
	then
		echo "Usage: $0 <peer_address> <peer_port> DELETE <filename>"
		exit 1
	fi
	opernd_1=$4
	rep_deg=""
	;;
RECLAIM)
	if(( argc != 4 ))
	then
		echo "Usage: $0 <peer_address> <peer_port> RECLAIM <max space>"
		exit 1
	fi
	opernd_1=$4
	rep_deg=""
	;;
STATE)
	if(( argc != 3 ))
	then
		echo "Usage: $0 <peer_address> <peer_port> STATE"
		exit 1
	fi
	opernd_1=""
	rep_deg=""
	;;
MAINSTATE)
	if(( argc != 3 ))
	then
		echo "Usage: $0 <peer_address> <peer_port> MAINSTATE"
		exit 1
	fi
	opernd_1=""
	rep_deg=""
	;;
*)
	echo "Usage: $0 <peer_address> <peer_port> BACKUP|RESTORE|DELETE|RECLAIM|STATE|MAINSTATE [<opnd_1> [<optnd_2]]"
	exit 1
	;;
esac

# Execute the program
# Should not need to change anything but the class and its package, unless you use any jar file

# echo "java test.TestApp ${pap} ${oper} ${opernd_1} ${rep_deg}"

java sdist1g21.TestApp ${addr} ${port} ${oper} ${opernd_1} ${rep_deg}
