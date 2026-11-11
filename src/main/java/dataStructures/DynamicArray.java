package dataStructures;

public class DynamicArray<T> {
    private int size ;
    private int capacity ;
    private T[] array ;

    public void add(T item){

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
