package network;

import dataStructures.DynamicArray;

public final class JsonArray {
    private final DynamicArray<Object> items = new DynamicArray<>();

    public JsonArray add(Object value) {
        items.add(value);
        return this;
    }

    public int size() { return items.getSize(); }
    public Object get(int index) { return items.get(index); }

    public String getString(int index) {
        Object v = items.get(index);
        return v instanceof String s ? s : null;
    }

    public JsonObject getObject(int index) {
        Object v = items.get(index);
        return v instanceof JsonObject o ? o : null;
    }

    public static JsonArray ofStrings(String[] values) {
        JsonArray a = new JsonArray();
        for (String v : values) a.add(v);
        return a;
    }

    public String[] toStringArray() {
        String[] out = new String[size()];
        for (int i = 0; i < out.length; i++) {
            Object v = items.get(i);
            if (!(v instanceof String)) throw new JsonException("Expected a string at index " + i);
            out[i] = (String) v;
        }
        return out;
    }

    public static JsonArray ofBooleans(boolean[] values) {
        JsonArray a = new JsonArray();
        for (boolean v : values) a.add(v);
        return a;
    }

    public boolean[] toBooleanArray() {
        boolean[] out = new boolean[size()];
        for (int i = 0; i < out.length; i++) {
            Object v = items.get(i);
            if (!(v instanceof Boolean)) throw new JsonException("Expected a boolean at index " + i);
            out[i] = (Boolean) v;
        }
        return out;
    }

    public static JsonArray ofInts(int[] values) {
        JsonArray a = new JsonArray();
        for (int v : values) a.add((long) v);
        return a;
    }

    public int[] toIntArray() {
        int[] out = new int[size()];
        for (int i = 0; i < out.length; i++) {
            Object v = items.get(i);
            if (!(v instanceof Number)) throw new JsonException("Expected a number at index " + i);
            out[i] = ((Number) v).intValue();
        }
        return out;
    }
}
