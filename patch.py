import re
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

text = re.sub(r'Thread.setDefaultUncaughtExceptionHandler.*?setContent \{.*?\}', 'Thread.setDefaultUncaughtExceptionHandler { _, e ->\n            android.util.Log.e("CrashReport", "Uncaught exception", e)\n            runOnUiThread {\n                setContent {\n                    androidx.compose.material3.Text(text = "CRASH: ${e.javaClass.simpleName}: ${e.message}", color = androidx.compose.ui.graphics.Color.Red, modifier = androidx.compose.ui.Modifier.padding(32.dp))\n                }', text, flags=re.DOTALL)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
