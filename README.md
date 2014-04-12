GliderWinchj1
=============

Rudimentary demo of handling messages from a CAN gateway in java.

The CAN message format expected by this routine is asci/hex.  See 'docs/gateway_format.txt' for details.

This routine associates with github repos: GliderWinch, HostManager.

This routine connects as a client to a socket and expects ascii/hex newline-terminated lines with CAN messages, sent by a gateway, e.g. see GliderWinch.

Usage: java -jar CanDemo.jar [ip address] [port]
default: ip = 127.0.0.1 port = 32123


