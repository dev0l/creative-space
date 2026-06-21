# Creative Space — ProGuard/R8 Rules
# ====================================

# Preserve line numbers for crash reports (Play Console / Firebase Crashlytics)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Compose — AGP 9.x bundles standard Compose rules automatically.
# No additional Compose-specific rules needed.

# org.json (JSONObject, JSONArray) — part of Android framework, not stripped by R8.
# No keep rules needed.