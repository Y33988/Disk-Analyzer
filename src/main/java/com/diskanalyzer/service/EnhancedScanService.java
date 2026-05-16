package com.diskanalyzer.service;

import com.diskanalyzer.model.EnhancedFileNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class EnhancedScanService {

    private static final int THREAD_COUNT = Math.max(4, Runtime.getRuntime().availableProcessors());

    private volatile boolean isScanning = false;
    private volatile boolean isCancelled = false;
    private final AtomicLong scannedFilesCount = new AtomicLong(0);
    private final AtomicLong totalSize = new AtomicLong(0);
    private final AtomicInteger activeWorkers = new AtomicInteger(0);

    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutor;

    private ScanProgressListener progressListener;

    private long scanStartTime;

    public EnhancedScanService() {
        executorService = Executors.newFixedThreadPool(THREAD_COUNT, r -> {
            Thread t = new Thread(r, "DiskScanner");
            t.setDaemon(true);
            return t;
        });
        scheduledExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "ScanProgress");
            t.setDaemon(true);
            return t;
        });
    }

    public void startScan(File directory) {
        if (isScanning) return;

        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            if (progressListener != null) progressListener.onScanFailed("无效的扫描目录");
            return;
        }

        if (executorService.isShutdown()) {
            executorService = Executors.newFixedThreadPool(THREAD_COUNT, r -> {
                Thread t = new Thread(r, "DiskScanner");
                t.setDaemon(true);
                return t;
            });
        }
        if (scheduledExecutor.isShutdown()) {
            scheduledExecutor = Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "ScanProgress");
                t.setDaemon(true);
                return t;
            });
        }

        isCancelled = false;
        isScanning = true;
        scannedFilesCount.set(0);
        totalSize.set(0);
        activeWorkers.set(1);
        scanStartTime = System.currentTimeMillis();

        EnhancedFileNode rootNode = new EnhancedFileNode(directory);

        startProgressReporter();

        executorService.submit(() -> {
            try {
                File[] entries = directory.listFiles();
                if (entries == null) {
                    complete(rootNode);
                    return;
                }

                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (File entry : entries) {
                    if (isCancelled) break;
                    if (!entry.isDirectory() && !entry.isFile()) continue;

                    if (entry.isDirectory()) {
                        activeWorkers.incrementAndGet();
                        CompletableFuture<Void> f = CompletableFuture.runAsync(() -> {
                            try {
                                EnhancedFileNode subRoot = scanTreeNio(entry.toPath());
                                synchronized (rootNode) {
                                    rootNode.addChildDirect(subRoot);
                                }
                            } catch (Exception ignored) {
                            } finally {
                                activeWorkers.decrementAndGet();
                            }
                        }, executorService);
                        futures.add(f);
                    } else {
                        EnhancedFileNode fileNode = new EnhancedFileNode(entry);
                        rootNode.addChildDirect(fileNode);
                        scannedFilesCount.incrementAndGet();
                        totalSize.addAndGet(entry.length());
                    }
                }

                for (CompletableFuture<Void> f : futures) {
                    try { f.get(); } catch (Exception ignored) {}
                }

                activeWorkers.decrementAndGet();
                rootNode.rebuildStatistics();
                rootNode.sortChildrenBySize();
                complete(rootNode);

            } catch (Exception e) {
                if (progressListener != null) progressListener.onScanFailed(e.getMessage());
                isScanning = false;
            }
        });
    }

    private EnhancedFileNode scanTreeNio(Path rootPath) throws IOException {
        EnhancedFileNode dirNode = new EnhancedFileNode(rootPath.toFile());
        List<EnhancedFileNode> children = new ArrayList<>();

        Files.walkFileTree(rootPath, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE,
            new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (isCancelled) return FileVisitResult.TERMINATE;
                    if (dir.equals(rootPath)) return FileVisitResult.CONTINUE;
                    EnhancedFileNode subDir = new EnhancedFileNode(dir.toFile());
                    children.add(subDir);
                    scannedFilesCount.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isCancelled) return FileVisitResult.TERMINATE;
                    EnhancedFileNode fileNode = new EnhancedFileNode(file.toFile());
                    children.add(fileNode);
                    scannedFilesCount.incrementAndGet();
                    totalSize.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            });

        if (isCancelled) return dirNode;

        dirNode.addChildrenDirect(children);
        dirNode.rebuildStatistics();
        return dirNode;
    }

    private void complete(EnhancedFileNode rootNode) {
        isScanning = false;
        long elapsed = System.currentTimeMillis() - scanStartTime;
        double rate = scannedFilesCount.get() / Math.max(elapsed / 1000.0, 0.001);

        if (progressListener != null) {
            progressListener.onProgressUpdate(String.format(
                "扫描完成！%d 文件 | %s | %.0f 文件/秒",
                scannedFilesCount.get(), formatSize(totalSize.get()), rate
            ));
            progressListener.onScanComplete(rootNode);
        }
    }

    private void startProgressReporter() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            if (!isScanning || isCancelled) return;
            long count = scannedFilesCount.get();
            long elapsed = System.currentTimeMillis() - scanStartTime;
            double rate = count / Math.max(elapsed / 1000.0, 0.001);
            int workers = activeWorkers.get();

            if (progressListener != null) {
                progressListener.onProgressUpdate(String.format(
                    "正在扫描: %d 文件 | %.0f 文件/秒 | %d 线程 | %s",
                    count, rate, workers, formatTime(elapsed)
                ));
            }
        }, 0, 300, TimeUnit.MILLISECONDS);
    }

    public void cancelScan() {
        isCancelled = true;
    }

    public long getScannedFilesCount() { return scannedFilesCount.get(); }
    public long getTotalSize() { return totalSize.get(); }

    public void setProgressListener(ScanProgressListener listener) {
        this.progressListener = listener;
    }

    public void shutdown() {
        isCancelled = true;
        isScanning = false;
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
            try { scheduledExecutor.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        }
        if (executorService != null) {
            executorService.shutdown();
            try { executorService.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        }
    }

    private String formatTime(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60000) return String.format("%.1fs", ms / 1000.0);
        long m = ms / 60000;
        long s = (ms % 60000) / 1000;
        return String.format("%dm%ds", m, s);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1073741824) return String.format("%.1f MB", bytes / 1073741824.0);
        return String.format("%.2f GB", bytes / 1073741824.0);
    }

    public interface ScanProgressListener {
        void onProgressUpdate(String message);
        void onScanComplete(EnhancedFileNode rootNode);
        void onScanFailed(String error);
    }
}