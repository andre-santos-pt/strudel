public class HeapSort {

    public static void sort(int[] a) {
        int N = a.length - 1;
        for (int k = N / 2; k >= 0; k--)
            sink(a, k, N);
        while (N > 1) {
            int swap = a[0];
            a[0] = a[N - 1];
            a[N - 1] = swap;
            N--;
            sink(a, 0, N);
        }
    }

    private static void sink(int[] a, int k, int N) {
        while (true) {
            if (2 * k + 1 > N) break;
            int biggestChild = 2 * k + 1; // left
            if (2 * k + 2 <= N && a[2 * k + 1] <= a[2 * k + 2])
                biggestChild = 2 * k + 2; // right
            if (a[k] > a[biggestChild]) break;
            int swap = a[k];
            a[k] = a[biggestChild];
            a[biggestChild] = swap;
            k = biggestChild;
        }
    }
}
