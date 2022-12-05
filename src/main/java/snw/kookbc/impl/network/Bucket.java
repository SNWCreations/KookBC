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

package snw.kookbc.impl.network;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.exceptions.TooFastException;
import snw.kookbc.impl.scheduler.SchedulerImpl;

// Represents the Bucket of Rate Limit.
// Not single instance. Created when network call requested.
// Cached.
public class Bucket {
    private static final Map<HttpAPIRoute, String> bucketNameMap = new HashMap<>();
    private static final Map<String, Bucket> map = new ConcurrentHashMap<>();

    private final KBCClient client;
    private final String name; // defined by response header
    int availableTimes = -1;
    private volatile boolean scheduledToUpdate;

    // Use get(KBCClient, String) method instead.
    private Bucket(KBCClient client, String name) {
        Validate.notNull(client);
        Validate.notNull(name);
        this.client = client;
        this.name = name;
    }

    public void scheduleUpdateAvailableTimes(int availableTimes, int after) {
        if (!scheduledToUpdate) {
            ((SchedulerImpl) client.getCore().getScheduler()).getPool().schedule(() -> {
                Bucket.this.availableTimes = availableTimes;
                Bucket.this.scheduledToUpdate = false;
            }, after, TimeUnit.SECONDS);
        }
    }

    // throw TooFastException if too fast, or just decrease one request remaining time.
    public void check() {
        if (availableTimes == -1) {
            // At this time, we don't know remaining time, so we can't check it
            // We should set the time after got response
            return;
        }
        if (availableTimes < 0) {
            throw new TooFastException(name);
        }
        availableTimes--;
    }

    public static Bucket get(KBCClient client, String bucketName) {
        return map.computeIfAbsent(bucketName, r -> new Bucket(client, r));
    }

    static {
        // TODO put Route->BucketName mapping here.
    }
}
