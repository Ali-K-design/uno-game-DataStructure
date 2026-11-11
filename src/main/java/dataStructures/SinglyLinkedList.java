package dataStructures;

import java.util.Comparator;

public class SinglyLinkedList<T> {

    private Node<T> head;
    private Node<T> tail ;
    private int size;


    public SinglyLinkedList() {
        head = null;
        tail = null;
        size = 0;
    }

    public void addLast(T data) {
        Node<T> newNode = new Node<>(data);

        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            tail.setNext(newNode);
            tail = newNode;
        }

        size++;
    }

    public T removeAt(int index){
        Node currentNode = head ;
        Node previous = null ;
        if (index == 0) {
            head = head.getNext();
            if (head == null) {
                tail = null;
            }
            size--;
            return (T) currentNode.getData();
        }
        for (int i = 0; i < index; i++) {
            previous = currentNode;
            currentNode = currentNode.getNext();
        }
        previous.setNext(currentNode.getNext());
        if (currentNode == tail) {
            tail = previous;
        }
        size--;
        return (T) currentNode.getData();
    }

    public T get(int index){
        Node currentNode = head ;
        for (int i = 0; i < index; i++) {
            currentNode=currentNode.getNext();
        }
        return (T) currentNode.getData();
    }

    public void sort(Comparator<T> cmp) {
        head = mergeSort(head, cmp);
        tail = head;
        if (tail != null) {
            while (tail.getNext() != null) {
                tail = tail.getNext();
            }
        }
    }

    private Node<T> mergeSort(Node<T> current, Comparator<T> cmp) {
        if (current == null || current.getNext() == null) {
            return current;
        }
        Node<T> slow = current;
        Node<T> fast = current.getNext();
        while (fast != null && fast.getNext() != null) {
            slow = slow.getNext();
            fast = fast.getNext().getNext();
        }
        Node<T> right = slow.getNext();
        slow.setNext(null);
        Node<T> leftSorted = mergeSort(current, cmp);
        Node<T> rightSorted = mergeSort(right, cmp);
        return merge(leftSorted, rightSorted, cmp);
    }

    private Node<T> merge(Node<T> left, Node<T> right, Comparator<T> cmp) {
        Node<T> temp = new Node<>(null);
        Node<T> current = temp;
        while (left != null && right != null) {
            if (cmp.compare(left.getData(), right.getData()) <= 0) {
                current.setNext(left);
                left = left.getNext();
            } else {
                current.setNext(right);
                right = right.getNext();
            }
            current = current.getNext();
        }
        if (left != null) {
            current.setNext(left);
        } else {
            current.setNext(right);
        }
        return temp.getNext();
    }

    public Node<T> getHead(){
        return this.head;
    }

    public int getSize(){
        return this.size;
    }

    public boolean isEmpty(){
        return (size==0);
    }
}

