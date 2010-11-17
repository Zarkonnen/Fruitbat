package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.ProgressMonitor;
import java.awt.Component;
import java.awt.Frame;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import static com.metalbeetle.fruitbat.util.Misc.*;

public class Dialogs implements ProgressMonitor {
	final JDialog progressDialog;
		final SimpleProgressPanel2 progressPanel;
	volatile int progressBarLevel = 0;

	Component dialogParentC;

	public Dialogs() {
		progressDialog = new JDialog((Frame) null, "Progress", /*modal*/ false);
			progressDialog.setContentPane(progressPanel = new SimpleProgressPanel2());
		progressDialog.pack();
		progressDialog.setSize(400, progressDialog.getHeight());
		progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		progressDialog.setResizable(false);
		dialogParentC = progressDialog;
	}

	public void showProgressBar(final String title, final String detail, final int numSteps) {
		SwingUtilities.invokeLater(new Runnable() { public void run() { synchronized (progressDialog) {
			progressBarLevel++;
			progressDialog.setTitle(title);
			progressPanel.getInfoLabel().setText(detail);
			if (numSteps > 0) { progressPanel.getProgressBar().setMaximum(numSteps); }
			progressPanel.getProgressBar().setIndeterminate(numSteps <= 0);
			progressPanel.getProgressBar().setValue(0);
			if (progressBarLevel == 1) {
				progressDialog.setLocationRelativeTo(null);
				progressDialog.setVisible(true);
			}
		}}});
	}

	public void progress(final String detail, final int step) {
		SwingUtilities.invokeLater(new Runnable() { public void run() { synchronized (progressDialog) {
			progressPanel.getInfoLabel().setText(detail);
			progressPanel.getProgressBar().setValue(step);
			progressPanel.getProgressBar().setIndeterminate(step < 0);
		}}});
	}

	public void hideProgressBar() {
		try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt();/*gaaaah*/ }
		SwingUtilities.invokeLater(new Runnable() { public void run() { synchronized (progressDialog) {
			if (--progressBarLevel == 0) {
				progressDialog.setVisible(false);
			}
		}}});
	}

	public void changeNumSteps(final int numSteps) {
		SwingUtilities.invokeLater(new Runnable() { public void run() { synchronized (progressDialog) {
			if (numSteps > 0) { progressPanel.getProgressBar().setMaximum(numSteps); }
			progressPanel.getProgressBar().setIndeterminate(numSteps <= 0);
			progressPanel.getProgressBar().setValue(0);
		}}});
	}

	Component dialogParent() {
		Component dpc = dialogParentC;
		if (dpc != null && dpc.isVisible()) {
			return dpc;
		} else {
			return null;
		}
	}

	public String askQuestion(String title, String question, String initialValue) {
		return (String) JOptionPane.showInputDialog(dialogParent(), question, title,
				JOptionPane.QUESTION_MESSAGE, null, null, initialValue);
	}

	public void showWarning(String type, String title, String message) {
		JOptionPane.showMessageDialog(dialogParent(), toExceptionMsg(message), title,
				JOptionPane.WARNING_MESSAGE);
	}

	public void handleException(Exception e, MainFrame affectedStore) {
		JOptionPane.showMessageDialog(dialogParent(), toExceptionMsg(getFullMessage(e)),
				"Error Message", JOptionPane.ERROR_MESSAGE);
		if (affectedStore != null) {
			affectedStore.setIsEmergencyShutdown();
			affectedStore.dispose();
		}
	}

	static String toExceptionMsg(String m) {
		String[] lines = m.split("\\n");
		StringBuilder m2 = new StringBuilder("<html>");
		int linel = 0;
		for (String l : lines) {
			for (String w : l.split(" ")) {
				if (linel > 0 && (linel + w.length()) > 80) {
					m2.append("<br>");
					linel = 0;
				}
				m2.append(w);
				m2.append(" ");
				linel = linel + w.length() + 1;
			}
			if (linel > 0) {
				m2.append("<br>");
				linel = 0;
			}
		}
		m2.append("</html>");
		return m2.toString();
	}
}
