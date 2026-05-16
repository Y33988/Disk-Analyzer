package com.diskanalyzer.service;

import javafx.scene.Scene;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class ThemeManager {
    
    public enum Theme {
        LIQUID_GLASS("液态玻璃", "/glass-styles.css",
            "#0f0c29", "#302b63", "#24243e",
            "#ffffff", "rgba(255,255,255,0.15)"),
        DARK_MODE("深色模式", "/dark-styles.css",
            "#1a1a1a", "#2d2d2d", "#1e1e1e",
            "#e0e0e0", "rgba(255,255,255,0.08)"),
        LIGHT_MODE("浅色模式", "/light-styles.css",
            "#f5f5f5", "#e0e0e0", "#ffffff",
            "#333333", "rgba(0,0,0,0.05)");
        
        private final String displayName;
        private final String cssPath;
        private final String bgStart;
        private final String bgMiddle;
        private final String bgEnd;
        private final String textColor;
        private final String panelBg;
        
        Theme(String displayName, String cssPath, String bgStart, String bgMiddle, String bgEnd, String textColor, String panelBg) {
            this.displayName = displayName;
            this.cssPath = cssPath;
            this.bgStart = bgStart;
            this.bgMiddle = bgMiddle;
            this.bgEnd = bgEnd;
            this.textColor = textColor;
            this.panelBg = panelBg;
        }
        
        public String getDisplayName() { return displayName; }
        public String getCssPath() { return cssPath; }
        public String getBgStart() { return bgStart; }
        public String getBgMiddle() { return bgMiddle; }
        public String getBgEnd() { return bgEnd; }
        public String getTextColor() { return textColor; }
        public String getPanelBg() { return panelBg; }
    }
    
    private Theme currentTheme = Theme.LIQUID_GLASS;
    private List<ThemeChangeListener> listeners = new ArrayList<>();
    private final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
    
    public interface ThemeChangeListener {
        void onThemeChanged(Theme newTheme);
    }
    
    public ThemeManager() {
        String savedTheme = prefs.get("currentTheme", Theme.LIQUID_GLASS.name());
        try {
            currentTheme = Theme.valueOf(savedTheme);
        } catch (IllegalArgumentException e) {
            currentTheme = Theme.LIQUID_GLASS;
        }
    }
    
    public Theme getCurrentTheme() {
        return currentTheme;
    }
    
    public void setTheme(Theme theme) {
        if (currentTheme != theme) {
            currentTheme = theme;
            prefs.put("currentTheme", theme.name());
            notifyListeners();
        }
    }
    
    public void applyTheme(Scene scene) {
        scene.getStylesheets().clear();
        String cssPath = getClass().getResource(currentTheme.getCssPath()).toExternalForm();
        scene.getStylesheets().add(cssPath);
    }
    
    public void addListener(ThemeChangeListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyListeners() {
        for (ThemeChangeListener listener : listeners) {
            listener.onThemeChanged(currentTheme);
        }
    }
    
    public Theme[] getAllThemes() {
        return Theme.values();
    }
}
