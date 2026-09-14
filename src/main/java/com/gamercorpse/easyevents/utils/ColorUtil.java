package com.gamercorpse.easyevents.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final MiniMessage MINI_MESSAGE =
            MiniMessage.miniMessage();

    private static final Pattern AMPERSAND_HEX =
            Pattern.compile(
                    "&#([A-Fa-f0-9]{6})"
            );

    private static final Pattern BRACED_HEX =
            Pattern.compile(
                    "\\{#([A-Fa-f0-9]{6})}"
            );

    private ColorUtil() {
    }

    public static Component colorize(String message) {

        if (message == null) {
            return Component.empty();
        }

        String converted =
                convertHex(message);

        converted =
                convertLegacyCodes(converted);

        try {

            return MINI_MESSAGE.deserialize(converted);

        } catch (Exception exception) {

            return Component.text(message);
        }
    }

    private static String convertHex(String message) {

        Matcher ampersandMatcher =
                AMPERSAND_HEX.matcher(message);

        StringBuffer ampersandBuffer =
                new StringBuffer();

        while (ampersandMatcher.find()) {

            ampersandMatcher.appendReplacement(
                    ampersandBuffer,
                    Matcher.quoteReplacement(
                            "<#" +
                                    ampersandMatcher.group(1) +
                                    ">"
                    )
            );
        }

        ampersandMatcher.appendTail(
                ampersandBuffer
        );

        Matcher bracedMatcher =
                BRACED_HEX.matcher(
                        ampersandBuffer.toString()
                );

        StringBuffer bracedBuffer =
                new StringBuffer();

        while (bracedMatcher.find()) {

            bracedMatcher.appendReplacement(
                    bracedBuffer,
                    Matcher.quoteReplacement(
                            "<#" +
                                    bracedMatcher.group(1) +
                                    ">"
                    )
            );
        }

        bracedMatcher.appendTail(
                bracedBuffer
        );

        return bracedBuffer.toString();
    }

    private static String convertLegacyCodes(
            String message
    ) {

        String converted = message;

        converted =
                replaceLegacy(
                        converted,
                        '0',
                        "<black>"
                );

        converted =
                replaceLegacy(
                        converted,
                        '1',
                        "<dark_blue>"
                );

        converted =
                replaceLegacy(
                        converted,
                        '2',
                        "<dark_green>"
                );

        converted =
                replaceLegacy(
                        converted,
                        '3',
                        "<dark_aqua>"
                );

        converted =
                replaceLegacy(
                        converted,
                        '4',
                        "<dark_red>"
                );

        converted =
                replaceLegacy(
                        converted,
                        '5',
                        "<dark_purple>"
                );

        converted =
                replaceLegacy(
                        converted,
                        '6',
                        "<gold>"
                );

        converted =
                replaceLegacy(
                        converted,
                        '7',
                        "<gray>"
                );

        converted =
                replaceLegacy(
                        converted,
                        '8',
                        "<dark_gray>"
                );

        converted =
                replaceLegacy(
                        converted,
                        '9',
                        "<blue>"
                );

        converted =
                replaceLegacy(
                        converted,
                        'a',
                        "<green>"
                );

        converted =
                replaceLegacy(
                        converted,
                        'b',
                        "<aqua>"
                );

        converted =
                replaceLegacy(
                        converted,
                        'c',
                        "<red>"
                );

        converted =
                replaceLegacy(
                        converted,
                        'd',
                        "<light_purple>"
                );

        converted =
                replaceLegacy(
                        converted,
                        'e',
                        "<yellow>"
                );

        converted =
                replaceLegacy(
                        converted,
                        'f',
                        "<white>"
                );

        converted =
                replaceLegacy(
                        converted,
                        'k',
                        "<obfuscated>"
                );

        converted =
                replaceLegacy(
                        converted,
                        'l',
                        "<bold>"
                );

        converted =
                replaceLegacy(
                        converted,
                        'm',
                        "<strikethrough>"
                );

        converted =
                replaceLegacy(
                        converted,
                        'n',
                        "<underlined>"
                );

        converted =
                replaceLegacy(
                        converted,
                        'o',
                        "<italic>"
                );

        converted =
                replaceLegacy(
                        converted,
                        'r',
                        "<reset>"
                );

        return converted;
    }

    private static String replaceLegacy(
            String input,
            char code,
            String replacement
    ) {

        return input
                .replace(
                        "&" + code,
                        replacement
                )
                .replace(
                        "&" + Character.toUpperCase(code),
                        replacement
                );
    }
}