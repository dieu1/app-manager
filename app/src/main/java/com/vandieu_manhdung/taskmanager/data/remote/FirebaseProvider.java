package com.vandieu_manhdung.taskmanager.data.remote;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public final class FirebaseProvider {

    private FirebaseProvider() {
    }

    public static boolean isConfigured(Context context) {
        if (!FirebaseApp.getApps(context).isEmpty()) {
            return true;
        }
        return FirebaseApp.initializeApp(context) != null;
    }

    public static FirebaseAuth auth(Context context) {
        requireConfigured(context);
        return FirebaseAuth.getInstance();
    }

    public static FirebaseFirestore firestore(Context context) {
        requireConfigured(context);
        return FirebaseFirestore.getInstance();
    }

    private static void requireConfigured(Context context) {
        if (!isConfigured(context)) {
            throw new IllegalStateException(
                    "Firebase chưa được cấu hình. Hãy thêm app/google-services.json."
            );
        }
    }
}
