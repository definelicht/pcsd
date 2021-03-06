package com.acertainbookstore.business;

import java.util.Set;

import com.acertainbookstore.business.ReplicationResult;
import com.acertainbookstore.interfaces.ReplicatedReadOnlyBookStore;
import com.acertainbookstore.interfaces.ReplicatedReadOnlyStockManager;
import com.acertainbookstore.interfaces.Replication;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreMessageTag;
import com.acertainbookstore.utils.BookStoreResult;

/**
 * SlaveCertainBookStore is a wrapper over the CertainBookStore class and
 * supports the ReplicatedReadOnlyBookStore and ReplicatedReadOnlyStockManager
 * interfaces
 *
 * This class must also handle replication requests sent by the master
 *
 */
public class SlaveCertainBookStore implements ReplicatedReadOnlyBookStore,
		ReplicatedReadOnlyStockManager, Replication {

	private final CertainBookStore bookStore;
	private long snapshotId = 0;

	public SlaveCertainBookStore() {
		bookStore = new CertainBookStore();
	}

	public synchronized BookStoreResult getBooks() throws BookStoreException {
		BookStoreResult result = new BookStoreResult(bookStore.getBooks(),
				snapshotId);
		return result;
	}

	public synchronized BookStoreResult getBooksInDemand()
			throws BookStoreException {
		throw new BookStoreException("Not implemented");
	}

	public synchronized BookStoreResult getBooks(Set<Integer> ISBNList)
			throws BookStoreException {
		BookStoreResult result = new BookStoreResult(
				bookStore.getBooks(ISBNList), snapshotId);
		return result;
	}

	public synchronized BookStoreResult getTopRatedBooks(int numBooks)
			throws BookStoreException {
		throw new BookStoreException("Not implemented");
	}

	public synchronized BookStoreResult getEditorPicks(int numBooks)
			throws BookStoreException {
		BookStoreResult result = new BookStoreResult(
				bookStore.getEditorPicks(numBooks), snapshotId);
		return result;
	}

	public BookStoreResult getBooksByISBN(Set<Integer> isbns)
			throws BookStoreException {
		BookStoreResult result = new BookStoreResult(
				bookStore.getBooksByISBN(isbns), snapshotId);
		return result;
	}


	@SuppressWarnings("unchecked")
	public ReplicationResult replicate(ReplicationRequest request) {
		try {
			BookStoreMessageTag tag = request.getMessageType();
			switch (tag) {
				case ADDBOOKS:
				  bookStore.addBooks((Set<StockBook>)request.getDataSet());
					break;
				case ADDCOPIES:
				  bookStore.addCopies((Set<BookCopy>)request.getDataSet());
					break;
				case BUYBOOKS:
				  bookStore.buyBooks((Set<BookCopy>)request.getDataSet());
					break;
				case UPDATEEDITORPICKS:
				  bookStore.updateEditorPicks(
					    (Set<BookEditorPick>)request.getDataSet());
					break;
				case REMOVEALLBOOKS:
				  bookStore.removeAllBooks();
					break;
				case REMOVEBOOKS:
				  bookStore.removeBooks((Set<Integer>)request.getDataSet());
					break;
				default:
				  throw new BookStoreException();
			}
		} catch (BookStoreException err) {
			return new ReplicationResult(null, false);
		}
		++snapshotId;
		return new ReplicationResult(null, true);
	}

}
