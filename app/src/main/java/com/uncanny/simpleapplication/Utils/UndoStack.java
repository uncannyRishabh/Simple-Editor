package com.uncanny.simpleapplication.Utils;
import java.util.Stack;

public class UndoStack<T> extends Stack<T> {
    private int capacity;

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public UndoStack(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public T push(T item) {
        if (this.size() >= capacity) {
            this.remove(0);
        }
        return super.push(item);
    }
}