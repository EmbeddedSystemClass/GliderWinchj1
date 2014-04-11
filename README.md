GliderWinchj1
=============

Rudimentary demo of handling messages from a CAN gateway in java.

See github repos: Embedded in GliderWinch are details of the CAN message format.

This routine connects as a client to a socket and expects ascii/hex newline-terminated lines with CAN messages, sent by a gateway, e.g. see GliderWinch.

Usage: java -jar CanDemo.jar <ip address> <port>

