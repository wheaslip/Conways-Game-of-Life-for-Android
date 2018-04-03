package com.heslihop.wesley.gameoflife;

public class SavedState {
    private String name, date;
    private String id_num;

    public SavedState (String name, String date, String id_num) {
        this.name = name;
        this.date = date;
        this.id_num = id_num;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getId() {
        return id_num;
    }

    public void setId(String id_num) {
        this.id_num = id_num;
    }

    public String toString () {
        return name + " " + id_num;
    }


}
