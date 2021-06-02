# SDIS Project

SDIS Project for group T1G21

Group members:

1. Beatriz Costa Silva Mendes (up201806551@fe.up.pt)
2. Carlos Miguel Sousa Vieira (up201606868@fe.up.pt)
3. José Pedro Nogueira Rodrigues (up201708806@fe.up.pt)
4. Mariana Oliveira Ramos (up201806868@fe.up.pt)

## Instructions

In the root of the project, there is a script named `compile.sh` that can be used to compile the project. This script should be the one executed first.

Usage: `./compile.sh`

After compiling, inside the folder `src/build` are 4 additional scripts that can be used in this project:

1. `mainPeer.sh`: script used to inititialize the Main Peer of the service

Usage: `./mainPeer.sh <main_peer_address> <main_peer_port>`

2. `peer.sh`: script used to initialize a normal Peer of the service

    _Note:_ The _peer_id_ has to start in "2", "1" is reserved for the Main Peer. If they're started in the same computer, the _peer_address_ and _peer_port_ have to differ between peers. The _SSLPort_ should be the same.

Usage: `./peer.sh <peer_id> <peer_address> <peer_port> <SSLPort> <main_peer_address> <main_peer_port>`

3. `test.sh`: script used to run the different protocols

    a. Backup Usage: `./test.sh <peer_address> <peer_port> BACKUP <filename> <rep degree>`
    b. Restore Usage: `./test.sh <peer_address> <peer_port> RESTORE <filename>`
    c. Delete Usage: `./test.sh <peer_address> <peer_port> DELETE <filename>`
    d. Reclaim Usage: `./test.sh <peer_address> <peer_port> RECLAIM <max space>`
    e. Peer State Usage: `./test.sh <peer_address> <peer_port> STATE`
    f. Main Peer State Usage: `./test.sh <peer_address> <peer_port> MAINSTATE`

4. `cleanup.sh`: script used to restart the state and backed up files from a given peer, can be used to cleanup the Main Peer as well by inputting "MainPeer" as _peer_id_

Usage: `./cleanup.sh <peer_id>`

