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

    private static final Pattern TOKEN = Pattern.compile("\\[\\[([^\\]]+)\\]\\]|\\*\\*(.+?)\\*\\*", Pattern.DOTALL);

    private Note() {}

    public static List<Piece> parse(String raw) {
        List<Piece> out = new ArrayList<>();
        Matcher m = TOKEN.matcher(raw);
        int at = 0;
        while (m.find()) {
            if (m.start() > at) out.add(new Piece(raw.substring(at, m.start()), null, false));
            if (m.group(1) != null) out.add(new Piece(null, qualify(m.group(1)), false));
            else out.add(new Piece(m.group(2), null, true));
            at = m.end();
        }
        if (at < raw.length()) out.add(new Piece(raw.substring(at), null, false));
        return out;
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
