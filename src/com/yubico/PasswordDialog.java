package com.yubico;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

public class PasswordDialog {
	final JPasswordField jpf = new JPasswordField();

	public PasswordDialog() {
		JOptionPane jop = new JOptionPane(jpf, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = jop.createDialog(null, "Password:");
		dialog.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						jpf.requestFocusInWindow();
					}
				});
			}
		});
		dialog.addWindowListener(new WindowAdapter() {
			public void windowActivated(WindowEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						jpf.requestFocusInWindow();
					}
				});
			}
		});

		dialog.setVisible(true);
		int result = (Integer) jop.getValue();
		dialog.dispose();
		char[] password = null;
		if (result == JOptionPane.OK_OPTION) {
			password = jpf.getPassword();
			String otp = new String(password);
			YubicoClient yc = new YubicoClient(YubicoClient.JIM_AUTH_ID);
			if (yc.verify(otp)) {
				System.out.println("\n* OTP verified OK");
			} else {
				System.out.println("\n* Failed to verify OTP");
			}

			System.out.println("\n* Last response:\n" + yc.getLastResponse());
		}
	}

	public static void main(String args[]) throws Exception {
		PasswordDialog passwordDialog = new PasswordDialog();
	}
}
