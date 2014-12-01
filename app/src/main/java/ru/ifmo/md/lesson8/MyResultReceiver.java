package ru.ifmo.md.lesson8;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Created by Nikita Yaschenko on 02.12.14.
 */
public class MyResultReceiver extends ResultReceiver {

    private Receiver mReceiver;

    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle data);
    }

    public MyResultReceiver(Handler handler) {
        super(handler);
    }

    public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle data) {
        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, data);
        }
    }


}