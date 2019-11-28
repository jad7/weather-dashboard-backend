package com.jad.dashboard.weather.math;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class RingBufferTimeserial {

   private final TimePoint[] array;
   private int size = 0;
   private int position = 0;


   public RingBufferTimeserial(int maxSize) {
      this.array = new TimePoint[maxSize];
      for (int i = 0; i < maxSize; i++) {
         array[i] = new TimePoint();
      }
   }



   public void add(double val) {
      add(System.currentTimeMillis(), val);
   }

   public void add(long time, double val) {
      if (this.position == this.array.length) {
         this.position = 0;
      }
      TimePoint timePoint = array[position];
      timePoint.time = time;
      timePoint.value = val;
      position++;
      if (size < array.length) {
         size++;
      }
   }

   public Double findMax(long fromTime) {
      int index = findIndex(fromTime);
      if (index < 0) {
         index = ~index;
      }
      if (index <= size) {
         return null;
      }
      double max = Double.MIN_VALUE;
      for (int i = index; i < size; i++) {
         max = Math.max(max, array[toIndx(i)].value);
      }
      return max;
   }

   public Double findMax(int val, TimeUnit unit) {
      return findMax(System.currentTimeMillis() - unit.toMillis(val));
   }

   private int findIndex(long timeKey) {
      int low = 0;
      int high = size;

      while (low <= high) {
         int mid = (low + high) >>> 1;

         long midVal = array[toIndx(mid)].time;

         if (midVal < timeKey)
            low = mid + 1;
         else if (midVal > timeKey)
            high = mid - 1;
         else
            return mid; // key found
      }
      return -(low + 1);  // key not found.
   }

   private int toIndx(int ind) {
      return (ind + position) % size;
   }


   public Double avg(int forLast, TimeUnit timeUnit) {
      return avg(System.currentTimeMillis() - timeUnit.toMillis(forLast));
   }

   public Double avg(long time) {
      int index = findIndex(time);
      if (index < 0) {
         index = ~index;
      }
      double avg = 0;
      int count = size - index;
      if (count <= 0) {
         return null;
      }
      for (int i = index; i < size; i++) {
         avg += array[toIndx(i)].value / count;
      }
      return avg;
   }

   public Double getLast() {
      if (position == 0) {
         return null;
      }
      return array[position - 1].value;
   }

    public Stream<TimePoint> getStreamFrom(long from) {
       final int[] index = {findIndex(from)};
       if (index[0] < 0) {
          index[0] = ~index[0];
       }
       int count = size - index[0];
       if (count <= 0) {
          return Stream.empty();
       }
       return Stream.generate(() -> array[toIndx(index[0]++)]).limit(count);
    }

   public Stream<TimePoint> getStreamFromTo(long from, long to) {
      final int[] index = {findIndex(from), findIndex(to)};
      if (index[0] < 0) {
         index[0] = ~index[0];
      }
      if (index[1] < 0) {
         index[1] = ~index[1];
      }
      int count = index[1] - index[0];
      if (count <= 0) {
         return Stream.empty();
      }
      return Stream.generate(() -> array[toIndx(index[0]++)]).limit(Math.min(count + 1, size - index[0]));
   }

   /**
    * min - oldest, max - newest
    */
    public MinMax<Long> timeMinMax() {
       if (position == 0) {
          return null;
       }
       return new MinMax<>(array[toIndx(0)].time, array[toIndx(size - 1)].time);
    }



   @Getter
   public static class MinMax<T> {
       private MinMax(T min, T max) {
          this.min = min;
          this.max = max;
       }

       private MinMax() {
       }

       private T min,max;
   }

    @ToString @Getter//For debug
   public static class TimePoint {
      long time;
      double value;
   }


}
