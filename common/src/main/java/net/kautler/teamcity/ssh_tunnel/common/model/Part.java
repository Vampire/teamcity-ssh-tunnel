/*
 * Copyright 2019 Björn Kautler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.kautler.teamcity.ssh_tunnel.common.model;

import java.util.Formattable;
import java.util.Formatter;
import java.util.Map;

import static java.util.FormattableFlags.LEFT_JUSTIFY;
import static java.util.FormattableFlags.UPPERCASE;

public interface Part extends Formattable {
    Map<String, String> getConfigParameters(String prefix, boolean emulationMode);

    @Override
    default void formatTo(Formatter formatter, int flags, int width, int precision) {
        boolean uppercase = (flags & UPPERCASE) != 0;
        boolean leftJustify = (flags & LEFT_JUSTIFY) != 0;

        String result = toString();
        if (uppercase) {
            result = result.toUpperCase(formatter.locale());
        }

        formatter.format(String.format("%%%s%s%ss",
                leftJustify ? "-" : "",
                (width == -1) ? "" : width,
                (precision == -1) ? "" : "." + precision), result);
    }
}
