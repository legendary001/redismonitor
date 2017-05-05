package com.chejh5.entity;

import com.google.gson.annotations.Expose;

/**
 * Created by chenjh5 on 2017/5/4.
 */
public class Article {

    private String docId;

    private String simId;

    private String title;

    private String date;

    private String hotLevel;

    private String docChannel;

    private String why;

    private String score;

    private String hotBoost;

    private String docType;

    private String readableFeatures;

    private String others;

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getSimId() {
        return simId;
    }

    public void setSimId(String simId) {
        this.simId = simId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getHotLevel() {
        return hotLevel;
    }

    public void setHotLevel(String hotLevel) {
        this.hotLevel = hotLevel;
    }

    public String getDocChannel() {
        return docChannel;
    }

    public void setDocChannel(String docChannel) {
        this.docChannel = docChannel;
    }

    public String getWhy() {
        return why;
    }

    public void setWhy(String why) {
        this.why = why;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getHotBoost() {
        return hotBoost;
    }

    public void setHotBoost(String hotBoost) {
        this.hotBoost = hotBoost;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getReadableFeatures() {
        return readableFeatures;
    }

    public void setReadableFeatures(String readableFeatures) {
        this.readableFeatures = readableFeatures;
    }

    public String getOthers() {
        return others;
    }

    public void setOthers(String others) {
        this.others = others;
    }

    @Override
    public String toString() {
        return "Article{" +
                "docId='" + docId + '\'' +
                ", simId='" + simId + '\'' +
                ", title='" + title + '\'' +
                ", date='" + date + '\'' +
                ", hotLevel='" + hotLevel + '\'' +
                ", docChannel='" + docChannel + '\'' +
                ", why='" + why + '\'' +
                ", score='" + score + '\'' +
                ", hotBoost='" + hotBoost + '\'' +
                ", docType='" + docType + '\'' +
                ", readableFeatures='" + readableFeatures + '\'' +
                ", others='" + others + '\'' +
                '}';
    }
}
