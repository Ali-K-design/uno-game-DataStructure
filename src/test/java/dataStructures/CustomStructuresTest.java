package dataStructures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomStructuresTest {
    @Test
    void queueIsFifoAndPeekLooksAtTheFront() {
        CustomQueue<Integer> q = new CustomQueue<>();
        q.enqueue(1); q.enqueue(2); q.enqueue(3);
        assertEquals(1, q.peek());
        assertEquals(1, q.dequeue());
        assertEquals(2, q.dequeue());
        assertEquals(1, q.getSize());
        q.dequeue();
        assertThrows(IllegalStateException.class, q::dequeue);
    }

    @Test
    void stackIsLifo() {
        CustomStack<Integer> s = new CustomStack<>();
        s.push(1); s.push(2);
        assertEquals(2, s.peek());
        assertEquals(2, s.pop());
        assertEquals(1, s.pop());
        assertThrows(IllegalStateException.class, s::pop);
    }

    @Test
    void dynamicArrayGrowsAndChecksBounds() {
        DynamicArray<Integer> a = new DynamicArray<>();
        for (int i = 0; i < 100; i++) a.add(i);
        assertEquals(100, a.getSize());
        assertEquals(57, a.get(57));
        assertThrows(IndexOutOfBoundsException.class, () -> a.get(100));
        a.swap(0, 99);
        assertEquals(99, a.get(0));
    }

    @Test
    void linkedListRemoveAndSort() {
        SinglyLinkedList<Integer> l = new SinglyLinkedList<>();
        l.addLast(5); l.addLast(1); l.addLast(3);
        assertEquals(1, l.removeAt(1));
        l.addLast(0);
        l.sort(Integer::compare);
        assertEquals(0, l.get(0));
        assertEquals(5, l.get(2));
    }
}
