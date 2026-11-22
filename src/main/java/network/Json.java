package network;

public final class Json {
    private static final int MAX_DEPTH = 64;

    private Json() { }

    public static String stringify(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(sb, s);
        } else if (value instanceof Boolean b) {
            sb.append(b ? "true" : "false");
        } else if (value instanceof Double d) {
            if (d.isNaN() || d.isInfinite()) throw new JsonException("NaN/Infinity cannot be represented in JSON");
            sb.append(d);
        } else if (value instanceof Number n) {
            sb.append(n.longValue());
        } else if (value instanceof JsonObject o) {
            sb.append('{');
            for (int i = 0; i < o.size(); i++) {
                if (i > 0) sb.append(',');
                writeString(sb, o.keyAt(i));
                sb.append(':');
                write(sb, o.valueAt(i));
            }
            sb.append('}');
        } else if (value instanceof JsonArray a) {
            sb.append('[');
            for (int i = 0; i < a.size(); i++) {
                if (i > 0) sb.append(',');
                write(sb, a.get(i));
            }
            sb.append(']');
        } else {
            throw new JsonException("Cannot serialize " + value.getClass().getSimpleName());
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    public static Object parse(String text) {
        if (text == null) throw new JsonException("No input");
        Parser p = new Parser(text);
        p.skipWhitespace();
        Object value = p.parseValue(0);
        p.skipWhitespace();
        if (!p.atEnd()) throw p.error("Unexpected data after the JSON value");
        return value;
    }

    public static JsonObject parseObject(String text) {
        Object value = parse(text);
        if (!(value instanceof JsonObject)) throw new JsonException("A JSON object was expected");
        return (JsonObject) value;
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) { this.s = s; }

        boolean atEnd() { return pos >= s.length(); }

        JsonException error(String message) { return new JsonException(message + " at position " + pos); }

        void skipWhitespace() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
                else break;
            }
        }

        Object parseValue(int depth) {
            if (depth > MAX_DEPTH) throw error("Nesting too deep");
            if (atEnd()) throw error("Unexpected end of input");
            char c = s.charAt(pos);
            switch (c) {
                case '{': return parseObject(depth);
                case '[': return parseArray(depth);
                case '"': return parseString();
                case 't': expectWord("true"); return Boolean.TRUE;
                case 'f': expectWord("false"); return Boolean.FALSE;
                case 'n': expectWord("null"); return null;
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();
                    throw error("Unexpected character '" + c + "'");
            }
        }

        void expectWord(String word) {
            if (!s.startsWith(word, pos)) throw error("Invalid literal");
            pos += word.length();
        }

        JsonObject parseObject(int depth) {
            JsonObject object = new JsonObject();
            pos++;
            skipWhitespace();
            if (peek('}')) { pos++; return object; }
            while (true) {
                skipWhitespace();
                if (!peek('"')) throw error("Object key expected");
                String key = parseString();
                skipWhitespace();
                if (!peek(':')) throw error("':' expected");
                pos++;
                skipWhitespace();
                object.put(key, parseValue(depth + 1));
                skipWhitespace();
                if (peek(',')) { pos++; continue; }
                if (peek('}')) { pos++; return object; }
                throw error("',' or '}' expected");
            }
        }

        JsonArray parseArray(int depth) {
            JsonArray array = new JsonArray();
            pos++;
            skipWhitespace();
            if (peek(']')) { pos++; return array; }
            while (true) {
                skipWhitespace();
                array.add(parseValue(depth + 1));
                skipWhitespace();
                if (peek(',')) { pos++; continue; }
                if (peek(']')) { pos++; return array; }
                throw error("',' or ']' expected");
            }
        }

        boolean peek(char c) { return pos < s.length() && s.charAt(pos) == c; }

        String parseString() {
            pos++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (atEnd()) throw error("Unterminated string");
                char c = s.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c < 0x20) throw error("Control character in string");
                if (c != '\\') { sb.append(c); continue; }
                if (atEnd()) throw error("Unterminated escape");
                char e = s.charAt(pos++);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u': {
                        if (pos + 4 > s.length()) throw error("Bad \\u escape");
                        int code = 0;
                        for (int i = 0; i < 4; i++) {
                            int digit = Character.digit(s.charAt(pos++), 16);
                            if (digit < 0) throw error("Bad \\u escape");
                            code = code * 16 + digit;
                        }
                        sb.append((char) code);
                        break;
                    }
                    default: throw error("Unknown escape \\" + e);
                }
            }
        }

        Object parseNumber() {
            int start = pos;
            if (peek('-')) pos++;
            if (atEnd() || !Character.isDigit(s.charAt(pos))) throw error("Digit expected");
            if (s.charAt(pos) == '0' && pos + 1 < s.length() && Character.isDigit(s.charAt(pos + 1))) throw error("Leading zero");
            while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
            boolean isDouble = false;
            if (peek('.')) {
                isDouble = true;
                pos++;
                if (atEnd() || !Character.isDigit(s.charAt(pos))) throw error("Digit expected after '.'");
                while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
            }
            if (peek('e') || peek('E')) {
                isDouble = true;
                pos++;
                if (peek('+') || peek('-')) pos++;
                if (atEnd() || !Character.isDigit(s.charAt(pos))) throw error("Digit expected in exponent");
                while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
            }
            String token = s.substring(start, pos);
            try {
                return isDouble ? (Object) Double.valueOf(token) : (Object) Long.valueOf(token);
            } catch (NumberFormatException e) {
                throw error("Number out of range");
            }
        }
    }
}
