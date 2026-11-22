package network;

import dataStructures.DynamicArray;

public final class JsonObject {
    private final DynamicArray<String> keys = new DynamicArray<>();
    private final DynamicArray<Object> values = new DynamicArray<>();

    public JsonObject put(String key, Object value) {
        Object normalized = value instanceof Integer i ? Long.valueOf(i) : value;
        int index = indexOf(key);
        if (index >= 0) {
            values.set(normalized, index);
        } else {
            keys.add(key);
            values.add(normalized);
        }
        return this;
    }

    private int indexOf(String key) {
        for (int i = 0; i < keys.getSize(); i++) {
            if (keys.get(i).equals(key)) return i;
        }
        return -1;
    }

    public boolean has(String key) { return indexOf(key) >= 0; }
    public int size() { return keys.getSize(); }
    public String keyAt(int i) { return keys.get(i); }
    public Object valueAt(int i) { return values.get(i); }

    public Object get(String key) {
        int i = indexOf(key);
        return i < 0 ? null : values.get(i);
    }

    public String getString(String key) {
        return get(key) instanceof String s ? s : null;
    }

    public int getInt(String key, int fallback) {
        return get(key) instanceof Number n ? n.intValue() : fallback;
    }

    public boolean getBool(String key, boolean fallback) {
        return get(key) instanceof Boolean b ? b : fallback;
    }

    public JsonObject getObject(String key) {
        return get(key) instanceof JsonObject o ? o : null;
    }

    public JsonArray getArray(String key) {
        return get(key) instanceof JsonArray a ? a : null;
    }

    @Override
    public String toString() { return Json.stringify(this); }
}
