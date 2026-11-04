import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.jar.JarFile;
import com.android.aapt.Resources.XmlNode;

/** Read-only verification; never reads a keystore or password. */
class VerifyBundleSignature {
    public static void main(String[] args) throws Exception {
        String expected = "C32561AF99BF0593178A31FA6B5FCE55A2419AD121184999C6EFD2DF6918B564";
        int count = 0;
        var names = new HashSet<String>();
        try (var jar = new JarFile(args[0], true)) {
            for (var entry : Collections.list(jar.entries())) {
                String name = entry.getName();
                if (!names.add(name)) throw new IllegalStateException("Duplicate ZIP entry");
                if (entry.isDirectory()) continue;
                try (var in = jar.getInputStream(entry)) {
                    in.transferTo(OutputStream.nullOutputStream());
                }
                String upper = name.toUpperCase(Locale.ROOT);
                if (upper.equals("META-INF/MANIFEST.MF")
                        || upper.matches("META-INF/[^/]+\\.(SF|RSA|DSA|EC)")
                        || upper.startsWith("META-INF/SIG-")) continue;
                var signers = entry.getCodeSigners();
                if (signers == null || signers.length != 1)
                    throw new IllegalStateException("Missing or ambiguous signature: " + name);
                byte[] cert = signers[0].getSignerCertPath().getCertificates().get(0).getEncoded();
                String sha = HexFormat.of().withUpperCase().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(cert));
                if (!expected.equals(sha)) throw new IllegalStateException("Wrong signing identity");
                if (upper.endsWith(".JKS") || upper.endsWith(".KEYSTORE")
                        || upper.endsWith("/SIGNING.PROPERTIES") || upper.endsWith(".P12")
                        || upper.endsWith(".PFX"))
                    throw new IllegalStateException("Signing material included in bundle");
                count++;
            }
            boolean template = false;
            try (InputStream in = jar.getInputStream(jar.getJarEntry("base/res/xml/automotive_app_desc.xml"))) {
                var root = XmlNode.parseFrom(in).getElement();
                if (!root.getName().equals("automotiveApp"))
                    throw new IllegalStateException("Invalid automotive descriptor");
                for (var child : root.getChildList())
                    if (child.hasElement() && child.getElement().getName().equals("uses"))
                        for (var attr : child.getElement().getAttributeList())
                            if (attr.getName().equals("name") && attr.getValue().equals("template"))
                                template = true;
            }
            if (!template) throw new IllegalStateException("Missing Android Auto template capability");
        }
        System.out.println("PASS: " + count + " signed bundle payloads; original certificate; Android Auto template.");
    }
}
