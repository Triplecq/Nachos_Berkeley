package nachos.threads;

import java.util.LinkedList;

import nachos.ag.BoatGrader;

public class Boat {
	static BoatGrader bg;

	public static void selfTest() {
		BoatGrader b = new BoatGrader();

		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);

		// System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		// begin(1, 2, b);
		//
		// System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		// begin(3, 3, b);
	}

	public static void begin(int adults, int children, BoatGrader b) {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here
		int totalChildren = children; // only accessible in begin and
										// isFinished()
		int totalAdults = adults; // only accessible in begin and isFinished()

		// Condition variables for problem, children and adults threads
		lck = new Lock();
		adultsWaitingOnOahu = new Condition(lck);
		childrenWaitingOnOahu = new Condition(lck);
		childrenWaitingOnMolokai = new Condition(lck);
		waitingOnBoat = new Condition(lck);
		boatProblem = new Condition(lck);

		// boat starts on Oahu
		boatLocation = "Oahu";

		// No one on boat in the beginning
		waitingChild = new LinkedList<KThread>();

		reportedChildrenOnMolokai = 0; // used by isFinished()
		reportedAdultsOnMolokai = 0; // used by isFinished()
		reportedChildrenOnOahu = 0;
		reportedAdultsOnOahu = 0;

		lastReportedChildrenOnOahu = 0;
		lastReportedAdultsOnOahu = 0;
		lastReportedChildrenOnMolokai = 0;
		lastReportedChildrenOnMolokai = 0;

		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.

		// Runnable r = new Runnable() {
		// public void run() {
		// SampleItinerary();
		// }
		// };
		// KThread t = new KThread(r);
		// t.setName("Sample Boat Thread");
		// t.fork();

		for (int i = 0; i < adults; i++) {
			Runnable r = new Runnable() {
				public void run() {
					AdultItinerary();
				}
			};
			KThread t = new KThread(r);
			t.setName("Adult Thread on Oahu");
			t.fork();
		}

		for (int j = 0; j < children; j++) {
			Runnable r = new Runnable() {
				public void run() {
					ChildItinerary();
				}
			};
			KThread t = new KThread(r);
			t.setName("Child Thread on Oahu");
			t.fork();
		}

		done = false;
		someOneWaiting = false;
		tryToGetOnBoat = false;
		onBoat = false;

		// One way communication requirement:
		// boatProblem thread can never directly or indirectly wake child or
		// adult threads.
		// However, it is fine for child or adult threads to wake boatProblem
		// thread multiple times
		lck.acquire();
		while (!isFinished(totalChildren, totalAdults)) {

			boatProblem.sleep();
		}

		done = true;
		childrenWaitingOnOahu.wakeAll();
		childrenWaitingOnMolokai.wakeAll();
		adultsWaitingOnOahu.wakeAll();
		lck.release();

		// System.out.println("End of boat test");

	}

	static void AdultItinerary() {
		/*
		 * This is where you should put your solutions. Make calls to the
		 * BoatGrader to show that it is synchronized. For example:
		 * bg.AdultRowToMolokai(); indicates that an adult has rowed the boat
		 * across to Molokai
		 */
		lck.acquire();
		reportedAdultsOnOahu++; // first time running this thread
		while (adultCase() == 0) {
			// case 0: no boat, or no child on Molokai, or a child is on boat
			if (boatLocation.equals("Oahu")) {
				childrenWaitingOnOahu.wake();
			} else {
				childrenWaitingOnMolokai.wake();
			}
			adultsWaitingOnOahu.sleep();
		}
		// case = 1
		adultRun(boatLocation); // run, update stuffs
		childrenWaitingOnMolokai.wake(); // attemp to wake up a child on Molokai
		lck.release();
	}

	static void ChildItinerary() {
		lck.acquire();
		reportedChildrenOnOahu++;
		while (!done) {
			// Case 0: No boat
			while (childCase() == 0) {
				if (boatLocation.equals("Oahu")) {
					childrenWaitingOnOahu.wake(); // tell whoever that have
													// acess to the boat to wake
													// up
					childrenWaitingOnMolokai.sleep();
				} else {
					childrenWaitingOnMolokai.wake(); // tell whoever that have
														// acess to the boat to
														// wake up
					childrenWaitingOnOahu.sleep();
				}
				adultsWaitingOnOahu.wake(); // wake up adult
			}
			// Case 1: boat ready, boat not full
			if (!done) {
				childRun(boatLocation); // run, update stuffs
				if (maybeFinished()) { // could be fake
					boatProblem.wake();
					adultsWaitingOnOahu.wake();
					childrenWaitingOnMolokai.sleep();
				} else {
					if (tryToGetOnBoat) {
						childrenWaitingOnOahu.wake();
						tryToGetOnBoat = false;
						onBoat = true;
						waitingOnBoat.sleep();
					} else {
						if (boatLocation.equals("Oahu")) {
							childrenWaitingOnOahu.wake();
							adultsWaitingOnOahu.wake();
							childrenWaitingOnOahu.sleep();
						} else {
							if (onBoat) {
								waitingOnBoat.wake();
								onBoat = false;
							}
							childrenWaitingOnMolokai.wake(); // wake up other
																// threads
							adultsWaitingOnOahu.wake();
							childrenWaitingOnMolokai.sleep(); // wait for
																// possible
																// future calls
						}
					}
				}
			}
		}
		lck.release();
	}

	static void SampleItinerary() {
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out
				.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}

	/**
	 * Prerequisite: adult is on Oahu AND there are at least one child on
	 * Molokai
	 * 
	 * @param location
	 */
	public static void adultRun(String location) {
		if (location.equals("Oahu")) {
			// still on Oahu
			reportedAdultsOnOahu--;
			lastReportedAdultsOnOahu = reportedAdultsOnOahu;
			lastReportedChildrenOnOahu = reportedChildrenOnOahu;

			bg.AdultRowToMolokai();

			// now on Molokai
			reportedAdultsOnMolokai++;
			boatLocation = "Molokai";
			KThread.currentThread().setName("Adult Thread on Molokai");

		} else {
			System.out
					.println("Adult should never leave Molokai. Error in adultCase");
		}
	}

	/**
	 * Prerequisite: child and boat is on the same island, boat is not full
	 * 
	 * @param location
	 */
	public static void childRun(String location) {
		if (location.equals("Oahu")) {
			if (!someOneWaiting) { // No one waiting, this is first child
				// Still on Oahu
				reportedChildrenOnOahu--;
				lastReportedChildrenOnOahu = reportedChildrenOnOahu;
				lastReportedAdultsOnOahu = reportedAdultsOnOahu;

				bg.ChildRowToMolokai(); // as pilot

				// Not yet on Molokai! Waiting for a second child
				KThread.currentThread().setName("Child Thread on Boat");
				someOneWaiting = true; // a child should never go alone from
										// Oahu to Molokai
				tryToGetOnBoat = true;
				waitingChild.add(KThread.currentThread());

			} else { // First child has been waiting

				// still on Oahu
				reportedChildrenOnOahu--;
				lastReportedChildrenOnOahu = reportedChildrenOnOahu;
				lastReportedAdultsOnOahu = reportedAdultsOnOahu;

				bg.ChildRideToMolokai(); // as passenger to Molokai

				// Now both children are on Molokai
				boatLocation = "Molokai";
				KThread.currentThread().setName("Child Thread on Molokai");
				KThread firstChild = waitingChild.removeFirst();
				firstChild.setName("Child Thread on Molokai");
				reportedChildrenOnMolokai = reportedChildrenOnMolokai + 2; // update
																			// for
																			// both
																			// children
				someOneWaiting = false;
			}
		} else {
			// location == Molokai; only take 1 child back to Oahu
			// still on Molokai
			reportedChildrenOnMolokai--;
			lastReportedChildrenOnMolokai = reportedChildrenOnMolokai;
			lastReportedAdultsOnMolokai = reportedAdultsOnMolokai;

			bg.ChildRowToOahu(); // pilot to Oahu

			// Now on Oahu
			reportedChildrenOnOahu++;
			KThread.currentThread().setName("Child Thread on Oahu");
			boatLocation = "Oahu";
		}
	}

	public static int adultCase() {
		if (boatLocation.equals("Oahu")) {
			if (KThread.currentThread().getName()
					.equals("Adult Thread on Oahu")
					&& lastReportedChildrenOnMolokai > 0 && !someOneWaiting) {
				// boat on Oahu, adult on Oahu, 1+ child on Molokai, no child on
				// boat => good to go
				return 1;
			} else {
				// boat on Oahu, but adult NOT on Oahu or no children on Molokai
				// => wait
				return 0;
			}
		} else {
			// boat is on Molokai => Adult never leaves Molokai
			return 0;
		}
	}

	// return 1 => Go
	// return 0 => wait
	public static int childCase() {
		if (boatLocation.equals("Oahu")) {
			if (KThread.currentThread().getName()
					.equals("Child Thread on Oahu")
					&& waitingChild.size() < 2) {
				// boat on Oahu, child on Oahu, boat is not full => Good to go
				return 1;
			} else {
				// Child is on the other bank or boat is full => can't get on
				// boat => wait
				return 0;
			}
		} else {
			// boat is on Molokai
			if (KThread.currentThread().getName()
					.equals("Child Thread on Molokai")) {
				// Child is on Molokai
				// If a child thread can get to this point, the process is not
				// yet finished.
				return 1;
			} else {
				// child not on Molokai
				return 0;
			}
		}
	}

	/**
	 * Prerequisite: Only called by begin
	 * 
	 * @param totalChildren
	 * @param totalAdults
	 * @return
	 */
	public static boolean isFinished(int totalChildren, int totalAdults) {
		if (boatLocation.equals("Molokai")
				&& reportedAdultsOnMolokai == totalAdults
				&& reportedChildrenOnMolokai == totalChildren) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Prerequisite: Child calling this method is on the same island as boat
	 * 
	 * @return
	 */
	public static boolean maybeFinished() {
		if (boatLocation.equals("Molokai") && lastReportedAdultsOnOahu == 0
				&& lastReportedChildrenOnOahu == 0) {
			return true;
		} else {
			return false;
		}
	}

	// "Globle" variables
	private static Condition adultsWaitingOnOahu;
	private static Condition childrenWaitingOnOahu;
	private static Condition childrenWaitingOnMolokai;
	private static Condition waitingOnBoat;
	private static boolean tryToGetOnBoat;
	private static boolean onBoat;
	private static Condition boatProblem;
	private static Lock lck;
	private static String boatLocation;
	private static LinkedList<KThread> waitingChild;
	private static boolean done;
	private static boolean someOneWaiting;
	// Globle transferable messages: Not necessarily accurate
	private static int lastReportedAdultsOnMolokai;
	private static int lastReportedChildrenOnMolokai;
	private static int lastReportedAdultsOnOahu;
	private static int lastReportedChildrenOnOahu;

	// accurate body count; accessible by people on Oahu only
	private static int reportedChildrenOnOahu;
	private static int reportedAdultsOnOahu;

	// accurate body count; accessible by people on Molokai only
	private static int reportedChildrenOnMolokai;
	private static int reportedAdultsOnMolokai;
}
