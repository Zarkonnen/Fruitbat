package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.gui.StoreFrame;

public interface ProgressMonitor {
	public void runBlockingTask(String taskName, BlockingTask bt);

	void progress(String detail, int step);
	void newProcess(String title, String detail, int numSteps);
	void changeNumSteps(int numSteps);
	void showWarning(String type, String title, String message);
	void handleException(Exception e, StoreFrame affectedStore);

	String askQuestion(String title, String question, String initialValue);
}
