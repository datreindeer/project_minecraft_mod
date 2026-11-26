package com.mechanical.quests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Places a chapter's quest tree for the quest screen, left to right: a quest's follow-ups stand in the column to its
 * right, each branch in its own horizontal band, so nothing overlaps. A plain tidy tree is far too tall for the big
 * chapters (some quests have 40 follow-ups that lead nowhere further), so four or more such leaves are packed into a
 * grid. Lines run only in the gutters: to a grid quest along the gutter above its row, then down into it.
 *
 * A big chapter would still be a narrow ribbon several screens tall, so its root's branches (and those of any single
 * branch still very tall) are folded into the columns that show it largest, joined by one line along the top.
 */
public final class QuestLayout {
    /** A node is SIZE square; GAP is the free column between a quest and its follow-ups (the lines' trunk is its middle). */
    public static final int SIZE = 26, GAP = 24, ROW = 34, GRID_X = 34, GRID_Y = 38, MAX_COLS = 7;
    private static final int BRANCH_GAP = 2;
    /** A chapter taller than this is folded into columns, aiming for this width to height. */
    private static final int FOLD_OVER = 420, FOLD_BRANCH_OVER = 800;
    private static final double SHAPE = 1.6;

    /** Top-left corners by node. */
    public record Point(int x, int y) {}

    /** The line from {@code parent} to {@code child} as points, alternating horizontal and vertical segments. */
    public record Line<T>(T parent, T child, int[] xy) {}

    public record Result<T>(Map<T, Point> nodes, List<Line<T>> lines, int width, int height) {}

    public static <T> Result<T> layout(T root, Function<T, List<T>> children) {
        return arrange(root, children, FOLD_OVER);
    }

    /** {@code node}'s subtree at (0, 0): straight, or folded if that is taller than {@code foldOver}. */
    private static <T> Result<T> arrange(T node, Function<T, List<T>> children, int foldOver) {
        Result<T> straight = straight(node, children);
        if (straight.height() <= foldOver) return straight;
        Result<T> folded = fold(node, children);
        return folded != null && folded.height() < straight.height() ? folded : straight;
    }

    /** Adds {@code part}, moved by (dx, dy). */
    private static <T> void add(Result<T> part, int dx, int dy, Map<T, Point> nodes, List<Line<T>> lines) {
        part.nodes().forEach((n, p) -> nodes.put(n, new Point(p.x() + dx, p.y() + dy)));
        for (Line<T> line : part.lines()) {
            int[] xy = line.xy().clone();
            for (int i = 0; i < xy.length; i += 2) {
                xy[i] += dx;
                xy[i + 1] += dy;
            }
            lines.add(new Line<>(line.parent(), line.child(), xy));
        }
    }

    private static int right(Map<?, Point> nodes) {
        return nodes.values().stream().mapToInt(p -> p.x() + SIZE).max().orElse(0);
    }

    /** One of a quest's branches (or its grid of leaves), laid out on its own with its left edge at x = 0. */
    private record Block<T>(List<T> tops, boolean grid, Result<T> part) {
        int width() {
            return part.width();
        }

        int height() {
            return part.height() + BRANCH_GAP;
        }
    }

    private static <T> Block<T> block(List<T> tops, boolean grid, Function<T, List<T>> children) {
        if (!grid) return new Block<>(tops, false, arrange(tops.get(0), children, FOLD_BRANCH_OVER));
        Map<T, Point> nodes = new HashMap<>();
        int height = grid(tops, 0, 0, nodes);
        return new Block<>(tops, true, new Result<>(nodes, List.of(), right(nodes), height));
    }

    /** {@code root}'s blocks shared out, in order, over the columns that show it largest; null if one column is best. */
    private static <T> Result<T> fold(T root, Function<T, List<T>> children) {
        List<T> all = children.apply(root), leaves = leaves(all, children);
        List<Block<T>> blocks = new ArrayList<>();
        for (T child : all)
            if (!leaves.contains(child)) blocks.add(block(List.of(child), false, children));
        if (!leaves.isEmpty()) blocks.add(block(leaves, true, children));
        int total = blocks.stream().mapToInt(Block::height).sum();

        // columns filled up to a shared height; of the heights, the one whose chapter shows largest on a SHAPE screen
        int tallest = blocks.stream().mapToInt(Block::height).max().orElse(0);
        List<List<Block<T>>> best = null;
        double bestNeed = Double.MAX_VALUE;
        for (int limit = tallest; limit < total + ROW; limit += ROW / 2) {
            List<List<Block<T>>> columns = new ArrayList<>();
            List<Block<T>> column = new ArrayList<>();
            int filled = 0, width = SIZE, height = 0;
            for (Block<T> b : blocks) {
                if (!column.isEmpty() && filled + b.height() > limit) {
                    columns.add(column);
                    column = new ArrayList<>();
                    filled = 0;
                }
                column.add(b);
                filled += b.height();
            }
            columns.add(column);
            for (List<Block<T>> c : columns) {
                width += GAP + c.stream().mapToInt(Block::width).max().orElse(0);
                height = Math.max(height, c.stream().mapToInt(Block::height).sum());
            }
            double need = Math.max(width / SHAPE, height);
            if (need < bestNeed) {
                bestNeed = need;
                best = columns;
            }
        }
        if (best == null || best.size() < 2) return null;

        Map<T, Point> nodes = new HashMap<>();
        List<Line<T>> lines = new ArrayList<>();
        int bus = 2, head = 8, x = SIZE + GAP, width = 0, height = 0;
        int mid = 0, firstTrunk = SIZE + GAP / 2;
        for (List<Block<T>> column : best) {
            int y = head, columnWidth = 0;
            for (Block<T> b : column) {
                add(b.part(), x, y, nodes, lines);
                if (!nodes.containsKey(root)) { // the root: level with the first thing it leads to, at the top
                    int rootY = nodes.get(b.tops().get(0)).y();
                    mid = rootY + SIZE / 2;
                    nodes.put(root, new Point(0, rootY));
                }
                int trunk = x - GAP / 2;
                for (T top : b.tops()) {
                    Point c = nodes.get(top);
                    List<Integer> xy = new ArrayList<>(List.of(SIZE, mid, firstTrunk, mid));
                    if (trunk != firstTrunk) xy.addAll(List.of(firstTrunk, bus, trunk, bus));
                    if (b.grid()) {
                        int row = c.y() - 6;
                        xy.addAll(List.of(trunk, row, c.x() + SIZE / 2, row, c.x() + SIZE / 2, c.y()));
                    } else {
                        xy.addAll(List.of(trunk, c.y() + SIZE / 2, c.x(), c.y() + SIZE / 2));
                    }
                    lines.add(new Line<>(root, top, xy.stream().mapToInt(Integer::intValue).toArray()));
                }
                y += b.height();
                columnWidth = Math.max(columnWidth, b.width());
            }
            height = Math.max(height, y);
            width = x + columnWidth;
            x += columnWidth + GAP;
        }
        return new Result<>(nodes, lines, width, height);
    }

    /** The follow-ups that go into a grid: those that lead no further, if there are four or more. */
    private static <T> List<T> leaves(List<T> all, Function<T, List<T>> children) {
        List<T> leaves = all.stream().filter(c -> children.apply(c).isEmpty()).toList();
        return leaves.size() >= 4 ? leaves : List.of();
    }

    /** Leaves packed in a grid with its left edge at {@code x}; returns its height. */
    private static <T> int grid(List<T> leaves, int x, int top, Map<T, Point> nodes) {
        int cols = Math.min(MAX_COLS, (int) Math.ceil(Math.sqrt(leaves.size() * 1.2)));
        for (int i = 0; i < leaves.size(); i++)
            nodes.put(leaves.get(i), new Point(x + i % cols * GRID_X, top + i / cols * GRID_Y + (GRID_Y - SIZE) - 2));
        return (leaves.size() + cols - 1) / cols * GRID_Y;
    }

    /** The follow-ups stacked in one column to the right, each branch in its own band, and the quest level with their middle. */
    private static <T> Result<T> straight(T node, Function<T, List<T>> children) {
        List<T> all = children.apply(node), leaves = leaves(all, children);
        Map<T, Point> nodes = new HashMap<>();
        List<Line<T>> lines = new ArrayList<>();
        int x = 0, top = 0, cx = SIZE + GAP, height = 0;
        List<T> direct = new ArrayList<>(); // follow-ups with a line straight from this quest
        for (T child : all) {
            if (leaves.contains(child)) continue;
            Result<T> part = arrange(child, children, FOLD_BRANCH_OVER);
            add(part, cx, height, nodes, lines);
            height += part.height() + BRANCH_GAP;
            direct.add(child);
        }
        if (!leaves.isEmpty()) height += grid(leaves, cx, height, nodes) + BRANCH_GAP;
        height = Math.max(ROW, height - (all.isEmpty() ? 0 : BRANCH_GAP));

        // this quest: level with the middle of its follow-ups (its only one: a straight line)
        int y = top + (ROW - SIZE) / 2;
        if (!all.isEmpty()) {
            int first = Integer.MAX_VALUE, last = Integer.MIN_VALUE;
            for (T child : all) {
                first = Math.min(first, nodes.get(child).y());
                last = Math.max(last, nodes.get(child).y());
            }
            y = (first + last) / 2;
        }
        nodes.put(node, new Point(x, y));

        int mid = y + SIZE / 2, trunk = x + SIZE + GAP / 2;
        for (T child : direct) {
            Point c = nodes.get(child);
            lines.add(new Line<>(node, child, new int[] {x + SIZE, mid, trunk, mid, trunk, c.y() + SIZE / 2, c.x(), c.y() + SIZE / 2}));
        }
        for (T child : leaves) {
            Point c = nodes.get(child);
            int bus = c.y() - 6;
            lines.add(new Line<>(node, child, new int[] {x + SIZE, mid, trunk, mid, trunk, bus, c.x() + SIZE / 2, bus, c.x() + SIZE / 2, c.y()}));
        }
        return new Result<>(nodes, lines, right(nodes), height);
    }

    private QuestLayout() {}
}
