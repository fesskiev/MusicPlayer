package com.fesskiev.mediacenter.data.model.vk;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Groups {

    @SerializedName("count")
    @Expose

    private int count;
    @SerializedName("items")
    @Expose
    private List<Group> groups = new ArrayList<>();

    public List<Group> getGroupsList() {
        return groups;
    }

}