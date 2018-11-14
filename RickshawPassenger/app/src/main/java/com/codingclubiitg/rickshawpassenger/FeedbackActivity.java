package com.codingclubiitg.rickshawpassenger;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.codingclubiitg.rickshawpassenger.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {

    private EditText feedback;
    private EditText email;
    private Button submit;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        feedback = findViewById(R.id.feedback_text);
        email = findViewById(R.id.email_feedback);
        submit = findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(email.getText().toString().endsWith("@iitg.ac.in") || email.getText().toString().endsWith("@iitg.ernet.in")) {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
//                    Feedback f = new Feedback(email.getText().toString(), feedback.getText().toString());
//                    databaseReference.push().setValue(f);
                    Map<String, Object> f = new HashMap<>();
                    f.put("email", email.getText().toString());
                    f.put("Message", feedback.getText().toString());
                    db.collection("overall_feedback").add(f);

                    Toast.makeText(getApplicationContext(), "Thank you for your feedback. Your feedback is recorded.", Toast.LENGTH_LONG).show();
                    finish();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Please enter a valid IITG email ID", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
