package com.mechanical.viewer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses the item notes' markup: [[id]] is an item link, **x** is bold. */
public final class Note {
    /** Exactly one of text / itemId is set. */
    public record Piece(String text, String itemId, boolean bold) {}

    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*", Pattern.DOTALL);
    private static final Pattern LINK = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");

    private Note() {}

    public static List<Piece> parse(String raw) {
        List<Piece> out = new ArrayList<>();
        Matcher m = BOLD.matcher(raw);
        int at = 0;
        while (m.find()) {
            spans(raw.substring(at, m.start()), false, out);
            spans(m.group(1), true, out); // links inside bold stay links
            at = m.end();
        }
        spans(raw.substring(at), false, out);
        return out;
    }

    private static void spans(String s, boolean bold, List<Piece> out) {
        Matcher m = LINK.matcher(s);
        int at = 0;
        while (m.find()) {
            if (m.start() > at) out.add(new Piece(s.substring(at, m.start()), null, bold));
            out.add(new Piece(null, qualify(m.group(1)), bold));
            at = m.end();
        }
        if (at < s.length()) out.add(new Piece(s.substring(at), null, bold));
    }

    public static Set<String> links(String raw) {
        Set<String> ids = new LinkedHashSet<>();
        for (Piece p : parse(raw)) if (p.itemId() != null) ids.add(p.itemId());
        return ids;
    }

    private static String qualify(String id) {
        id = id.trim();
        return id.indexOf(':') >= 0 ? id : "mechanical:" + id;
    }
}
