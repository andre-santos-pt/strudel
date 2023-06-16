class IntArrayList {
    int[] elements;
    int next;

    static IntArrayList $init(IntArrayList $this) {
        $this.elements = new int [10];
        $this.next = 0;
        return $this;
    }

    static IntArrayList $init(IntArrayList $this, int cap) {
        $this.elements = new int [cap];
        $this.next = 0;
        return $this;
    }

    static int size(IntArrayList $this) {
        return $this.next;
    }

    static boolean isFull(IntArrayList $this) {
        return ($this.next == $this.elements.length);
    }

    static void add(IntArrayList $this, int e) {
        $this.elements[$this.next] = e;
        $this.next = ($this.next + 1);
    }

    static void doubleCapacity(IntArrayList $this) {
        int[] newElements;
        newElements = new int [($this.elements.length * 2)];
        {
            int i;
            i = 0;
            while((i < $this.next)) {
                newElements[i] = $this.elements[i];
                i = (i + 1);
            }
        }
        $this.elements = newElements;
    }
}

class Test {


    public static void main(java.lang.String[] args) {
        IntArrayList list;
        list = IntArrayList.$init(new IntArrayList(), 30);
        IntArrayList.add(list, 1);
        IntArrayList.add(list, 2);
        IntArrayList.add(list, 3);
        IntArrayList.doubleCapacity(list);
        System.out.println(IntArrayList.size(list));
    }
}



