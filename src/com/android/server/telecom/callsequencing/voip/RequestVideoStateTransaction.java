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

package com.android.server.telecom.callsequencing.voip;

import static com.android.server.telecom.callsequencing.voip.VideoStateTranslation
        .TransactionalVideoStateToVideoProfileState;

import android.telecom.CallException;
import android.telecom.VideoProfile;
import android.util.Log;

import com.android.server.telecom.CallsManager;
import com.android.server.telecom.Call;
import com.android.server.telecom.callsequencing.CallTransaction;
import com.android.server.telecom.callsequencing.CallTransactionResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class RequestVideoStateTransaction extends CallTransaction {

    private static final String TAG = RequestVideoStateTransaction.class.getSimpleName();
    private final Call mCall;
    private final int mVideoProfileState;

    public RequestVideoStateTransaction(CallsManager callsManager, Call call,
            int transactionalVideoState) {
        super(callsManager.getLock());
        mCall = call;
        mVideoProfileState = TransactionalVideoStateToVideoProfileState(transactionalVideoState);
    }

    @Override
    public CompletionStage<CallTransactionResult> processTransaction(Void v) {
        Log.d(TAG, "processTransaction");
        CompletableFuture<CallTransactionResult> future = new CompletableFuture<>();

        if (isRequestingVideoTransmission(mVideoProfileState) &&
                !mCall.isVideoCallingSupportedByPhoneAccount()) {
            future.complete(new CallTransactionResult(
                    CallException.CODE_ERROR_UNKNOWN /*TODO:: define error code. b/335703584 */,
                    "Video calling is not supported by the target account"));
        } else {
            mCall.setVideoState(mVideoProfileState);
            future.complete(new CallTransactionResult(
                    CallTransactionResult.RESULT_SUCCEED,
                    "The Video State was changed successfully"));
        }
        return future;
    }

    private boolean isRequestingVideoTransmission(int targetVideoState) {
        return targetVideoState != VideoProfile.STATE_AUDIO_ONLY;
    }
}