package dataStructures;

public class DoublyCircularLinkedList<T> {
    private int size ;
    private DoublyNode<T> head ;
    private DoublyNode<T> tail ;

    public DoublyCircularLinkedList() {
        head = null;
        tail = null;
        size = 0;
    }

    public void add(T data) {
        DoublyNode<T> newNode = new DoublyNode<>(data);

        if (head == null) {
            head = newNode;
            tail = newNode;
            head.setNext(head);
            head.setPrev(head);

        } else {
            newNode.setPrev(tail);
            newNode.setNext(head);
            tail.setNext(newNode);
            head.setPrev(newNode);
            tail = newNode;
        }
        size++;
    }

    public int getSize(){
        return this.size;
    }

    public DoublyNode<T> getHead(){
        return this.head;
    }

    public DoublyNode<T> getTail(){
        return this.tail;
    }

    public boolean isEmpty(){
        return (size==0) ;
    }
}
