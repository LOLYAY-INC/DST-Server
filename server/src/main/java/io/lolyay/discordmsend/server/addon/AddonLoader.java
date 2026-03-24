package io.lolyay.discordmsend.server.addon;

import com.google.gson.Gson;
import io.lolyay.discordmsend.server.DstServer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.extern.slf4j.Slf4j;
import org.spongepowered.configurate.objectmapping.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
public class AddonLoader {
    private final static File addonsDir = new File("addons");
    private final static String ADDON_INFO_FILE = "dst.addon.json";
    private URLClassLoader addonClassLoader;

    private static record AddonInfo(
            String id,
            List<String> depend,
            String version,
            String entryPoint
    ) {
    }

    public List<DstImplAddon> load(DstServer server) {
        log.info("Loading addons...");
        if (!addonsDir.exists()) {
            addonsDir.mkdir();
            log.info("Created addons directory");
            return List.of();
        }

        Map<AddonInfo, Class<? extends DstImplAddon>> addonClasses = locateAddons(addonsDir);
        List<DstImplAddon> addons = new ObjectArrayList<>();

        for (Map.Entry<AddonInfo, Class<? extends DstImplAddon>> entry : addonClasses.entrySet()) {
            AddonInfo info = entry.getKey();
            Class<? extends DstImplAddon> addonClass = entry.getValue();

            try {
                DstImplAddon addonInstance = addonClass.getDeclaredConstructor().newInstance();
                addons.add(addonInstance);
                log.debug("Created Instance for {} (v{})", info.id, info.version);
            } catch (Exception e) {
                log.error("Error Creating Addon {} (v{})", info.id, info.version, e);
            }
        }
        return addons;
    }

    private Map<AddonInfo, Class<? extends DstImplAddon>> locateAddons(File folder) {
        Map<AddonInfo, Class<? extends DstImplAddon>> loadedAddons = new HashMap<>();

        File[] jarFiles = folder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            log.info("No addon jars found in {}", folder.getAbsolutePath());
            return loadedAddons;
        }

        URL[] urls = Arrays.stream(jarFiles).map(file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                log.error("Failed to parse URL for file: {}", file.getName(), e);
                throw new RuntimeException(e);
            }
        }).toArray(URL[]::new);
        this.addonClassLoader = new URLClassLoader(urls, this.getClass().getClassLoader());

            for (File jarFile : jarFiles) {
                try (JarFile jar = new JarFile(jarFile)) {
                    JarEntry entry = jar.getJarEntry(ADDON_INFO_FILE);

                    if (entry == null) {
                        continue;
                    }

                    log.debug("Found {} in {}", ADDON_INFO_FILE, jarFile.getName());

                    try (InputStream is = jar.getInputStream(entry)) {
                        AddonInfo addonInfo = new Gson().fromJson(new InputStreamReader(is), AddonInfo.class);

                        if (addonInfo.entryPoint() == null || addonInfo.entryPoint().isBlank()) {
                            log.warn("Addon {} has no entryPoint defined. Skipping.", addonInfo.id());
                            continue;
                        }

                        Class<?> clazz = addonClassLoader.loadClass(addonInfo.entryPoint());

                        if (DstImplAddon.class.isAssignableFrom(clazz)) {
                            @SuppressWarnings("unchecked")
                            Class<? extends DstImplAddon> validAddonClass = (Class<? extends DstImplAddon>) clazz;
                            loadedAddons.put(addonInfo, validAddonClass);
                            log.info("Successfully loaded addon class: {} (v{})", addonInfo.id(), addonInfo.version());
                        } else {
                            log.error("Addon {} entry point {} does not implement DstImplAddon!", addonInfo.id(), addonInfo.entryPoint());
                        }
                    }

                } catch (IOException | ClassNotFoundException e) {
                    log.error("Failed to process addon jar: {}", jarFile.getName(), e);
                }
            }
        return loadedAddons;
    }
}