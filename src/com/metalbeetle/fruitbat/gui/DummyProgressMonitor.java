package com.metalbeetle.fruitbat.gui;

public class DummyProgressMonitor implements ProgressMonitor {
	public void hideProgressBar() {}
	public void progress(String detail, int step) {}
	public void showProgressBar(String title, String detail, int numSteps) {}
}
