package com.nflpickem.pickem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class DiscordMessage {
    private String content;
    private String username;
    
    @JsonProperty("avatar_url")
    private String avatarUrl;
    
    private List<DiscordEmbed> embeds = new ArrayList<>();
    
    public DiscordMessage() {}
    
    public DiscordMessage(String content) {
        this.content = content;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public List<DiscordEmbed> getEmbeds() {
        return embeds;
    }
    
    public void setEmbeds(List<DiscordEmbed> embeds) {
        this.embeds = embeds;
    }
    
    public void addEmbed(DiscordEmbed embed) {
        this.embeds.add(embed);
    }
}
