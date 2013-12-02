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
	}

	static void ChildItinerary() {
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
