public class QuickSort {

    // Knuth Shuffle
    private static void shuffle(int[] a) {
        for (int i = 0; i < a.length; i++) {
            int j = (int) (Math.random() * i);
            int swap = a[i];
            a[i] = a[j];
            a[j] = swap;
        }
    }

    public static int partition(int[] a, int lo, int hi) {
        int i = lo; // Left starts iterating on the first
        int j = hi + 1; // Start iterating *after* the last because we do --j

        while (true) {
            i++;
            while (a[i] <= a[lo]) { // Find item on left to swap
                if (i == hi) break; // If it goes over the upper limit, stop
                i++;
            }

            j--;
            while (a[lo] <= a[j]) { // Find item on right to swap
                if (j == lo) break; // If it goes under the lower limit, stop
                j--;
            }

            if (i >= j) break; // If the indices cross, stop

            // Exchange found elements
            int swap = a[i];
            a[i] = a[j];
            a[j] = swap;
        }

        // If above while stopped, then indices crossed --> exchange a[lo] with a[j]
        int swap = a[lo];
        a[lo] = a[j];
        a[j] = swap;

        return j; // Return index of the partitioning element
    }

    public static void sort(int[] a, int lo, int hi) {
        if (hi <= lo) return;
        int j = partition(a, lo, hi);
        sort(a, lo, j - 1);
        sort(a, j + 1, hi);
    }

    public static void sort(int[] a) {
        shuffle(a);
        sort(a, 0, a.length - 1);
    }
}
