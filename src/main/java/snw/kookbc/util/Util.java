/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 KookBC contributors
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

import snw.jkook.JKook;
import snw.jkook.command.JKookCommand;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Inflater;

public class Util {

    public static byte[] decompressDeflate(byte[] data) {
        byte[] output = null;

        Inflater decompressor = new Inflater();
        decompressor.reset();
        decompressor.setInput(data);

        try (ByteArrayOutputStream o = new ByteArrayOutputStream(data.length)) {
            byte[] buf = new byte[1024];
            while (!decompressor.finished()) {
                int i = decompressor.inflate(buf);
                o.write(buf, 0, i);
            }
            output = o.toByteArray();
        } catch (Exception e) {
            JKook.getLogger().error("Unexpected exception happened while we attempting to decompress the ZLIB/DEFLATE compressed data.", e);
        }

        decompressor.end();
        return output;
    }

    // -1 = 过期
    // 0 = 最新版
    // 1 = 未来版
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

        // 以下比较是否是 SNAPSHOT 版，虽然我平常不发 SNAPSHOT 。。。但总要考虑
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

    public static List<String> getHelp(JKookCommand[] commands) {
        List<String> result = new LinkedList<>();
        result.add("-------- 命令帮助 --------");
        for (JKookCommand command : commands) {
            result.add(String.format("(%s)%s: %s", String.join("|", command.getPrefixes()), command.getRootName(),
                    (command.getDescription() == null) ? "此命令没有简介。" : command.getDescription()
            ));
        }
        result.add("注: 在每条命令帮助的开头，括号中用 \"|\" 隔开的字符为此命令的前缀。");
        result.add("如 \"(/|.)blah\" 即 \"/blah\", \".blah\" 为同一条命令。");
        result.add("-------------------------");
        return result;
    }

}
