package com.example.colma.testapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class MessageListAdapter extends BaseAdapter implements ListAdapter {
    private ArrayList<Message> list = new ArrayList<Message>();
    private Context context;

    public MessageListAdapter(ArrayList<Message> list, Context context) {
        this.list = list;
        this.context = context;
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
        TextView noteText = (TextView)view.findViewById(R.id.noteText);
        noteText.setText(list.get(position).getMessageText());

        //Handle buttons and add onClickListeners
        Button thumbsUp = (Button)view.findViewById(R.id.thumbsUp);
        Button thumbsDown = (Button)view.findViewById(R.id.thumbsDown);

        thumbsUp.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // Upvote

            }
        });
        thumbsDown.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //Downvote
            }
        });

        return view;
    }
}
