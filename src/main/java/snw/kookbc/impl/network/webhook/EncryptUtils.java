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

package snw.kookbc.impl.network.webhook;

import com.google.gson.JsonParser;
import snw.kookbc.impl.KBCClient;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncryptUtils {
    public static String decrypt(KBCClient client, String src) {
        String key = client.getConfig().getString("webhook-encrypt-key");
        if (key == null || key.isEmpty()) {
            throw new RuntimeException("Decryption failed: No encrypt key provided.");
        }
        String decodedBase64 = new String(
                Base64.getDecoder().decode(
                        JsonParser.parseString(src).getAsJsonObject().get("encrypt").getAsString()
                )
        );
        String iv = decodedBase64.substring(0, 16);
        String newSecret = decodedBase64.substring(16);
        byte[] queuedData = Base64.getDecoder().decode(newSecret);
        StringBuilder finalKeyBuilder = new StringBuilder(key);
        while (finalKeyBuilder.length() < 32) {
                finalKeyBuilder.append("\0");
        }
        String finalKey = finalKeyBuilder.toString();
        return decrypt(queuedData, iv, finalKey);
    }

    static String decrypt(byte[] src, String iv, String key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key.getBytes(), "AES"),
                    new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8))
            );
            return new String(cipher.doFinal(src));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed.", e);
        }
    }
}
