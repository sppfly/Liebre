package scheduling.impl;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import common.component.Component;
import common.util.AliasMethod;
import common.util.StatisticFilename;
import scheduling.TaskPool;
import scheduling.priority.PriorityMetric;
import scheduling.priority.PriorityMetricFactory;

public class ProbabilisticTaskPool implements TaskPool<Component> {

  protected final List<Component> tasks = new ArrayList<>();
  protected final List<Component> passiveTasks = new ArrayList<>();
  private final PriorityMetricFactory metricFactory;
  private final AtomicReference<Turn> turns;
  private final int priorityScalingFactor;
  private final CSVPrinter csv;
  private final boolean statisticsEnabled;
  private final ThreadLocal<Integer> cache = new ThreadLocal<>();
  private AtomicReference<AliasMethod> sampler = new AtomicReference<AliasMethod>(null);
  private volatile PriorityMetric metric;
  private volatile int nThreads;
  private AtomicReferenceArray<Boolean> available;
  private volatile boolean enabled;

  // FIXME: Builder
  public ProbabilisticTaskPool(PriorityMetricFactory metricFactory, int priorityScalingFactor,
      long priorityUpdateIntervalNanos) {
    this(metricFactory, priorityScalingFactor, priorityUpdateIntervalNanos, null);
  }

  public ProbabilisticTaskPool(PriorityMetricFactory metricFactory, int priorityScalingFactor,
      long priorityUpdateIntervalNanos, String statisticsFolder) {
    this.metricFactory = metricFactory;
    this.priorityScalingFactor = priorityScalingFactor;
    this.turns = new AtomicReference<Turn>(new Turn(0, System.nanoTime(),
        priorityUpdateIntervalNanos));
    // TODO: Refactor/remove
    if (statisticsFolder != null) {
      try {
        csv = new CSVPrinter(
            new FileWriter(StatisticFilename.INSTANCE.get(statisticsFolder, "taskPool", "prio")),
            CSVFormat.DEFAULT);
        this.statisticsEnabled = true;
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    } else {
      this.csv = null;
      this.statisticsEnabled = false;
    }
  }

  @Override
  public void register(Component task) {
    if (isEnabled()) {
      throw new IllegalStateException("Cannot add tasks in an enabled TaskPool!");
    }
    tasks.add(task);
  }

  @Override
  public void registerPassive(Component task) {
    if (isEnabled()) {
      throw new IllegalStateException("Cannot add tasks in an enabled TaskPool!");
    }
    tasks.add(task);
    passiveTasks.add(task);
  }

  @Override
  public Component getNext(int threadId) {

    Turn turn = turns.get();
    if (turn.isTime(threadId)) {
      updatePriorities(threadId);
      turns.set(turn.next(nThreads));
    }

    // If there is an operator already locked, run it immediately
    Integer cachedTask = cache.get();
    if (cachedTask != null) {
      cache.set(null);
      return tasks.get(cachedTask);
    }

    // Otherwise, search for unlocked probabilistically
    AliasMethod alias = sampler.get();
    while (true) {
      int k = alias.next();
      if (available.compareAndSet(k, true, false)) {
        return tasks.get(k);
      }
    }
  }

  @Override
  public void put(Component task, int threadId) {
    // If a new random choice gives us the same task, then keep running it and don't unlock it
    if (sampler.get().next() == task.getIndex()) {
      cache.set(task.getIndex());
      return;
    }
    available.set(task.getIndex(), true);
  }

  private void updatePriorities(long threadId) {
    List<Double> probabilities = metric.getPriorities(priorityScalingFactor);
    recordStatistics(probabilities, threadId);
    sampler.set(new AliasMethod(probabilities));
  }

  @Override
  public void setThreadsNumber(int activeThreads) {
    if (isEnabled()) {
      throw new IllegalStateException("Cannot set threads number when TaskPool is enabled");
    }
    this.nThreads = activeThreads;
  }

  @Override
  public void enable() {

    if (nThreads == 0) {
      throw new IllegalStateException("Thread number not set!");
    }
    // Initialize locks and operator index
    available = new AtomicReferenceArray<>(tasks.size());
    // Sort tasks according to their indexes
    tasks.sort(
        (Component t1, Component t2) -> Integer.compare(t1.getIndex(), t2.getIndex()));
    metric = metricFactory.newInstance(tasks, passiveTasks, nThreadsTotal());
    for (Component task : tasks) {
      boolean isActive = !passiveTasks.contains(task);
      // Only the active tasks can be picked for execution
      // by the task pool
      available.set(task.getIndex(), isActive);
      task.setPriorityMetric(metric);
      task.enable();
    }
    recordStatisticsHeader();
    // Initialize priorities
    updatePriorities(1);
    this.enabled = true;
  }

  @Override
  public boolean isEnabled() {
    return this.enabled;
  }

  @Override
  public void disable() {
    this.enabled = false;
    for (Component t : tasks) {
      t.disable();
    }
    if (statisticsEnabled) {
      try {
        csv.close();
      } catch (IOException e) {
        System.err
            .format("[WARN] Failed to close statistics file for TaskPool: %s%n", e.getMessage());
      }
    }
  }

  private int nThreadsTotal() {
    return nThreads + passiveTasks.size();
  }

  private void recordStatisticsHeader() {
    if (statisticsEnabled) {
      try {
        csv.printRecord(tasks);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private void recordStatistics(List<Double> probabilities, long threadId) {
    if (!isEnabled()) {
      System.err.println("[WARN] Ignoring append, TaskPool is disabled");
      return;
    }
    if (statisticsEnabled && threadId % 4 == 0) {
      try {
        csv.printRecord(probabilities);
      } catch (IOException e) {
        System.err.format("[WARN] Failed to record statistics for TaskPool: %s%n", e.getMessage());
      }
    }
  }

  private static class Turn {

    private final long ts;
    private final long threadId;
    private final long turnPeriodNanos;


    public Turn(long threadId, long ts, long turnPeriodNanos) {
      this.threadId = threadId;
      this.ts = ts;
      this.turnPeriodNanos = turnPeriodNanos;
    }

    public Turn next(int nThreads) {
      return new Turn((threadId + 1) % nThreads, System.nanoTime(), turnPeriodNanos);
    }

    public Turn skip(int nThreads) {
      return new Turn((threadId + 1) % nThreads, ts, turnPeriodNanos);
    }

    public boolean isTime(long threadId) {
      return this.threadId == threadId && (System.nanoTime() >= ts + turnPeriodNanos);
    }
  }

}
