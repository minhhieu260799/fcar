package com.fcar.core.util;

import org.springframework.web.util.HtmlUtils;

/**
 * HTML escaping for email bodies and other server-generated HTML snippets.
 */
public final class HtmlEscapes {

    private HtmlEscapes() {
    }

    public static String html(String s) {
        if (s == null) {
            return "";
        }
        return HtmlUtils.htmlEscape(s);
    }
}
