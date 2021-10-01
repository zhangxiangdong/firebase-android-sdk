package com.google.firebase.firestore;

import static com.google.firebase.firestore.util.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

public class FirestoreIndexConfiguration {

  private final List<FirestoreIndexEntry> indexEntries;

  public FirestoreIndexConfiguration(List<FirestoreIndexEntry> indexEntries) {
    this.indexEntries = indexEntries;
  }

  @NonNull
  public List<FirestoreIndexEntry> getIndexEntries() {
    return indexEntries;
  }

  public static final class Builder {
    private List<FirestoreIndexEntry> indexEntries;

    public Builder() {
      indexEntries = new ArrayList<>(indexEntries);
    }

    public Builder(@NonNull FirestoreIndexConfiguration firestoreIndexConfiguration) {
      checkNotNull(firestoreIndexConfiguration, "Provided index must not be null.");
      indexEntries = new ArrayList<>(firestoreIndexConfiguration.indexEntries);
    }

    @NonNull
    public Builder addIndexEntry(@NonNull FirestoreIndexEntry entry) {
      indexEntries.add(entry);
      return this;
    }

    @NonNull
    public FirestoreIndexConfiguration build() {
      return new FirestoreIndexConfiguration(indexEntries);
    }
  }
}
