class BinarySearch {
    static boolean binarySearch(int[] a, int e) {
        int l = 0;
        int r = a.length - 1;
        while (l <= r) {
            int m = l + ((r - l) / 2);
            if (a[m] == e) return true;
            if (a[m] < e) l = m + 1;
            else r = m - 1;
        }
        return false;
    }
}