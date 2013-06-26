package com.mineshaftersquared.gui.tabs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.creatifcubed.simpleapi.SimpleISettings;
import com.creatifcubed.simpleapi.SimpleSwingWaiter;
import com.creatifcubed.simpleapi.SimpleUtils;
import com.creatifcubed.simpleapi.SimpleWaiter;
import com.mineshaftersquared.models.LocalMCVersion;
import com.mineshaftersquared.models.MCVersion;
import com.mineshaftersquared.resources.GameUpdaterProxy;
import com.mineshaftersquared.resources.MCDownloader;
import com.mineshaftersquared.resources.Utils;

public class VersionsTabPane extends AbstractTabPane {
	private final SimpleISettings prefs;
	private static final String[] COLUMNS = {"Name", "Version", "Type", "Release Date", "Location", "Status"};
	private MCVersion[] remoteMCVersions;
	private final List<LocalMCVersion> localMCVersions;
	private final JTable localVersionsTable;
	private final VersionsTableModel localVersionsTableModel;
	private final JComboBox remoteVersionsComboBox;
	public static final Logger log = Logger.getLogger(VersionsTabPane.class.getCanonicalName());

	public VersionsTabPane(SimpleISettings prefs) {
		super(new BorderLayout());
		this.prefs = prefs;
		this.localVersionsTableModel = new VersionsTableModel();
		this.remoteMCVersions = null;
		this.localMCVersions = new LinkedList<LocalMCVersion>();
		this.localVersionsTable = new JTable(this.localVersionsTableModel);
		this.localVersionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.add(new JScrollPane(this.localVersionsTable), BorderLayout.CENTER);
		JPanel remotesToolbar = new JPanel(new FlowLayout(FlowLayout.CENTER));
		this.remoteVersionsComboBox = new JComboBox(new DefaultComboBoxModel());

		JButton refreshRemotes = new JButton("Refresh");
		refreshRemotes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				VersionsTabPane.this.reloadRemoteData();
			}
		});
		JButton download = new JButton("Download");
		download.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				final Object version = VersionsTabPane.this.remoteVersionsComboBox.getSelectedItem();
				if (version == null) {
					JOptionPane.showMessageDialog(null, "Remote versions not loaded yet, please wait");
					return;
				}
				final SimpleSwingWaiter waiter = new SimpleSwingWaiter("MS2 - Downloading");
				final MCDownloader downloader = new MCDownloader();
				waiter.worker = new SimpleSwingWaiter.Worker(waiter) {
					@Override
					public Void doInBackground() {
						if (downloader.downloadVersion(MCVersion.find((String) version), new File(System.getProperty("user.dir")))) {
							waiter.doneMessage = "Download appears to have completed succesfully";
						} else {
							waiter.doneMessage = "There appears to be an error downloading the files. Please check the console";
						}
						return null;
					}
				};
				downloader.aggregate.addListener(waiter.stdout());
				waiter.run();
			}
		});
		remotesToolbar.add(new JLabel("Remote Versions"));
		remotesToolbar.add(this.remoteVersionsComboBox);
		remotesToolbar.add(download);
		remotesToolbar.add(refreshRemotes);

		JPanel localsToolbar = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton delete = new JButton("Delete");
		JButton show = new JButton("Show in Folder");
		JButton duplicate = new JButton("Duplicate");
		JButton refreshLocals = new JButton("Refresh");
		refreshLocals.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				VersionsTabPane.this.reloadLocalData();
			}
		});

		localsToolbar.add(new JLabel("Local Versions"));
		localsToolbar.add(delete);
		localsToolbar.add(show);
		localsToolbar.add(duplicate);
		localsToolbar.add(refreshLocals);

		JPanel topPanel = new JPanel(new BorderLayout());
		JButton help = new JButton("Info");
		help.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (!SimpleUtils.openLink("http://ms2.creatifcubed.com/versions_manager.php")) {
					JOptionPane.showMessageDialog(null, "Unable to open link ms2.creatifcubed.com/versions_manager.php");
				}
			}
		});
		JPanel topEastPanelContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));

		topEastPanelContainer.add(help);
		topPanel.add(remotesToolbar, BorderLayout.CENTER);
		topPanel.add(topEastPanelContainer, BorderLayout.EAST);

		this.add(topPanel, BorderLayout.NORTH);
		this.add(localsToolbar, BorderLayout.SOUTH);

		this.reloadRemoteData();
		new Thread(new Runnable() {
			@Override
			public void run() {
				log.info("Reloading versions data (versions tab)");
				VersionsTabPane.this.reloadLocalData();
			}
		}).start();
	}

	private void reloadRemoteData() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				VersionsTabPane.this.remoteVersionsComboBox.setModel(new DefaultComboBoxModel());
				VersionsTabPane.this.remoteMCVersions = MCVersion.getVersions(MCVersion.VERSIONS_LIST_URL, true);
				String[] versions = new String[VersionsTabPane.this.remoteMCVersions.length];
				for (int i = 0; i < versions.length; i++) {
					versions[i] = VersionsTabPane.this.remoteMCVersions[i].versionId;
				}
				VersionsTabPane.this.remoteVersionsComboBox.setModel(new DefaultComboBoxModel(versions));
			}
		}).start();
	}
	private void reloadLocalData() {
		this.localMCVersions.clear();
		
		SimpleUtils.addArrayToList(Utils.getLocalLocationVersions(), this.localMCVersions);
		SimpleUtils.addArrayToList(Utils.getDefaultLocationVersions(), this.localMCVersions);
		
		this.localVersionsTableModel.fireTableDataChanged();
//		SwingUtilities.invokeLater(new Runnable() {
//			@Override
//			public void run() {
//				VersionsTabPane.this.localVersionsTableModel.fireTableDataChanged();
//			}
//		});
		
	}

	private class VersionsTableModel extends AbstractTableModel {
		@Override
		public int getColumnCount() {
			return COLUMNS.length;
		}

		@Override
		public int getRowCount() {
			if (VersionsTabPane.this.remoteMCVersions != null) {
				return VersionsTabPane.this.localMCVersions.size();
			}
			return 0;
		}

		@Override
		public Object getValueAt(int row, int col) {
			LocalMCVersion version = VersionsTabPane.this.localMCVersions.get(row);
			switch (col) {
			case 0:
				return version.name;
			case 1:
				return version.versionId;
			case 2:
				return version.type.toString();
			case 3:
				return version.releaseTime.toString();
			case 4:
				return version.isLocal ? "Local" : "App Data";
			case 5:
				String[] errors = version.checkComplete();
				if (errors.length == 0) {
					return "OK";
				}
				return SimpleUtils.implode("; ", errors);
			}
			return null;
		}

		@Override
		public String getColumnName(int column) {
			return COLUMNS[column];
		}
	}
}
