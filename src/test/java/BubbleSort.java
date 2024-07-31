public class BubbleSort {

    public static void sort(int[] a) {
        int i, j;
        boolean swapped;
        for (i = 0; i < a.length - 1; i++) {
            for (j = 0; j < a.length - i - 1; j++) {
                if (a[j] > a[j + 1]) {
                    int swap = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = swap;
                }
            }
        }
    }
}
