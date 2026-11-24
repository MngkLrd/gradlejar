package com.kirill.screenshotmanager.core;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/** Recovery sidecars use the same fingerprinted format; never overwrite an explicit saved project. */
public final class RecoveryStore {
    public static void save(Path source,List<ImageDocument.Operation> ops)throws IOException{ProjectStore.saveRecovery(source,ops);}
    public static List<ImageDocument.Operation> load(Path source)throws IOException{return ProjectStore.loadRecovery(source);}
    public static boolean exists(Path source){return Files.isRegularFile(source.resolveSibling(source.getFileName()+".smrecovery"));}
}
