public class LinearSearch {

    public static boolean search(int[] a, int e) {
        for (int i = 0; i < a.length; i++)
            if (a[i] == e)
                return true;
        return false;
    }
}
