package com.metalbeetle.fruitbat;

public interface BlockingTask {
	public boolean run();
	public void onSuccess();
	public void onFailure();
}
