public class ShellSort {

    public static void sort(int[] a) {
        int h = 1;
        while (h < a.length / 3)
            h = 3 * h + 1;

        while (h >= 1) {
            for (int i = h; i < a.length; i++) {
                for (int j = i; j >= h && a[j] < a[j - h]; j -= h) {
                    int swap = a[j];
                    a[j] = a[j - h];
                    a[j - h] = swap;
                }
            }
            h /= 3;
        }
    }
}
