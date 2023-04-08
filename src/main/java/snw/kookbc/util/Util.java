/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 - 2023 KookBC contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package snw.kookbc.util;

import snw.jkook.plugin.Plugin;
import snw.jkook.util.Validate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class Util {

    private Util() {} // cannot call constructor

    // -1 = Outdated
    // 0 = Latest
    // 1 = From Future (means this version is not on GitHub. Development version?)
    public static int getVersionDifference(String current, String versionToCompare) {
        if (current.equals(versionToCompare))
            return 0;
        if (current.split("\\.").length != 3 || versionToCompare.split("\\.").length != 3)
            return -1;

        int curMaj = Integer.parseInt(current.split("\\.")[0]);
        int curMin = Integer.parseInt(current.split("\\.")[1]);
        String curPatch = current.split("\\.")[2];

        int relMaj = Integer.parseInt(versionToCompare.split("\\.")[0]);
        int relMin = Integer.parseInt(versionToCompare.split("\\.")[1]);
        String relPatch = versionToCompare.split("\\.")[2];

        if (curMaj < relMaj)
            return -1;
        if (curMaj > relMaj)
            return 1;
        if (curMin < relMin)
            return -1;
        if (curMin > relMin)
            return 1;

        // Detect snapshot (if exists)
        int curPatchN = Integer.parseInt(curPatch.split("-")[0]);
        int relPatchN = Integer.parseInt(relPatch.split("-")[0]);
        if (curPatchN < relPatchN)
            return -1;
        if (curPatchN > relPatchN)
            return 1;
        if (!relPatch.contains("-") && curPatch.contains("-"))
            return -1;
        if (relPatch.contains("-") && curPatch.contains("-"))
            return 0;

        return 1;
    }

    public static String toEnglishNumOrder(int num) {
        String numStr = String.valueOf(num);
        String suffix;
        switch (Integer.parseInt(String.valueOf(numStr.charAt(numStr.length() - 1)))) {
            case 1:
                suffix = "st";
                break;
            case 2:
                suffix = "nd";
                break;
            case 3:
                suffix = "rd";
                break;
            default:
                suffix = "th";
                break;
        }
        return numStr + suffix;
    }

    public static void pluginNotNull(Plugin plugin) {
        Validate.notNull(plugin, "The provided plugin is null");
    }

    public static void ensurePluginEnabled(Plugin plugin) {
        pluginNotNull(plugin);
        Validate.isTrue(plugin.isEnabled(), "The plugin is disabled");
    }

    public static byte[] decompressDeflate(byte[] data) throws IOException, DataFormatException {
        Inflater decompressor = new Inflater();
        decompressor.reset();
        decompressor.setInput(data);

        try (ByteArrayOutputStream o = new ByteArrayOutputStream(data.length)) {
            byte[] buf = new byte[1024];
            while (!decompressor.finished()) {
                int i = decompressor.inflate(buf);
                o.write(buf, 0, i);
            }
            return o.toByteArray();
        } finally {
            decompressor.end();
        }
    }

    public static byte[] inputStreamToByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }

    public static boolean isStartByLaunch() {
        return Boolean.getBoolean("kookbc.launch");
    }
}
