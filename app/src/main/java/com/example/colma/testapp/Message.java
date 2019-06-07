package com.example.colma.testapp;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class Message implements Parcelable, Comparable<Message> {
    String id;
    private String messageText;
    Loc location;
    private int upVotes;
    private int downVotes;

    public Message()
    {

    }

    public Message(String id, String messageText, Loc loc, int up, int down) {
        this.id = id;
        this.messageText = messageText;
        this.location = loc;
        this.upVotes = up;
        this.downVotes = down;
    }

    private Message(Parcel source) {
        id = source.readString();
        messageText = source.readString();
        location = source.readParcelable(getClass().getClassLoader());
    }

    public String getId() {
        return id;
    }

    public String getMessageText() {
        return messageText;
    }

    public Loc getLocation() {
        return location;
    }

    public int getUpVotes() {
        return upVotes;
    }

    public int getDownVotes() {
        return downVotes;
    }

    @Override

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(messageText);
        dest.writeParcelable(location, flags);
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }

        @Override
        public Message createFromParcel(Parcel source) {
            return new Message(source);
        }
    };

    @Override
    public int compareTo(Message compareMessage) {
        int compareVote = compareMessage.getUpVotes() - compareMessage.getDownVotes();
        int thisVote = this.getUpVotes() - this.getDownVotes();

        return compareVote - thisVote;
    }
}
