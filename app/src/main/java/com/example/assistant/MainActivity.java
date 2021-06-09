package com.example.assistant;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.basgeekball.awesomevalidation.AwesomeValidation;
import com.basgeekball.awesomevalidation.ValidationStyle;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {
    private Button sign_up;
    private EditText first_name;
    private EditText last_name, dob;
    private EditText phone_number;
    private EditText email_id, password, conf_password;
    private TextView sign_in;


    FirebaseAuth mFirebaseAuth;
    AwesomeValidation awesomeValidation;
    FirebaseDatabase mDatabase;
    DatabaseReference mDatabaseReference;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFirebaseAuth = FirebaseAuth.getInstance();
        awesomeValidation=new AwesomeValidation(ValidationStyle.BASIC);
        mDatabase=FirebaseDatabase.getInstance();
        mDatabaseReference=mDatabase.getReference("User");

        init();

        if(mFirebaseAuth.getCurrentUser() != null) {
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            finish();
        }

//        validation();
        awesomeValidation.addValidation(this, R.id.et_first_name, "[a-z,A-Z]*", R.string.nameerror);
        awesomeValidation.addValidation(this, R.id.et_last_name, "[a-z,A-Z]*", R.string.nameerror);
        awesomeValidation.addValidation(this, R.id.et_Dob, "^(?:(?:31(\\/|-|\\.)(?:0?[13578]|1[02]))\\1|(?:(?:29|30)(\\/|-|\\.)(?:0?[1,3-9]|1[0-2])\\2))(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$|^(?:29(\\/|-|\\.)0?2\\3(?:(?:(?:1[6-9]|[2-9]\\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|^(?:0?[1-9]|1\\d|2[0-8])(\\/|-|\\.)(?:(?:0?[1-9])|(?:1[0-2]))\\4(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$", R.string.birtherror);
        awesomeValidation.addValidation(this, R.id.et_phone_number, "[5-9]{1}[0-9]{9}$", R.string.mobileerror);
        awesomeValidation.addValidation(this, R.id.email_id, Patterns.EMAIL_ADDRESS, R.string.emailerror);
        awesomeValidation.addValidation(this, R.id.login_password, ".{6,}", R.string.passerror);
        awesomeValidation.addValidation(this, R.id.confirm_password, R.id.login_password, R.string.confirmpasserror);

        sign_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String FirstName = first_name.getText().toString().trim();
                final String LastName = last_name.getText().toString().trim();
                final String Dob = dob.getText().toString().trim();
                final String Email = email_id.getText().toString().trim();
                final String pwd = password.getText().toString().trim();
                final String Phone = phone_number.getText().toString().trim();
                String checkspaces = "(?=\\s+$)";
                if((FirstName.isEmpty())&&(FirstName.length() > 20) && (!(FirstName.matches(checkspaces)))) {
                    first_name.setError("Please enter valid name");
                    first_name.requestFocus();
                }
                else if(Dob.isEmpty()) {
                    dob.setError("Please enter valid date");
                    dob.requestFocus();
                }
                else if((LastName.isEmpty())&&(LastName.length() > 20) && (!(LastName.matches(checkspaces)))) {
                    last_name.setError("Please enter valid name");
                    last_name.requestFocus();
                }
                else if(Email.isEmpty()) {
                    email_id.setError("Please enter Email Id");
                    email_id.requestFocus();
                }
                else if(FirstName.isEmpty()) {
                    email_id.setError("Please enter your first name");
                    email_id.requestFocus();
                }
                else if(LastName.isEmpty()) {
                    last_name.setError("Please enter your last name");
                    last_name.requestFocus();
                }
                else if(Phone.isEmpty()) {
                    phone_number.setError("Please enter your phone number");
                    phone_number.requestFocus();
                }
                else if( FirstName.isEmpty() && LastName.isEmpty() && Dob.isEmpty() && Phone.isEmpty() && Email.isEmpty() && pwd.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Fields Are Empty!", Toast.LENGTH_SHORT).show();
                }
                else if((!(FirstName.isEmpty() && LastName.isEmpty() && Dob.isEmpty() && Phone.isEmpty() && Email.isEmpty() && pwd.isEmpty())) && awesomeValidation.validate()) {
                    mFirebaseAuth.createUserWithEmailAndPassword(Email, pwd) .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(!task.isSuccessful()) {
                                Toast.makeText(MainActivity.this, "SignUp Unsuccessful", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                String uid = task.getResult().getUser().getUid();
                                User user = new User(uid, FirstName, LastName, Dob, Phone, Email, pwd);
                                mDatabase.getReference().child("Users").child(uid).setValue(user);
//                                addDataToFirebase(uid, FirstName, LastName, Dob, Phone, Email, pwd);
                                Toast.makeText(MainActivity.this, "SignUp Successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(MainActivity.this, HomeActivity.class));
                            }
                        }
                    });
                }
                else {
                    Toast.makeText(MainActivity.this, "Error Occurred!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        sign_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Please Login", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });
    }
    private void init(){

        sign_up=findViewById(R.id.btnSignUp);
        first_name=findViewById(R.id.et_first_name);
        last_name=findViewById(R.id.et_last_name);
        dob=findViewById(R.id.et_Dob);
        phone_number=findViewById(R.id.et_phone_number);
        email_id=findViewById(R.id.email_id);
        password=findViewById(R.id.login_password);
        conf_password= findViewById(R.id.confirm_password);
        sign_in=findViewById(R.id.tvSignIn);
    }
    private void validation(){
        awesomeValidation.addValidation(this, R.id.et_first_name, "[a-z,A-Z]*", R.string.nameerror);
        awesomeValidation.addValidation(this, R.id.et_last_name, "[a-z,A-Z]*", R.string.nameerror);
        awesomeValidation.addValidation(this, R.id.et_Dob, "^(?:(?:31(\\/|-|\\.)(?:0?[13578]|1[02]))\\1|(?:(?:29|30)(\\/|-|\\.)(?:0?[1,3-9]|1[0-2])\\2))(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$|^(?:29(\\/|-|\\.)0?2\\3(?:(?:(?:1[6-9]|[2-9]\\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|^(?:0?[1-9]|1\\d|2[0-8])(\\/|-|\\.)(?:(?:0?[1-9])|(?:1[0-2]))\\4(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$", R.string.birtherror);
        awesomeValidation.addValidation(this, R.id.et_phone_number, "[5-9]{1}[0-9]{9}$", R.string.mobileerror);
        awesomeValidation.addValidation(this, R.id.email_id, Patterns.EMAIL_ADDRESS, R.string.emailerror);
        awesomeValidation.addValidation(this, R.id.login_password, ".{6,}", R.string.passerror);
        awesomeValidation.addValidation(this, R.id.confirm_password, R.id.login_password, R.string.confirmpasserror);

    }
}