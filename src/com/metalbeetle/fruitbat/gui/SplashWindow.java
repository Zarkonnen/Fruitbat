package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.ProgressMonitor;
import com.metalbeetle.fruitbat.Fruitbat;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JWindow;

public class SplashWindow extends JWindow implements ProgressMonitor {
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
	long appearance;

	int progressBarLevel = 0;

    public SplashWindow() {
		pack();
		setSize(400, 320);
		setPreferredSize(new Dimension(400, 320));
		dialogs.dialogParentC = this;
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

	public void hideProgressBar() {
		progress("", NO_BAR);
		if (--progressBarLevel == 0) {
			if (System.currentTimeMillis() - appearance < 1000) {
				try { Thread.sleep(1000 - System.currentTimeMillis() + appearance); } catch (InterruptedException e) {Thread.currentThread().interrupt();}
			}
			setVisible(false);
		}
	}

	public void progress(String detail, int step) {
		this.detail = detail;
		this.step = step;
		repaint();
	}

	public void showProgressBar(String title, String detail, int numSteps) {
		changeNumSteps(numSteps);
		this.detail = detail;
		if (progressBarLevel++ == 0) {
			setLocationRelativeTo(null);
			setVisible(true);
			appearance = System.currentTimeMillis();
		}
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
