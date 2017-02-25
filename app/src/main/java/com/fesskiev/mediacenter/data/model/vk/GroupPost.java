package com.fesskiev.mediacenter.data.model.vk;


import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.fesskiev.mediacenter.utils.download.DownloadGroupAudioFile;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class GroupPost implements Parcelable {

    @SerializedName("attachments")
    @Expose
    private List<Attachment> attachments = new ArrayList<>();

    @SerializedName("copy_history")
    @Expose
    private CopyHistory copyHistory = new CopyHistory();

    @SerializedName("text")
    @Expose
    private String text;

    @SerializedName("date")
    @Expose
    private long date;

    @SerializedName("id")
    @Expose
    private int id;

    @SerializedName("likes")
    @Expose
    private Likes likes;

    @SerializedName("reposts")
    @Expose
    private Reposts reposts;

    private transient List<DownloadGroupAudioFile> downloadGroupAudioFiles;
    private transient boolean openMusicItems;

    public GroupPost(Parcel in) {
        this.text = in.readString();
        this.date = in.readLong();
        this.id = in.readInt();
        this.likes.setCount(in.readInt());
        this.reposts.setCount(in.readInt());
    }

    public GroupPost() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(text);
        out.writeLong(date);
        out.writeInt(id);
        out.writeInt(likes.getCount());
        out.writeInt(reposts.getCount());
    }

    public static final Parcelable.Creator<GroupPost> CREATOR
            = new Parcelable.Creator<GroupPost>() {
        public GroupPost createFromParcel(Parcel in) {
            return new GroupPost(in);
        }

        public GroupPost[] newArray(int size) {
            return new GroupPost[size];
        }
    };

    public List<Attachment> getAttachments() {
        return attachments;
    }


    public long getDate() {
        return date;
    }

    public int getId() {
        return id;
    }

    public Likes getLikes() {
        return likes;
    }

    public Reposts getReposts() {
        return reposts;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setLikes(Likes likes) {
        this.likes = likes;
    }

    public void setReposts(Reposts reposts) {
        this.reposts = reposts;
    }

    public CopyHistory getCopyHistory() {
        return copyHistory;
    }

    public void setCopyHistory(CopyHistory copyHistory) {
        this.copyHistory = copyHistory;
    }

    public List<DownloadGroupAudioFile> getDownloadGroupAudioFiles() {
        return downloadGroupAudioFiles;
    }

    public void setDownloadGroupAudioFiles(List<DownloadGroupAudioFile> downloadGroupAudioFiles) {
        this.downloadGroupAudioFiles = downloadGroupAudioFiles;
    }

    public boolean isOpenMusicItems() {
        return openMusicItems;
    }

    public void setOpenMusicItems(boolean openMusicItems) {
        this.openMusicItems = openMusicItems;
    }

    public List<Audio> getAudio() {
        List<Audio> audios = new ArrayList<>();

        List<Attachment> copyAttachments = copyHistory.getAttachments();
        for (Attachment copyAttachment : copyAttachments) {
            Audio copyAudio = copyAttachment.getAudio();
            if (copyAudio != null) {
                audios.add(copyAudio);
            }
        }

        for (Attachment attachment : attachments) {
            Audio audio = attachment.getAudio();
            if (audio != null) {
                audios.add(audio);
            }
        }
        return audios;
    }

    public Photo getPhoto() {
        if (!attachments.isEmpty()) {
            for (Attachment attachment : attachments) {
                if (attachment.getType().equals(Attachment.TYPE_PHOTO)) {
                    return attachment.getPhoto();
                }
            }
        } else {
            List<Attachment> copyAttachments = copyHistory.getAttachments();
            for (Attachment copyAttachment : copyAttachments) {
                if (copyAttachment.getType().equals(Attachment.TYPE_PHOTO)) {
                    return copyAttachment.getPhoto();
                }
            }
        }
        return null;
    }

    public String getText() {
        if (text != null && !TextUtils.isEmpty(text)) {
            return text;
        } else {
            return copyHistory.getText();
        }
    }

    @Override
    public String toString() {
        return "GroupPost{" +
                "attachments=" + attachments +
                ", copyHistory=" + copyHistory +
                ", text='" + text + '\'' +
                ", date=" + date +
                ", id=" + id +
                ", likes=" + likes +
                ", reposts=" + reposts +
                ", downloadGroupAudioFiles=" + downloadGroupAudioFiles +
                ", openMusicItems=" + openMusicItems +
                '}';
    }
}
