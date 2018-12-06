package ru.shemplo.pluses.struct;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PairTest {

    @Test
    @DisplayName("Test pair constructor")
    void pairConstructorTest() {
        Pair<Integer, Integer> p = new Pair<>(1, 2);
        int first = p.F;
        assertEquals(1, first);
        int second = p.S;
        assertEquals(2, second);
    }
}
