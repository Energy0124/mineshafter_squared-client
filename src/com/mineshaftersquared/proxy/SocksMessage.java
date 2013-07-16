package com.mineshaftersquared.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Credits to download13 of Mineshafter
 * Modified by Adrian
 */
public abstract class SocksMessage {

	public int command;
	public InetAddress ip;
	public int port;
	public int version;
	
	public String host;
	public String user;

	public SocksMessage(int command, InetAddress ip, int port, int version) {
		this.command = command;
		this.ip = ip;
		this.port = port;
		this.version = version;
	}

	public abstract void read(InputStream paramInputStream) throws IOException;
	public abstract void write(OutputStream paramOutputStream) throws IOException;

	public String debug() {
		return "{Socks Message: {"
				+ "Command: " + this.command
				+ ", IP: " + this.ip.toString()
				+ ", Port: " + this.port
				+ ", Version: " + this.version
				+ ", Host: " + this.host
				+ ", User: " + this.user
				+ "}}";
	}
}
