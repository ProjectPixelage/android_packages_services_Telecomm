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

import java.util.Objects;

public class CallTransactionResult {
    public static final int RESULT_SUCCEED = 0;
    private static final String VOIP_TRANSACTION_TAG = "VoipCallTransactionResult";
    private static final String PSTN_TRANSACTION_TAG = "PstnTransactionResult";

    // NOTE: if the CallTransactionResult should not use the RESULT_SUCCEED to represent a
    // successful transaction, use an error code defined in the
    // {@link android.telecom.CallException} class

    private final int mResult;
    private final String mMessage;
    private final Call mCall;
    private final String mCallType;

    public CallTransactionResult(int result, String message) {
        mResult = result;
        mMessage = message;
        mCall = null;
        mCallType = "";
    }

    public CallTransactionResult(int result, Call call, String message, boolean isVoip) {
        mResult = result;
        mCall = call;
        mMessage = message;
        mCallType = isVoip ? VOIP_TRANSACTION_TAG : PSTN_TRANSACTION_TAG;
    }

    public int getResult() {
        return mResult;
    }

    public String getMessage() {
        return mMessage;
    }

    public Call getCall(){
        return mCall;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CallTransactionResult)) return false;
        CallTransactionResult that = (CallTransactionResult) o;
        return mResult == that.mResult && Objects.equals(mMessage, that.mMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mResult, mMessage);
    }

    @Override
    public String toString() {
        return new StringBuilder().
                append("{ ").
                append(mCallType).
                append(": [mResult: ").
                append(mResult).
                append("], [mCall: ").
                append((mCall != null) ? mCall : "null").
                append("], [mMessage=").
                append(mMessage).append("]  }").toString();
    }
}
