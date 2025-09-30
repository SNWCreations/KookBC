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
package snw.kookbc.impl.permissions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import snw.jkook.permissions.PermissionAttachmentInfo;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
public class UserPermissionSaved {
    private final String uid;
    private final Map<String, Boolean> permissions;

    public UserPermissionSaved(String uid, Map<String, Boolean> permissions) {
        this.uid = uid;
        this.permissions = permissions;
    }

    public UserPermissionSaved(String uid, Set<PermissionAttachmentInfo> attachmentInfos) {
        this.uid = uid;
        this.permissions = new HashMap<>();
        attachmentInfos.forEach(permissionAttachmentInfo -> {
            this.permissions.put(permissionAttachmentInfo.getPermission().toLowerCase(Locale.ENGLISH), permissionAttachmentInfo.getValue());
        });
    }

    public String getUid() {
        return uid;
    }

    public Map<String, Boolean> getPermissions() {
        return permissions;
    }

    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    public static String toString(UserPermissionSaved... array) {
        return GSON.toJson(array);
    }

    public static UserPermissionSaved parse(String json) {
        return GSON.fromJson(json, UserPermissionSaved.class);
    }

    public static List<UserPermissionSaved> parseList(String json) {
        return GSON.fromJson(json, new TypeToken<List<UserPermissionSaved>>() {}.getType());
    }
}
