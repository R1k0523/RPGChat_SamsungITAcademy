package com.boringowl.rpgchat.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.boringowl.rpgchat.CharacterActivity;
import com.boringowl.rpgchat.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Objects;

public class CharactersAdapter extends RecyclerView.Adapter<CharactersAdapter.GroupsViewHolder> {
    private List<String> charactersList;
    private Context mContext;
    private String groupId;
    private DatabaseReference UsersRef, GroupRef;

    public CharactersAdapter(Context mContext, List<String> charactersList, String groupId) {
        this.mContext = mContext;
        this.charactersList = charactersList;
        this.groupId = groupId;
    }

    @NonNull
    @Override
    public CharactersAdapter.GroupsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_character, viewGroup, false);

        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        GroupRef = FirebaseDatabase.getInstance().getReference().child("Groups").child(groupId);

        return new GroupsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final CharactersAdapter.GroupsViewHolder holder, final int position) {
        final String characterId = charactersList.get(position);

        GroupRef.child("Characters").child(characterId).child("info").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("name")) {
                    holder.characterText.setText(Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString());
                }
                if (dataSnapshot.hasChild("level")) {
                    holder.levelText.setText(Objects.requireNonNull(dataSnapshot.child("level").getValue()).toString());
                }
                if (dataSnapshot.hasChild("owner")) {
                    final String ownerId = Objects.requireNonNull(dataSnapshot.child("owner").getValue()).toString();
                    UsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.hasChild(ownerId)) {
                                holder.ownerText.setText(Objects.requireNonNull(dataSnapshot.child(ownerId).child("name").getValue()).toString());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
                }
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent characterIntent = new Intent(mContext, CharacterActivity.class);
                        characterIntent.putExtra("groupID", groupId);
                        characterIntent.putExtra("characterName", characterId);
                        mContext.startActivity(characterIntent);
                    }
                });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    @Override
    public int getItemCount() {
        return charactersList.size();
    }

    class GroupsViewHolder extends RecyclerView.ViewHolder {
        TextView characterText, ownerText, levelText;

        GroupsViewHolder(@NonNull View itemView) {
            super(itemView);

            characterText = itemView.findViewById(R.id.name);
            ownerText = itemView.findViewById(R.id.owner);
            levelText = itemView.findViewById(R.id.level);
        }
    }
}
