package dev.damaso.market.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jetbrains.bio.npy.NpyFile;

public class NpyBytes {
    static public byte [] fromArray(float [] fs, int [] shape) throws IOException {
        // Use random file to allow simultaneous requests
        int random = (int)(Math.random() * 10000000);
        Path path = new File(String.format("tmp-%d.npy", random)).toPath();
        NpyFile.write(path, fs, shape);
        byte [] bs = Files.readAllBytes(path);
        path.toFile().delete();
        return bs;
    }
}
