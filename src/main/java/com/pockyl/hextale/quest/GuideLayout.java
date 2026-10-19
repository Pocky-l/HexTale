package com.pockyl.hextale.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * The guide's map. Steps with a place in their data ({@code pos}) stand there; the others (a device added in a data pack,
 * say) take the first free spot on rings around their first parent, trying the direction away from where that parent
 * grows from first and then turning left and right. A spot is free when it keeps clear of every step, its link crosses
 * no other link and runs over no step, and no link runs over it. Then every link bows into a gentle arc, to the side and
 * as far as it can without touching another link or a step. Deterministic, so the server and every reader agree.
 */
final class GuideLayout {
    /** Steps without a place stand this far out from their parent's rim, then further ring by ring. */
    private static final int RING = 36;
    private static final int RING_STEP = 8;
    /** No two steps closer than this, centre to centre. */
    private static final int CROWD = 34;
    /** How far a link bows out of the straight line at most, and as a share of its length. */
    private static final double BOW = 22;
    private static final double BOW_SHARE = 0.18;
    /** Points a link's curve is checked at. */
    static final int LINK_SAMPLES = 16;

    private GuideLayout() {
    }

    /** The room a step's own medallion needs around its centre. */
    static double body(Quest quest) {
        return quest.landmark() ? 46 : quest.station() ? 32 : quest.kind() == Quest.Kind.WIP ? 17 : quest.important() ? 23 : 20;
    }

    /** Places every step (in the guide's order, parents first) and bows the links. */
    static void lay(List<Quest> all) {
        List<Quest> placed = new ArrayList<>();
        Set<Quest> isPlaced = new HashSet<>();
        for (Quest quest : all) {
            quest.definition().pos().ifPresent(pos -> {
                quest.x = pos.x();
                quest.y = pos.y();
                placed.add(quest);
                isPlaced.add(quest);
            });
        }
        List<Quest[]> links = new ArrayList<>();
        for (Quest quest : all) {
            if (quest.parent() != null && isPlaced.contains(quest) && isPlaced.contains(quest.parent())) {
                links.add(new Quest[]{quest.parent(), quest});
            }
        }
        for (Quest quest : all) {
            if (isPlaced.contains(quest)) {
                continue;
            }
            Quest parent = quest.parent();
            if (parent == null) {
                quest.x = 0;
                quest.y = 0;
            } else {
                findSpot(quest, parent, placed, links);
            }
            placed.add(quest);
            isPlaced.add(quest);
            for (Quest other : all) {
                if (other.parent() == quest && isPlaced.contains(other) || other == quest && parent != null) {
                    links.add(new Quest[]{other.parent(), other});
                }
            }
        }
        bowLinks(placed, links);
    }

    private static void findSpot(Quest quest, Quest parent, List<Quest> placed, List<Quest[]> links) {
        Quest grand = parent.parent();
        double out = grand != null ? Math.atan2(parent.y - grand.y, parent.x - grand.x) : -Math.PI / 2;
        for (int ring = 0; ring < 12; ring++) {
            double radius = body(parent) + RING + ring * RING_STEP;
            for (int turn = 0; turn < 25; turn++) {
                double angle = out + (turn % 2 == 0 ? 1 : -1) * ((turn + 1) / 2) * Math.toRadians(15);
                quest.x = (int) Math.round(parent.x + Math.cos(angle) * radius);
                quest.y = (int) Math.round(parent.y + Math.sin(angle) * radius);
                if (free(quest, parent, placed, links)) {
                    return;
                }
            }
        }
        // No room anywhere near: below the parent, where QuestGameTests.guideIsLaidOut will point at the crowding.
        quest.x = parent.x;
        quest.y = parent.y + 300;
    }

    private static boolean free(Quest quest, Quest parent, List<Quest> placed, List<Quest[]> links) {
        for (Quest other : placed) {
            if (Math.hypot(quest.x - other.x, quest.y - other.y) < Math.max(CROWD, body(quest) + body(other))) {
                return false;
            }
            if (other != parent && distanceToSegment(other, parent, quest) < body(other) - 2) {
                return false;
            }
        }
        for (Quest[] link : links) {
            if (link[0] != parent && link[1] != parent && crosses(parent, quest, link[0], link[1])) {
                return false;
            }
            if (distanceToSegment(quest, link[0], link[1]) < body(quest)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Bows every link, in order, as far as it can: first to the side its pair prefers (so neighbouring links bow
     * differently), then to the other side, then less and less. A bow is kept only when the curve keeps clear of every
     * step and crosses no other link as it is now; a straight link is always clear of links, so bowing never adds a
     * crossing.
     */
    private static void bowLinks(List<Quest> placed, List<Quest[]> links) {
        Map<Quest, double[][]> curves = new HashMap<>();
        for (Quest[] link : links) {
            link[1].bow = 0;
            curves.put(link[1], curve(link[0], link[1], 0));
        }
        for (Quest[] link : links) {
            Quest from = link[0];
            Quest to = link[1];
            double most = Math.min(BOW, Math.hypot(to.x - from.x, to.y - from.y) * BOW_SHARE);
            double side = (from.index + to.index) % 2 == 0 ? 1 : -1;
            for (double bow : new double[]{side * most, -side * most, side * most * 0.6, -side * most * 0.6, side * most * 0.3,
                    -side * most * 0.3}) {
                double[][] curve = curve(from, to, bow);
                if (clearCurve(from, to, curve, placed, links, curves)) {
                    to.bow = (float) bow;
                    curves.put(to, curve);
                    break;
                }
            }
        }
    }

    private static boolean clearCurve(Quest from, Quest to, double[][] curve, List<Quest> placed, List<Quest[]> links,
                                      Map<Quest, double[][]> curves) {
        for (Quest step : placed) {
            if (step != from && step != to && distanceToCurve(step.x, step.y, curve) < Math.max(14, body(step) - 6)) {
                return false;
            }
        }
        for (Quest[] link : links) {
            if (link[1] == to) {
                continue;
            }
            Quest shared = link[0] == from || link[0] == to ? link[0] : link[1] == from ? link[1] : null;
            if (curvesCross(curve, curves.get(link[1]), shared)) {
                return false;
            }
        }
        return true;
    }

    /** How many pairs of links cross now (a hand-placed map can make them; the game tests then fail). */
    static int crossings(List<Quest> all) {
        List<Quest> linked = all.stream().filter(quest -> quest.parent() != null).toList();
        Map<Quest, double[][]> curves = new HashMap<>();
        for (Quest quest : linked) {
            curves.put(quest, curve(quest.parent(), quest, quest.bow));
        }
        int count = 0;
        for (int i = 0; i < linked.size(); i++) {
            for (int j = i + 1; j < linked.size(); j++) {
                Quest a = linked.get(i);
                Quest b = linked.get(j);
                Quest shared = a.parent() == b.parent() ? a.parent() : a.parent() == b ? b : b.parent() == a ? a : null;
                if (curvesCross(curves.get(a), curves.get(b), shared)) {
                    count++;
                }
            }
        }
        return count;
    }

    /** A link's curve sampled from its first parent's centre to its step's centre. */
    static double[][] curve(Quest from, Quest to, double bow) {
        double[][] points = new double[LINK_SAMPLES + 1][];
        for (int i = 0; i <= LINK_SAMPLES; i++) {
            points[i] = linkPoint(from, to, bow, i / (double) LINK_SAMPLES);
        }
        return points;
    }

    /**
     * A point of the link from {@code from} to {@code to} at {@code t} (0..1): a quadratic curve whose middle bows
     * {@code bow} map pixels to the side (positive = to the right of the way from {@code from}).
     */
    static double[] linkPoint(Quest from, Quest to, double bow, double t) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double length = Math.max(1, Math.hypot(dx, dy));
        double cx = (from.x + to.x) / 2.0 - dy / length * bow;
        double cy = (from.y + to.y) / 2.0 + dx / length * bow;
        double u = 1 - t;
        return new double[]{u * u * from.x + 2 * u * t * cx + t * t * to.x, u * u * from.y + 2 * u * t * cy + t * t * to.y};
    }

    private static boolean curvesCross(double[][] a, double[][] b, @Nullable Quest shared) {
        double room = shared == null ? 0 : body(shared) + 4;
        for (int i = 0; i + 1 < a.length; i++) {
            if (near(a[i], shared, room) || near(a[i + 1], shared, room)) {
                continue;
            }
            for (int j = 0; j + 1 < b.length; j++) {
                if (near(b[j], shared, room) || near(b[j + 1], shared, room)) {
                    continue;
                }
                if (segmentsCross(a[i], a[i + 1], b[j], b[j + 1])) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean near(double[] point, @Nullable Quest step, double room) {
        return step != null && Math.hypot(point[0] - step.x, point[1] - step.y) <= room;
    }

    /** Whether two segments cross (touching ends do not count). */
    static boolean segmentsCross(double[] a0, double[] a1, double[] b0, double[] b1) {
        return turn(b0, b1, a0) * turn(b0, b1, a1) < 0 && turn(a0, a1, b0) * turn(a0, a1, b1) < 0;
    }

    private static int turn(double[] from, double[] to, double[] point) {
        return (int) Math.signum((to[0] - from[0]) * (point[1] - from[1]) - (to[1] - from[1]) * (point[0] - from[0]));
    }

    /** The distance from a point to a sampled curve. */
    static double distanceToCurve(double x, double y, double[][] curve) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i + 1 < curve.length; i++) {
            double dx = curve[i + 1][0] - curve[i][0];
            double dy = curve[i + 1][1] - curve[i][1];
            double t = Math.clamp(((x - curve[i][0]) * dx + (y - curve[i][1]) * dy) / Math.max(1e-9, dx * dx + dy * dy), 0, 1);
            best = Math.min(best, Math.hypot(curve[i][0] + dx * t - x, curve[i][1] + dy * t - y));
        }
        return best;
    }

    private static boolean crosses(Quest a0, Quest a1, Quest b0, Quest b1) {
        return side(b0, b1, a0) * side(b0, b1, a1) < 0 && side(a0, a1, b0) * side(a0, a1, b1) < 0;
    }

    private static int side(Quest from, Quest to, Quest point) {
        return Long.signum((long) (to.x - from.x) * (point.y - from.y) - (long) (to.y - from.y) * (point.x - from.x));
    }

    private static double distanceToSegment(Quest point, Quest from, Quest to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double t = Math.clamp(((point.x - from.x) * dx + (point.y - from.y) * dy) / Math.max(1e-9, dx * dx + dy * dy), 0, 1);
        return Math.hypot(from.x + dx * t - point.x, from.y + dy * t - point.y);
    }
}
