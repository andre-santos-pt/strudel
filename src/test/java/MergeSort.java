public class MergeSort {

    private static void merge(int[] a, int[] aux, int lo, int mid, int hi) {
        if (hi + 1 - lo >= 0) System.arraycopy(a, lo, aux, lo, hi + 1 - lo);
        int i = lo;
        int j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) {
                a[k] = aux[j];
                j++;
            }
            else if (j > hi) {
                a[k] = aux[i];
                i++;
            }
            else if (aux[j] < aux[i]) {
                a[k] = aux[j];
                j++;
            }
            else {
                a[k] = aux[i];
                i++;
            }
        }
    }

    private static void sort(int[] a, int[] aux, int lo, int hi) {
        if (hi <= lo) return;
        int mid = lo + (hi - lo) / 2;
        sort(a, aux, lo, mid);
        sort(a, aux, mid + 1, hi);
        merge(a, aux, lo, mid, hi);
    }

    public static void sort(int[] a) {
        int[] aux = new int[a.length];
        sort(a, aux, 0, a.length - 1);
    }
}
