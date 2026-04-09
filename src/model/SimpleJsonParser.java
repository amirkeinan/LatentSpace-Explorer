package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A custom, lightweight JSON parser designed to read the specific embedding
 * data files.
 * This class implements a recursive descent parser to handle JSON arrays,
 * objects,
 * strings, and number arrays without external dependencies.
 */
public class SimpleJsonParser {

    private String json;
    private int pos;

    /**
     * Constructs a parser for the given JSON string.
     * 
     * @param json The JSON string to parse.
     */
    public SimpleJsonParser(String json) {
        this.json = json;
        this.pos = 0;
    }

    /**
     * Static convenience method to parse a JSON array from a string.
     * 
     * @param jsonText The JSON text.
     * @return A List of Maps representing the JSON objects in the array.
     */
    public static List<Map<String, Object>> parseJsonArray(String jsonText) {
        return new SimpleJsonParser(jsonText).parseArray();
    }

    /**
     * Parses a JSON array: [ element, element, ... ]
     * 
     * @return A List of Maps.
     */
    private List<Map<String, Object>> parseArray() {
        List<Map<String, Object>> list = new ArrayList<>();
        skipWhitespace();
        if (pos >= json.length() || json.charAt(pos) != '[') {
            throw new RuntimeException("Expected '[' at start of array " + getContext());
        }
        pos++; // skip [

        while (pos < json.length()) {
            skipWhitespace();
            if (json.charAt(pos) == ']') {
                pos++;
                break;
            }

            list.add(parseObject());

            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == ',') {
                pos++;
            }
        }
        return list;
    }

    /**
     * Parses a JSON object: { key: value, ... }
     * 
     * @return A Map representing the object.
     */
    private Map<String, Object> parseObject() {
        Map<String, Object> map = new HashMap<>();
        skipWhitespace();
        if (json.charAt(pos) != '{') {
            throw new RuntimeException("Expected '{' at start of object " + getContext());
        }
        pos++;

        while (pos < json.length()) {
            skipWhitespace();
            if (json.charAt(pos) == '}') {
                pos++;
                return map;
            }

            String key = parseString();
            skipWhitespace();
            if (pos >= json.length() || json.charAt(pos) != ':') {
                throw new RuntimeException("Expected ':' after key '" + key + "' " + getContext());
            }
            pos++;

            Object value = parseValue();
            map.put(key, value);

            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == ',') {
                pos++;
            }
        }
        return map;
    }

    /**
     * Parses a JSON value (string, array, or number).
     * 
     * @return The parsed Object.
     */
    private Object parseValue() {
        skipWhitespace();
        if (pos >= json.length())
            throw new RuntimeException("Unexpected end of input " + getContext());
        char c = json.charAt(pos);
        if (c == '"') {
            return parseString();
        } else if (c == '[') {
            return parseDoubleArray();
        } else if (Character.isDigit(c) || c == '-') {
            return parseNumber();
        } else {
            throw new RuntimeException("Unexpected character '" + c + "' " + getContext());
        }
    }

    /**
     * Parses a JSON string, handling simple escape sequences.
     * 
     * @return The distinct string value.
     */
    private String parseString() {
        skipWhitespace();
        if (json.charAt(pos) != '"')
            throw new RuntimeException("Expected '\"' " + getContext());
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < json.length()) {
            char c = json.charAt(pos);
            if (c == '\\') {
                pos++;
                if (pos >= json.length())
                    throw new RuntimeException("Unexpected end in string escape " + getContext());
                char escaped = json.charAt(pos);
                switch (escaped) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    default:
                        sb.append(escaped);
                        break;
                }
                pos++;
            } else if (c == '"') {
                pos++;
                return sb.toString();
            } else {
                sb.append(c);
                pos++;
            }
        }
        throw new RuntimeException("Unterminated string " + getContext());
    }

    /**
     * Parses a JSON array of numbers (e.g., [1.0, 2.5, -3.1]).
     * 
     * @return A double array.
     */
    private double[] parseDoubleArray() {
        skipWhitespace();
        if (json.charAt(pos) != '[')
            throw new RuntimeException("Expected '[' " + getContext());
        pos++;

        List<Double> doubles = new ArrayList<>();
        while (pos < json.length()) {
            skipWhitespace();
            if (json.charAt(pos) == ']') {
                pos++;
                break;
            }

            doubles.add(parseNumber());

            skipWhitespace();
            if (json.charAt(pos) == ',') {
                pos++;
            }
        }

        double[] arr = new double[doubles.size()];
        for (int i = 0; i < doubles.size(); i++)
            arr[i] = doubles.get(i);
        return arr;
    }

    /**
     * Parses a numeric value.
     * 
     * @return The parsed double.
     */
    private double parseNumber() {
        skipWhitespace();
        int start = pos;
        while (pos < json.length() && (Character.isDigit(json.charAt(pos)) || json.charAt(pos) == '.'
                || json.charAt(pos) == '-' || json.charAt(pos) == 'e' || json.charAt(pos) == 'E')) {
            pos++;
        }
        String numStr = json.substring(start, pos);
        try {
            return Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid number format: " + numStr + " " + getContext());
        }
    }

    /**
     * Advances the position index past any whitespace.
     */
    private void skipWhitespace() {
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
    }

    /**
     * Provides a snippet of the JSON around the current position for error logging.
     * 
     * @return A string showing context.
     */
    private String getContext() {
        int start = Math.max(0, pos - 20);
        int end = Math.min(json.length(), pos + 20);
        return "..." + json.substring(start, end) + "... at pos " + pos;
    }
}
