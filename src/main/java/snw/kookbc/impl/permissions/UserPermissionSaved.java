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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import snw.jkook.permissions.PermissionAttachmentInfo;
import snw.kookbc.util.JacksonUtil;

import java.util.*;

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

    // Jackson ObjectMapper - 高性能 JSON 处理
    private static final ObjectMapper MAPPER = JacksonUtil.createPrettyMapper();

    public static String toString(UserPermissionSaved... array) {
        try {
            return MAPPER.writeValueAsString(array);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize UserPermissionSaved array", e);
        }
    }

    public static UserPermissionSaved parse(String json) {
        try {
            return MAPPER.readValue(json, UserPermissionSaved.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse UserPermissionSaved", e);
        }
    }

    public static List<UserPermissionSaved> parseList(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<List<UserPermissionSaved>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse UserPermissionSaved list", e);
        }
    }
}
