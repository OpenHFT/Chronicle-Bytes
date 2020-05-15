package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.BytesUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum PropertyReplacer {
    ;

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]*)\\}");

    public static String replaceTokensWithProperties(String expression) {

        StringBuilder result = new StringBuilder(expression.length());
        int i = 0;
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
        while (matcher.find()) {
            // Strip leading "${" and trailing "}" off.
            result.append(expression.substring(i, matcher.start()));
            String property = matcher.group();
            property = property.substring(2, property.length() - 1);

            //look up property and replace
            String p = System.getProperty(property);
            result.append((p != null) ? p : matcher.group());

            i = matcher.end();
        }
        result.append(expression.substring(i));
        return result.toString();
    }

    public static String replaceTokensWithProperties(String expression, Properties properties) {

        StringBuilder result = new StringBuilder(expression.length());
        int i = 0;
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
        while (matcher.find()) {
            // Strip leading "${" and trailing "}" off.
            result.append(expression.substring(i, matcher.start()));
            String property = matcher.group();
            property = property.substring(2, property.length() - 1);

            //look up property and replace
            String p = properties.getProperty(property);
            result.append((p != null) ? p : matcher.group());

            i = matcher.end();
        }
        result.append(expression.substring(i));
        return result.toString();
    }


    @NotNull
    public static String fileAsString(String fileName) throws IOException {
        try {
            return convertStreamToString(PropertyReplacer.class.getResourceAsStream(fileName));
        } catch (Exception e) {
            return BytesUtil.readFile(fileName).toString();
        }
    }

    @NotNull
    private static String convertStreamToString(@NotNull java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
