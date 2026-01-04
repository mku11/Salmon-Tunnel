# Salmon Tunnel
Salmon Tunnel is a client-server software that let you encrypt your network communications.  
It is an alternative to SSH tunneling and works with VNC, FTP, and any other TCP related protocol.  
Provided Java and Android server/clients.  
Salmon Tunnel is currently in beta stage.  
Published under MIT License  
  
### Supports:
- AES-256 encryption in CTR Mode  
- HMAC SHA-256 authentication (data integrity)  
- SHA-256 PBKDF2 key derivation   

### Run:
Make sure you have Java installed on your machine  
For mac and linux users you will need to provide executable permissions to start.sh on the command line:  

### Server options:
The source port is the port that your service is usually running on (ie ftp:21, vnc:5900, etc).  
The target port is the tunneled port which will provide encrypt communication and can be any valid number you choose.  
Make sure you provide a password for the tunneling encryption.  
Make sure the firewall on your server does not allow connections to the server port but only to the target port.  
To run the server tunnel:  
```
salmon-tunnel.bat -sp=<source port> -tp=<target port>
```
ie: to start a tunnel on a server that is running a vnc service:  
```
salmon-tunnel.bat -sp=5900 -tp=15900
```

### Client options:
The source port is the port that you usually connect to (ie ftp:21, vnc:5900, etc).  
The target port is the tunneled port on the server and can be any valid number you choose.  
To run the server tunnel:  
```
salmon-tunnel.bat -sp=<source port> -tp=<target port>
```
ie: to connect to the vnc server above:  
```
salmon-tunnel.bat -host=192.168.1.3 -sp=5900 -tp=15900
```

Icons are provided under their own license:  
uxwing icons - https://uxwing.com  
license: https://uxwing.com/license/  