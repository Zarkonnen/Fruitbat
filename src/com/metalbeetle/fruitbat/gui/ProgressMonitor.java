package com.metalbeetle.fruitbat.gui;

public interface ProgressMonitor {

	void hideProgressBar();

	void progress(String detail, int step);

	void showProgressBar(String title, String detail, int numSteps);

}
