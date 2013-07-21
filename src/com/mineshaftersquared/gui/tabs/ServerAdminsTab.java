package com.mineshaftersquared.gui.tabs;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.creatifcubed.simpleapi.SimpleUtils;
import com.creatifcubed.simpleapi.swing.SimpleSwingWaiter;
import com.mineshaftersquared.UniversalLauncher;
import com.mineshaftersquared.misc.MS2Utils;
import com.mineshaftersquared.misc.JavaProcessOutputRedirector;
import com.mineshaftersquared.models.MCVersion;

public class ServerAdminsTab extends JPanel {
	
	private final UniversalLauncher app;
	public static final String SERVER_NAME_TEMPLATE = "minecraft_server.%1$s.jar";
	public static final String SERVER_DOWNLOAD_TEMPLATE = String.format("https://s3.amazonaws.com/Minecraft.Download/versions/%%1$s/%1$s", SERVER_NAME_TEMPLATE);
	private JTextArea javaArgs;
	private JTextArea mcArgs;
	
	public ServerAdminsTab(UniversalLauncher app) {
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.app = app;
		this.javaArgs = null;
		this.mcArgs = null;
		
		this.add(this.createInfoPanel());
		this.add(this.createLaunchPanel());
		this.add(this.createOptionsPanel());
		//this.add(new Box.Filler(new Dimension(0, 0), new Dimension(0, Integer.MAX_VALUE), new Dimension(0, Integer.MAX_VALUE)));
	}
	
	private JPanel createInfoPanel() {
		JPanel wrapper = new JPanel();
		JPanel panel = new JPanel(new GridBagLayout());
		wrapper.setBorder(BorderFactory.createTitledBorder("Info"));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = new Insets(5, 5, 5, 5);
		c.weightx = 1;
		c.weighty = 1;
		
		JLabel info = new JLabel(
			"<html><ul>"
				+ "<li>Put server Jars in the same folder as this launcher</li>"
				+ "<li>You can start the server by command line:<ul>"
					+ "<li>java [java options, such as -Xmx2G for 2GB of RAM] -jar [mineshaftersquared.jar]</li>"
					+ "<li>[MS2 options: -server=&lt;&gt; -bukkit, or -help for all options]</li>"
					+ "<li>-mc (this tells MS2 that the rest of the arguments are for Minecraft)</li>"
					+ "<li>[Minecraft options (usually only for a few server mods)]</li>"
				+ "</ul></li>"
				+ "<li>For more information go to ms2.creatifcubed.com/server_admins.php</li>"
			+ "</ul></html>");
		
		panel.add(info, c);
		wrapper.add(panel);
		return wrapper;
	}
	
	private JPanel createLaunchPanel() {
		JPanel wrapper = new JPanel();
		JPanel panel = new JPanel(new GridBagLayout());
		wrapper.setBorder(BorderFactory.createTitledBorder("Launch"));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = new Insets(5, 5, 5, 5);
		c.ipadx = 10;
		c.weightx = 1;
		c.weighty = 1;
		
		JLabel downloadLabel = new JLabel("Download");
		final JComboBox<MCVersion> downloadableVersions = new JComboBox<MCVersion>();
		JButton download = new JButton("Download");
		JButton downloadBukkit = new JButton("Download Bukkit");
		JButton openLocalDir = new JButton("Open local folder");
		JLabel serverLabel = new JLabel("Server");
		final JComboBox server = new JComboBox();
		final JCheckBox isBukkit = new JCheckBox("Is Bukkit?");
		JButton launch = new JButton("Launch");
		JButton refresh = new JButton("Refresh");
		

		final File local = MS2Utils.getLocalDir();
		new Thread(new Runnable() {
			@Override
			public void run() {
				final MCVersion[] versions = ServerAdminsTab.this.app.versionsManager.getVersions();
				ArrayUtils.reverse(versions);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						downloadableVersions.setModel(new DefaultComboBoxModel<MCVersion>(versions));
					}
				});
			}
		}).start();
		this.refreshLocalServerJars(local, server);
		
		download.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				final MCVersion version = (MCVersion) downloadableVersions.getSelectedItem();
				if (version != null) {
					final SimpleSwingWaiter waiter = new SimpleSwingWaiter("Downloading Server", ServerAdminsTab.this.app.mainWindow());
					waiter.worker = new SimpleSwingWaiter.Worker(waiter) {
						@Override
						protected Void doInBackground() throws Exception {
							String serverDownload = String.format(SERVER_DOWNLOAD_TEMPLATE, version.versionId);
							String serverName = String.format(String.format(SERVER_NAME_TEMPLATE, version.versionId));
							FileUtils.copyURLToFile(new URL(serverDownload), new File(local, serverName));
							waiter.stdout().println("Download from " + serverDownload + " to " + serverName + " ...");
							return null;
						}
					};
					waiter.run();
				}
			}
		});
		downloadBukkit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				SimpleUtils.openLink("http://dl.bukkit.org/downloads/craftbukkit/");
			}
		});
		openLocalDir.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				try {
					Desktop.getDesktop().open(local);
				} catch (IOException ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(ServerAdminsTab.this, "Unable to open folder " + local.getAbsolutePath());
				}
			}
		});
		launch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				String serverStr = (String) server.getSelectedItem();
				String authserver = ServerAdminsTab.this.app.prefs.getString("proxy.authserver", UniversalLauncher.DEFAULT_AUTH_SERVER);
				boolean isBukkitBool = isBukkit.isSelected();
				String[] javaArgs = ServerAdminsTab.this.javaArgs();
				String[] mcArgs = ServerAdminsTab.this.mcArgs();
				Process p = MS2Utils.launchServer(local, serverStr, authserver, isBukkitBool, javaArgs, mcArgs);
				if (p == null) {
					JOptionPane.showMessageDialog(ServerAdminsTab.this, "Unable to start server. See debug tab");
				} else {
					new Thread(new JavaProcessOutputRedirector(p, "[MS2Server] %s")).start();
				}
			}
		});
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				ServerAdminsTab.this.refreshLocalServerJars(local, server);
			}
		});
		
		c.gridx = 0;
		c.gridy = 0;
		panel.add(downloadLabel, c);
		c.gridx = 1;
		panel.add(downloadableVersions, c);
		c.gridx = 2;
		panel.add(download, c);
		c.gridx = 3;
		panel.add(downloadBukkit, c);
		c.gridx = 4;
		panel.add(openLocalDir, c);
		c.gridx = 0;
		c.gridy++;
		panel.add(serverLabel, c);
		c.gridx = 1;
		panel.add(server, c);
		c.gridx = 2;
		panel.add(isBukkit, c);
		c.gridx = 3;
		panel.add(launch, c);
		c.gridx = 4;
		panel.add(refresh, c);
		
		wrapper.add(panel);
		return wrapper;
	}
	
	private JPanel createOptionsPanel() {
		JPanel wrapper = new JPanel();
		JPanel panel = new JPanel(new GridBagLayout());
		wrapper.setBorder(BorderFactory.createTitledBorder("Options"));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = new Insets(5, 5, 5, 5);
		c.ipadx = 10;
		c.weightx = 1;
		c.weighty = 1;
		
		JLabel info = new JLabel("Separate arguments with a new line");
		JLabel javaOptionsLabel = new JLabel("Java Options (e.g. -Xms2G (newline) -Xmx4G)");
		JTextArea javaOptions = new JTextArea(4, 24);
		JLabel ms2OptionsLabel = new JLabel("Mineshafter Squared Options (use -help)");
		JTextArea ms2Options = new JTextArea(4, 24);
		JLabel minecraftOptionsLabel = new JLabel("Minecraft Options (for mods)");
		JTextArea minecraftOptions = new JTextArea(4, 24);
		JButton save = new JButton("Save");
		
		this.javaArgs = javaOptions;
		this.mcArgs = minecraftOptions;
		
		javaOptions.setLineWrap(true);
		ms2Options.setLineWrap(true);
		minecraftOptions.setLineWrap(true);
		javaOptions.setText(StringUtils.join(this.app.prefs.getStringArray("server.javaargs"), System.getProperty("line.separator")));
		minecraftOptions.setText(StringUtils.join(this.app.prefs.getStringArray("server.mcargs"), System.getProperty("line.separator")));
		save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				ServerAdminsTab.this.app.prefs.setProperty("server.javaargs", ServerAdminsTab.this.javaArgs());
				ServerAdminsTab.this.app.prefs.setProperty("server.mcargs", ServerAdminsTab.this.mcArgs());
			}
		});
		
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 3;
		panel.add(info, c);
		c.gridy++;
		c.gridwidth = 1;
		panel.add(javaOptionsLabel, c);
//		c.gridx = 1;
//		panel.add(ms2OptionsLabel, c);
		c.gridx = 1;
		panel.add(minecraftOptionsLabel, c);
		c.gridx = 0;
		c.gridy++;
		panel.add(new JScrollPane(javaOptions, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), c);
//		c.gridx = 1;
//		panel.add(new JScrollPane(ms2Options, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), c);
		c.gridx = 1;
		panel.add(new JScrollPane(minecraftOptions, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), c);
		c.gridx = 1;
		c.gridy++;
		c.anchor = GridBagConstraints.FIRST_LINE_END;
		panel.add(save, c);
		
		wrapper.add(panel);
		return wrapper;
	}
	
	private void refreshLocalServerJars(File local, JComboBox server) {
		String[] serverJars = local.list(new FilenameFilter() {
			@Override
			public boolean accept(File file, String name) {
				return name.endsWith(".jar") && !name.equals(SimpleUtils.getJarPath().getName());
			}
		});
		server.setModel(new DefaultComboBoxModel(serverJars));
	}
	
	private String[] javaArgs() {
		return this.filterEmpty(this.javaArgs.getText().split(System.getProperty("line.separator")));
	}
	
	private String[] mcArgs() {
		return this.filterEmpty(this.mcArgs.getText().split(System.getProperty("line.separator")));
	}
	
	private String[] filterEmpty(String[] arr) {
		List<String> list = Arrays.asList(arr);
		Iterator<String> it = list.iterator();
		while (it.hasNext()) {
			if (it.next().isEmpty()) {
				it.remove();
			}
		};
		return list.toArray(new String[list.size()]);
	}
}
