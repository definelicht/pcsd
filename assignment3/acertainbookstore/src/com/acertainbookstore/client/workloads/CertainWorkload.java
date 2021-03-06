/**
 *
 */
package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ConcurrentCertainBookStore;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 *
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the environment for the workers using WorkloadConfiguration
 * objects and reports the metrics
 *
 */
public class CertainWorkload {

	public static final int INITIAL_BOOKS_IN_STORE = 50;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		int numConcurrentWorkloadThreads;
		if (args.length > 0) {
			numConcurrentWorkloadThreads = Integer.parseInt(args[0]);
		} else {
			numConcurrentWorkloadThreads = 10;
		}
		System.out.println("Running benchmark with " + numConcurrentWorkloadThreads
		                   + " worker threads.");
		String serverAddress = "http://localhost:8081";
		boolean localTest = true;
		List<WorkerRunResult> workerRunResults = new ArrayList<WorkerRunResult>();
		List<Future<WorkerRunResult>> runResults =
		    new ArrayList<Future<WorkerRunResult>>();

		// Initialize the RPC interfaces if its not a localTest, the variable is
		// overriden if the property is set
		String localTestProperty = System
				.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
		localTest = (localTestProperty != null) ? Boolean
				.parseBoolean(localTestProperty) : localTest;

		BookStore bookStore = null;
		StockManager stockManager = null;
		if (localTest) {
			ConcurrentCertainBookStore store = new ConcurrentCertainBookStore();
			bookStore = store;
			stockManager = store;
		} else {
			stockManager = new StockManagerHTTPProxy(serverAddress + "/stock");
			bookStore = new BookStoreHTTPProxy(serverAddress);
		}

		// Generate data in the bookstore before running the workload
		initializeBookStoreData(bookStore, stockManager);

		ExecutorService exec = Executors
				.newFixedThreadPool(numConcurrentWorkloadThreads);

		for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
			WorkloadConfiguration config = new WorkloadConfiguration(bookStore,
					stockManager);
			Worker workerTask = new Worker(config);
			// Keep the futures to wait for the result from the thread
			runResults.add(exec.submit(workerTask));
		}

		// Get the results from the threads using the futures returned
		for (Future<WorkerRunResult> futureRunResult : runResults) {
			WorkerRunResult runResult = futureRunResult.get(); // blocking call
			workerRunResults.add(runResult);
		}

		exec.shutdownNow(); // shutdown the executor

		// Finished initialization, stop the clients if not localTest
		if (!localTest) {
			((BookStoreHTTPProxy) bookStore).stop();
			((StockManagerHTTPProxy) stockManager).stop();
		}

		reportMetric(workerRunResults);
	}

	/**
	 * Computes the metrics and prints them
	 *
	 * @param workerRunResults
	 */
	public static void reportMetric(List<WorkerRunResult> workerRunResults) {
		int runs = 0, successful = 0, frequentSuccessful = 0;
		double throughput = 0, latency = 0;
		for (WorkerRunResult result : workerRunResults) {
			runs += result.getTotalRuns();
			successful += result.getSuccessfulInteractions();
			frequentSuccessful +=
			    result.getSuccessfulFrequentBookStoreInteractionRuns();
 			throughput += (double)result.getSuccessfulInteractions() /
			              ((double)result.getElapsedTimeInNanoSecs()*1e-9);
			latency += ((double)result.getElapsedTimeInNanoSecs()*1e-9) /
								  (double)result.getSuccessfulInteractions();
		}
		assert((float)successful / (float)runs > 0.99);
		assert(Math.abs((float)frequentSuccessful / (float)successful - .6) < .05);
		latency /= (double)workerRunResults.size();
		System.out.println("Throughput: " + throughput + "\nLatency: " + latency);
	}

	/**
	 * Generate the data in bookstore before the workload interactions are run
	 *
	 * Ignores the serverAddress if its a localTest
	 *
	 */
	public static void initializeBookStoreData(BookStore bookStore,
			StockManager stockManager) throws BookStoreException {

		// The BookSetGenerator should really be instantiated for the entire test,
		// but since these methods are static this cannot be done without adding
		// mutable static members or changing the signature of this function
		BookSetGenerator generator = new BookSetGenerator();
		stockManager.addBooks(
		  generator.nextSetOfStockBooks(INITIAL_BOOKS_IN_STORE)
		);

	}
}
