package com.example.zypherprojectpart3;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> test = new HashMap<>();
        test.put("status", "ok");
        test.put("from", "android");

        db.collection("debug").document("ping").set(test)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "✅ Firestore write success", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Firestore write failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
