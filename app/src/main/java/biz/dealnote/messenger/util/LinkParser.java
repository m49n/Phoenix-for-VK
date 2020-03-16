package biz.dealnote.messenger.util;

import android.text.SpannableStringBuilder;
import android.util.Patterns;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkParser {
    public static final Pattern MENTIONS_PATTERN = Pattern.compile("\\[((?:id|club)[0-9]+)\\|([^\\]]+)\\]");
    public static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("\\+\\d{8,15}");
    public static Pattern REPLY_URL_PATTERN;
    public static Pattern URL_PATTERN;

    private static class CharRange {
        int end;
        int start;

        public CharRange(int i, int i2) {
            this.start = i;
            this.end = i2;
        }
    }

    static {
        URL_PATTERN = null;
        REPLY_URL_PATTERN = null;
        try {
            URL_PATTERN = Pattern.compile("((?:(http|https|Http|Https|ftp|Ftp):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?(?:" + Pattern.compile("(([a-zA-Z0-9 -퟿豈-﷏ﷰ-￯]([a-zA-Z0-9 -퟿豈-﷏ﷰ-￯\\-]{0,61}[a-zA-Z0-9 -퟿豈-﷏ﷰ-￯]){0,1}\\.)+([a-zA-Z0-9-]{2,63}|рф|бел|укр)|" + Patterns.IP_ADDRESS + ")") + ")" + "(?:\\:\\d{1,5})?)" + "(\\/(?:(?:[" + "a-zA-Z0-9 -퟿豈-﷏ﷰ-￯" + "\\;\\/\\?\\:\\@\\&\\=\\#\\~" + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?" + "(?:\\b|$)");
            REPLY_URL_PATTERN = Pattern.compile("\\[(" + URL_PATTERN + ")\\|([^\\]]+)\\]");
        } catch (Exception e) {
        }
    }

    public static CharSequence parseLinks(CharSequence charSequence) {
        return parseLinks(charSequence, 15);
    }

    public static CharSequence parseLinks(CharSequence charSequence, int i) {
        return parseLinks(charSequence, i, null);
    }

    public static boolean isNumber(String str) {
        return str != null && str.matches("\\d+");
    }

    public static CharSequence parseLinks(CharSequence charSequence, int i, String str) {
        SpannableStringBuilder spannableStringBuilder;
        CharSequence r12;
        if (i == 7) {
            i = 15;
        }
        ArrayList arrayList = new ArrayList();
        SpannableStringBuilder spannableStringBuilder2 = new SpannableStringBuilder(charSequence);
        if ((i & 16) > 0) {
            Matcher matcher = REPLY_URL_PATTERN.matcher(charSequence);
            SpannableStringBuilder spannableStringBuilder3 = spannableStringBuilder2;
            int i2 = 0;
            while (matcher.find()) {
                LinkSpan linkSpan = new LinkSpan(matcher.group(1), (String) null);
                SpannableStringBuilder replace = spannableStringBuilder3.replace(matcher.start() - i2, matcher.end() - i2, matcher.group(14));
                replace.setSpan(linkSpan, matcher.start() - i2, (matcher.start() - i2) + matcher.group(14).length(), 0);
                i2 += matcher.group().length() - matcher.group(14).length();
                spannableStringBuilder3 = replace;
            }
            r12 = spannableStringBuilder3;
            spannableStringBuilder = spannableStringBuilder3;
        } else {
            spannableStringBuilder = spannableStringBuilder2;
            r12 = charSequence;
        }
        if ((i & 1) > 0) {
            Matcher matcher2 = URL_PATTERN.matcher(r12);
            while (matcher2.find()) {
                if (!isNumber(matcher2.group(6)) && (matcher2.start() <= 0 || spannableStringBuilder.charAt(matcher2.start() - 1) != '@')) {
                    spannableStringBuilder.setSpan(new LinkSpan(matcher2.group(), str), matcher2.start(), matcher2.end(), 0);
                    arrayList.add(new CharRange(matcher2.start(), matcher2.end()));
                }
            }
        }
        if ((i & 8) > 0) {
            Matcher matcher3 = PHONE_NUMBER_PATTERN.matcher(r12);
            while (matcher3.find()) {
                spannableStringBuilder.setSpan(new LinkSpan("tel:" + matcher3.group(), null), matcher3.start(), matcher3.end(), 0);
            }
        }
        if ((i & 2) <= 0) {
            return spannableStringBuilder;
        }
        Matcher matcher5 = MENTIONS_PATTERN.matcher(r12);
        SpannableStringBuilder spannableStringBuilder4 = spannableStringBuilder;
        int i3 = 0;
        while (matcher5.find()) {
            LinkSpan linkSpan2 = new LinkSpan("vkontakte_mp3://vk.com/" + matcher5.group(1), str);
            SpannableStringBuilder replace2 = spannableStringBuilder4.replace(matcher5.start() - i3, matcher5.end() - i3, matcher5.group(2));
            replace2.setSpan(linkSpan2, matcher5.start() - i3, (matcher5.start() - i3) + matcher5.group(2).length(), 0);
            i3 = (matcher5.group().length() - matcher5.group(2).length()) + i3;
            spannableStringBuilder4 = replace2;
        }
        return spannableStringBuilder4;
    }
}
