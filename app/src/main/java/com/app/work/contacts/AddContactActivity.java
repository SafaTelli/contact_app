package com.app.work.contacts;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.persistence.room.Room;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteConstraintException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.app.work.contacts.db.AppDatabase;
import com.app.work.contacts.db.ContactDAO;
import com.app.work.contacts.models.Contact;

import net.gotev.speech.GoogleVoiceTypingDisabledException;
import net.gotev.speech.Speech;
import net.gotev.speech.SpeechDelegate;
import net.gotev.speech.SpeechRecognitionNotAvailable;
import net.gotev.speech.SpeechUtil;
import net.gotev.speech.ui.SpeechProgressView;

import java.util.Date;
import java.util.List;

public class AddContactActivity extends AppCompatActivity implements SpeechDelegate {

    private final int PERMISSIONS_REQUEST = 1;

    private ImageButton button;
    private Button  addContact ;
    private EditText textName;
    private SpeechProgressView progress;
    private LinearLayout linearLayout;
    private EditText mNumTel ;

    private ContactDAO mContactDAO;
    @SuppressLint("CutPasteId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);
        Speech.init(this, getPackageName());
        mContactDAO = Room.databaseBuilder(this, AppDatabase.class, "db-contacts")
                .allowMainThreadQueries()   //Allows room to do operation on main thread
                .build()
                .getContactDAO();

        linearLayout = findViewById(R.id.linearLayout);

        button = findViewById(R.id.button);
        addContact = findViewById(R.id.add);
        mNumTel = findViewById(R.id.tel) ;
        button.setOnClickListener(view -> onButtonClick());


        addContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {



                String name = textName.getText().toString() ;
                String phoneNumber = mNumTel.getText().toString() ;

                if (name.length() == 0 || phoneNumber.length() == 0) {
                    Toast.makeText(AddContactActivity.this, "Vérifiez que vos informations sont corrects ", Toast.LENGTH_SHORT).show();
                    return;
                }

                Contact contact = new Contact();
                contact.setFirstName(name);
                contact.setLastName(name);
                contact.setPhoneNumber(phoneNumber);
                contact.setCreatedDate(new Date());

                //Insert to database
                try {
                    mContactDAO.insert(contact);
                    setResult(RESULT_OK);
                    finish();
                } catch (SQLiteConstraintException e) {
                    Toast.makeText(AddContactActivity.this, "A cotnact with same phone number already exists.", Toast.LENGTH_SHORT).show();
                }


            }
        });


        textName = findViewById(R.id.text);
        progress = findViewById(R.id.progress);

        int[] colors = {
                ContextCompat.getColor(this, android.R.color.black),
                ContextCompat.getColor(this, android.R.color.darker_gray),
                ContextCompat.getColor(this, android.R.color.black),
                ContextCompat.getColor(this, android.R.color.holo_orange_dark),
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
        };
        progress.setColors(colors);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Speech.getInstance().shutdown();
    }

    private void onButtonClick() {
        if (Speech.getInstance().isListening()) {
            Speech.getInstance().stopListening();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                onRecordAudioPermissionGranted();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != PERMISSIONS_REQUEST) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        } else {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay!
                onRecordAudioPermissionGranted();
            } else {
                // permission denied, boo!
                Toast.makeText(AddContactActivity.this, R.string.permission_required, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void onRecordAudioPermissionGranted() {
        button.setVisibility(View.GONE);
        linearLayout.setVisibility(View.VISIBLE);

        try {
            Speech.getInstance().stopTextToSpeech();
            Speech.getInstance().startListening(progress, AddContactActivity.this);

        } catch (SpeechRecognitionNotAvailable exc) {
            showSpeechNotSupportedDialog();

        } catch (GoogleVoiceTypingDisabledException exc) {
            showEnableGoogleVoiceTyping();
        }
    }


    @Override
    public void onStartOfSpeech() {
    }

    @Override
    public void onSpeechRmsChanged(float value) {
        //Log.d(getClass().getSimpleName(), "Speech recognition rms is now " + value +  "dB");
    }

    @Override
    public void onSpeechResult(String result) {

        button.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.GONE);

        textName.setText(result);

        if (result.isEmpty()) {
            Speech.getInstance().say(getString(R.string.repeat));

        } else {
            Speech.getInstance().say(result);
        }
    }

    @Override
    public void onSpeechPartialResults(List<String> results) {
        textName.setText("");
        for (String partial : results) {
            textName.append(partial + " ");
        }
    }

    private void showSpeechNotSupportedDialog() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        SpeechUtil.redirectUserToGoogleAppOnPlayStore(AddContactActivity.this);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.speech_not_available)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, dialogClickListener)
                .setNegativeButton(R.string.no, dialogClickListener)
                .show();
    }

    private void showEnableGoogleVoiceTyping() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.enable_google_voice_typing)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                })
                .show();
    }
}