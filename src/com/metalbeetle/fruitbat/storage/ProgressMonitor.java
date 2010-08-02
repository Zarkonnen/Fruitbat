package com.metalbeetle.fruitbat.storage;

import com.metalbeetle.fruitbat.gui.MainFrame;

public interface ProgressMonitor {
	void hideProgressBar();
	void progress(String detail, int step);
	void showProgressBar(String title, String detail, int numSteps);
	void changeNumSteps(int numSteps);
	void showWarning(String type, String title, String message);
	void handleException(Exception e, MainFrame affectedStore);

	String askQuestion(String title, String question, String initialValue);
}
