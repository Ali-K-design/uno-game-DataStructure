package dataStructures;

public class CustomQueue<T> {
    private Node<T> rear ;
    private Node<T> front ;
    private int size ;

    public void enqueue(T item){
       Node newNode = new Node<>(item);
       if(front == null){
           front = newNode ;
           rear = newNode ;
       }
       else{
           rear.setNext(newNode);
           rear = newNode;
       }
       size ++ ;
    }

    public T dequeue(){
        T data = front.getData();
        front = front.getNext();
        if (front == null) {
            rear = null;
        }
        size--;
        return data;
    }

    public T peek(){
        return rear.getData();
    }

    public boolean isEmpty(){
        return (size==0);
    }

    public int getSize(){
        return this.size;
    }
}
