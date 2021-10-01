package com.google.firebase.firestore;

import static com.google.firebase.firestore.util.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FirestoreIndexEntry {

  private final List<IndexField> fields;

  public FirestoreIndexEntry(List<IndexField> fields) {
    this.fields = fields;
  }

  public List<IndexField> getFields() {
    return Collections.unmodifiableList(fields);
  }

  public static final class Builder {

    private List<IndexField> fields;

    public Builder() {}

    public Builder(@NonNull FirestoreIndexEntry firestoreIndexEntry) {
      checkNotNull(firestoreIndexEntry, "Provided index must not be null.");
      fields = new ArrayList<>(firestoreIndexEntry.fields);
    }

    public Builder addField(@NonNull IndexField field) {
      fields.add(field);
      return this;
    }

    public List<IndexField> getFields() {
      return Collections.unmodifiableList(fields);
    }

    public FirestoreIndexEntry build() {
      return new FirestoreIndexEntry(fields);
    }
  }

  static class IndexField {
    enum ArrayConfig {
      CONTAINS;
    }

    enum Order {
      ASCENDING,
      DESCENDING;
    }

    private final FieldPath fieldPath;
    private final @Nullable ArrayConfig arrayConfig;
    private final @Nullable Order order;

    IndexField(FieldPath fieldPath, @Nullable ArrayConfig arrayConfig, @Nullable Order order) {
      this.fieldPath = fieldPath;
      this.arrayConfig = arrayConfig;
      this.order = order;
    }

    public FieldPath getFieldPath() {
      return fieldPath;
    }

    @Nullable
    public ArrayConfig getArrayConfig() {
      return arrayConfig;
    }

    @Nullable
    public Order getOrder() {
      return order;
    }

    public static final class Builder {

      private @Nullable FieldPath fieldPath;
      private @Nullable ArrayConfig arrayConfig;
      private @Nullable Order order;

      public Builder() {}

      public Builder(@NonNull IndexField indexField) {
        checkNotNull(indexField, "Provided index must not be null.");
        fieldPath = indexField.fieldPath;
        arrayConfig = indexField.arrayConfig;
        order = indexField.order;
      }

      public Builder setFieldPath(@NonNull FieldPath fieldPath) {
        this.fieldPath = fieldPath;
        return this;
      }

      public Builder setArrayConfig(@Nullable ArrayConfig arrayConfig) {
        this.arrayConfig = arrayConfig;
        return this;
      }

      public Builder setOrder(@Nullable Order order) {
        this.order = order;
        return this;
      }

      //            @Nullable
      //            public FieldPath getFieldPath() {
      //                return fieldPath;
      //            }
      //
      //            @Nullable
      //            public ArrayConfig getArrayConfig() {
      //                return arrayConfig;
      //            }
      //
      //            @Nullable
      //            public Order getOrder() {
      //                return order;
      //            }

      public void build() {
        // ...
      }
    }
  }
}
