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

package snw.kookbc.impl.network.policy;

import snw.kookbc.impl.KBCClient;
import snw.kookbc.interfaces.network.policy.RateLimitPolicy;

// Represents the policy which will wait until the remaining limit is reset.
public class WaitUntilOKRateLimitPolicy implements RateLimitPolicy {

    // Called when Rate Limit is reached.
    // route is the request target route enum.
    // resetTime means the seconds needed to wait until limit reset.
    @Override
    public void perform(KBCClient client, String route, int resetTime) {
        if (resetTime < 1) {
            resetTime = 1;
        }
        resetTime += 3; // give 3 second to reset the limit
        try {
            Thread.sleep(resetTime * 1000L);
        } catch (InterruptedException ignored) {
        }
    }
}
