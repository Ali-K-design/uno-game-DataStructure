package dataStructures;

public class MonotoneStack extends CustomStack<Integer> {
    private final boolean strict;

    public MonotoneStack(boolean strict) {
        this.strict = strict;
    }

    public boolean canPush(int value) {
        return !strict || isEmpty() || value >= peek();
    }

    @Override
    public void push(Integer item) {
        if (!canPush(item)) {
            throw new IllegalArgumentException("Penalty " + item + " is smaller than the top (" + peek() + ")");
        }
        super.push(item);
    }

    public boolean isStrict() { return strict; }
}
