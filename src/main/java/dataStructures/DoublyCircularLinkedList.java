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

    public void remove(DoublyNode<T> node) {
        if (node == null || size == 0) return;
        if (size == 1) {
            head = null;
            tail = null;
            size = 0;
            return;
        }
        node.getPrev().setNext(node.getNext());
        node.getNext().setPrev(node.getPrev());
        if (node == head) head = node.getNext();
        if (node == tail) tail = node.getPrev();
        size--;
    }

    public DoublyNode<T> findNode(T data) {
        DoublyNode<T> node = head;
        for (int i = 0; i < size; i++) {
            if (node.getData() == data) return node;
            node = node.getNext();
        }
        return null;
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
