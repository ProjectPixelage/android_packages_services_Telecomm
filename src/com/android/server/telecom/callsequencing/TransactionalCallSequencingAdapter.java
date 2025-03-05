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

import android.os.OutcomeReceiver;
import android.telecom.CallException;
import android.telecom.DisconnectCause;

import com.android.server.telecom.Call;
import com.android.server.telecom.CallsManager;
import com.android.server.telecom.callsequencing.voip.EndCallTransaction;
import com.android.server.telecom.callsequencing.voip.HoldCallTransaction;
import com.android.server.telecom.callsequencing.voip.MaybeHoldCallForNewCallTransaction;
import com.android.server.telecom.callsequencing.voip.RequestNewActiveCallTransaction;
import com.android.server.telecom.callsequencing.voip.SerialTransaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Helper adapter class used to centralized code that will be affected by toggling the
 * {@link Flags#enableCallSequencing()} flag.
 */
public class TransactionalCallSequencingAdapter {
    private final TransactionManager mTransactionManager;
    private final CallsManager mCallsManager;
//    private final boolean mIsCallSequencingEnabled;

    public TransactionalCallSequencingAdapter(TransactionManager transactionManager,
            CallsManager callsManager, boolean isCallSequencingEnabled) {
        mTransactionManager = transactionManager;
        mCallsManager = callsManager;
        // TODO implement call sequencing changes
//        mIsCallSequencingEnabled = isCallSequencingEnabled;
    }

    /**
     * Client -> Server request to set a call active
     */
    public void setActive(Call call,
            OutcomeReceiver<CallTransactionResult, CallException> receiver) {
        setActiveFlagOff(call, receiver);
    }

    /**
     * Client -> Server request to answer a call
     */
    public void setAnswered(Call call, int newVideoState,
            OutcomeReceiver<CallTransactionResult, CallException> receiver) {
        setAnsweredFlagOff(call, newVideoState, receiver);
    }

    /**
     * Client -> Server request to set a call to disconnected
     */
    public void setDisconnected(Call call, DisconnectCause dc,
            OutcomeReceiver<CallTransactionResult, CallException> receiver) {
        setDisconnectedFlagOff(call, dc, receiver);
    }

    /**
     * Client -> Server request to set a call to inactive
     */
    public void setInactive(Call call,
            OutcomeReceiver<CallTransactionResult, CallException> receiver) {
        setInactiveFlagOff(call, receiver);
    }

    /**
     * Server -> Client command to set the call active, which if it fails, will try to reset the
     * state to what it was before the call was set to active.
     */
    public CompletableFuture<Boolean> onSetActive(Call call,
            CallTransaction clientCbT,
            OutcomeReceiver<CallTransactionResult, CallException> receiver) {
        return onSetActiveFlagOff(call, clientCbT, receiver);
    }

    /**
     * Server -> Client command to answer an incoming call, which if it fails, will trigger the
     * disconnect of the call and then reset the state of the other call back to what it was before.
     */
    public void onSetAnswered(Call call, int videoState, CallTransaction clientCbT,
            OutcomeReceiver<CallTransactionResult, CallException> receiver) {
        onSetAnsweredFlagOff(call, videoState, clientCbT, receiver);
    }

    /**
     * Server -> Client command to set the call as inactive
     */
    public CompletableFuture<Boolean> onSetInactive(Call call,
            CallTransaction clientCbT,
            OutcomeReceiver<CallTransactionResult, CallException> receiver) {
        return onSetInactiveFlagOff(call, clientCbT, receiver);
    }

    /**
     * Server -> Client command to disconnect the call
     */
    public CompletableFuture<Boolean> onSetDisconnected(Call call,
            DisconnectCause dc, CallTransaction clientCbT, OutcomeReceiver<CallTransactionResult,
            CallException> receiver) {
        return onSetDisconnectedFlagOff(call, dc, clientCbT, receiver);
    }

    /**
     * Clean up the calls that have been passed in from CallsManager
     */
    public void cleanup(Collection<Call> calls) {
        cleanupFlagOff(calls);
    }

    private void setActiveFlagOff(Call call,
            OutcomeReceiver<CallTransactionResult, CallException> receiver) {
        CompletableFuture<Boolean> transactionResult = mTransactionManager
                .addTransaction(createSetActiveTransactions(call,
                true /* callControlRequest */), receiver);
    }

    private void setAnsweredFlagOff(Call call, int newVideoState,
            OutcomeReceiver<CallTransactionResult, CallException> receiver) {
        CompletableFuture<Boolean> transactionResult = mTransactionManager
                .addTransaction(createSetActiveTransactions(call,
                                true /* callControlRequest */),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(CallTransactionResult callTransactionResult) {
                        call.setVideoState(newVideoState);
                        receiver.onResult(callTransactionResult);
                    }

                    @Override
                    public void onError(CallException error) {
                        receiver.onError(error);
                    }
                });
    }

    private void setDisconnectedFlagOff(Call call, DisconnectCause dc,
            OutcomeReceiver<CallTransactionResult, CallException> receiver) {
        CompletableFuture<Boolean> transactionResult = mTransactionManager
                .addTransaction(new EndCallTransaction(mCallsManager,
                        dc, call), receiver);
    }

    private void setInactiveFlagOff(Call call,
            OutcomeReceiver<CallTransactionResult, CallException> receiver) {
        CompletableFuture<Boolean> transactionResult = mTransactionManager
                .addTransaction(new HoldCallTransaction(mCallsManager,call), receiver);
    }

    private CompletableFuture<Boolean> onSetActiveFlagOff(Call call,
            CallTransaction clientCbT,
            OutcomeReceiver<CallTransactionResult, CallException> receiver) {
        // save CallsManager state before sending client state changes
        Call foregroundCallBeforeSwap = mCallsManager.getForegroundCall();
        boolean wasActive = foregroundCallBeforeSwap != null && foregroundCallBeforeSwap.isActive();
        SerialTransaction serialTransactions = createSetActiveTransactions(call,
                false /* callControlRequest */);
        serialTransactions.appendTransaction(clientCbT);
        // do CallsManager workload before asking client and
        //   reset CallsManager state if client does NOT ack
        return mTransactionManager.addTransaction(
                serialTransactions,
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(CallTransactionResult result) {
                        receiver.onResult(result);
                    }

                    @Override
                    public void onError(CallException exception) {
                        mCallsManager.markCallAsOnHold(call);
                        maybeResetForegroundCall(foregroundCallBeforeSwap, wasActive);
                        receiver.onError(exception);
                    }
                });
    }

    private void onSetAnsweredFlagOff(Call call, int videoState, CallTransaction clientCbT,
            OutcomeReceiver<CallTransactionResult, CallException> receiver) {
        // save CallsManager state before sending client state changes
        Call foregroundCallBeforeSwap = mCallsManager.getForegroundCall();
        boolean wasActive = foregroundCallBeforeSwap != null && foregroundCallBeforeSwap.isActive();
        SerialTransaction serialTransactions = createSetActiveTransactions(call,
                false /* callControlRequest */);
        serialTransactions.appendTransaction(clientCbT);
        // do CallsManager workload before asking client and
        //   reset CallsManager state if client does NOT ack
        CompletableFuture<Boolean> transactionResult = mTransactionManager
                .addTransaction(serialTransactions,
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(CallTransactionResult result) {
                        call.setVideoState(videoState);
                        receiver.onResult(result);
                    }

                    @Override
                    public void onError(CallException exception) {
                        // This also sends the signal to untrack from TSW and the client_TSW
                        removeCallFromCallsManager(call,
                                new DisconnectCause(DisconnectCause.REJECTED,
                                        "client rejected to answer the call;"
                                                + " force disconnecting"));
                        maybeResetForegroundCall(foregroundCallBeforeSwap, wasActive);
                        receiver.onError(exception);
                    }
                });
    }

    private CompletableFuture<Boolean> onSetInactiveFlagOff(Call call,
            CallTransaction clientCbT,
            OutcomeReceiver<CallTransactionResult, CallException> receiver) {
        return mTransactionManager.addTransaction(clientCbT,
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(CallTransactionResult callTransactionResult) {
                        mCallsManager.markCallAsOnHold(call);
                        receiver.onResult(callTransactionResult);
                    }

                    @Override
                    public void onError(CallException error) {
                        receiver.onError(error);
                    }
                });
    }

    /**
     * Server -> Client command to disconnect the call
     */
    private CompletableFuture<Boolean> onSetDisconnectedFlagOff(Call call,
            DisconnectCause dc, CallTransaction clientCbT,
            OutcomeReceiver<CallTransactionResult, CallException> receiver) {
        return mTransactionManager.addTransaction(clientCbT,
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(CallTransactionResult result) {
                        removeCallFromCallsManager(call, dc);
                        receiver.onResult(result);
                    }

                    @Override
                    public void onError(CallException exception) {
                        removeCallFromCallsManager(call, dc);
                        receiver.onError(exception);
                    }
                }
        );
    }

    private SerialTransaction createSetActiveTransactions(Call call, boolean isCallControlRequest) {
        // create list for multiple transactions
        List<CallTransaction> transactions = new ArrayList<>();

        // potentially hold the current active call in order to set a new call (active/answered)
        transactions.add(new MaybeHoldCallForNewCallTransaction(mCallsManager, call,
                isCallControlRequest));
        // And request a new focus call update
        transactions.add(new RequestNewActiveCallTransaction(mCallsManager, call));

        return new SerialTransaction(transactions, mCallsManager.getLock());
    }

    private void removeCallFromCallsManager(Call call, DisconnectCause cause) {
        if (cause.getCode() != DisconnectCause.REJECTED) {
            mCallsManager.markCallAsDisconnected(call, cause);
        }
        mCallsManager.removeCall(call);
    }

    private void maybeResetForegroundCall(Call foregroundCallBeforeSwap, boolean wasActive) {
        if (foregroundCallBeforeSwap == null) {
            return;
        }
        if (wasActive && !foregroundCallBeforeSwap.isActive()) {
            mCallsManager.markCallAsActive(foregroundCallBeforeSwap);
        }
    }
    private void cleanupFlagOff(Collection<Call> calls) {
        for (Call call : calls) {
            mCallsManager.markCallAsDisconnected(call,
                    new DisconnectCause(DisconnectCause.ERROR, "process died"));
            mCallsManager.removeCall(call); // This will clear mTrackedCalls && ClientTWS
        }
    }
}
