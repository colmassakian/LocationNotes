package com.example.colma.testapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageListAdapter extends BaseAdapter implements ListAdapter {
    private final String TAG = "Adapter";
    private ArrayList<Message> list;
    private Context context;
    private Context applicationContext;
    private String reference;
    private DatabaseReference messagesDB;
    private Map<String, List<Integer>> voteMap;
    TextView voteText;
    private MessageListAdapter mla;

    MessageListAdapter(ArrayList<Message> list, Context context, Context appContext, String ref, Map<String, List<Integer>> voteInfo) {
        mla = this;
        this.list = list;
        this.context = context;
        this.applicationContext = appContext;
        this.reference = ref;
        this.voteMap = voteInfo;
        messagesDB = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Message getItem(int pos) {
        return list.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return 0;
        //just return 0 if your list items do not have an Id variable.
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.custom_layout, null);
        }

        //Handle TextView and display string from your list
        TextView noteText = view.findViewById(R.id.noteText);
        noteText.setText(list.get(position).getMessageText());

        //Handle buttons and add onClickListeners
        ImageButton thumbsUp = view.findViewById(R.id.thumbsUp);
        ImageButton thumbsDown = view.findViewById(R.id.thumbsDown);

        SharedPreferences pref = applicationContext.getSharedPreferences("UserVotes", 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();

        String id = list.get(position).getId();
        int currVoteStatus = pref.getInt(id + "VoteStatus", 0);
        if(currVoteStatus == 1)
        {
            thumbsUp.setImageResource(R.drawable.ic_green_thumb_up);
            thumbsDown.setImageResource(R.drawable.ic_thumb_down);
        }
        else if(currVoteStatus == -1)
        {
            thumbsUp.setImageResource(R.drawable.ic_thumb_up);
            thumbsDown.setImageResource(R.drawable.ic_red_thumb_down);
        }
        else
        {
            thumbsUp.setImageResource(R.drawable.ic_thumb_up);
            thumbsDown.setImageResource(R.drawable.ic_thumb_down);
        }


        List<Integer> votes = voteMap.get(list.get(position).getId());
        int currUpVotes = votes.get(0);
        int currDownVotes = votes.get(1);

        voteText = view.findViewById(R.id.voteText);
        voteText.setText(Integer.toString(currUpVotes - currDownVotes));


        thumbsUp.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // Upvote

                String id = list.get(position).getId();
                updateUpVotes(id);
            }
        });
        thumbsDown.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //Downvote

                String id = list.get(position).getId();
                updateDownVotes(id);
            }
        });

        return view;
    }


    private void updateUpVotes(String id) {
        final DatabaseReference getDataRef = FirebaseDatabase.getInstance().getReference(reference);

        getDataRef.orderByChild("id").equalTo(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot datas: dataSnapshot.getChildren()){
                    String id = datas.child("id").getValue().toString();
                    int upVotes = datas.child("upVotes").getValue(Integer.class);
                    int downVotes = datas.child("downVotes").getValue(Integer.class);

                    SharedPreferences pref = applicationContext.getSharedPreferences("UserVotes", 0); // 0 - for private mode
                    SharedPreferences.Editor editor = pref.edit();


                    int currVoteStatus = pref.getInt(id + "VoteStatus", 0);

                    if(currVoteStatus == 0) // No previous vote
                    {
                        editor.putInt(id + "VoteStatus", 1);
                        upVotes ++;
                        getDataRef.child(id).child("upVotes").setValue(upVotes);
                    }
                    else if(currVoteStatus == -1) // Downvoted
                    {
                        editor.putInt(id + "VoteStatus", 1);
                        upVotes ++;
                        downVotes --;
                        getDataRef.child(id).child("upVotes").setValue(upVotes);
                        getDataRef.child(id).child("downVotes").setValue(downVotes);
                    }
                    else // Already upvoted
                    {
                        editor.putInt(id + "VoteStatus", 0);
                        upVotes --;
                        getDataRef.child(id).child("upVotes").setValue(upVotes);
                    }
                    List<Integer> votesList = new ArrayList<>();
                    votesList.add(upVotes);
                    votesList.add(downVotes);
                    voteMap.put(id, votesList);
                    editor.commit();
                    mla.notifyDataSetChanged();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void updateDownVotes(String id) {
        final DatabaseReference getDataRef = FirebaseDatabase.getInstance().getReference(reference);

        getDataRef.orderByChild("id").equalTo(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot datas: dataSnapshot.getChildren()){
                    String id = datas.child("id").getValue().toString();
                    int upVotes = datas.child("upVotes").getValue(Integer.class);
                    int downVotes = datas.child("downVotes").getValue(Integer.class);

                    SharedPreferences pref = applicationContext.getSharedPreferences("UserVotes", 0); // 0 - for private mode
                    SharedPreferences.Editor editor = pref.edit();


                    int currVoteStatus = pref.getInt(id + "VoteStatus", 0);

                    if(currVoteStatus == 0) // No previous vote
                    {
                        editor.putInt(id + "VoteStatus", -1);
                        downVotes ++;
                        messagesDB.child(reference).child(id).child("downVotes").setValue(downVotes);
                    }
                    else if(currVoteStatus == 1) // Upvoted
                    {
                        editor.putInt(id + "VoteStatus", -1);
                        upVotes --;
                        downVotes ++;
                        messagesDB.child(reference).child(id).child("upVotes").setValue(upVotes);
                        messagesDB.child(reference).child(id).child("downVotes").setValue(downVotes);
                    }
                    else // Already downvoted
                    {
                        editor.putInt(id + "VoteStatus", 0);
                        downVotes --;
                        messagesDB.child(reference).child(id).child("downVotes").setValue(downVotes);
                    }
                    List<Integer> votesList = new ArrayList<>();
                    votesList.add(upVotes);
                    votesList.add(downVotes);
                    voteMap.put(id, votesList);
                    editor.commit();
                    mla.notifyDataSetChanged();

                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
}
