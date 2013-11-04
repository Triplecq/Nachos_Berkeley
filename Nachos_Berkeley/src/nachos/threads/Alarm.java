package nachos.threads;

import java.util.PriorityQueue;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		// ----triplecq----
		long currentTime = Machine.timer().getTime();
		boolean intStatus = Machine.interrupt().disable();
		// wake up threads
		while (!waitQueue.isEmpty() && waitQueue.peek().waketime <= currentTime) {
			ThreadTime threadTime = waitQueue.poll();
			KThread thread = threadTime.thread;
			if (thread != null)
				thread.ready();
		}
		// tell current thread to yield
		KThread.yield();
		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x
	 *            the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// ----triplecq----
		long wakeTime = Machine.timer().getTime() + x;
		KThread thread = KThread.currentThread();
		ThreadTime threadTime = new ThreadTime(thread, wakeTime);
		boolean intStatus = Machine.interrupt().disable();
		waitQueue.add(threadTime);
		thread.sleep();
		Machine.interrupt().restore(intStatus);
	}

	// ----triplecq----
	// Inner private class implements Comparable
	private class ThreadTime implements Comparable<ThreadTime> {
		public ThreadTime(KThread thread, long waketime) {
			this.thread = thread;
			this.waketime = waketime;
		}

		public int compareTo(ThreadTime threadtime) {
			if (this.waketime > threadtime.waketime)
				return 1;
			else if (this.waketime < threadtime.waketime)
				return -1;
			else
				return 0;
		}

		private KThread thread;
		private long waketime;
	}

	private PriorityQueue<ThreadTime> waitQueue = new PriorityQueue<ThreadTime>();
}
