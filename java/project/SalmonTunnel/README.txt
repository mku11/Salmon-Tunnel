Salmon Tunnel
version: 1.0.1
project: https://github.com/mku11/Salmon-Tunnel
license: MIT License https://github.com/mku11/Salmon-Tunnel/blob/main/LICENSE

Salmon Tunnel is a client-server software that let you encrypt your network communications.
It is an alternative to SSH tunneling and works with VNC, FTP, and any other TCP related protocol.

Supports:
Password-Based authentication.
Message Authentication (data integrity).
Provided Java and Android server/clients.
Salmon Tunnel is currently in beta stage.

Run:
Make sure you have Java installed on your machine
For mac and linux users you will need to provide executable permissions to start.sh on the command line:

Server options:
The source port is the port that your service is usually running on (ie ftp:21, vnc:5900, etc).
The target port is the tunneled port which will provide encrypt communication and can be any valid number you choose.
Make sure you provide a password for the tunneling encryption.
Make sure the firewall on your server does not allow connections to the server port but only to the target port.
To run the server tunnel:
salmon-tunnel.bat -sp=<source port> -tp=<target port>
ie: to start a tunnel on a server that is running a vnc service:
salmon-tunnel.bat -sp=5900 -tp=15900

Client options:
The source port is the port that you usually connect to (ie ftp:21, vnc:5900, etc).
The target port is the tunneled port on the server and can be any valid number you choose.
To run the server tunnel:
salmon-tunnel.bat -sp=<source port> -tp=<target port>
ie: to connect to the vnc server above:
salmon-tunnel.bat -host=192.168.1.3 -sp=5900 -tp=15900

Open source projects included:
salmon-core
project: https://github.com/mku11/Salmon-AES-CTR
license: MIT License https://github.com/mku11/Salmon-AES-CTR/blob/main/LICENSE

Icons:
"uxwing icons - https://uxwing.com\n\n" +
license: https://uxwing.com/license/

Build
To build the app you will need:  
1. Intellij IDEA.
2. Gradle

Run the build task from gradle instead of the Intellij IDEA. This will include the native library.
Alternatively you can build from the command line:
gradlew.bat build -x test --rerun-tasks

To refresh development packages make sure you delete the salmon packages in the cache:
C:\Users\<username>\.gradle\caches\modules-2\files-2.1\com.mku.salmon.*
Then refresh the gradle dependencies from the IDE or from command line:
gradlew.bat --refresh-dependencies

To run/debug the app from within the IDE open gradle tab and run the task "runApp" under Application. This will ensure that the salmon native library is loaded.

Package:
To package the app build the artifacts from Intellij IDEA.
