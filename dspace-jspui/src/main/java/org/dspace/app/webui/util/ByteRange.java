package org.dspace.app.webui.util;

final public class ByteRange {

    
    private final long start;
    private final long end;

    
    public ByteRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    
    public ByteRange(String string) throws NumberFormatException {
        string = string.trim();
        int dashPos = string.indexOf('-');
        int length = string.length();
        if (string.indexOf(',') != -1) {
            throw new NumberFormatException("Simple ByteRange String contains a comma.");
        }
        if (dashPos > 0) {
            this.start = Integer.parseInt(string.substring(0, dashPos));
        } else {
            this.start = Long.MIN_VALUE;
        }
        if (dashPos < length - 1) {
            this.end = Integer.parseInt(string.substring(dashPos + 1, length));
        } else {
            this.end = Long.MAX_VALUE;
        }
        if (this.start > this.end) {
            throw new NumberFormatException("Start value is greater than end value.");
        }
    }

    
    public long getStart() {
        return this.start;
    }

    
    public long getEnd() {
        return this.end;
    }

    
    public long length() {
        return this.end - this.start + 1;
    }

    
    public ByteRange intersection(ByteRange range) {
        if (range.end < this.start || this.end < range.start) {
            return null;
        } else {
            long start = (this.start > range.start) ? this.start : range.start;
            long end = (this.end < range.end) ? this.end : range.end;
            return new ByteRange(start, end);
        }
    }


    public String toString() {
        return this.start + "-" + this.end;
    }

}
