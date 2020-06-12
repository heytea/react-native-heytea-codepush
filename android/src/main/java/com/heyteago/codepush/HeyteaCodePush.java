package com.heyteago.codepush;

import android.content.Context;

import com.heyteago.codepush.delegate.FlowDelegate;
import com.heyteago.codepush.delegate.IndexFlowDelegate;

public final class HeyteaCodePush {
    private HeyteaCodePush() {}

    private static volatile FlowDelegate flowDelegate;

    public static FlowDelegate getFlowDelegate(Context context) {
        if (flowDelegate == null) {
            synchronized (HeyteaCodePush.class) {
                if (flowDelegate == null) {
                    flowDelegate = new IndexFlowDelegate(context);
                }
            }
        }
        return flowDelegate;
    }

    public static String getJsBundleFile(Context context) {
        return getFlowDelegate(context).getJsBundleFile();
    }
}
