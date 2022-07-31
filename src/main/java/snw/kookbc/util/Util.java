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

public class Util {

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

}
