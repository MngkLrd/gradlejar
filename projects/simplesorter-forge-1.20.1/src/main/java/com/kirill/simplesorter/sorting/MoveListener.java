package com.kirill.simplesorter.sorting;

@FunctionalInterface
public interface MoveListener {
    void onMove(MoveRecord record);
}
