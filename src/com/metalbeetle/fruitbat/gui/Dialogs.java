package com.metalbeetle.fruitbat.gui;

import java.awt.Frame;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

public class Dialogs implements ProgressMonitor {
	String askQuestion(String title, String question, String initialValue) {
		return (String) JOptionPane.showInputDialog(null, question, title,
				JOptionPane.QUESTION_MESSAGE, null, null, initialValue);
	}

	final JDialog progressDialog;
		final ProgressPanel progressPanel;

	public Dialogs() {
		progressDialog = new JDialog((Frame) null, "Progress", /*modal*/ false);
			progressDialog.setContentPane(progressPanel = new ProgressPanel());
		progressDialog.pack();
		progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		progressDialog.setResizable(false);
	}

	public void showProgressBar(String title, String detail, int numSteps) {
		progressDialog.setTitle(title);
		progressPanel.getInfoLabel().setText(detail);
		progressPanel.getProgressBar().setMaximum(numSteps);
		progressPanel.getProgressBar().setValue(0);
		progressPanel.getProgressBar().setIndeterminate(numSteps <= 0);
		progressDialog.setLocationRelativeTo(null);
		progressDialog.setVisible(true);
	}

	public void progress(String detail, int step) {
		progressPanel.getInfoLabel().setText(detail);
		progressPanel.getProgressBar().setValue(step);
		progressPanel.getProgressBar().setIndeterminate(step <= 0);
	}

	public void hideProgressBar() {
		progressDialog.setVisible(false);
	}
}
