package com.svgs.helper;

public class JTimer {
    private String header;
    private long startTime;
    private long og;
    public JTimer(String header) {
        this.header = header;
        startTime = System.nanoTime();
        og = startTime;
    }

    public void lap() {
        long time = System.nanoTime();
        System.out.printf("%s %.3fs%n", header, (time-startTime)/1e9);
    }

    public void lap(String name) {
        long time = System.nanoTime();
        System.out.printf("%s %.3fs%n", header + " " + name, (time-startTime)/1e9);
    }

    public void reset() {
        startTime = System.nanoTime();
    }

    public void reset(String name) {
        lap(name);
        startTime = System.nanoTime();
    }

    public void og(String name) {
        long time = System.nanoTime();
        System.out.printf("%s %.3fs%n", (header + " " + name), (time-og)/1e9);
    }
}
