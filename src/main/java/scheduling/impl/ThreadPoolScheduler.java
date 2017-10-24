package scheduling.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import operator.Operator;
import scheduling.Scheduler;
import scheduling.TaskPool;

public class ThreadPoolScheduler implements Scheduler {

	private final TaskPool<Operator<?, ?>> availableTasks;
	private final List<WorkerThread> workers = new ArrayList<>();

	public ThreadPoolScheduler(int nThreads, long interval, TimeUnit unit, TaskPool<Operator<?, ?>> availableTasks) {
		this.availableTasks = availableTasks;
		for (int i = 0; i < nThreads; i++) {
			workers.add(new WorkerThread(availableTasks, interval, unit));
		}
	}

	@Override
	public void addTasks(Collection<? extends Operator<?, ?>> tasks) {
		for (Operator<?, ?> task : tasks) {
			availableTasks.put(task);
		}
	}

	@Override
	public void startTasks() {
		for (WorkerThread workerThread : workers) {
			workerThread.activate();
			workerThread.start();
		}
		// FIXME: Observer pattern to detect thread crashes
	}

	@Override
	public void stopTasks() {
		for (WorkerThread workerThread : workers) {
			try {
				workerThread.deActivate();
				workerThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}
	}

}
