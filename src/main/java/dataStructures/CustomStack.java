package dataStructures;

public class CustomStack<T> {
    private Node<T> top ;
    private int size ;

    public void push(T item){
        Node newNode = new Node(item);
        newNode.setNext(top);
        top = newNode;
        size++ ;
    }

    public T pop(){
        if (top == null) throw new IllegalStateException("Stack is empty");
        T data = top.getData();
        top = top.getNext();
        size--;
        return data ;
    }

    public T peek(){
        if (top == null) throw new IllegalStateException("Stack is empty");
        return top.getData();
    }

    public int getSize(){
        return this.size;
    }

    public boolean isEmpty(){
        return (size==0);
    }
}

