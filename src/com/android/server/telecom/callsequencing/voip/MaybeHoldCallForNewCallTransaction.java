/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.telecom.callsequencing.voip;

import android.os.OutcomeReceiver;
import android.telecom.CallException;
import android.util.Log;

import com.android.server.telecom.Call;
import com.android.server.telecom.CallsManager;
import com.android.server.telecom.callsequencing.CallTransaction;
import com.android.server.telecom.callsequencing.CallTransactionResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * This VOIP CallTransaction is responsible for holding any active call in favor of a new call
 * request. If the active call cannot be held or disconnected, the transaction will fail.
 */
public class MaybeHoldCallForNewCallTransaction extends CallTransaction {

    private static final String TAG = MaybeHoldCallForNewCallTransaction.class.getSimpleName();
    private final CallsManager mCallsManager;
    private final Call mCall;
    private final boolean mIsCallControlRequest;

    public MaybeHoldCallForNewCallTransaction(CallsManager callsManager, Call call,
            boolean isCallControlRequest) {
        super(callsManager.getLock());
        mCallsManager = callsManager;
        mCall = call;
        mIsCallControlRequest = isCallControlRequest;
    }

    @Override
    public CompletionStage<CallTransactionResult> processTransaction(Void v) {
        Log.d(TAG, "processTransaction");
        CompletableFuture<CallTransactionResult> future = new CompletableFuture<>();

        mCallsManager.transactionHoldPotentialActiveCallForNewCall(mCall, mIsCallControlRequest,
                new OutcomeReceiver<>() {
            @Override
            public void onResult(Boolean result) {
                Log.d(TAG, "processTransaction: onResult");
                future.complete(new CallTransactionResult(
                        CallTransactionResult.RESULT_SUCCEED, null));
            }

            @Override
            public void onError(CallException exception) {
                Log.d(TAG, "processTransaction: onError");
                future.complete(new CallTransactionResult(
                       exception.getCode(), exception.getMessage()));
            }
        });

        return future;
    }
}
