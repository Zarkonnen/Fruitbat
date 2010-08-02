package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.ProgressMonitor;

public class DummyProgressMonitor implements ProgressMonitor {
	public void hideProgressBar() {}
	public void progress(String detail, int step) {}
	public void showProgressBar(String title, String detail, int numSteps) {}
	public void changeNumSteps(int numSteps) {}
	public void showWarning(String type, String title, String message) {}
	public void handleException(Exception e, MainFrame mf) {}
	public String askQuestion(String title, String question, String initialValue) { return initialValue; }
}
