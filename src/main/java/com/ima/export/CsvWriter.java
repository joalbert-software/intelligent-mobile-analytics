package com.ima.export;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public final class CsvWriter {
    private final Writer output;

    public CsvWriter(Writer output) {
        if (output == null) {
            throw new IllegalArgumentException("writer is required");
        }
        this.output = output;
    }

    public void row(List<?> cells) throws IOException {
        for (int index = 0; index < cells.size(); index++) {
            if (index > 0) {
                output.write(',');
            }
            cell(cells.get(index));
        }
        output.write("\r\n");
    }

    private void cell(Object value) throws IOException {
        if (value == null) {
            return;
        }
        String text = value.toString();
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        boolean quote = text.indexOf(',') >= 0 || text.indexOf('"') >= 0
                || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0;
        if (quote) {
            output.write('"');
        }
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '"') {
                output.write('"');
            }
            output.write(character);
        }
        if (quote) {
            output.write('"');
        }
    }

    public void flush() throws IOException {
        output.flush();
    }
}
