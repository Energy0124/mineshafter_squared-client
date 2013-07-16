package com.mineshaftersquared.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import com.mineshaftersquared.UniversalLauncher;

public class MS2Proxy implements Runnable {
	
	public static final int MAX_NUM_CONNECTIONS = 16;
	public static final int TIMEOUT_DURATION = 5 * 1000;
	
	public static Pattern SKIN_URL = Pattern.compile("http://skins\\.minecraft\\.net/MinecraftSkins/(.+?)\\.png");
	public static Pattern CLOAK_URL = Pattern.compile("http://skins\\.minecraft\\.net/MinecraftCloaks/(.+?)\\.png");
	public static Pattern AUTH_URL = Pattern.compile("http://authserver\\.mojang\\.com/(.*)");
	
	public final String authserver;
	private ServerSocket server;
	private HandlerFactory handlerFactory;
	private volatile boolean shouldStop;
	private boolean hasStarted;
	private final Object isRunningLock;
	private boolean isInitialized;
	private final Object isInitializedLock;
	
	public MS2Proxy(String authserver, HandlerFactory handlerFactory) {
		this.authserver = authserver;
		this.server = null;
		this.handlerFactory = handlerFactory;
		this.shouldStop = false;
		this.hasStarted = false;
		this.isRunningLock = new Object();
		this.isInitialized = false;
		this.isInitializedLock = new Object();
	}
	
	public void startAsync() {
		try {
			this.initialize();
			new Thread(this).start();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		synchronized (this.isRunningLock) {
			if (this.hasStarted) {
				throw new IllegalStateException("MS2Proxy already running");
			}
			this.hasStarted = true;
		}
		while (true) {
			try {
				Socket s = null;
				try {
					if (this.shouldStop) {
						break;
					}
					s = this.server.accept();
					final Socket socket = s;
					final Handler handler = this.handlerFactory.createHandler(this);
					new Thread(new Runnable() {
						@Override
						public void run() {
							handler.handle(socket);
						}
					}).start();
				} catch (SocketTimeoutException acceptable) {
					//
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			} finally {
				IOUtils.closeQuietly(this.server);
			}
		}
	}
	
	public void initialize() throws IOException {
		synchronized (this.isInitializedLock) {
			if (this.isInitialized) {
				throw new IllegalStateException("MS2Proxy already initialized");
			}
			this.isInitialized = true;
		}
		this.server = new ServerSocket(0, MAX_NUM_CONNECTIONS, InetAddress.getLoopbackAddress());
		UniversalLauncher.log.info("Proxy on: " + this.server.getLocalSocketAddress().toString());
		this.server.setSoTimeout(TIMEOUT_DURATION);
	}
	
	public int getProxyPort() {
		return this.server.getLocalPort();
	}
	
	public void stopProxy() {
		this.shouldStop = true;
	}
	
	public static abstract class Handler {
		public final MS2Proxy ms2Proxy;
		public Handler(MS2Proxy ms2Proxy) {
			this.ms2Proxy = ms2Proxy;
		}
		public abstract void handle(Socket socket);
	}
	
	public static interface HandlerFactory {
		public Handler createHandler(MS2Proxy proxy);
	}
}
