package com.jad.dashboard.weather.math;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;

class RingBufferTimeserialTest {

    @Test
    void getStreamFromHP() { //HP - happy pass
        final RingBufferTimeserial timeserial = new RingBufferTimeserial(8);
        for (int i = 0; i < 10; i++) {
            timeserial.add(i, i);
        }
        final Stream<RingBufferTimeserial.TimePoint> streamFrom = timeserial.getStreamFrom(3);
        final Set<Double> set = streamFrom.map(RingBufferTimeserial.TimePoint::getValue)
                .collect(toSet());

        assertEquals(7, set.size());
        for (double i = 3; i < 10; i++) {
            Assertions.assertTrue(set.contains(i));
        }

    }
}