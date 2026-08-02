package com.arlight.tetris.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public class Bag7Randomizer {
    private final Random random;
    private final Deque<TetrominoType> queue = new ArrayDeque<>();

    public Bag7Randomizer(long seed) {
        this.random = new Random(seed);
        refillBag();
        refillBag();
    }

    private void refillBag() {
        List<TetrominoType> bag = new ArrayList<>(List.of(TetrominoType.values()));
        Collections.shuffle(bag, random);
        queue.addAll(bag);
    }

    public TetrominoType next() {
        if (queue.size() <= 7) refillBag();
        return queue.poll();
    }

    public List<TetrominoType> peek(int count) {
        while (queue.size() < count) refillBag();
        return new ArrayList<>(queue).subList(0, count);
    }
}
