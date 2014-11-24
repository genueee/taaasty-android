package ru.taaasty.utils;

import rx.Subscription;

/**
 * Subscriptions.empty(), но на isUnsubscribed() возвращает true
 */
public class SubscriptionHelper {

    public static Subscription empty() {
        return EMPTY;
    }

    private static final Empty EMPTY = new Empty();
    private static final class Empty implements Subscription {
        @Override
        public void unsubscribe() {
        }

        @Override
        public boolean isUnsubscribed() {
            return true;
        }
    }
}
