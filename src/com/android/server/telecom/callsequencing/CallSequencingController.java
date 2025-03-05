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

import android.os.Handler;
import android.os.HandlerThread;

import com.android.server.telecom.Call;
import com.android.server.telecom.CallsManager;

import java.util.concurrent.CompletableFuture;

/**
 * Controls the sequencing between calls when moving between the user ACTIVE (RINGING/ACTIVE) and
 * user INACTIVE (INCOMING/HOLD/DISCONNECTED) states.
 */
public class CallSequencingController {
//    private final CallsManager mCallsManager;
    private final TransactionManager mTransactionManager;
//    private final Handler mHandler;
//    private boolean mCallSequencingEnabled;

    public CallSequencingController(CallsManager callsManager, boolean callSequencingEnabled) {
//        mCallsManager = callsManager;
        mTransactionManager = TransactionManager.getInstance();
        HandlerThread handlerThread = new HandlerThread(this.toString());
        handlerThread.start();
//        mHandler = new Handler(handlerThread.getLooper());
//        mCallSequencingEnabled = callSequencingEnabled;
    }

    public void answerCall(Call incomingCall, int videoState) {
        // Todo: call sequencing logic (stubbed)
    }

//    private CompletableFuture<Boolean> holdActiveCallForNewCallWithSequencing(Call call) {
//        // Todo: call sequencing logic (stubbed)
//        return null;
//    }

    public void unholdCall(Call call) {
        // Todo: call sequencing logic (stubbed)
    }

    public CompletableFuture<Boolean> makeRoomForOutgoingCall(boolean isEmergency, Call call) {
        // Todo: call sequencing logic (stubbed)
        return CompletableFuture.completedFuture(true);
//        return isEmergency ? makeRoomForOutgoingEmergencyCall(call) : makeRoomForOutgoingCall(call);
    }

//    private CompletableFuture<Boolean> makeRoomForOutgoingEmergencyCall(Call emergencyCall) {
//        // Todo: call sequencing logic (stubbed)
//        return CompletableFuture.completedFuture(true);
//    }

//    private CompletableFuture<Boolean> makeRoomForOutgoingCall(Call call) {
//        // Todo: call sequencing logic (stubbed)
//        return CompletableFuture.completedFuture(true);
//    }

//    private void resetProcessingCallSequencing() {
//        mTransactionManager.setProcessingCallSequencing(false);
//    }

    public CompletableFuture<Boolean> disconnectCall() {
        return CompletableFuture.completedFuture(true);
    }
}
