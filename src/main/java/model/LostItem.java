
package model;

import java.sql.Date;

public class LostItem {

    private int id;
    private String username;
    private String itemName;
    private String category;
    private String description;
    private String lostLocation;
    private Date lostDate;
    private String contactInfo;
    private String status;

    public LostItem() {
    }

    public LostItem(String username, String itemName,
                    String category, String description,
                    String lostLocation, Date lostDate,
                    String contactInfo) {
        this.username = username;
        this.itemName = itemName;
        this.category = category;
        this.description = description;
        this.lostLocation = lostLocation;
        this.lostDate = lostDate;
        this.contactInfo = contactInfo;
        this.status = "LOST";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLostLocation() {
        return lostLocation;
    }

    public void setLostLocation(String lostLocation) {
        this.lostLocation = lostLocation;
    }

    public Date getLostDate() {
        return lostDate;
    }

    public void setLostDate(Date lostDate) {
        this.lostDate = lostDate;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
