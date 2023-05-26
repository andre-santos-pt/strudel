package pt.iscte.strudel.tests.javaparser;

class Sorting {
    public static void bubbleSort(int[] arr) {
        int n = arr.length;
        int temp = 0;
        for(int i=0; i < n; i++){
            for(int j=1; j < (n-i); j++){
               if(arr[j-1] > arr[j]){  
                 temp = arr[j-1];  
                 arr[j-1] = arr[j];  
                 arr[j] = temp;  
               }
            }
        }
    }
    
    // TODO short circuit
    public static void insertionsort(int array[]) {
        int n = array.length;
        for (int j = 1; j < n; j++) {
            int key = array[j];
            int i = j - 1;
            while ((i > -1) && (array[i] > key)) {
                array[i + 1] = array[i];
                i--;
            }
            array[i + 1] = key;
        }
    }
    
     public static void selectionSort(int[] arr){  
        for (int i = 0; i < arr.length - 1; i++)  
        {  
            int index = i;  
            for (int j = i + 1; j < arr.length; j++){  
                if (arr[j] < arr[index]){  
                    index = j;//searching for lowest index  
                }  
            }  
            int smallerNumber = arr[index];   
            arr[index] = arr[i];  
            arr[i] = smallerNumber;  
        }  
    }  
    
    public static void quickSort(int arr[]) {
        quickSortAux(arr, 0, arr.length-1);
    }
    
    private static void quickSortAux(int arr[], int begin, int end) {
    if (begin < end) {
        int partitionIndex = partition(arr, begin, end);

        quickSortAux(arr, begin, partitionIndex-1);
        quickSortAux(arr, partitionIndex+1, end);
    }
    }
    
    private static int partition(int arr[], int begin, int end) {
    int pivot = arr[end];
    int i = (begin-1);

    for (int j = begin; j < end; j++) {
        if (arr[j] <= pivot) {
            i++;

            int tmp1 = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp1;
        }
    }

    int tmp2 = arr[i+1];
    arr[i+1] = arr[end];
    arr[end] = tmp2;

    return i+1;
}

  }