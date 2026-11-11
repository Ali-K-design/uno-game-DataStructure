package dataStructures;

public class DoublyNode<T> {
    private T data;
    private DoublyNode<T> next;
    private DoublyNode<T> previous;

    public DoublyNode(T data) {
        this.data = data;
        this.next = null;
        this.previous = null;
    }

    public T getData() {
        return data;
    }

    public DoublyNode<T> getNext() {
        return next;
    }

    public void setNext(DoublyNode<T> next) {
        this.next = next;
    }

    public DoublyNode<T> getPrev() {
        return previous;
    }

    public void setPrev(DoublyNode<T> prev) {
        this.previous = prev;
    }
}
