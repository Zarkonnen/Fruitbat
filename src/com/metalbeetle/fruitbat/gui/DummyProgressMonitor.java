package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.BlockingTask;
import com.metalbeetle.fruitbat.ProgressMonitor;

public class DummyProgressMonitor implements ProgressMonitor {
	public void runBlockingTask(String taskName, BlockingTask bt) {
		if (bt.run()) {
			bt.onSuccess();
		} else {
			bt.onFailure();
		}
	}

	public void hideProgressBar() {}
	public void progress(String detail, int step) {}
	public void newProcess(String title, String detail, int numSteps) {}
	public void changeNumSteps(int numSteps) {}
	public void showWarning(String type, String title, String message) {
        System.err.println(type + ": " + title + ": " + message);
    }
	public void handleException(Exception e, MainFrame mf) {}
	public String askQuestion(String title, String question, String initialValue) { return initialValue; }
}
