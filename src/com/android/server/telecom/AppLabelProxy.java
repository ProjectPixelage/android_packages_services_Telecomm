/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.telecom;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import com.android.server.telecom.flags.FeatureFlags;
import android.os.UserHandle;
import android.telecom.Log;

/**
 * Abstracts away dependency on the {@link PackageManager} required to fetch the label for an
 * app.
 */
public interface AppLabelProxy {
    String LOG_TAG = AppLabelProxy.class.getSimpleName();

    class Util {
        /**
         * Default impl of getAppLabel.
         * @param context Context instance that is not necessarily associated with the correct user.
         * @param userHandle UserHandle instance of the user that is associated with the app.
         * @param packageName package name to look up.
         */
        public static CharSequence getAppLabel(Context context, UserHandle userHandle,
                String packageName, FeatureFlags featureFlags) {
            try {
                if (featureFlags.telecomAppLabelProxyHsumAware()){
                    Context userContext = context.createContextAsUser(userHandle, 0 /* flags */);
                    PackageManager userPackageManager = userContext.getPackageManager();
                    if (userPackageManager == null) {
                        Log.w(LOG_TAG, "Could not determine app label since PackageManager is "
                                + "null. Package name is %s", packageName);
                        return null;
                    }
                    ApplicationInfo info = userPackageManager.getApplicationInfo(packageName, 0);
                    CharSequence result = userPackageManager.getApplicationLabel(info);
                    Log.i(LOG_TAG, "package %s: name is %s for user = %s", packageName, result,
                            userHandle.toString());
                    return result;
                } else {
                    // Legacy code path:
                    PackageManager pm = context.getPackageManager();
                    ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
                    CharSequence result = pm.getApplicationLabel(info);
                    Log.i(LOG_TAG, "package %s: name is %s", packageName, result);
                    return result;
                }
            } catch (PackageManager.NameNotFoundException nnfe) {
                Log.w(LOG_TAG, "Could not determine app label. Package name is %s", packageName);
            }

            return null;
        }
    }

    CharSequence getAppLabel(String packageName, UserHandle userHandle);
}
