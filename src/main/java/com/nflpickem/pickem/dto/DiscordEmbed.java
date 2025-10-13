package com.nflpickem.pickem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class DiscordEmbed {
    private String title;
    private String description;
    private int color;
    private String timestamp;
    private List<Field> fields = new ArrayList<>();
    private Footer footer;
    
    public DiscordEmbed() {}
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getColor() {
        return color;
    }
    
    public void setColor(int color) {
        this.color = color;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public List<Field> getFields() {
        return fields;
    }
    
    public void setFields(List<Field> fields) {
        this.fields = fields;
    }
    
    public Footer getFooter() {
        return footer;
    }
    
    public void setFooter(Footer footer) {
        this.footer = footer;
    }
    
    public void addField(String name, String value, boolean inline) {
        fields.add(new Field(name, value, inline));
    }
    
    public static class Field {
        private String name;
        private String value;
        private boolean inline;
        
        public Field() {}
        
        public Field(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        public boolean isInline() {
            return inline;
        }
        
        public void setInline(boolean inline) {
            this.inline = inline;
        }
    }
    
    public static class Footer {
        private String text;
        
        public Footer() {}
        
        public Footer(String text) {
            this.text = text;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
    }
}
