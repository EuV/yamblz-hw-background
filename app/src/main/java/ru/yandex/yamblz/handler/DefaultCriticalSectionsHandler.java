package ru.yandex.yamblz.handler;

import android.os.Handler;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DefaultCriticalSectionsHandler implements CriticalSectionsHandler {
    private final Handler uiThreadHandler;
    private final Queue<Task> tasks = new ConcurrentLinkedQueue<>();
    private final Set<Integer> sections = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<Task, WeakReference<Runnable>> futureTasks = Collections.synchronizedMap(new WeakHashMap<>());

    public DefaultCriticalSectionsHandler(Handler uiThreadHandler) {
        this.uiThreadHandler = uiThreadHandler;
    }


    @Override
    public void startSection(int id) {
        sections.add(id);
    }


    @Override
    public void stopSection(int id) {
        sections.remove(id);
        if (sections.isEmpty()) {
            runTasks();
        }
    }


    @Override
    public void stopSections() {
        sections.clear();
        runTasks();
    }


    @Override
    public void postLowPriorityTask(Task task) {
        if (sections.isEmpty()) {
            runTask(task);
        } else {
            tasks.add(task);
        }
    }


    @Override
    public void postLowPriorityTaskDelayed(Task task, int delay) {
        if (delay <= 0) {
            postLowPriorityTask(task);
            return;
        }

        Runnable futureTask = () -> postLowPriorityTask(task);
        futureTasks.put(task, new WeakReference<>(futureTask));
        uiThreadHandler.postDelayed(futureTask, delay);
    }


    @Override
    public void removeLowPriorityTask(Task task) {
        tasks.remove(task);

        WeakReference<Runnable> refFutureTask = futureTasks.remove(task);
        if (refFutureTask != null) {
            Runnable futureTask = refFutureTask.get();
            if (futureTask != null) {
                uiThreadHandler.removeCallbacks(futureTask);
            }
        }
    }


    @Override
    public void removeLowPriorityTasks() {
        for (Task task : tasks) {
            removeLowPriorityTask(task);
        }
    }


    private void runTasks() {
        for (Task task : tasks) {
            runTask(task);
        }
    }


    private void runTask(Task task) {
        tasks.remove(task);
        uiThreadHandler.post(task::run);
    }
}
