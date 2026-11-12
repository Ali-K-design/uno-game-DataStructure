package dataStructures;

public class DynamicArray<T> {
    private int size ;
    private int capacity ;
    private T[] array ;

    public DynamicArray(){
        capacity = 5 ;
        size = 0 ;
        array = (T[]) new Object[capacity];
    }

    public void add(T item){
        if (size == capacity) {
            resize();
        }
        array[size] = item;
        size++;
    }

    public void set(T item , int index){
        array[index]=item ;
    }

    public void swap(int index1 , int index2){
        T temp = array[index2];
        array[index2] = array[index1];
        array[index1]= temp ;

    }

    public void resize(){
        capacity *= 2;
        T[] newArray = (T[]) new Object[capacity];
        for (int i = 0; i < size; i++) {
            newArray[i] = array[i];
        }
        array = newArray;
    }

    public T get (int index){
        return array[index];
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public T[] getArray() {
        return array;
    }

    public void setArray(T[] array) {
        this.array = array;
    }


}
