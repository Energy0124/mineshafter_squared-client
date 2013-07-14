package com.mineshaftersquared.gui.misc;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang.StringUtils;

import com.mineshaftersquared.UniversalLauncher;
import com.mineshaftersquared.gui.tabs.ProfilesTab;
import com.mineshaftersquared.misc.MS2Utils;
import com.mineshaftersquared.models.MCProfile;

public class AddEditProfileForm extends JFrame {

	private final UniversalLauncher app;
	private final MCProfile profile;
	private final ProfilesTab profilesTab;

	private final JLabel profileNameLabel;
	private final JTextField profileName;
	private final JCheckBox isLocal;
	private final JLabel isLocalLabel;
	private final JCheckBox gameDirEnabled;
	private final JTextField gameDir;
	private final JButton gameDirOpen;
	private final JLabel versionLabel;
	private final JComboBox<String> version;
	private final JLabel javaArgsLabel;
	private final JTextArea javaArgs;

	public AddEditProfileForm(UniversalLauncher app, ProfilesTab profilesTab, MCProfile profile) {
		this.app = app;
		this.profilesTab = profilesTab;
		this.profile = profile;
		
		JPanel panel = new JPanel(new BorderLayout());

		this.profileNameLabel = new JLabel("Profile Name");
		this.profileName = new JTextField(profile == null ? "" : profile.getName());
		this.isLocal = new JCheckBox("Is Local", profile == null ? false : profile.getIsLocal());
		this.isLocalLabel = new JLabel();
		this.gameDirEnabled = new JCheckBox("Game Directory");
		String gameDirVal = null;
		try {
			System.out.println("Path:");
			System.out.println(profile == null ? MS2Utils.getDefaultMCDir() : profile.getGameDir().getAbsolutePath());
			gameDirVal = (profile == null ? MS2Utils.getDefaultMCDir() : profile.getGameDir()).getCanonicalPath();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		this.gameDir = new JTextField(gameDirVal);
		this.gameDirOpen = new JButton("Find");
		this.versionLabel = new JLabel("Version");
		this.version = new JComboBox<String>();
		this.javaArgsLabel = new JLabel("<html>Java Args<br />(separate with newline)</html>");
		this.javaArgs = new JTextArea(profile == null ? "" : StringUtils.join(profile.getJavaArgs(), System.getProperty("line.separator")), 4, 32);

		panel.add(this.createProfilePanel(), BorderLayout.CENTER);
		panel.add(this.createActionsPanel(), BorderLayout.SOUTH);
		
		this.add(panel, BorderLayout.CENTER);
		
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	private JPanel createProfilePanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Profile"));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		
		ChangeListener formChangedChangeListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				AddEditProfileForm.this.formChanged();
			}
		};
		DocumentListener formChangedDocumentListener = new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent event) {
				AddEditProfileForm.this.formChanged();
				
			}
			@Override
			public void insertUpdate(DocumentEvent event) {
				AddEditProfileForm.this.formChanged();
				
			}
			@Override
			public void removeUpdate(DocumentEvent event) {
				AddEditProfileForm.this.formChanged();
				
			}
		};
		this.profileName.getDocument().addDocumentListener(formChangedDocumentListener);
		this.isLocal.addChangeListener(formChangedChangeListener);
		this.updateIsLocalField();
		this.gameDir.getDocument().addDocumentListener(formChangedDocumentListener);
		this.gameDirOpen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				JFileChooser dirChooser = new JFileChooser(new File(gameDir.getText()));
				dirChooser.setDialogTitle("MS2 - Profile Install Location");
				dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (dirChooser.showOpenDialog(AddEditProfileForm.this) == JFileChooser.APPROVE_OPTION) {
					try {
						AddEditProfileForm.this.gameDir.setText(dirChooser.getSelectedFile().getCanonicalPath());
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		});

		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		panel.add(this.profileNameLabel, c);
		c.gridy++;
		panel.add(this.isLocal, c);
		c.gridy++;
		panel.add(this.gameDirEnabled, c);
		c.gridy++;
		panel.add(this.versionLabel, c);
		c.gridy++;
		panel.add(this.javaArgsLabel, c);

		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(this.profileName, c);
		c.gridy++;
		panel.add(this.isLocalLabel, c);
		c.gridy++;
		panel.add(this.gameDir, c);
		c.gridx = 2;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		panel.add(this.gameDirOpen, c);
		c.gridx = 1;
		c.gridy++;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(this.version, c);
		c.gridy++;
		panel.add(new JScrollPane(this.javaArgs, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), c);

		return panel;
	}

	private JPanel createActionsPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		JButton cancel = new JButton("Cancel");
		JButton save = new JButton("Save");
		
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				AddEditProfileForm.this.dispose();
			}
		});
		save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				String[] errors = AddEditProfileForm.this.saveProfile();
				if (errors.length == 0) {
					AddEditProfileForm.this.dispose();
					AddEditProfileForm.this.profilesTab.refreshProfiles();
				} else {
					JOptionPane.showMessageDialog(AddEditProfileForm.this, AddEditProfileForm.this.formatErrors(errors));
				}
			}
		});

		panel.add(cancel, BorderLayout.WEST);
		panel.add(save, BorderLayout.EAST);

		return panel;
	}

	private String[] saveProfile() {
		MCProfile profile = this.liveProfile();
		String[] errors = this.app.profileManager.validateProfile(profile);
		if (errors.length > 0) {
			return errors;
		}
		if (this.profile != null) {
			if (!this.app.profileManager.deleteProfile(this.profile)) {
				return new String[] { "Unknown error deleting old profile. Please contact the developer" };
			}
		}
		if (!this.app.profileManager.addProfile(profile)) {
			return new String[] { "Unknown error adding profile. Please contact the developer" };
		}
		try {
			if (!this.app.profileManager.saveProfiles()) {
				return new String[] { "Unknown error saving profiles. Please contact the developer" };
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			return new String[] { "Exception saving profiles: " + ex.getMessage() };
		}
		return new String[0];
	}
	
	private MCProfile liveProfile() {
		return new MCProfile(this.profileName.getText().trim(), this.version.getSelectedIndex() == 0 ? null : ((String) this.version.getSelectedItem()), new File(this.gameDir.getText()), this.javaArgs.getText().split(System.getProperty("line.separator")), this.isLocal.isSelected());
	}

	private void formChanged() {
		this.updateIsLocalField();
	}

	private String formatErrors(String[] errors) {
		String bin = "<html><ul>";
		if (errors.length == 0) {
			bin += "<li>No errors</li>";
		}
		for (String each : errors) {
			bin += "<li>" + each + "</li>";
		}
		return bin + "</html></ul>";
	}
	
	private void updateIsLocalField() {
		this.isLocalLabel.setText(this.isLocal.isSelected() ? "Profile will be installed and only available locally (good for USBs)" : "Profile will be installed in the default location and available globally");
	}
}
