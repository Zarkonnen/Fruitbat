package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.BlockingTask;
import com.metalbeetle.fruitbat.ProgressMonitor;
import com.metalbeetle.fruitbat.Fruitbat;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

public class SplashWindow extends JDialog implements ProgressMonitor {
	static final Image SPLASH_IMG;
	static final int NO_BAR = -1001;
	final Dialogs dialogs = new Dialogs();

	static {
		Image img = null;
		try {
			img = ImageIO.read(SplashWindow.class.getResourceAsStream("splash.png"));
		} catch (IOException e) {}
		SPLASH_IMG = img;
	}

	String detail;
	int numSteps;
	int step;

    public SplashWindow() {
		setModal(true);
		setUndecorated(true);
		pack();
		setSize(400, 320);
		setPreferredSize(new Dimension(400, 320));
		dialogs.dialogParentC = this;
    }

	public void runBlockingTask(final String taskName, final BlockingTask bt) {
		newProcess(taskName, "", -1);
		setLocationRelativeTo(null);
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			new Thread("Worker Thread: " + taskName) {
				@Override
				public void run() {
					if (bt.run()) {
						setVisible(false);
						SwingUtilities.invokeLater(new Runnable() { public void run() {
							bt.onSuccess();
						}});
					} else {
						setVisible(false);
						SwingUtilities.invokeLater(new Runnable() { public void run() {
							bt.onFailure();
						}});
					}
				}
			}.start();
		}});
		// NB This causes the event dispatch loop to be run inside this call, which is why we need
		// to put everything after setVisible into an invokeLater.
		setVisible(true);
	}

	@Override
	public void paint(Graphics g) {
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 400, 320);
		if (SPLASH_IMG != null) {
			g.drawImage(SPLASH_IMG, 0, 0, this);
		}
		g.setColor(Color.GRAY);
		int w = g.getFontMetrics().stringWidth(Fruitbat.VERSION);
		g.drawString(Fruitbat.VERSION, 400 - w - 10, 300);
		if (step != NO_BAR) {
			g.fillRect(10, 280, 380, 4);
			g.setColor(Color.BLACK);
			if (numSteps > 0) {
				g.fillRect(10, 280, 380 * step / numSteps, 4);
			}
			g.drawString(detail, 10, 300);
		}
	}

	public void progress(String detail, int step) {
		this.detail = detail;
		this.step = step;
		repaint();
	}

	public void newProcess(String title, String detail, int numSteps) {
		changeNumSteps(numSteps);
		this.detail = detail;
		setTitle(title);
	}

	public void changeNumSteps(int numSteps) {
		this.numSteps = numSteps;
		this.step = 0;
		repaint();
	}

	public void showWarning(String type, String title, String message) {
		toBack();
		dialogs.showWarning(type, title, message);
	}

	public void handleException(Exception e, MainFrame affectedStore) {
		toBack();
		dialogs.handleException(e, affectedStore);
	}

	public String askQuestion(String title, String question, String initialValue) {
		toBack();
		return dialogs.askQuestion(title, question, initialValue);
	}
}
