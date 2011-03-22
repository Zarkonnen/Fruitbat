package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.BlockingTask;
import com.metalbeetle.fruitbat.ProgressMonitor;
import java.awt.Component;
import java.awt.Frame;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import static com.metalbeetle.fruitbat.util.Misc.*;

public class Dialogs implements ProgressMonitor {
	final JDialog progressDialog;
		final SimpleProgressPanel2 progressPanel;

	Component dialogParentC;

	public Dialogs() {
		progressDialog = new JDialog((Frame) null, "Progress", /*modal*/ true);
			progressDialog.setContentPane(progressPanel = new SimpleProgressPanel2());
		progressDialog.pack();
		progressDialog.setSize(400, progressDialog.getHeight());
		progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		progressDialog.setResizable(false);
		dialogParentC = progressDialog;
	}

	public void runBlockingTask(final String taskName, final BlockingTask bt) {
		newProcess(taskName, "", -1);
		progressDialog.setLocationRelativeTo(null);
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			new Thread("Worker Thread: " + taskName) {
				@Override
				public void run() {
					if (bt.run()) {
						progressDialog.setVisible(false);
						SwingUtilities.invokeLater(new Runnable() { public void run() {
							bt.onSuccess();
						}});
					} else {
						progressDialog.setVisible(false);
						SwingUtilities.invokeLater(new Runnable() { public void run() {
							bt.onFailure();
						}});
					}

				}
			}.start();
		}});
		// NB This causes the event dispatch loop to be run inside this call, which is why we need
		// to put everything after setVisible into an invokeLater.
		progressDialog.setVisible(true);
	}

	public void newProcess(final String title, final String detail, final int numSteps) {
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			progressDialog.setTitle(title);
			progressPanel.getInfoLabel().setText(detail);
			if (numSteps > 0) { progressPanel.getProgressBar().setMaximum(numSteps); }
			progressPanel.getProgressBar().setIndeterminate(numSteps <= 0);
			progressPanel.getProgressBar().setValue(0);
		}});
	}

	public void progress(final String detail, final int step) {
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			progressPanel.getInfoLabel().setText(detail);
			progressPanel.getProgressBar().setValue(step);
			progressPanel.getProgressBar().setIndeterminate(step < 0);
		}});
	}

	public void changeNumSteps(final int numSteps) {
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			if (numSteps > 0) { progressPanel.getProgressBar().setMaximum(numSteps); }
			progressPanel.getProgressBar().setIndeterminate(numSteps <= 0);
			progressPanel.getProgressBar().setValue(0);
		}});
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

	public void handleException(Exception e, StoreFrame affectedStore) {
		e.printStackTrace();
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
