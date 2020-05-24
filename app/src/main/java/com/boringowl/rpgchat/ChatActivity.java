package com.boringowl.rpgchat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.boringowl.rpgchat.adapters.MessageAdapter;
import com.boringowl.rpgchat.dialogs.ProfileDialog;
import com.boringowl.rpgchat.models.Messages;
import com.boringowl.rpgchat.tools.TimeHandler;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;


public class ChatActivity extends AppCompatActivity {
    private final List<Messages> messagesList = new ArrayList<>();
    private String messageReceiverID;
    private String messageSenderID;
    private TextView userLastSeen;
    private DatabaseReference RootRef;
    private EditText messageInputText;
    private MessageAdapter messageAdapter;
    private RecyclerView userMessagesList;


    private String checker = "", myUrl = "";
    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        messageSenderID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();

        messageReceiverID = Objects.requireNonNull(Objects.requireNonNull(getIntent().getExtras()).get("visit_user_id")).toString();
        String messageReceiverName = Objects.requireNonNull(getIntent().getExtras().get("visit_user_name")).toString();
        String messageReceiverImage = Objects.requireNonNull(getIntent().getExtras().get("visit_image")).toString();

        Toolbar chatToolBar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(chatToolBar);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert layoutInflater != null;
        View actionBarView = layoutInflater.inflate(R.layout.bar_chat, null);
        actionBar.setCustomView(actionBarView);

        TextView userName = findViewById(R.id.chat_name);
        CircleImageView userImage = findViewById(R.id.chat_image);
        userLastSeen = findViewById(R.id.user_last_seen);

        ImageButton sendMessageButton = findViewById(R.id.send_message_button);
        ImageButton sendFilesButton = findViewById(R.id.send_files_btn);
        messageInputText = findViewById(R.id.input_message);


        messageAdapter = new MessageAdapter(ChatActivity.this, messagesList, messageReceiverID);
        userMessagesList = findViewById(R.id.private_messages_list_of_users);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);


        userName.setText(messageReceiverName);
        Picasso.get().load(messageReceiverImage).placeholder(R.drawable.profile_image).into(userImage);
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
                userMessagesList.scrollToPosition(Objects.requireNonNull(userMessagesList.getAdapter()).getItemCount() - 1);
            }
        });
        sendFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checker = "image";
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Image"), 443);
                userMessagesList.scrollToPosition(Objects.requireNonNull(userMessagesList.getAdapter()).getItemCount() - 1);
            }
        });
        userImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileDialog dialog = new ProfileDialog(messageReceiverID);
                dialog.show(getSupportFragmentManager(), "a");
            }
        });
        displayLastSeen();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 443 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            fileUri = data.getData();
            if (checker.equals("image")) {
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");
                final String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
                final String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;


                DatabaseReference userMessageKeyRef = RootRef.child("Messages")
                        .child(messageSenderID).child(messageReceiverID).push();

                final String messagePushID = userMessageKeyRef.getKey();

                final StorageReference filePath = storageReference.child(messagePushID + "." + "jpg");

                StorageTask uploadTask = filePath.putFile(fileUri);
                uploadTask.continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {
                        if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        Uri downloadUrl = task.getResult();
                        myUrl = downloadUrl.toString();

                        Map<String, String> messageTextBody = new HashMap<>();
                        messageTextBody.put("message", myUrl);
                        messageTextBody.put("name", fileUri.getLastPathSegment());
                        messageTextBody.put("type", checker);
                        messageTextBody.put("from", messageSenderID);
                        messageTextBody.put("to", messageReceiverID);
                        messageTextBody.put("messageID", messagePushID);
                        messageTextBody.put("time", TimeHandler.getTime());

                        Map<String, Object> messageBodyDetails = new HashMap<>();
                        messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
                        messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

                        RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                            @Override
                            public void onComplete(@NonNull Task task) {
                                String toastText = "Error";
                                if (task.isSuccessful())
                                    toastText = "Message Sent Successfully";
                                Toast.makeText(ChatActivity.this, toastText, Toast.LENGTH_SHORT).show();
                                messageInputText.setText("");
                                userMessagesList.scrollToPosition(userMessagesList.getAdapter().getItemCount() - 1);
                            }
                        });
                        RootRef.child("Contacts").child(messageReceiverID).child(messageSenderID)
                                .child("LastMessage").setValue(TimeHandler.getLastTime());
                        RootRef.child("Contacts").child(messageSenderID).child(messageReceiverID)
                                .child("LastMessage").setValue(TimeHandler.getLastTime());
                    }
                });

            } else {
                Toast.makeText(this, "nothing selected,error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void displayLastSeen() {
        RootRef.child("Users").child(messageReceiverID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.child("userState").hasChild("state")) {
                            String state = dataSnapshot.child("userState").child("state").getValue().toString();
                            String timeDate = dataSnapshot.child("userState").child("time").getValue().toString();
                            timeDate = TimeHandler.convertTime(timeDate);
                            String date = timeDate.split(",")[0];
                            String time = timeDate.split(",")[1];

                            if (state.equals("online")) {
                                userLastSeen.setText("online");
                            } else if (state.equals("offline")) {
                                Calendar calendar = Calendar.getInstance();
                                SimpleDateFormat nowDate = new SimpleDateFormat("dd MMM", Locale.ENGLISH);
                                String currentDate = nowDate.format(calendar.getTime());
                                String status = "Last Seen: " + date + ", " + time;
                                if (currentDate.equals(date))
                                    status = "Last Seen: " + time;
                                userLastSeen.setText(status);
                            }
                        } else {
                            userLastSeen.setText("online");
                        }
                        userMessagesList.scrollToPosition(userMessagesList.getAdapter().getItemCount() - 1);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }

                });
    }


    @Override
    protected void onStart() {
        super.onStart();
        TimeHandler.update("online");
        RootRef.child("Messages").child(messageSenderID).child(messageReceiverID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        messagesList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Messages messages = snapshot.getValue(Messages.class);
                            messagesList.add(messages);
                        }
                        messageAdapter = new MessageAdapter(ChatActivity.this, messagesList, messageReceiverID);
                        userMessagesList.setAdapter(messageAdapter);
                        userMessagesList.scrollToPosition(userMessagesList.getAdapter().getItemCount() - 1);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void sendMessage() {
        String messageText = messageInputText.getText().toString();

        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, "first write your message...", Toast.LENGTH_SHORT).show();
        } else {

            String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
            String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;

            DatabaseReference userMessageKeyRef = RootRef.child("Messages")
                    .child(messageSenderID).child(messageReceiverID).push();

            String messagePushID = userMessageKeyRef.getKey();

            Map<String, String> messageTextBody = new HashMap<>();
            messageTextBody.put("message", messageText);
            messageTextBody.put("type", "text");
            messageTextBody.put("from", messageSenderID);
            messageTextBody.put("to", messageReceiverID);
            messageTextBody.put("messageID", messagePushID);
            messageTextBody.put("time", TimeHandler.getTime());

            Map<String, Object> messageBodyDetails = new HashMap<>();
            messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
            messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    messageInputText.setText("");
                }
            });
            RootRef.child("Contacts").child(messageReceiverID).child(messageSenderID)
                    .child("LastMessage").setValue(TimeHandler.getLastTime());
            RootRef.child("Contacts").child(messageSenderID).child(messageReceiverID)
                    .child("LastMessage").setValue(TimeHandler.getLastTime());
            userMessagesList.scrollToPosition(Objects.requireNonNull(userMessagesList.getAdapter()).getItemCount() - 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TimeHandler.update("online");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TimeHandler.update("offline");
    }
}
