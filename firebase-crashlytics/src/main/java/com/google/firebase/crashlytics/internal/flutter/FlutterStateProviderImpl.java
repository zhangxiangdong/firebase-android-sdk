// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.crashlytics.internal.flutter;

import androidx.annotation.NonNull;
import com.google.firebase.crashlytics.internal.Logger;
import java.lang.reflect.Method;

public class FlutterStateProviderImpl implements FlutterStateProvider {

  @NonNull
  @Override
  public FlutterState getFlutterState() {
    ClassLoader loader = this.getClass().getClassLoader();
    try {
      Class<?> clazz = loader.getClass();
      Method method = null;
      while (clazz != null && method == null) {
        Logger.getLogger().d(clazz.getName());
        Method[] methods = clazz.getDeclaredMethods();
        for (Method m : methods) {
          if (m.getName().equals("findLibrary")) {
            method = m;
            break;
          }
        }
        clazz = clazz.getSuperclass();
      }
      if (method == null) {
        // Shouldn't be possible
        return FlutterState.UNKNOWN;
      }
      method.setAccessible(true);
      String path = (String) method.invoke(loader, "flutter");
      return path == null || path.isEmpty() ? FlutterState.MISSING : FlutterState.INSTALLED;
    } catch (Exception e) {
      // Swallow all exceptions for non-critical path.
      return FlutterState.UNKNOWN;
    }
  }
}
