package timekiller;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Game {

    public static void main(String[] args) {
        Queue<Round> queue = new PriorityQueue<>(Comparator.comparingInt(r -> r.all));
        Set<Round> cache = new HashSet<>();
        AtomicInteger count = new AtomicInteger();

        queue.add(Round.INITIAL);

        while (true) {
            Round round = queue.poll();

            if (round == null)
                return;

            round.moves(r -> {
                if (r.win()) {
                    if (!cache.contains(r)) {
                        cache.add(r);

                        System.out.println(r);

                        System.out.println(count.incrementAndGet());
                        System.out.println("-----------------------------------------------------------------------------");
                    }
                } else if (r.all <= 70 && cache.add(r)) {
                    queue.add(r);
                }
            });
        }
    }

    private static class Round {
        private final Round pr;
        private final Round cr;
        private final Round prev;
        private final long[] rows;
        private final int cnt;
        private final int all;

        public static final Round INITIAL = new Round(null, null, new long[] {
            0x987654321L, // reversed
            0x141312111L,
            0x918171615L
        }, 27, 27);

        public Round(Round pr, Round prev, long[] rows, int cnt, int all) {
            this.pr = pr;
            this.cr = this;
            this.prev = prev;
            this.rows = rows;
            this.cnt = cnt;
            this.all = all;
        }

        public Round(Round pr, Round cr, Round prev, long[] rows, int cnt, int all) {
            this.pr = pr;
            this.cr = cr;
            this.prev = prev;
            this.rows = rows;
            this.cnt = cnt;
            this.all = all;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            Round that = (Round) o;

            return all == that.all && Arrays.equals(rows, that.rows) && Objects.equals(pr, that.pr);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(pr) * 31 + Arrays.hashCode(rows);
        }

        @Override
        public String toString() {
            return toString0(new StringBuilder()) + '\n' + all;
        }

        private String toString0(StringBuilder sb) {
            if (prev != null) {
                prev.toString0(sb);
                sb.append('\n');
            }

            for (long row : rows) {
                for (int i = 0; i < 9; i++) {
                    int val = get(row, i);

                    if (val == 0)
                        sb.append('#');
                    else if (val == 0xF)
                        continue;
                    else
                        sb.append(val);

                }

                sb.append('\n');
            }

            return sb.toString();
        }

        private int get(long row, int i) {
            return (int) ((row >> (i << 2)) & 0xFL);
        }

        private long clear(long row, int i) {
            return row & ~(0xFL << (i << 2));
        }

        private long set(long row, int i, int val) {
            assert get(row, i) == 0;

            return row | (((long)val) << (i << 2));
        }

        public boolean win() {
            return cnt == 0;
        }

        public void moves(Consumer<Round> consumer) {
            assert !win();

            int tail = 8 - (all - 1) % 9;
            int length = rows.length;

            int movesDone = 0;

            int max = length * 9 - tail;
            for (int i = 0; i < max; i++) {
                int val = get(rows[i / 9], i % 9);
                if (val == 0)
                    continue;

                {
                    int j = i + 1;
                    while (j < max && get(rows[j / 9], j % 9) == 0)
                        ++j;

                    if (j < max) {
                        int nextVal = get(rows[j / 9], j % 9);

                        if (val == nextVal || val + nextVal == 10) {
                            long[] newRows = rows.clone();

                            newRows[i / 9] = clear(rows[i / 9], i % 9);
                            newRows[j / 9] = clear(newRows[j / 9], j % 9);

                            ++movesDone;
                            consumer.accept(new Round(pr, cr, this, compact(newRows), cnt - 2, all));
                        }
                    }
                }

                {
                    int r = i / 9 + 1;
                    int c = i % 9;

                    while (r < length && get(rows[r], c) == 0)
                        ++r;

                    if (r < length && get(rows[r], c) != 0xF) {
                        int nextVal = get(rows[r], c);
                        if (val == nextVal || val + nextVal == 10) {
                            long[] newRows = rows.clone();

                            newRows[i / 9] = clear(rows[i / 9], c);
                            newRows[r] = clear(newRows[r], c);

                            ++movesDone;
                            consumer.accept(new Round(pr, cr, this, compact(newRows), cnt - 2, all));
                        }
                    }
                }
            }

            if (movesDone == 0)
                consumer.accept(rollover());
        }

        private Round rollover() {
            assert !win();

            int tail = 8 - (all - 1) % 9;
            int length = rows.length;
            long lastRow = rows[length - 1];

            int add = (cnt - tail + 8) / 9;

            long[] newRows = add == 0 ? rows.clone() : Arrays.copyOf(rows, length + add);

            int i = 9 * length - tail;

            while (tail != 0) {
                lastRow = clear(lastRow, 9 - tail);

                --tail;
            }

            newRows[length - 1] = lastRow;

            for (int c = 0, oI = 0; c < cnt; c++) {
                while (get(rows[oI / 9], oI % 9) == 0)
                    ++oI;

                newRows[i / 9] = set(newRows[i / 9], i % 9, get(rows[oI / 9], oI % 9));

                ++oI; ++i;
            }

            while (i < newRows.length * 9) {
                newRows[i / 9] = set(newRows[i / 9], i % 9, 0xF);
                ++i;
            }

            return new Round(cr, this, newRows, cnt * 2, all + cnt);
        }

        private long[] compact(long[] arr) {
            return arr;
//            int zeros = 0;
//            for (long row : arr) {
//                if (row == 0L)
//                    ++zeros;
//            }
//
//            if (zeros == 0)
//                return arr;
//
//            long[] newArr = new long[arr.length - zeros];
//            for (int r = 0, nR= 0; r < arr.length; r++) {
//                if (arr[r] != 0L) {
//                    newArr[nR] = arr[r];
//                    ++nR;
//                }
//            }
//
//            return newArr;
        }
    }
}
