package org.mariotaku.pass.util;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Patterns;

import org.mariotaku.pass.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mariotaku on 15/10/30.
 */
public class Utils {

    public static final Pattern PATTERN_XML_RESOURCE_IDENTIFIER = Pattern.compile("res/xml/([\\w_]+)\\.xml");
    public static final Pattern PATTERN_RESOURCE_IDENTIFIER = Pattern.compile("@([\\w_]+)/([\\w_]+)");

    public static List<String> extractLinks(CharSequence text) {
        if (TextUtils.isEmpty(text)) return Collections.emptyList();
        List<String> links = new ArrayList<>();
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            String url = m.group();
            links.add(url);
        }

        return links;
    }

    @NonNull
    public static List<String> getHosts(final Context context, final Uri uri, int level) {
        final String host = uri.getHost();
        if (host == null) return Collections.emptyList();
        if (isValidIPAddress(host)) return Collections.singletonList(host);
        if (isMultiLevelTLD(context, host)) {
            level = level + 1;
        }
        final String[] hostSegs = host.split(Pattern.quote("."));
        List<String> hosts = new ArrayList<>();
        for (int i = 0, j = hostSegs.length - level + 1; i < j; i++) {
            StringBuilder sb = new StringBuilder();
            for (int k = i, l = hostSegs.length; k < l; k++) {
                if (k > i) {
                    sb.append('.');
                }
                sb.append(hostSegs[k]);
            }
            hosts.add(0, sb.toString());
        }
        return hosts;
    }

    private static boolean isMultiLevelTLD(final Context context, @NonNull final String host) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(context.getResources()
                .openRawResource(R.raw.two_level_tlds), Charset.defaultCharset()))) {
            for (String s; (s = br.readLine()) != null; ) {
                if (host.endsWith("." + s)) return true;
            }
        } catch (IOException e) {
        }
        return false;
    }

    public static boolean isValidIPAddress(final String host) {
        if (TextUtils.isEmpty(host)) {
            return false;
        }
        return Patterns.IP_ADDRESS.matcher(host).matches();
    }

    public static int getResId(final Context context, final String string) {
        if (context == null || string == null) return 0;
        Matcher m = PATTERN_RESOURCE_IDENTIFIER.matcher(string);
        final Resources res = context.getResources();
        if (m.matches()) return res.getIdentifier(m.group(2), m.group(1), context.getPackageName());
        m = PATTERN_XML_RESOURCE_IDENTIFIER.matcher(string);
        if (m.matches()) return res.getIdentifier(m.group(1), "xml", context.getPackageName());
        return 0;
    }

    public static boolean isWebBrowser(final Context context, final String packageName) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setPackage(packageName);
        intent.setData(Uri.parse("http://"));
        return context.getPackageManager().resolveActivity(intent, 0) != null;
    }
}
