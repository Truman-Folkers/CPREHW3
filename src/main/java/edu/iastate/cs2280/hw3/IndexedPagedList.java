/**
 * @author YOUR NAME HERE
 */
package edu.iastate.cs2280.hw3;

import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * An implementation of the List interface that uses a doubly-linked list of
 * "pages" (nodes), where each page stores multiple items in an array.
 * * This structure is optimized for fast, $O(\log N_{\text{pages}})$
 * index-based access (get, set) and iterator initialization. It achieves this
 * using a separate `ArrayList` (`pageIndex`) that acts as a top-level index,
 * allowing binary search to quickly find the correct page for a given logical
 * index.
 * * Structural modifications (add(int, E), remove(int)) are
 * $O(N_{\text{pages}})$ in the worst case. While finding the target page is
 * fast ($O(\log N_{\text{pages}})$), these operations must update the logical
 * indices of all subsequent pages in the `pageIndex`, which requires a linear
 * scan of the index. This is still significantly faster than a standard
 * `LinkedList` ($O(N)$) for modifications in the middle of the list.
 * * This implementation includes page-splitting (when a page becomes full) and
 * page-merging/rebalancing (when a page becomes under-filled) to maintain
 * efficiency.
 *
 * @param <E> the type of elements in this list
 */
public class IndexedPagedList<E> extends AbstractSequentialList<E> implements List<E> {

  /**
   * Default capacity for each page (node).
   */
  private static final int PAGE_CAPACITY = 4;

  /**
   * The minimum number of items a page must have. If a page's count drops
   * below this, it will trigger a merge or rebalance.
   */
  private static final int HALF_CAPACITY = PAGE_CAPACITY / 2;
  /**
   * A static, reusable comparator for performing binary searches on the
   * `pageIndex`. It compares an `IndexEntry` from the list (the first argument)
   * with a logical index (the "key", or second argument).
   */
  private static final Comparator<Object> INDEX_COMPARATOR = new Comparator<Object>() {
    /**
     * Compares an IndexEntry (from the list) to a logical index (the key).
     *
     * @param entryObj The IndexEntry (cast from Object) from the list.
     * @param key      The logical index (Integer) we are searching for.
     * @return a negative integer, zero, or a positive integer as the
     *         entry's logical index is less than, equal to, or greater than the
     *         key.
     */
    @Override
    public int compare(Object entryObj, Object key) {
      @SuppressWarnings("unchecked") IndexEntry<?> entry = (IndexEntry<?>) entryObj;
      Integer logicalIndex = (Integer) key;
      return entry.logicalIndex.compareTo(logicalIndex);
    }
  };
  /**
   * The dummy head node of the doubly-linked list of pages.
   */
  private final Page<E> head;
  /**
   * The dummy tail node of the doubly-linked list of pages.
   */
  private final Page<E> tail;
  /**
   * The "fast-lane" index. This `ArrayList` stores an `IndexEntry` for
   * *every* data page in the list. It allows for $O(\log N_{\text{pages}})$
   * binary search
   * to find the page corresponding to a given logical index.
   * * This index *must* be kept in sync with the linked list of pages.
   */
  private final ArrayList<IndexEntry<E>> pageIndex;
  /**
   * The total number of elements stored in the list.
   */
  private int totalSize;
  /**
   * A counter for structural modifications (add, remove). Used by the
   * iterator to detect concurrent modifications and fail-fast.
   */
  private int modificationCount;

  /**
   * Constructs a new, empty IndexedPagedList.
   * Initializes the dummy head and tail nodes and the empty page index.
   */
  public IndexedPagedList() {
    // TODO
    // Initialize head and tail dummy nodes, link them.
    // Initialize totalSize, modificationCount.
    // Initialize the pageIndex ArrayList.
	  
	  head = new Page<E>(null, null);
	  tail = new Page<E>(null, null);
	  head.prev = tail;
	  tail.next = head;
	  
	  pageIndex = new ArrayList<IndexEntry<E>>();
	  
  }

  /**
   * Returns the number of elements in this list.
   *
   * @return the number of elements in this list
   */
  @Override
  public int size() {
    // TODO
	  return 0;
	  
  }

  /**
   * Appends the specified element to the end of this list.
   * * This operation is $O(N_{\text{pages}})$ in the worst case, identical to
   * add(size(), item).
   *
   * @param item element to be appended to this list
   * @return {@code true} (as specified by List#add)
   * @throws NullPointerException if the specified element is null
   */
  @Override
  public boolean add(E item) {
    // TODO - Avoid code duplication if you can
	  add(size(), item);
	  return true;
  }

  /**
   * Inserts the specified element at the specified position in this list.
   * * This operation is $O(N_{\text{pages}})$ in the worst case.
   * Finding the insertion point is fast ($O(\log N_{\text{pages}})$),
   * but updating the `pageIndex` after the insertion requires a
   * linear scan of the index ($O(N_{\text{pages}})$).
   * * This method performs the following steps:
   * 1. Finds the target page and offset for insertion using findPageForLogicalIndex.
   * 2. Increment modCount, totalSize.
   * 3. Handles three cases:
   * - List is empty: Creates a new page.
   * - Target page has space: Adds the item.
   * - Target page is full: Performs a <b>split</b> operation.
   * 4. Updates the `pageIndex` to reflect changes in page counts,
   * new pages, and subsequent logical indices.
   *
   * @param pos  the index at which the specified element is to be inserted
   * @param item the element to be inserted
   * @throws NullPointerException      if the specified element is null
   * @throws IndexOutOfBoundsException if the index is out of range
   *                                   ({@code pos < 0 || pos > size()})
   */
  @Override
  public void add(int pos, E item) {
    // TODO
	  if (item == null) throw new NullPointerException();
	  if (pos < 0 || pos > this.totalSize) throw new IndexOutOfBoundsException();
	  
	  this.totalSize++;
	  this.modCount++;
	  
	  if(this.size() == 1) {
	    Page<E> p = new Page<E>(tail, head);
        p.items[0] = item;
        p.count = 1;

        // link between head and tail
        head.next = p;
        p.prev = head;
        p.next = tail;
        tail.prev = p;

        // Create new IndexEntry
        pageIndex.add(new IndexEntry<>(p, 0, 1));
        return;
	  }
	  
	  
	  PageInfo<E> pinfo = findPageForLogicalIndex(pos);
	  Page<E> page = pinfo.page;
	  int offset = pinfo.offset;

	  int pageIdx = findIndexInPageIndex(page);    
	  IndexEntry<E> entry = pageIndex.get(pageIdx);
	  
	  if(page.count < PAGE_CAPACITY) {
		  for(int i = page.count; i < offset; i--) {
			  page.items[i] = page.items[i-1];
		  }
		  
		  page.items[offset] = item;
	      page.count++;
	      entry.count = page.count;

	        // update logicalIndex for all later pages
	      updatePageIndex(pageIdx + 1, +1);
	      return;
	  }
	  
	  Page<E> newPage = new Page<E>(page, page.next);
	  int move = HALF_CAPACITY;

	  // start index to move from
	  int start = page.count - move;

	    // move HALF_CAPACITY items to newPage
	  for (int i = 0; i < move; i++) {
	        newPage.items[i] = page.items[start + i];
	        page.items[start + i] = null;
	  }

	  newPage.count = move;
	  page.count -= move;

	    // update old page entry
	  entry.count = page.count;

	    // link newPage after page
	  Page<E> after = page.next;
	  page.next = newPage;
	  newPage.prev = page;
	  newPage.next = after;
	  after.prev = newPage;

	    // insert new index entry
	  int newPageIdx = pageIdx + 1;
	  int newLogicalIndex = entry.logicalIndex + page.count;
	  pageIndex.add(newPageIdx,
	            new IndexEntry<>(newPage, newLogicalIndex, newPage.count));

	    // ----- 4b. Decide which page gets the new item -----
	  if (pos <= entry.logicalIndex + page.count) {
	        // insert into original page

	      for (int i = page.count; i > offset; i--) {
	          page.items[i] = page.items[i - 1];
	      }
	      page.items[offset] = item;
	      page.count++;
	      entry.count = page.count;

	  } else {
	        // insert into new page
	      int newOffset = pos - newLogicalIndex;

	      for (int i = newPage.count; i > newOffset; i--) {
	          newPage.items[i] = newPage.items[i - 1];
	      }
	      newPage.items[newOffset] = item;
	      newPage.count++;

	      pageIndex.get(newPageIdx).count = newPage.count;
	  }

	    // ----- 5. Update logicalIndex for all pages AFTER the insertion -----
	  updatePageIndex(newPageIdx + 1, +1);
	  
	  
	  
  }

  /**
   * Removes the element at the specified position in this list.
   * * This operation is $O(N_{\text{pages}})$ in the worst case.
   * Finding the element is fast ($O(\log N_{\text{pages}})$),
   * but updating the `pageIndex` after the removal (and any potential
   * merge) requires a linear scan of the index ($O(N_{\text{pages}})$).
   * * This method performs the following steps:
   * 1. Finds the target page and offset for removal using findPageForLogicalIndex.
   * 2. Increment modCount, decrement totalSize.
   * 2. Removes the item from the page's array.
   * 3. If the page's count drops below `HALF_CAPACITY`, it triggers:
   * - <b>Rebalance:</b> If the *successor* page has items to spare,
   * one item is moved to the current page.
   * - <b>Full Merge:</b> If the *successor* page does not have items
   * to spare, all items from the successor are moved to the
   * current page, and the successor page is removed.
   * 4. Updates the `pageIndex` to reflect all changes.
   * 5. Make sure to handle the edge case of page becoming empty AND it's the last page.
   *
   * @param pos the index of the element to be removed
   * @return the element previously at the specified position
   * @throws IndexOutOfBoundsException if the index is out of range
   *                                   ({@code pos < 0 || pos >= size()})
   */
  @Override
  public E remove(int pos) {
    // TODO
    if (pos < 0 || pos >= totalSize) {
        throw new IndexOutOfBoundsException();
    }

    // ----- 1. Find the page + offset -----
    PageInfo<E> info = findPageForLogicalIndex(pos);
    Page<E> page = info.page;
    int offset = info.offset;

    // Save removed element
    E removed = page.items[offset];

    // Structural modification
    modCount++;
    totalSize--;

    // ----- 2. Remove element from page array -----
    for (int i = offset; i < page.count - 1; i++) {
        page.items[i] = page.items[i + 1];
    }
    // Update IndexEntry for this page
    int pageIdx = findIndexInPageIndex(page);
    pageIndex.get(pageIdx).count = page.count;

    // ----- 3. Underflow? (count < HALF_CAPACITY) -----
    if (page.count < HALF_CAPACITY) {

        Page<E> succ = page.next;

        // We only rebalance/merge if successor is a real page (not tail)
        if (succ != tail) {

            // ----- 3a. REBALANCE -----
            if (succ.count > HALF_CAPACITY) {

                // Move successor.items[0] → end of current page
                page.items[page.count] = succ.items[0];
                page.count++;
                pageIndex.get(pageIdx).count = page.count;

                // Shift successor left
                for (int i = 0; i < succ.count - 1; i++) {
                    succ.items[i] = succ.items[i + 1];
                }
                succ.items[succ.count - 1] = null;
                succ.count--;
                pageIndex.get(pageIdx + 1).count = succ.count;

                // Done with rebalance

            } else {
                // ----- 3b. FULL MERGE -----

                // Move all succ items → end of current page
                for (int i = 0; i < succ.count; i++) {
                    page.items[page.count + i] = succ.items[i];
                }

                page.count += succ.count;
                pageIndex.get(pageIdx).count = page.count;

                // Unlink successor from linked list
                Page<E> succNext = succ.next;
                page.next = succNext;
                succNext.prev = page;

                // Remove successor IndexEntry
                pageIndex.remove(pageIdx + 1);
            }
        }
    }

    // ----- 4. Edge case — page becomes empty AND is last page -----
    if (page.count == 0 && page != head && page.next == tail) {

        // unlink the empty last page
        Page<E> prev = page.prev;
        prev.next = tail;
        tail.prev = prev;

        // remove its IndexEntry
        pageIndex.remove(pageIdx);
    }

    // ----- 5. Update logicalIndex for all following pages -----
    updatePageIndex(pageIdx + 1, -1);

    return removed;
	
	
  }

  /**
   * Returns a list iterator over the elements in this list (in proper
   * sequence), starting at the specified position in the list.
   * * The iterator is initialized in $O(\log N_{\text{pages}})$ time by using
   * `findPageForLogicalIndex` to immediately seek to the correct starting
   * page and offset.
   *
   * @param pos the index of the first element to be returned from the
   *            list iterator (by a call to ListIterator#next)
   * @return a list iterator over the elements in this list (in proper
   * sequence), starting at the specified position in the list
   * @throws IndexOutOfBoundsException if the index is out of range
   *                                   ({@code pos < 0 || pos > size()})
   */
  @Override
  public ListIterator<E> listIterator(int pos) {
    // TODO
	 if(pos < 0 || pos > this.size()) throw new IndexOutOfBoundsException();
	 return new PagedListIterator(pos);
  }

  // --- Private Helper Methods and Classes ---

  /**
   * Finds the page and in-page offset for a given logical index.
   * * This method runs in $O(\log N_{\text{pages}})$ time by performing a
   * binary search on the `pageIndex`.
   * * This method performs the following steps:
   * 1. Preform a binary search on the `pageIndex` using Collections.binarySearch with INDEX_COMPARATOR.
   * 2. Handle cases for index >= 0, index < 0.
   * 3. Calculate the offset within the page's array.
   * 4. Return the page and offset.
   *
   * @param pos The logical index to find.
   * @return A PageInfo object containing the page and offset.
   */
  private PageInfo<E> findPageForLogicalIndex(int pos) {
    // TODO
	  if(pos <= 0) throw new IllegalArgumentException();
	  if(pos > pageIndex.size()) throw new IllegalArgumentException();
	  
	  int index = Collections.binarySearch(pageIndex, pos, INDEX_COMPARATOR);
	  if(index > pageIndex.size()) throw new IllegalArgumentException();  //?
	  
	  int offset = index - pageIndex.get(index).logicalIndex;
	  
	  return new PageInfo(pageIndex.get(index).page, offset);
  }

  /**
   * Finds the index (in the `pageIndex` `ArrayList`) of the `IndexEntry`
   * that corresponds to the given `page`.
   * * This is an $O(N_{\text{pages}})$ operation (linear scan). It is only
   * called by `add` and `remove`, which are already $O(N_{\text{pages}})$
   * due to `updatePageIndex`.
   *
   * @param page The page to find.
   * @return The index in `pageIndex`, or -1 if not found.
   */
  private int findIndexInPageIndex(Page<E> page) {
    // TODO
	  for(IndexEntry<E> i : pageIndex) {
		  if(i.page.equals(page)){
			  return pageIndex.indexOf(i);
		  }
	  }
	  
	  return -1;
  }

  /**
   * Updates the `logicalIndex` of all `IndexEntry` objects in the
   * `pageIndex` starting from `startIndex`, adding `delta` to each.
   * * This is an $O(N_{\text{pages}})$ operation and is the reason why
   * `add` and `remove` are not $O(\log N)$.
   *
   * @param startIndex The index in `pageIndex` to start updating from.
   * @param delta      The amount to add to each `logicalIndex` (e.g., +1 or -1).
   */
  private void updatePageIndex(int startIndex, int delta) {
    // TODO
	  for(int i = startIndex; i < pageIndex.size(); i++) {
		  pageIndex.get(i).logicalIndex += delta;
	  }
  }

  /**
   * Returns a string representation of the internal structure of the list.
   * This string shows the contents of each page (node) and uses "-"
   * to represent empty slots in each page's array.
   *
   * @return A string in the format [(item1, item2, -, -), (item3, -, -, -)]
   */
  public String toStringInternal() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");

    Page<E> currentPage = head.next;
    while (currentPage != tail) {
      sb.append("(");
      for (int i = 0; i < PAGE_CAPACITY; i++) {
        if (i < currentPage.count) {
          sb.append(currentPage.items[i]);
        } else {
          sb.append("-");
        }

        if (i < PAGE_CAPACITY - 1) {
          sb.append(", ");
        }
      }
      sb.append(")");
      if (currentPage.next != tail) {
        sb.append(", ");
      }
      currentPage = currentPage.next;
    }

    sb.append("]");
    return sb.toString();
  }

  /**
   * Returns a string representation of the internal structure of the list,
   * including the position of a ListIterator's cursor.
   *
   * The cursor position (represented by "|") is determined by the
   * iterator's `nextIndex()` method.
   *
   * @param iter The ListIterator to visualize.
   * @return A string showing page contents and the iterator's cursor position.
   */
  public String toStringInternal(ListIterator<E> iter) {
    // Find the logical position of the iterator's cursor
    int logicalIndex = iter.nextIndex();
    // Find the physical page and offset that correspond to that logical index
    PageInfo<E> cursorInfo = findPageForLogicalIndex(logicalIndex);
    Page<E> cursorPage = cursorInfo.page;
    int cursorOffset = cursorInfo.offset;

    StringBuilder sb = new StringBuilder();
    sb.append("[");

    Page<E> currentPage = head.next;
    while (currentPage != tail) {
      sb.append("(");
      for (int i = 0; i < PAGE_CAPACITY; i++) {
        // Check if the cursor should be printed *before* this item
        if (currentPage == cursorPage && i == cursorOffset) {
          sb.append("| ");
        }

        if (i < currentPage.count) {
          sb.append(currentPage.items[i]);
        } else {
          sb.append("-");
        }

        if (i < PAGE_CAPACITY - 1) {
          sb.append(", ");
        }
      }
      sb.append(")");
      if (currentPage.next != tail) {
        sb.append(", ");
      }
      currentPage = currentPage.next;
    }

    // Handle case where cursor is at the very end of the list (pos == totalSize)
    if (cursorPage == tail) {
      sb.append(", (| -)");
    }

    sb.append("]");
    return sb.toString();
  }

  /**
   * A private static inner class representing a "page" (node) in the
   * doubly-linked list. Each page holds an array of items.
   */
  private static class Page<E> {
    /**
     * The array holding the data items.
     */
    final E[] items;

    /**
     * The number of items currently stored in this page.
     */
    int count;

    /**
     * The link to the previous page in the list.
     */
    Page<E> prev;

    /**
     * The link to the next page in the list.
     */
    Page<E> next;

    /**
     * Constructs a new page.
     *
     * @param prev The previous page.
     * @param next The next page.
     */
    @SuppressWarnings("unchecked")
    Page(Page<E> prev, Page<E> next) {
      this.items = (E[]) new Object[PAGE_CAPACITY];
      this.count = 0;
      this.prev = prev;
      this.next = next;
    }

    /**
     * Inserts an item into this page's array at a specific offset.
     * Shifts existing items to the right.
     *
     * @param offset The index in the `items` array to insert at.
     * @param item   The item to insert.
     */
    void addItem(int offset, E item) {
      if (offset < 0 || offset > count) {
        throw new IndexOutOfBoundsException("Internal add error: offset " + offset + ", count " + count);
      }
      System.arraycopy(items, offset, items, offset + 1, count - offset);
      items[offset] = item;
      count++;
    }

    /**
     * Removes an item from this page's array at a specific offset.
     * Shifts existing items to the left.
     *
     * @param offset The index in the `items` array to remove.
     * @return The item that was removed.
     */
    E removeItem(int offset) {
      if (offset < 0 || offset >= count) {
        throw new IndexOutOfBoundsException("Internal remove error: offset " + offset + ", count " + count);
      }
      E item = items[offset];
      int numToMove = count - offset - 1;
      if (numToMove > 0) {
        System.arraycopy(items, offset + 1, items, offset, numToMove);
      }
      items[count - 1] = null;
      count--;
      return item;
    }
  }

  /**
   * A private static inner class that holds information about a page,
   * used as an entry in the `pageIndex`.
   */
  private static class IndexEntry<E> {
    /**
     * A direct reference to the page object.
     */
    final Page<E> page;

    /**
     * The logical index of the *first* item on this page (items[0]).
     */
    Integer logicalIndex;

    /**
     * The number of items currently stored on this page.
     */
    int count;

    /**
     * Constructs a new index entry.
     *
     * @param page         The page to reference.
     * @param logicalIndex The logical index of the first item on this page.
     * @param count        The current item count of this page.
     */
    IndexEntry(Page<E> page, int logicalIndex, int count) {
      this.page = page;
      this.logicalIndex = logicalIndex;
      this.count = count;
    }
  }

  /**
   * A simple private inner class to return two values (page and offset)
   * from the `findPageForLogicalIndex` method.
   *
   * @param <T> The type of element held in the page.
   */
  private class PageInfo<T> {
    /**
     * The page found.
     */
    final Page<T> page;

    /**
     * The calculated offset within the page's array.
     */
    final int offset;

    /**
     * Constructs a new PageInfo.
     *
     * @param page   The page.
     * @param offset The offset.
     */
    PageInfo(Page<T> page, int offset) {
      this.page = page;
      this.offset = offset;
    }
  }

  /**
   * The private implementation of the `ListIterator` interface.
   * * This iterator is "fail-fast" and will throw a
   * ConcurrentModificationException if the list is structurally
   * modified by any method other than the iterator's own `add` or `remove`
   * methods.
   */
  private class PagedListIterator implements ListIterator<E> {

    /**
     * The page that contains the *next* element to be returned by `next()`.
     */
    private Page<E> currentPage;

    /**
     * The offset within `currentPage` of the *next* element to be
     * returned by `next()`.
     */
    private int pageOffset;

    /**
     * The logical index of the *next* element to be returned by `next()`.
     */
    private int logicalIndex;

    /**
     * The logical index of the element last returned by `next()` or
     * `previous()`. Used by `set()` and `remove()`.
     * Set to -1 if no such element exists.
     */
    private int lastItemIndex;

    /**
     * The direction of the last move, to validate `set()` and `remove()` calls.
     */
    private Direction lastDirection;

    /**
     * The value of `modificationCount` that this iterator expects.
     * Used to detect concurrent modifications.
     */
    private int expectedModificationCount;

    /**
     * Constructs a new iterator starting at the given logical position.
     * This operation is fast ($O(\log N_{\text{pages}})$) due to
     * `findPageForLogicalIndex`.
     * * This method performs the following steps:
     * 1. Use findPageForLogicalIndex(pos) to get the starting PageInfo.
     * 2. Initialize currentPage and pageOffset from the PageInfo.
     * 3. Initialize logicalIndex to pos.
     * 4. Initialize lastItemIndex to -1, lastDirection to NONE.
     * 5. Initialize expectedModificationCount to modificationCount.
     *
     * @param pos The logical index to start at.
     */
    PagedListIterator(int pos) {
      // TODO
    	this.expectedModificationCount = modificationCount;
    	lastItemIndex = -1;
    	lastDirection = Direction.NONE;
        // Logical index the iterator starts at
        this.logicalIndex = pos;

        // Find the exact starting page + in-page offset in O(log N_pages)
        PageInfo<E> info = findPageForLogicalIndex(pos);

        this.currentPage = info.page;
        this.pageOffset = info.offset;
    	
    }

    /**
     * Checks for concurrent modification.
     *
     * @throws ConcurrentModificationException if the list has been modified
     *                                         externally.
     */
    private void checkComodification() {
      if (expectedModificationCount != modificationCount) {
        throw new ConcurrentModificationException();
      }
    }

    /**
     * Returns `true` if this list iterator has more elements when
     * traversing the list in the forward direction.
     * * This method performs the following steps:
     * 1. Check for comodification.
     * 2. Check if logicalIndex is less than totalSize.
     */
    @Override
    public boolean hasNext() {
      // TODO
    	checkComodification();
    	 return logicalIndex < totalSize;
    }

    /**
     * Returns the next element in the list and advances the cursor
     * position. Remember to check for comodification.
     */
    @Override
    public E next() {
      // TODO
    	checkComodification();

        // ----- No next element -----
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        // The element to return is currentPage.items[pageOffset]
        E item = currentPage.items[pageOffset];


        // Advance logical cursor
        logicalIndex++;

        // Advance pageOffset inside the page
        pageOffset++;

        // ----- Move to next page if we passed the end of the current page -----
        if (pageOffset >= currentPage.count) {
            // move to the next page and reset offset to 0
            currentPage = currentPage.next;
            pageOffset = 0;
        }

        return item; 
    }

    /**
     * Returns `true` if this list iterator has more elements when
     * traversing the list in the reverse direction. Remember to check for comodification.
     */
    @Override
    public boolean hasPrevious() {
      // TODO
    	checkComodification();
    	return logicalIndex > 0;
    }

    /**
     * Returns the previous element in the list and moves the cursor
     * position backwards. Remember to check for comodification.
     */
    @Override
    public E previous() {
      // TODO
    	if(!hasNext()) {
    		throw new NoSuchElementException();
    	}
    	
    	E item = currentPage.items[pageOffset];
    	
    	logicalIndex--;
    	pageOffset--;
    	
    	if(pageOffset <= 0) {
    		currentPage = currentPage.prev;
    		pageOffset = currentPage.count;
    	}
    	
    	return item;
    }

    /**
     * Returns the index of the element that would be returned by a
     * subsequent call to `next()`. Remember to check for comodification.
     */
    @Override
    public int nextIndex() {
      // TODO
    	checkComodification();
    	return logicalIndex;
    }

    /**
     * Returns the index of the element that would be returned by a
     * subsequent call to `previous()`. Remember to check for comodification.
     */
    @Override
    public int previousIndex() {
      // TODO
    	checkComodification();
    	return logicalIndex - 1;
    }

    /**
     * Removes from the list the last element that was returned by
     * `next()` or `previous()`.
     * * This operation delegates to the outer class's `remove(int)` method,
     * which runs in $O(N_{\text{pages}})$ time. The iterator then
     * re-synchronizes its position in $O(\log N_{\text{pages}})$ time.
     * * This method performs the following steps:
     * 1. Check for comodification.
     * 2. Check for IllegalStateException.
     * 3. Call the outer class's remove: IndexedPagedList.this.remove(lastItemIndex);
     * 4. Update iterator state
     * 5. Update modification counts
     * 6. Reset lastDirection to NONE and lastItemIndex to -1.
     *
     * @throws IllegalStateException if `next()` or `previous()` have not
     *                               been called, or `remove()` or `add()`
     *                               have been called since the last move.
     */
    @Override
    public void remove() {
      // TODO
    }

    /**
     * Replaces the last element returned by `next()` or `previous()`
     * with the specified element.
     * * This operation is $O(1)$ after finding the page in
     * $O(\log N_{\text{pages}})$ time.
     * * This method performs the following steps:
     * 1. Check for comodification.
     * 2. Check for Exceptions.
     * 3. Find the page/offset for lastItemIndex using findPageForLogicalIndex.
     * 4. Set the item in that page's array.
     * 5. Do NOT change modificationCount.
     *
     *
     * @throws IllegalStateException if `next()` or `previous()` have not
     *                               been called, or `remove()` or `add()`
     *                               have been called since the last move.
     * @throws NullPointerException  if the specified element is null.
     */
    @Override
    public void set(E item) {
      // TODO
    }

    /**
     * Inserts the specified element into the list at the iterator's
     * current position.
     * * This operation delegates to the outer class's `add(int, E)` method,
     * which runs in $O(N_{\text{pages}})$ time. The iterator then
     * re-synchronizes its position in $O(\log N_{\text{pages}})$ time.
     * * This method performs the following steps:
     * 1. Check for comodification.
     * 2. Check for Exceptions.
     * 3. Call the outer class's add: IndexedPagedList.this.add(logicalIndex, item);
     * 4. Update iterator state
     * 5. Update modification counts
     * 6. Reset lastDirection to NONE and lastItemIndex to -1.
     *
     * @throws NullPointerException if the specified element is null.
     */
    @Override
    public void add(E item) {
      // TODO
    }

    /**
     * Enum to track the last operation (`next` or `previous`)
     * for `remove()` and `set()` validation.
     */
    private enum Direction {
      NONE, NEXT, PREVIOUS
    }
  }
}
