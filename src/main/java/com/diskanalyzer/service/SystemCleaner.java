package com.diskanalyzer.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class SystemCleaner {
    
    private static final Logger logger = Logger.getLogger(SystemCleaner.class.getName());
    
    public enum CleanCategory {
        TEMP_FILES("临时文件", "%TEMP%", "*.tmp"),
        RECYCLE_BIN("回收站", "$Recycle.Bin", null),
        BROWSER_CACHE("浏览器缓存", "AppData\\Local", "*.cache"),
        WINDOWS_UPDATE("Windows更新缓存", "Windows\\SoftwareDistribution\\Download", null),
        LOG_FILES("日志文件", "Windows\\Logs", "*.log"),
        THUMBNAILS("缩略图缓存", "AppData\\Local\\Microsoft\\Windows\\Explorer", "*.db"),
        PREFETCH("预读文件", "Windows\\Prefetch", "*.pf"),
        ERROR_REPORTS("错误报告", "ProgramData\\Microsoft\\Windows\\WER", null);
        
        private final String displayName;
        private final String basePath;
        private final String pattern;
        
        CleanCategory(String displayName, String basePath, String pattern) {
            this.displayName = displayName;
            this.basePath = basePath;
            this.pattern = pattern;
        }
        
        public String getDisplayName() { return displayName; }
        public String getBasePath() { return basePath; }
        public String getPattern() { return pattern; }
    }
    
    public static class CleanResult {
        public final CleanCategory category;
        public final long cleanedSize;
        public final int fileCount;
        public final String message;
        public final boolean success;
        
        public CleanResult(CleanCategory category, long cleanedSize, int fileCount, String message, boolean success) {
            this.category = category;
            this.cleanedSize = cleanedSize;
            this.fileCount = fileCount;
            this.message = message;
            this.success = success;
        }
    }
    
    private CleanProgressListener listener;
    
    public interface CleanProgressListener {
        void onCategoryStart(CleanCategory category);
        void onCategoryComplete(CleanResult result);
        void onAllComplete(List<CleanResult> results);
        void onError(String message);
    }
    
    public void setListener(CleanProgressListener listener) {
        this.listener = listener;
    }
    
    public CompletableFuture<List<CleanResult>> clean(List<CleanCategory> categories) {
        return CompletableFuture.supplyAsync(() -> {
            List<CleanResult> results = new ArrayList<>();
            for (CleanCategory category : categories) {
                try {
                    if (listener != null) listener.onCategoryStart(category);
                    CleanResult result = cleanCategory(category);
                    results.add(result);
                    if (listener != null) listener.onCategoryComplete(result);
                } catch (Exception e) {
                    CleanResult errorResult = new CleanResult(category, 0, 0, "清理失败: " + e.getMessage(), false);
                    results.add(errorResult);
                    if (listener != null) listener.onCategoryComplete(errorResult);
                }
            }
            if (listener != null) listener.onAllComplete(results);
            return results;
        });
    }
    
    private CleanResult cleanCategory(CleanCategory category) {
        String basePath = resolvePath(category.getBasePath());
        if (basePath == null || !new File(basePath).exists()) {
            return new CleanResult(category, 0, 0, "路径不存在", false);
        }
        
        AtomicLong totalSize = new AtomicLong(0);
        AtomicLong fileCount = new AtomicLong(0);
        
        try {
            File dir = new File(basePath);
            if (category.getPattern() != null) {
                scanAndClean(dir, category.getPattern(), totalSize, fileCount);
            } else {
                scanAndCleanAll(dir, totalSize, fileCount);
            }
            
            return new CleanResult(category, totalSize.get(), fileCount.intValue(), 
                    "清理完成: " + formatSize(totalSize.get()) + " (" + fileCount.get() + " 个文件)", true);
        } catch (Exception e) {
            return new CleanResult(category, totalSize.get(), fileCount.intValue(), 
                    "部分清理完成: " + formatSize(totalSize.get()) + "，但发生错误: " + e.getMessage(), false);
        }
    }
    
    private void scanAndClean(File dir, String pattern, AtomicLong totalSize, AtomicLong fileCount) {
        try {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString();
                    if (matchesPattern(fileName, pattern)) {
                        try {
                            totalSize.addAndGet(attrs.size());
                            Files.delete(file);
                            fileCount.incrementAndGet();
                        } catch (IOException e) {
                            logger.warning("无法删除文件: " + file + " - " + e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.warning("扫描目录失败: " + dir + " - " + e.getMessage());
        }
    }
    
    private void scanAndCleanAll(File dir, AtomicLong totalSize, AtomicLong fileCount) {
        try {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        totalSize.addAndGet(attrs.size());
                        Files.delete(file);
                        fileCount.incrementAndGet();
                    } catch (IOException e) {
                        logger.warning("无法删除文件: " + file + " - " + e.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    try {
                        Files.delete(dir);
                    } catch (IOException e) {
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.warning("扫描目录失败: " + dir + " - " + e.getMessage());
        }
    }
    
    private boolean matchesPattern(String fileName, String pattern) {
        if (pattern == null) return true;
        String regex = pattern.replace("*", ".*").replace("?", ".");
        return fileName.toLowerCase().matches(regex);
    }
    
    private String resolvePath(String basePath) {
        if (basePath.startsWith("%") && basePath.endsWith("%")) {
            String envVar = basePath.substring(1, basePath.length() - 1);
            return System.getenv(envVar);
        }
        
        String osDrive = System.getenv("SystemDrive") != null ? System.getenv("SystemDrive") : "C:";
        return osDrive + "\\" + basePath;
    }
    
    public long estimateSize(CleanCategory category) {
        String basePath = resolvePath(category.getBasePath());
        if (basePath == null || !new File(basePath).exists()) return 0;
        
        AtomicLong totalSize = new AtomicLong(0);
        try {
            File dir = new File(basePath);
            if (category.getPattern() != null) {
                estimateSize(dir, category.getPattern(), totalSize);
            } else {
                estimateAllSize(dir, totalSize);
            }
        } catch (Exception e) {
            logger.warning("估算大小失败: " + category.getDisplayName());
        }
        return totalSize.get();
    }
    
    private void estimateSize(File dir, String pattern, AtomicLong totalSize) {
        try {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (matchesPattern(file.getFileName().toString(), pattern)) {
                        totalSize.addAndGet(attrs.size());
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.warning("估算大小失败: " + dir);
        }
    }
    
    private void estimateAllSize(File dir, AtomicLong totalSize) {
        try {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    totalSize.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.warning("估算大小失败: " + dir);
        }
    }
    
    public String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}
