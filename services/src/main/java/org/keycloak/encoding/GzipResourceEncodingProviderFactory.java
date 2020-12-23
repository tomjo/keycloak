package org.keycloak.encoding;

import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.Version;
import org.keycloak.models.KeycloakSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class GzipResourceEncodingProviderFactory implements ResourceEncodingProviderFactory {

    private static final Logger logger = Logger.getLogger(GzipResourceEncodingProviderFactory.class);

    private Set<String> excludedContentTypes = new HashSet<>();

    private File cacheDir;

    @Override
    public ResourceEncodingProvider create(KeycloakSession session) {
        if (cacheDir == null) {
            cacheDir = initCacheDir();
        }

        return new GzipResourceEncodingProvider(session, cacheDir);
    }

    @Override
    public void init(Config.Scope config) {
        String e = config.get("excludedContentTypes", "image/png image/jpeg");
        for (String s : e.split(" ")) {
            excludedContentTypes.add(s);
        }
    }

    @Override
    public boolean encodeContentType(String contentType) {
        return !excludedContentTypes.contains(contentType);
    }

    @Override
    public String getId() {
        return "gzip";
    }

    private synchronized File initCacheDir() {
        if (cacheDir != null) {
            return cacheDir;
        }

        File cacheRoot = new File(System.getProperty("java.io.tmpdir"), "kc-gzip-cache");
        File cacheDir = new File(cacheRoot, Version.RESOURCES_VERSION);

        if (cacheRoot.isDirectory()) {
            for (File f : cacheRoot.listFiles()) {
                if (!f.getName().equals(Version.RESOURCES_VERSION)) {
                    try {
                        FileUtils.deleteDirectory(f);
                    } catch (IOException e) {
                        logger.warn("Failed to delete old gzip cache directory", e);
                    }
                }
            }
        }

        if (!cacheDir.isDirectory() && !cacheDir.mkdirs()) {
            logger.warn("Failed to create gzip cache directory: "+cacheDir.getAbsolutePath());
            try {
                Files.createDirectories(cacheDir.toPath());
                return cacheDir;
            } catch (IOException e) {
                logger.error("Other attempt at creating gzip cache directory failed", e);
            }
            return null;
        }

        return cacheDir;
    }
}
