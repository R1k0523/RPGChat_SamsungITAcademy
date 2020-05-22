package com.boringowl.rpgchat.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.boringowl.rpgchat.ImageViewerActivity;
import com.boringowl.rpgchat.models.Messages;
import com.boringowl.rpgchat.R;
import com.boringowl.rpgchat.tools.TimeHandler;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private static final int MSG_TYPE_IN = 0;
    private static final int MSG_TYPE_OUT = 1;
    private Context mContext;
    private List<Messages> userMessagesList;
    private FirebaseAuth mAuth;

    private String messageReceiver;

    public MessageAdapter(Context mContext, List<Messages> userMessagesList, String messageReceiver) {
        this.mContext = mContext;
        this.userMessagesList = userMessagesList;
        this.messageReceiver = messageReceiver;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        mAuth = FirebaseAuth.getInstance();
        if (viewType == MSG_TYPE_OUT) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_message_out, viewGroup, false);
            return new MessageAdapter.MessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_message_in, viewGroup, false);
            return new MessageAdapter.MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder messageViewHolder, final int position) {
        final String messageSenderId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        Messages messages = userMessagesList.get(position);

        final String fromUserID = messages.getFrom();
        String fromMessageType = messages.getType();

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserID);

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("image")) {
                    String receiverImage = Objects.requireNonNull(dataSnapshot.child("image").getValue()).toString();

                    Picasso.get().load(receiverImage).placeholder(R.drawable.profile_image).into(messageViewHolder.receiverProfileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        messageViewHolder.messageText.setVisibility(View.GONE);
        messageViewHolder.receiverProfileImage.setVisibility(View.GONE);
        messageViewHolder.messagePicture.setVisibility(View.GONE);
        messageViewHolder.messageTime.setVisibility(View.VISIBLE);
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat nowDate = new SimpleDateFormat("dd MMM", Locale.ENGLISH);
        String currentDate = nowDate.format(calendar.getTime());

        String timeDate = TimeHandler.convertTime(messages.getTime());
        String date = timeDate.split(",")[0];
        String time = timeDate.split(",")[1];

        timeDate = date + "\n" + time;
        if (currentDate.equals(date))
            timeDate = time;

        messageViewHolder.messageTime.setText(timeDate);

        if (fromMessageType.equals("text")) {
            messageViewHolder.messageText.setVisibility(View.VISIBLE);
            messageViewHolder.messageText.setText(messages.getMessage());

        } else if (fromMessageType.equals("image")) {
            messageViewHolder.messagePicture.setVisibility(View.VISIBLE);
            Picasso.get().load(messages.getMessage()).into(messageViewHolder.messagePicture);
        }

        messageViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final AlertDialog.Builder builder = new AlertDialog.Builder(messageViewHolder.itemView.getContext());
                builder.setTitle("Message Control");

                final List<String> options = new ArrayList<>();
                options.add("Delete For me");
                if (fromUserID.equals(messageSenderId)) {
                    options.add("Delete For Everyone");
                } else
                    options.add("Cancel");
                if (userMessagesList.get(position).getType().equals("image")) {
                    options.add("View This Image");
                }

                final CharSequence[] csoptions = options.toArray(new CharSequence[options.size()]);
                builder.setItems(csoptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case 0:
                                deleteSentMessage(position, messageViewHolder);
                                break;
                            case 1:
                                if (fromUserID.equals(messageSenderId))
                                    deleteMessageForEveryone(position, messageViewHolder);
                                break;
                            case 2:
                                Intent intent = new Intent(messageViewHolder.itemView.getContext(), ImageViewerActivity.class);
                                intent.putExtra("url", userMessagesList.get(position).getMessage());
                                messageViewHolder.itemView.getContext().startActivity(intent);
                                break;
                            default:
                        }
                    }
                });
                builder.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return userMessagesList.size();
    }

    @Override
    public int getItemViewType(int position) {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        assert fUser != null;
        if (userMessagesList.get(position).getFrom().equals(fUser.getUid())) {
            return MSG_TYPE_OUT;
        } else return MSG_TYPE_IN;
    }

    private void deleteSentMessage(final int position, final MessageViewHolder holder) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages")
                .child(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                .child(messageReceiver)
                .child(userMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(holder.itemView.getContext(), "Deleted Successfully.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(holder.itemView.getContext(), "Error Occurred.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void deleteReceiverMessage(final int position, final MessageViewHolder holder) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages")
                .child(userMessagesList.get(position).getTo())
                .child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(holder.itemView.getContext(), "Deleted Successfully.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(holder.itemView.getContext(), "Error Occurred.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void deleteMessageForEveryone(final int position, final MessageViewHolder holder) {
        deleteReceiverMessage(position, holder);
        deleteSentMessage(position, holder);
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, messageTime;
        CircleImageView receiverProfileImage;
        ImageView messagePicture;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.message_text);
            messageTime = itemView.findViewById(R.id.message_time);
            receiverProfileImage = itemView.findViewById(R.id.message_profile_image);
            messagePicture = itemView.findViewById(R.id.message_image_view);
        }
    }
}
