/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.server.telecom.callsequencing;

import com.android.server.telecom.Call;
import com.android.server.telecom.CallsManager;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction layer for CallsManager to perform call sequencing operations through CallsManager
 * or CallSequencingController, which is controlled by {@link FeatureFlags#enableCallSequencing()}.
 */
public class CallsManagerCallSequencingAdapter {

    private final CallsManager mCallsManager;
    private final CallSequencingController mSequencingController;
    private final boolean mIsCallSequencingEnabled;

    public CallsManagerCallSequencingAdapter(CallsManager callsManager,
            CallSequencingController sequencingController,
            boolean isCallSequencingEnabled) {
        mCallsManager = callsManager;
        mSequencingController = sequencingController;
        mIsCallSequencingEnabled = isCallSequencingEnabled;
    }

    public void answerCall(Call incomingCall, int videoState) {
        if (mIsCallSequencingEnabled && !incomingCall.isTransactionalCall()) {
            mSequencingController.answerCall(incomingCall, videoState);
        } else {
            mCallsManager.answerCallOld(incomingCall, videoState);
        }
    }

    public void unholdCall(Call call) {
        if (mIsCallSequencingEnabled) {
            mSequencingController.unholdCall(call);
        } else {
            mCallsManager.unholdCallOld(call);
        }
    }

    public void holdCall(Call call) {
        // Sequencing already taken care of for CSW/TSW in Call class.
        call.hold();
    }

    public void unholdCallForRemoval(Call removedCall,
            boolean isLocallyDisconnecting) {
        // Todo: confirm verification of disconnect logic
        // Sequencing already taken care of for CSW/TSW in Call class.
        mCallsManager.maybeMoveHeldCallToForeground(removedCall, isLocallyDisconnecting);
    }

    public CompletableFuture<Boolean> makeRoomForOutgoingCall(boolean isEmergency, Call call) {
        if (mIsCallSequencingEnabled) {
            return mSequencingController.makeRoomForOutgoingCall(isEmergency, call);
        } else {
            return isEmergency
                    ? CompletableFuture.completedFuture(
                            makeRoomForOutgoingEmergencyCallFlagOff(call))
                    : CompletableFuture.completedFuture(makeRoomForOutgoingCallFlagOff(call));
        }
    }

    private boolean makeRoomForOutgoingCallFlagOff(Call call) {
        return mCallsManager.makeRoomForOutgoingCall(call);
    }

    private boolean makeRoomForOutgoingEmergencyCallFlagOff(Call call) {
        return mCallsManager.makeRoomForOutgoingEmergencyCall(call);
    }
}
