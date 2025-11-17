package edu.iastate.cs2280.hw3;
import java.util.ListIterator;

public class maintest {

	
    public static void main(String[] args) {
        // Note: PAGE_CAPACITY is 4 in the skeleton

        IndexedPagedList<String> list = new IndexedPagedList<>();
        System.out.println("New list: " + list.toStringInternal());
        list.add("A");
        list.add("B");
        list.add("C");
        list.add("D");
        System.out.println("Add A,B,C,D: " + list.toStringInternal() + " (Size: " + list.size() + ")");

        // --- Test Split ---
        list.add("E");
        System.out.println("Add E (split): " + list.toStringInternal() + " (Size: " + list.size() + ")");
        list.add("F");
        System.out.println("Add F: " + list.toStringInternal() + " (Size: " + list.size() + ")");
        list.add("G");
        System.out.println("Add G (split): " + list.toStringInternal() + " (Size: " + list.size() + ")");

        // --- Test Rebalance ---
        list.remove(2);
        System.out.println("Remove C @ 2: " + list.toStringInternal() + " (Size: " + list.size() + ")");
        System.out.println("(Rebalance triggered)");

        // --- Test Merge ---
        list.remove(2);
        System.out.println("Remove D @ 2: " + list.toStringInternal() + " (Size: " + list.size() + ")");
        System.out.println("(Merge triggered)");

        // --- Test Iterator ---
        ListIterator<String> iter = list.listIterator(3);
        System.out.println("\nIterator @ 3: " + list.toStringInternal(iter));
        System.out.println("iter.next() -> " + iter.next());
        System.out.println("After next(): " + list.toStringInternal(iter));
        System.out.println("iter.previous() -> " + iter.previous());
        System.out.println("After prev(): " + list.toStringInternal(iter));
        iter.remove();
        System.out.println("iter.remove(): " + list.toStringInternal(iter) + " (Size: " + list.size() + ")");
        iter.add("Z");
        System.out.println("iter.add(Z): " + list.toStringInternal(iter) + " (Size: " + list.size() + ")");
        System.out.println("iter.next() -> " + iter.next());
        System.out.println("Final state: " + list.toStringInternal(iter) + " (Size: " + list.size() + ")");
    }
	
}
