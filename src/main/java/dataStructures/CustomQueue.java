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
        if (front == null) throw new IllegalStateException("Queue is empty");
        T data = front.getData();
        front = front.getNext();
        if (front == null) {
            rear = null;
        }
        size--;
        return data;
    }

    public void appendList(SinglyLinkedList<T> list) {
        if (list.isEmpty()) return;
        if (front == null) {
            front = list.getHead();
        } else {
            rear.setNext(list.getHead());
        }
        rear = list.getTail();
        size += list.getSize();
        list.clear();
    }

    public T peek(){
        if (front == null) throw new IllegalStateException("Queue is empty");
        return front.getData();
    }

    public boolean isEmpty(){
        return (size==0);
    }

    public int getSize(){
        return this.size;
    }
}
