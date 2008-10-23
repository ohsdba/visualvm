/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.sun.tools.visualvm.application.type.custom.images;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 *
 * @author Jaroslav Bachorik
 */
class ImageResolver {

    final private static Pattern favicoLinkPattern = Pattern.compile("\\<link(.+?)/?\\>", Pattern.MULTILINE | Pattern.DOTALL);
    final private static Pattern favicoHrefPattern = Pattern.compile("href=[\\\"'](.+?)[\\\"']", Pattern.MULTILINE | Pattern.DOTALL);
    final private static String[] extensions = new String[]{"png", "gif", "jpg", "jpeg"};

    BufferedImage resolveImage(URL url) {
        BufferedImage resolvedImage = null;

        for (String extension : extensions) {
            String favIcon = "favicon." + extension;
            try {
                URL favicoUrl = new URL(url.toString() + "/" + favIcon);
                resolvedImage = ImageIO.read(favicoUrl);
                if (resolvedImage != null && resolvedImage.getWidth() > -1) {
                    break;
                }
            } catch (IOException ex) {
                // ignore
                }
        }

        if (resolvedImage == null) {
            resolvedImage = resolveFromLink(url);
        }

        return resolvedImage != null ? (resolvedImage.getWidth() > -1 ? resolvedImage : null) : null;
    }

    private BufferedImage resolveFromLink(URL url) {
        try {
            String index = readIndex(url.openStream());
            Matcher linkMatcher = favicoLinkPattern.matcher(index);
            String favicoPath = null;
            while (linkMatcher.find()) {
                String content = linkMatcher.group(1);
                if (content.contains("shortcut") || content.contains("link")) {
                    Matcher hrefMatcher = favicoHrefPattern.matcher(content);
                    if (hrefMatcher.find()) {
                        favicoPath = hrefMatcher.group(1);
                        if (isSupported(favicoPath)) {
                            break;
                        } else {
                            favicoPath = null;
                        }
                    }
                }
            }
            if (favicoPath != null) {
                String basePath = url.toString();
                if (basePath.endsWith("/") || favicoPath.startsWith("/")) {
                    favicoPath = basePath + favicoPath;
                } else {
                    favicoPath = basePath + "/" + favicoPath;
                }

                URL favicoUrl = new URL(favicoPath);
                System.err.println("Resolving image: " + favicoUrl.toString());

                return ImageIO.read(favicoUrl);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String readIndex(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            do {
                line = br.readLine();
                if (line != null) {
                    sb.append(line).append('\n');
                }
            } while (line != null);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
        return sb.toString();
    }

    private boolean isSupported(String imagePath) {
        for (String ext : extensions) {
            if (imagePath.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}