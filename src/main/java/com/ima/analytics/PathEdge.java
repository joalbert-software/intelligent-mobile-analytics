package com.ima.analytics;

import java.util.Objects;

public final class PathEdge implements Comparable<PathEdge> {
    private final String from;
    private final String to;

    public PathEdge(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    @Override
    public int compareTo(PathEdge other) {
        int first = from.compareTo(other.from);
        return first == 0 ? to.compareTo(other.to) : first;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PathEdge)) {
            return false;
        }
        PathEdge edge = (PathEdge) other;
        return from.equals(edge.from) && to.equals(edge.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}
