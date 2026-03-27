package com.icarus.events;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

public class EventStatusBackgroundWorker extends Worker {
    public EventStatusBackgroundWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Timestamp now = Timestamp.now();

        // Find all events where registration has ended
        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                .whereLessThan("close", now)  // Events where close date has passed
                .get()
                .addOnSuccessListener(eventSnapshots -> {
                    for (QueryDocumentSnapshot eventSnapshot : eventSnapshots) {
                        String eventId = eventSnapshot.getId();

                        //Find all entrants with "selected" status and change it to "rejected"
                        db.collection(FirestoreCollections.EVENTS_COLLECTION)
                                .document(eventId)
                                .collection("entrants")
                                .whereEqualTo("status", "selected")
                                .get()
                                .addOnSuccessListener(entrantSnapshots -> {
                                    WriteBatch batch = db.batch();
                                    for (QueryDocumentSnapshot entrant : entrantSnapshots) {
                                        batch.update(entrant.getReference(), "status", "rejected");
                                    }
                                    batch.commit();
                                });
                    }
                });

        return Result.success();
    }
}