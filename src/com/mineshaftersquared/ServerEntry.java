package com.mineshaftersquared;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;

import sun.applet.Main;

import com.creatifcubed.simpleapi.SimpleAggregateOutputStream;
import com.creatifcubed.simpleapi.swing.SimpleGUIConsole;
import com.creatifcubed.simpleapi.swing.SimpleSwingUtils;
import com.mineshaftersquared.misc.ExtendedGnuParser;
import com.mineshaftersquared.misc.MS2Utils;
import com.mineshaftersquared.proxy.MS2Proxy;
import com.mineshaftersquared.proxy.MS2ProxyHandlerFactory;

public class ServerEntry {
	
	public static final Dimension SERVER_CONSOLE_DIMENSIONS = new Dimension(854, 480);

	public static final Options options = new Options() {{
		addOption(MS2Entry.serverOption);
		addOption(MS2Entry.bukkitOption);
		addOption(MS2Entry.authserverOption);
		addOption(MS2Entry.mcSeparatorOption);
	}};

	public static void main(String[] args) throws ParseException {
		CommandLineParser cmdParser = new ExtendedGnuParser();
		CommandLine cmd = cmdParser.parse(options, args);

		String server = cmd.getOptionValue("server");
		boolean isBukkit = cmd.hasOption("bukkit");
		String authserver = cmd.getOptionValue("authserver", UniversalLauncher.DEFAULT_AUTH_SERVER);

		List<String> mcArgs = new LinkedList<String>();
		boolean mcArgsFlag = false;
		for (String each : args) {
			if (mcArgsFlag) {
				mcArgs.add(each);
			} else {
				if (each.equals("-mc")) {
					mcArgsFlag = true;
				}
			}
		}

		MS2Proxy proxy = new MS2Proxy(new MS2Proxy.MS2RoutesDataSource(authserver), new MS2ProxyHandlerFactory());
		proxy.startAsync();

		System.setProperty("http.proxyHost", InetAddress.getLoopbackAddress().getHostAddress());
		System.setProperty("http.proxyPort", "" + proxy.getProxyPort());
		
		if (isBukkit) {
			final SimpleGUIConsole console = new SimpleGUIConsole();
			console.init();
			System.setOut(new PrintStream(new SimpleAggregateOutputStream(System.out, console.getOut())));
			System.setErr(new PrintStream(new SimpleAggregateOutputStream(System.err, console.getErr())));
			System.setIn(console.getIn());
			SimpleSwingUtils.setAutoscroll(console.getOutputField(), true);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					final JFrame frame = new JFrame("MS2 Server Console");
					frame.setContentPane(console.getCompleteConsole());
					frame.setPreferredSize(SERVER_CONSOLE_DIMENSIONS);
					frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
					frame.addWindowListener(new WindowAdapter() {
						@Override
						public void windowClosing(WindowEvent event) {
							if (JOptionPane.showConfirmDialog(frame, "Are you sure you want to force quit?") == JOptionPane.YES_OPTION) {
								System.exit(0);
							}
						}
					});
					frame.pack();
					frame.setVisible(true);
				}
			});
			mcArgs.add("--nojline");
		}
		launchServer(server, mcArgs.toArray(new String[mcArgs.size()]));
	}
	
	private static void launchServer(String server, String[] mcArgs) {
		try {
			Attributes attributes = null;
			String mainClassName = null;
			JarFile jar = new JarFile(server);
			Class<?> clazz = null;
			URLClassLoader cl = new URLClassLoader(new URL[] { new File(server).toURI().toURL() }, Main.class.getClassLoader());
			try {
				attributes = jar.getManifest().getMainAttributes();
				mainClassName = attributes.getValue("Main-Class");
			} finally {
				IOUtils.closeQuietly(jar);
			}
			clazz = cl.loadClass(mainClassName);
			UniversalLauncher.log.info("Starting class " + mainClassName + " ... Passing args " + Arrays.asList(mcArgs) + " ...");
			Method main = clazz.getDeclaredMethod("main", new Class[] { String[].class });
			main.invoke(clazz, new Object[] { mcArgs });
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		} catch (NoSuchMethodException ex) {
			ex.printStackTrace();
		} catch (SecurityException ex) {
			ex.printStackTrace();
		} catch (IllegalAccessException ex) {
			ex.printStackTrace();
		} catch (IllegalArgumentException ex) {
			ex.printStackTrace();
		} catch (InvocationTargetException ex) {
			ex.printStackTrace();
		}
	}
}
