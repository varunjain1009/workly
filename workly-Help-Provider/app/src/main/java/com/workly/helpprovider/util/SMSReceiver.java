package com.workly.helpprovider.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

public class SMSReceiver extends BroadcastReceiver {

    private OTPListener otpListener;

    public interface OTPListener {
        void onOTPReceived(String otp);

        void onOTPTimeOut();
    }

    public void setOTPListener(OTPListener otpListener) {
        this.otpListener = otpListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

            switch (status.getStatusCode()) {
                case CommonStatusCodes.SUCCESS:
                    String message = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                    if (otpListener != null && message != null) {
                        // Extract OTP from message (implementation depends on message format)
                        // Example message: <#> Your OTP is 123456 abcdefgh (app hash)
                        otpListener.onOTPReceived(message);
                    }
                    break;
                case CommonStatusCodes.TIMEOUT:
                    if (otpListener != null) {
                        otpListener.onOTPTimeOut();
                    }
                    break;
            }
        }
    }
}
