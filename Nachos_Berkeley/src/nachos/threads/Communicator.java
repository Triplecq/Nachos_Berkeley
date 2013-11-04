package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		lock = new Lock();

		speakerWaitQueue = new Condition2(lock);
		listenerWaitQueue = new Condition2(lock);

		speakerSending = new Condition2(lock);
		listenerReceiving = new Condition2(lock);

		speakerWaiting = false;
		listenerWaiting = false;
		received = false;
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word
	 *            the integer to transfer.
	 */
	public void speak(int word) {
		lock.acquire();

		// if there are other speakers before this one
		while (speakerWaiting)
			speakerWaitQueue.sleep();

		speakerWaiting = true;
		/*
		 * Until speakerWaiting is set to false, the process below is
		 * inaccessible to other speakers
		 */
		this.word = word;

		// if there are no listeners or the word has not been received
		while (!listenerWaiting || !received) {
			listenerReceiving.wake(); // wake up a potential partner
			speakerSending.sleep(); // put this speaker to sending queue
		}

		/*
		 * At this point a listener has received a word
		 */

		// make other listeners can get to receivingQueue
		listenerWaiting = false;
		// make other speakers can get to sendingQueue
		speakerWaiting = false;
		received = false;

		speakerWaitQueue.wake(); // wake up a waiting speaker
		listenerWaitQueue.wake(); // wake up a waiting listener

		lock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		lock.acquire();

		// if there are other listeners before this one
		while (listenerWaiting)
			listenerWaitQueue.sleep();

		listenerWaiting = true;
		/*
		 * Until the listenerWaiting is set to false, the process below is
		 * inaccessible to other listeners
		 */

		// if there are no waiting speakers
		while (!speakerWaiting)
			// set this listener to receive the word first
			listenerReceiving.sleep();

		speakerSending.wake(); // wake up a speaker in sendingQueue
		received = true;
		lock.release();
		return word;
	}

	private Lock lock;
	/**
	 * queue of speakers that are waiting for listeners
	 * 
	 * queue of listeners that are waiting for speakers
	 */
	private Condition2 speakerWaitQueue;
	private Condition2 listenerWaitQueue;
	/**
	 * queue of speakers that are sending a word
	 * 
	 * queue of listeners that are waiting for a word
	 */
	private Condition2 speakerSending;
	private Condition2 listenerReceiving;
	/**
	 * boolean indicating if a speaker is waiting
	 * 
	 * boolean indicating if a listener is waiting
	 * 
	 * boolean indicating if a word has been received
	 */
	private boolean speakerWaiting;
	private boolean listenerWaiting;
	private boolean received;
	private int word;
}
