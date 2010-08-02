package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.ProgressMonitor;
import java.awt.Frame;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Dialogs implements ProgressMonitor {
	public String askQuestion(String title, String question, String initialValue) {
		return (String) JOptionPane.showInputDialog(null, question, title,
				JOptionPane.QUESTION_MESSAGE, null, null, initialValue);
	}

	final JDialog progressDialog;
		final SimpleProgressPanel2 progressPanel;

	public Dialogs() {
		progressDialog = new JDialog((Frame) null, "Progress", /*modal*/ true);
			progressDialog.setContentPane(progressPanel = new SimpleProgressPanel2());
		progressDialog.pack();
		progressDialog.setSize(400, progressDialog.getHeight());
		progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		progressDialog.setResizable(false);
	}

	public void showProgressBar(final String title, final String detail, final int numSteps) {
		showProgressBar(title, detail, numSteps, /*modal*/true);
	}

	public void showProgressBar(final String title, final String detail, final int numSteps, boolean modal) {
		if (SwingUtilities.isEventDispatchThread()) { return; }
		progressDialog.setModal(modal);
		progressDialog.setTitle(title);
		progressPanel.getInfoLabel().setText(detail);
		changeNumSteps(numSteps);
		progressDialog.setLocationRelativeTo(null);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progressDialog.setVisible(true);
			}
		});
	}

	public void progress(final String detail, final int step) {
		if (SwingUtilities.isEventDispatchThread()) { return; }
		progressPanel.getInfoLabel().setText(detail);
		progressPanel.getProgressBar().setValue(step);
		progressPanel.getProgressBar().setIndeterminate(step < 0);
	}

	public void hideProgressBar() {
		progressDialog.setVisible(false);
	}

	public void changeNumSteps(final int numSteps) {
		if (SwingUtilities.isEventDispatchThread()) { return; }
		progressPanel.getProgressBar().setMaximum(numSteps);
		progressPanel.getProgressBar().setIndeterminate(numSteps <= 0);
		progressPanel.getProgressBar().setValue(0);
	}

	public void showWarning(String type, String title, String message) {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
	}

	public void handleException(Exception e, MainFrame affectedStore) {
		String msg = (e instanceof FatalStorageException)
				? ((FatalStorageException) e).getFullMessage()
				: e.getMessage();
		JOptionPane.showMessageDialog(null,
				"<html>" + msg.replace("\n", "<br>") + "</html>",
				"Storage System Error", JOptionPane.ERROR_MESSAGE);
		if (affectedStore != null) {
			affectedStore.setIsEmergencyShutdown();
			affectedStore.dispose();
		}
	}
}
