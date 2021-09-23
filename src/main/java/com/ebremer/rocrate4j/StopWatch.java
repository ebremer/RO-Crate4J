package com.ebremer.rocrate4j;

/**
 *
 * @author erich
 */
public final class StopWatch {
    private long total;
    private long begin;
    
    public StopWatch() {
        Reset();
    }
    
    public void Reset() {
        total = System.nanoTime();
        begin = total;
    }
    
    public void Lapse(String msg) {
        System.out.println(msg+" --> "+((System.nanoTime()-total)/1000000000d)+" seconds");
    }

    public void LapStart(String msg) {
        begin = System.nanoTime();
        System.out.println(msg+" --> "+((System.nanoTime()-begin)/1000000000d)+" seconds");   
    }
    
    public void Lap(String msg) {
        System.out.println(msg+" --> "+((System.nanoTime()-begin)/1000000000d)+" seconds");
        begin = System.nanoTime();   
    }
}
